# Kafka bank transfers

A two-service Spring Boot example that moves money between MySQL accounts through an Avro Kafka event. It demonstrates a Kafka transaction at publication time and safe, idempotent debit/credit processing at consumption time.

## Architecture and design

```text
                 POST /api/transfers
Client  ─────────────────────────────> producer-service
                                            │
                  Kafka transaction         │ Avro TransferRequested
            (idempotent producer, acks=all) │ keyed by debit account id
                                            ▼
                               bank.transfer.requested.v1
                                            │
                         read_committed     ▼
                               Schema Registry
                                      schema id │
                                      consumer-service
                                            │
                           one MySQL transaction
                                            ▼
                  accounts + account_movements + processed_transfers
```

### Components

- `shared-model/src/main/avro/TransferRequestedAvro.avsc` is the source-of-truth event contract. Maven generates the `TransferRequestedAvro` `SpecificRecord` class at build time.
- `producer-service` exposes `POST /api/transfers`, validates the request, and publishes the generated Avro record with `KafkaTemplate.executeInTransaction` and Confluent's `KafkaAvroSerializer`.
- Kafka runs as a single-node KRaft broker. The `kafka-init` Compose job creates `bank.transfer.requested.v1` with three partitions.
- Schema Registry stores the versioned Avro contract and is configured for `BACKWARD` compatibility. The producer registers the schema automatically on its first publish.
- `consumer-service` consumes with `KafkaAvroDeserializer`, `specific.avro.reader=true`, and `isolation.level=read_committed`; it therefore receives the generated Avro class and never sees aborted Kafka transactions.
- MySQL holds the account balances, an immutable debit/credit audit trail (`account_movements`), and the inbox/idempotency table (`processed_transfers`). Flyway creates and seeds this schema.

### Money-movement guarantees

1. The producer uses Kafka idempotence, `acks=all`, a transaction id prefix, and Avro serialization. Schema Registry assigns the writer schema an id; the serialized Kafka value contains that id and the compact Avro payload. A transfer event becomes visible only if the Kafka transaction commits.
2. The consumer locks both account rows in ascending account-id order, preventing opposing transfers from deadlocking. It verifies funds, debits one account, credits the other, writes two movement rows, and writes the inbox marker in one MySQL transaction.
3. `processed_transfers.transfer_id` is unique. If Kafka redelivers an event after MySQL committed but before its offset was committed, the consumer sees the inbox marker and performs no second debit or credit.

Kafka and MySQL are separate resource managers, so this is deliberately not an XA/distributed two-phase commit. The design provides at-least-once consumption and effectively-once money movement in MySQL through the inbox pattern.

For multiple producer replicas, each replica must have its own `KAFKA_TRANSACTION_ID_PREFIX` to avoid producer fencing. Consumer replicas should share the `bank-ledger-v1` group so Kafka distributes topic partitions between them.

### Evolving the Avro contract

Change `shared-model/src/main/avro/TransferRequestedAvro.avsc`, not the generated class under `target/`. With the configured backward-compatibility policy, add new fields with a default value; do not remove, rename, or change the type of an existing field. Run `mvn verify` before publishing a changed schema.

## Prerequisites

- Docker Desktop or Docker Engine with Compose v2
- Java 21 and Maven 3.9+ only when running the local Maven tests

Ports are configurable when a local service already uses one:

| Service | Default host port | Override variable |
| --- | ---: | --- |
| Producer API | 8080 | `PRODUCER_HOST_PORT` |
| Consumer API | 8081 | `CONSUMER_HOST_PORT` |
| Kafka | 29092 | `KAFKA_HOST_PORT` |
| Schema Registry | 8085 | `SCHEMA_REGISTRY_HOST_PORT` |
| MySQL | 33306 | `MYSQL_HOST_PORT` |

## Start the application

From this directory, first reset the demo database if you need the original balances:

```bash
docker compose down -v
```

Start all services in the background:

```bash
docker compose up --build -d
docker compose ps
```

Expected state:

- `kafka`, `schema-registry`, and `mysql` are `healthy`.
- `kafka-init` exits with status `0`; it is a one-time topic-creation job.
- `producer-service` and `consumer-service` are `Up`.

If a service does not start, inspect its logs:

```bash
docker compose logs --tail=100 kafka schema-registry mysql producer-service consumer-service
```

## End-to-end verification

The seeded balances are account 1 = `1000.00`, account 2 = `250.00`, and account 3 = `500.00`.

### 1. Confirm the initial account state

```bash
curl --fail --silent http://localhost:8081/api/accounts
```

Expected response (field ordering may differ):

```json
[
  {"id":1,"accountNumber":"ACC-1001","balance":1000.00},
  {"id":2,"accountNumber":"ACC-1002","balance":250.00},
  {"id":3,"accountNumber":"ACC-1003","balance":500.00}
]
```

### 2. Publish a transfer

```bash
curl --fail --silent -X POST http://localhost:8080/api/transfers \
  -H 'Content-Type: application/json' \
  -d '{"debitAccountId":1,"creditAccountId":2,"amount":25.00}'
```

Expected response:

```json
{"transferId":"<uuid>","status":"PUBLISHED"}
```

Save the returned `transferId`; it is used in the database and replay checks below.

### 3. Confirm the debit and credit

The consumer is asynchronous. Run this command again after a moment if the first response still shows the original balances:

```bash
curl --fail --silent http://localhost:8081/api/accounts
```

Expected balances after the `25.00` transfer:

```json
[
  {"id":1,"accountNumber":"ACC-1001","balance":975.00},
  {"id":2,"accountNumber":"ACC-1002","balance":275.00},
  {"id":3,"accountNumber":"ACC-1003","balance":500.00}
]
```

### 4. Verify MySQL audit and idempotency records

Replace `<transfer-id>` with the UUID returned in step 2.

```bash
docker compose exec -T mysql mysql -ubank -pbank bankdb -e "
  SELECT transfer_id, account_id, direction, amount
  FROM account_movements
  WHERE transfer_id = '<transfer-id>'
  ORDER BY id;

  SELECT transfer_id, processed_at
  FROM processed_transfers
  WHERE transfer_id = '<transfer-id>';
"
```

Expected result: exactly two movement rows (`DEBIT` for account 1 and `CREDIT` for account 2) and one row in `processed_transfers`.

### 5. Verify Avro schema registration

After the first transfer, the default topic naming strategy creates the subject `bank.transfer.requested.v1-value`.

```bash
curl --fail --silent http://localhost:8085/subjects
curl --fail --silent \
  http://localhost:8085/subjects/bank.transfer.requested.v1-value/versions/latest
```

The second response contains the registered Avro schema, including the decimal logical type used for `amount`, and its Schema Registry id. The producer writes that id into every Kafka message so the consumer can select the correct reader schema.

### 6. Verify duplicate-event safety

Duplicate protection is covered by the automated `TransferProcessorTest`: an existing `processed_transfers` record produces no balance change and writes no additional audit rows. Any external producer used to test a replay must use the same Avro schema and Schema Registry; raw JSON written with `kafka-console-producer.sh` is intentionally not accepted by this consumer.

### 7. Verify request validation

The producer rejects a transfer to the same account before publishing anything:

```bash
curl -i -X POST http://localhost:8080/api/transfers \
  -H 'Content-Type: application/json' \
  -d '{"debitAccountId":1,"creditAccountId":1,"amount":25.00}'
```

Expected status: `400 Bad Request`.

## Automated tests

Run the Maven test suite from this directory:

```bash
mvn verify
```

The unit tests cover a successful debit/credit, insufficient funds (no balance or audit mutation), and duplicate-event no-op processing. The build also generates and compiles the Avro `SpecificRecord` from the schema file.

## Inspect Kafka and stop the stack

View the topic and consumer-related logs:

```bash
docker compose exec -T kafka /opt/kafka/bin/kafka-topics.sh \
  --bootstrap-server kafka:9092 \
  --describe --topic bank.transfer.requested.v1

docker compose logs --tail=100 consumer-service

curl --fail --silent \
  http://localhost:8085/subjects/bank.transfer.requested.v1-value/versions/latest
```

Stop the containers while keeping MySQL data:

```bash
docker compose down
```

Remove the containers and demo database volume to begin again with seed balances:

```bash
docker compose down -v
```

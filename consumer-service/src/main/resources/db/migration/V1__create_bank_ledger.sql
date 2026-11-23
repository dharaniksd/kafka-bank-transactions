CREATE TABLE accounts (
    id BIGINT NOT NULL AUTO_INCREMENT,
    account_number VARCHAR(64) NOT NULL,
    balance DECIMAL(19, 2) NOT NULL,
    version BIGINT,
    PRIMARY KEY (id),
    CONSTRAINT uk_accounts_account_number UNIQUE (account_number)
) ENGINE=InnoDB;

CREATE TABLE processed_transfers (
    id BIGINT NOT NULL AUTO_INCREMENT,
    transfer_id VARCHAR(36) NOT NULL,
    processed_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_processed_transfers_transfer_id UNIQUE (transfer_id)
) ENGINE=InnoDB;

CREATE TABLE account_movements (
    id BIGINT NOT NULL AUTO_INCREMENT,
    transfer_id VARCHAR(36) NOT NULL,
    account_id BIGINT NOT NULL,
    direction VARCHAR(6) NOT NULL,
    amount DECIMAL(19, 2) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (id),
    INDEX ix_account_movements_account_id (account_id),
    INDEX ix_account_movements_transfer_id (transfer_id),
    CONSTRAINT fk_account_movements_account FOREIGN KEY (account_id) REFERENCES accounts(id)
) ENGINE=InnoDB;

INSERT INTO accounts (account_number, balance, version) VALUES
    ('ACC-1001', 1000.00, 0),
    ('ACC-1002', 250.00, 0),
    ('ACC-1003', 500.00, 0);

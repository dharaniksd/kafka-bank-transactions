FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY pom.xml ./
COPY shared-model/pom.xml shared-model/pom.xml
COPY producer-service/pom.xml producer-service/pom.xml
COPY consumer-service/pom.xml consumer-service/pom.xml
RUN mvn -q -pl producer-service -am dependency:go-offline
COPY shared-model/src shared-model/src
COPY producer-service/src producer-service/src
RUN mvn -q -pl producer-service -am package -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /workspace/producer-service/target/producer-service-*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]

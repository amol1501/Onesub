# OneSub Backend

Java 21 and Spring Boot 3.5 modular-monolith REST API.

```bash
export JAVA_HOME=/opt/homebrew/opt/openjdk@21
./mvnw spring-boot:run
./mvnw test
./mvnw package
```

The local profile validates PostgreSQL schema through Flyway migrations. Set `SPRING_PROFILES_ACTIVE=prod` for production.

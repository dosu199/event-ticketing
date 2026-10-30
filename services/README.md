# Services

Each service is an independent Spring Boot app with its **own database**. Services never read another service's tables.

## Package convention (per service)

```
com.ticketing.<service>
├── api          # REST controllers, request/response DTOs, exception handlers
├── domain       # Entities, value objects, enums, domain rules
├── service      # Use cases / application logic, transactions
├── repository   # Spring Data repositories
├── messaging    # Kafka producers/consumers, outbox
└── config       # Spring configuration classes
```

`src/main/resources/db/migration` holds Flyway scripts (`V1__init.sql`, ...).

Suggested dependencies from start.spring.io: Web, Data JPA, PostgreSQL Driver, Flyway, Validation, Actuator, OAuth2 Resource Server, Spring for Apache Kafka, Lombok, Testcontainers.

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

## Files every service has (since Phase 0)

| File | Purpose |
|---|---|
| `pom.xml` | Build file. Inherits versions from the root `pom.xml`. |
| `src/main/java/.../XxxApplication.java` | `main()` entry point. Lives in the root package so component scanning finds everything below it. |
| `src/main/resources/application.yml` | Name, port, and Actuator settings |
| `src/test/java/.../XxxApplicationTests.java` | Smoke test: the Spring context must start |

## Dependencies
Phase 0 adds only `webmvc` (gateway: `gateway-server-webflux`), `actuator`, and `test`. **Add each new dependency in the phase where you first use it**, and note why in the service README. Planned (Spring Boot 4 artifact names):

| Dependency | Phase | Used by |
|---|---|---|
| `spring-boot-starter-data-jpa`, `postgresql`, `spring-boot-starter-flyway` | 1 | event, booking, payment, ticket |
| `spring-boot-starter-validation` | 1 | all with REST input |
| `spring-boot-starter-security-oauth2-resource-server` | 2 | all + gateway |
| `spring-boot-starter-data-redis` | 3 | booking, gateway |
| `spring-boot-starter-kafka` | 4 | booking, payment, ticket, notification |
| `spring-boot-starter-mail` | 5 | notification |
| `resilience4j`, `micrometer-registry-prometheus`, tracing | 6 | all |
| `testcontainers` | 7 (or earlier) | all |

## Running a service
```bash
./mvnw -pl services/<name> spring-boot:run      # from the repo root
curl localhost:<port>/actuator/health
```

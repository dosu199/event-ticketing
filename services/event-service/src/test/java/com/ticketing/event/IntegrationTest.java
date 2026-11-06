package com.ticketing.event;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * Base class for tests that need the full application and a real database.
 *
 * <p>One PostgreSQL container is started for the whole test run ("singleton container"
 * pattern) and shared by every test class, which is much faster than one per class.
 * Testcontainers removes it automatically when the JVM exits.
 *
 * <p>Flyway runs all migrations (V1..V4) against it at startup, so tests use exactly the
 * same schema as production, and Hibernate's ddl-auto=validate checks the entities.
 */
@SpringBootTest
public abstract class IntegrationTest {

    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:17-alpine");

    static {
        POSTGRES.start();
    }

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }
}

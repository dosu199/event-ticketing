package com.ticketing.notification;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Smoke test: starts the full Spring application context.
 *
 * <p>It has no assertions on purpose. If any bean fails to be created
 * (bad configuration, missing property, broken wiring), the context fails
 * to start and this test fails. It's the cheapest way to catch "the app
 * doesn't even boot" before anything else.
 */
@SpringBootTest
class NotificationServiceApplicationTests {

    @Test
    void contextLoads() {
    }
}

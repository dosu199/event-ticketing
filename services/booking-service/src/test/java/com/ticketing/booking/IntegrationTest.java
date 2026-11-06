package com.ticketing.booking;

import com.ticketing.booking.catalog.CatalogEvent;
import com.ticketing.booking.catalog.CatalogSeat;
import com.ticketing.booking.catalog.EventCatalogClient;
import com.ticketing.booking.catalog.EventNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.stream.LongStream;

/**
 * Base class: full application + a real Redis 7.4 in Docker (shared by all test classes).
 *
 * <p>event-service is replaced by an in-memory fake ({@link FakeCatalog}), so these tests
 * don't need it running and always see the same events:
 * <ul>
 *   <li>event 1: PUBLISHED, seats 1..10</li>
 *   <li>event 2: DRAFT (not on sale), seats 11..12</li>
 *   <li>anything else: not found</li>
 * </ul>
 */
@SpringBootTest
@Import(IntegrationTest.FakeCatalogConfig.class)
public abstract class IntegrationTest {

    // Same Redis version as docker-compose: per-field hash expiry (HPEXPIRE) needs 7.4+.
    static final GenericContainer<?> REDIS = new GenericContainer<>(DockerImageName.parse("redis:7.4-alpine"))
            .withExposedPorts(6379);

    static {
        REDIS.start();
    }

    @DynamicPropertySource
    static void redisProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", REDIS::getHost);
        registry.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379));
    }

    @Autowired
    protected StringRedisTemplate redis;

    @BeforeEach
    void clearRedis() {
        Set<String> keys = redis.keys("hold:*");
        if (keys != null && !keys.isEmpty()) {
            redis.delete(keys);
        }
    }

    @TestConfiguration
    static class FakeCatalogConfig {

        @Bean
        @Primary
        EventCatalogClient fakeCatalog() {
            return new FakeCatalog();
        }
    }

    static class FakeCatalog implements EventCatalogClient {

        @Override
        public CatalogEvent getEvent(long eventId) {
            if (eventId == 1) return new CatalogEvent(1L, "Published event", "PUBLISHED");
            if (eventId == 2) return new CatalogEvent(2L, "Draft event", "DRAFT");
            throw new EventNotFoundException(eventId);
        }

        @Override
        public List<CatalogSeat> getSeats(long eventId) {
            if (eventId == 1) return seats(1, 10);
            if (eventId == 2) return seats(11, 12);
            throw new EventNotFoundException(eventId);
        }

        private static List<CatalogSeat> seats(long from, long to) {
            return LongStream.rangeClosed(from, to)
                    .mapToObj(id -> new CatalogSeat(id, "A", (int) id, "STANDARD", new BigDecimal("10.00")))
                    .toList();
        }
    }
}

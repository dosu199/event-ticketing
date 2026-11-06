package com.ticketing.booking.hold;

import com.ticketing.booking.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** Holds expire on their own: no cleanup job needed. Uses a 1-second TTL for speed. */
@TestPropertySource(properties = "booking.hold.ttl=PT1S")
class HoldExpiryIntegrationTest extends IntegrationTest {

    @Autowired
    private HoldService holds;

    @Test
    void expiredHoldFreesSeatsForEveryone() throws InterruptedException {
        holds.hold("alice", 1, List.of(3L));
        assertThat(holds.availability(1, "bob").unavailableSeatIds()).containsExactly(3L);

        Thread.sleep(1500);

        assertThat(holds.availability(1, "bob").unavailableSeatIds()).isEmpty();
        assertThat(holds.availability(1, "alice").myHold()).isNull();
        holds.hold("bob", 1, List.of(3L)); // the seat can be taken again
    }
}

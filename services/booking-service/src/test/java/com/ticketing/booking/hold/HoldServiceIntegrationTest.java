package com.ticketing.booking.hold;

import com.ticketing.booking.IntegrationTest;
import com.ticketing.booking.catalog.EventNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class HoldServiceIntegrationTest extends IntegrationTest {

    @Autowired
    private HoldService holds;

    @Test
    void heldSeatsAreUnavailableToOthersButMineForTheHolder() {
        Hold hold = holds.hold("alice", 1, List.of(1L, 2L));

        assertThat(hold.seatIds()).containsExactly(1L, 2L);
        assertThat(holds.availability(1, "bob").unavailableSeatIds()).containsExactlyInAnyOrder(1L, 2L);

        Availability forAlice = holds.availability(1, "alice");
        assertThat(forAlice.unavailableSeatIds()).isEmpty();
        assertThat(forAlice.myHold().id()).isEqualTo(hold.id());
    }

    @Test
    void holdIsAllOrNothing() {
        holds.hold("alice", 1, List.of(2L));

        assertThatThrownBy(() -> holds.hold("bob", 1, List.of(2L, 3L)))
                .isInstanceOf(SeatsUnavailableException.class)
                .satisfies(e -> assertThat(((SeatsUnavailableException) e).getSeatIds()).containsExactly(2L));

        // Seat 3 was NOT held for bob, because seat 2 failed.
        assertThat(holds.availability(1, "carol").unavailableSeatIds()).containsExactly(2L);
    }

    /**
     * The core guarantee: many users click the same seat at the same moment, exactly one wins.
     * All threads wait at a latch and start together to maximise the overlap.
     */
    @Test
    void onlyOneOfManyConcurrentUsersGetsTheSeat() throws Exception {
        int users = 50;
        ExecutorService pool = Executors.newFixedThreadPool(users);
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger winners = new AtomicInteger();
        AtomicInteger losers = new AtomicInteger();
        ConcurrentLinkedQueue<Throwable> unexpected = new ConcurrentLinkedQueue<>();

        for (int i = 0; i < users; i++) {
            String user = "user-" + i;
            pool.submit(() -> {
                try {
                    start.await();
                    holds.hold(user, 1, List.of(5L));
                    winners.incrementAndGet();
                } catch (SeatsUnavailableException e) {
                    losers.incrementAndGet();
                } catch (Throwable t) {
                    unexpected.add(t);
                }
            });
        }
        start.countDown();
        pool.shutdown();
        assertThat(pool.awaitTermination(30, TimeUnit.SECONDS)).isTrue();

        assertThat(unexpected).isEmpty();
        assertThat(winners.get()).isEqualTo(1);
        assertThat(losers.get()).isEqualTo(users - 1);
    }

    @Test
    void userCanHaveOnlyOneHoldPerEvent() {
        holds.hold("alice", 1, List.of(1L));

        assertThatThrownBy(() -> holds.hold("alice", 1, List.of(2L)))
                .isInstanceOf(AlreadyHoldingException.class);
    }

    @Test
    void releaseFreesTheSeatsImmediately() {
        Hold hold = holds.hold("alice", 1, List.of(7L, 8L));

        holds.release("alice", hold.id());

        assertThat(holds.availability(1, "bob").unavailableSeatIds()).isEmpty();
        assertThat(holds.availability(1, "alice").myHold()).isNull();
        holds.hold("bob", 1, List.of(7L)); // now free for others
    }

    @Test
    void otherUsersCannotSeeOrReleaseMyHold() {
        Hold hold = holds.hold("alice", 1, List.of(9L));

        assertThatThrownBy(() -> holds.getHold("mallory", hold.id())).isInstanceOf(HoldNotFoundException.class);
        assertThatThrownBy(() -> holds.release("mallory", hold.id())).isInstanceOf(HoldNotFoundException.class);
        assertThat(holds.availability(1, "bob").unavailableSeatIds()).containsExactly(9L);
    }

    @Test
    void rejectsInvalidRequests() {
        assertThatThrownBy(() -> holds.hold("alice", 2, List.of(11L)))       // event not on sale
                .isInstanceOf(InvalidHoldRequestException.class);
        assertThatThrownBy(() -> holds.hold("alice", 1, List.of(11L)))       // seat of another event
                .isInstanceOf(InvalidHoldRequestException.class);
        assertThatThrownBy(() -> holds.hold("alice", 1, List.of(1L, 2L, 3L, 4L, 5L, 6L, 7L))) // > max-seats (6)
                .isInstanceOf(InvalidHoldRequestException.class);
        assertThatThrownBy(() -> holds.hold("alice", 99, List.of(1L)))       // unknown event
                .isInstanceOf(EventNotFoundException.class);

        assertThat(holds.availability(1, "bob").unavailableSeatIds()).isEmpty();
    }

    @Test
    void duplicateSeatIdsAreHeldOnce() {
        Hold hold = holds.hold("alice", 1, List.of(4L, 4L));
        assertThat(hold.seatIds()).containsExactly(4L);
    }
}

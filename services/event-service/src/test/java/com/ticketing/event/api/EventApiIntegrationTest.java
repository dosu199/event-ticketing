package com.ticketing.event.api;

import com.jayway.jsonpath.JsonPath;
import com.ticketing.event.IntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Calls the real HTTP layer (controllers, validation, error handling) with MockMvc,
 * backed by the real service, repositories and PostgreSQL from IntegrationTest.
 *
 * <p>Data: Flyway's seed migrations are present (venue 1 = Budapest Park, event 1 =
 * Summer Rock Night with 40 seats). Tests that change data create their own events,
 * so they don't depend on each other or on execution order.
 */
class EventApiIntegrationTest extends IntegrationTest {

    private static final long SEEDED_VENUE_ID = 1;
    private static final long SEEDED_EVENT_WITH_SEATS = 1;

    @Autowired
    private WebApplicationContext context;

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).build();
    }

    // ---------- helpers ----------

    private static String futureTime() {
        return Instant.now().plus(30, ChronoUnit.DAYS).truncatedTo(ChronoUnit.SECONDS).toString();
    }

    private ResultActions createEvent(String json) throws Exception {
        return mvc.perform(post("/events").contentType(MediaType.APPLICATION_JSON).content(json));
    }

    private long createDraftEvent() throws Exception {
        String body = createEvent("""
                {"venueId": %d, "title": "Test Event", "description": "Created by a test", "startTime": "%s"}
                """.formatted(SEEDED_VENUE_ID, futureTime()))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(body, "$.id")).longValue();
    }

    private void expectProblem(ResultActions result, int status) throws Exception {
        result.andExpect(status().is(status))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(status))
                .andExpect(jsonPath("$.title").exists());
    }

    // ---------- GET /events ----------

    @Nested
    class ListEvents {

        @Test
        void returnsFirstPageWithDefaultSize() throws Exception {
            mvc.perform(get("/events"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.page").value(0))
                    .andExpect(jsonPath("$.size").value(20))
                    .andExpect(jsonPath("$.content", hasSize(lessThanOrEqualTo(20))))
                    .andExpect(jsonPath("$.content[0].venue.name").exists());
        }

        @Test
        void filtersByStatusInTheDatabase() throws Exception {
            mvc.perform(get("/events").param("status", "CANCELLED").param("size", "50"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[*].status", everyItem(is("CANCELLED"))));
        }

        @Test
        void rejectsPageSizeAboveLimit() throws Exception {
            expectProblem(mvc.perform(get("/events").param("size", "500")), 400);
        }

        @Test
        void rejectsUnknownStatus() throws Exception {
            expectProblem(mvc.perform(get("/events").param("status", "FOO")), 400);
        }
    }

    // ---------- GET /events/{id} ----------

    @Test
    void unknownEventReturns404ProblemDetail() throws Exception {
        mvc.perform(get("/events/999999"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.detail").value("Event 999999 not found"));
    }

    // ---------- POST /events ----------

    @Nested
    class CreateEvent {

        @Test
        void createsDraftAndReturnsLocation() throws Exception {
            MvcResult created = createEvent("""
                    {"venueId": %d, "title": "  New Show  ", "startTime": "%s"}
                    """.formatted(SEEDED_VENUE_ID, futureTime()))
                    .andExpect(status().isCreated())
                    .andExpect(header().exists("Location"))
                    .andReturn();

            long id = ((Number) JsonPath.read(created.getResponse().getContentAsString(), "$.id")).longValue();
            assertThat(created.getResponse().getHeader("Location")).endsWith("/events/" + id);

            mvc.perform(get("/events/" + id))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.title").value("New Show"))
                    .andExpect(jsonPath("$.status").value("DRAFT"))
                    .andExpect(jsonPath("$.venue.id").value((int) SEEDED_VENUE_ID));
        }

        @Test
        void rejectsInvalidFieldsWithFieldErrors() throws Exception {
            ResultActions result = createEvent("""
                    {"venueId": %d, "title": "   ", "startTime": "2001-01-01T10:00:00Z"}
                    """.formatted(SEEDED_VENUE_ID));
            expectProblem(result, 400);
            result.andExpect(jsonPath("$.errors.title").exists())
                    .andExpect(jsonPath("$.errors.startTime").exists());
        }

        @Test
        void rejectsUnknownVenue() throws Exception {
            ResultActions result = createEvent("""
                    {"venueId": 999999, "title": "Nowhere", "startTime": "%s"}
                    """.formatted(futureTime()));
            expectProblem(result, 400);
            result.andExpect(jsonPath("$.detail").value("Venue 999999 does not exist"));
        }

        @Test
        void rejectsMalformedJson() throws Exception {
            expectProblem(createEvent("{ not json"), 400);
        }
    }

    // ---------- publish / cancel ----------

    @Nested
    class Lifecycle {

        @Test
        void draftCanBePublishedOnce() throws Exception {
            long id = createDraftEvent();

            mvc.perform(post("/events/" + id + "/publish"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("PUBLISHED"));

            expectProblem(mvc.perform(post("/events/" + id + "/publish")), 409);
        }

        @Test
        void publishedEventCanBeCancelledButNotTwice() throws Exception {
            long id = createDraftEvent();
            mvc.perform(post("/events/" + id + "/publish")).andExpect(status().isOk());

            mvc.perform(post("/events/" + id + "/cancel"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("CANCELLED"));

            expectProblem(mvc.perform(post("/events/" + id + "/cancel")), 409);
        }

        @Test
        void cancelledEventCannotBePublished() throws Exception {
            long id = createDraftEvent();
            mvc.perform(post("/events/" + id + "/cancel")).andExpect(status().isOk());

            expectProblem(mvc.perform(post("/events/" + id + "/publish")), 409);
        }

        @Test
        void unknownEventCannotBePublished() throws Exception {
            expectProblem(mvc.perform(post("/events/999999/publish")), 404);
        }
    }

    // ---------- GET /events/{id}/seats ----------

    @Nested
    class Seats {

        @Test
        void returnsSeatsOrderedByRowAndNumber() throws Exception {
            mvc.perform(get("/events/" + SEEDED_EVENT_WITH_SEATS + "/seats"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(40)))
                    .andExpect(jsonPath("$[0].rowLabel").value("A"))
                    .andExpect(jsonPath("$[0].seatNumber").value(1))
                    .andExpect(jsonPath("$[0].category").value("VIP"))
                    .andExpect(jsonPath("$[39].rowLabel").value("D"))
                    .andExpect(jsonPath("$[39].seatNumber").value(10));
        }

        @Test
        void newEventHasNoSeats() throws Exception {
            long id = createDraftEvent();
            mvc.perform(get("/events/" + id + "/seats"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(0)));
        }

        @Test
        void unknownEventReturns404() throws Exception {
            expectProblem(mvc.perform(get("/events/999999/seats")), 404);
        }
    }
}

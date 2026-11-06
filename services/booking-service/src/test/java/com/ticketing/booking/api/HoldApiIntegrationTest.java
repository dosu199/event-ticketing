package com.ticketing.booking.api;

import com.jayway.jsonpath.JsonPath;
import com.ticketing.booking.IntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** HTTP layer: status codes, headers and Problem Details. */
class HoldApiIntegrationTest extends IntegrationTest {

    @Autowired
    private WebApplicationContext context;

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).build();
    }

    private ResultActions hold(String user, String seatIdsJson) throws Exception {
        return mvc.perform(post("/holds")
                .header("X-User-Id", user)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"eventId\": 1, \"seatIds\": " + seatIdsJson + "}"));
    }

    @Test
    void createHoldThenSeeItInAvailability() throws Exception {
        String body = hold("alice", "[1, 2]")
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.seatIds", contains(1, 2)))
                .andExpect(jsonPath("$.expiresAt").exists())
                .andReturn().getResponse().getContentAsString();
        String holdId = JsonPath.read(body, "$.holdId");

        mvc.perform(get("/events/1/availability").header("X-User-Id", "alice"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.unavailableSeatIds", hasSize(0)))
                .andExpect(jsonPath("$.myHold.holdId").value(holdId));

        mvc.perform(get("/events/1/availability").header("X-User-Id", "bob"))
                .andExpect(jsonPath("$.unavailableSeatIds", contains(1, 2)))
                .andExpect(jsonPath("$.myHold").doesNotExist());
    }

    @Test
    void conflictListsTheTakenSeats() throws Exception {
        hold("alice", "[3]").andExpect(status().isCreated());

        hold("bob", "[3, 4]")
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.unavailableSeatIds", contains(3)));
    }

    @Test
    void releaseReturns204AndSecondReleaseReturns404() throws Exception {
        String body = hold("alice", "[6]").andReturn().getResponse().getContentAsString();
        String holdId = JsonPath.read(body, "$.holdId");

        mvc.perform(delete("/holds/" + holdId).header("X-User-Id", "alice")).andExpect(status().isNoContent());
        mvc.perform(delete("/holds/" + holdId).header("X-User-Id", "alice")).andExpect(status().isNotFound());
    }

    @Test
    void missingOrInvalidUserIdIsRejected() throws Exception {
        mvc.perform(post("/holds").contentType(MediaType.APPLICATION_JSON).content("{\"eventId\": 1, \"seatIds\": [1]}"))
                .andExpect(status().isBadRequest());
        hold("not valid!", "[1]").andExpect(status().isBadRequest());
    }

    @Test
    void emptySeatListFailsValidation() throws Exception {
        hold("alice", "[]")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.seatIds").exists());
    }

    @Test
    void unknownEventIs404() throws Exception {
        mvc.perform(post("/holds").header("X-User-Id", "alice").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"eventId\": 99, \"seatIds\": [1]}"))
                .andExpect(status().isNotFound());
    }
}

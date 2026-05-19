package com.voxops.incident;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class IncidentApiIntegrationTest extends AbstractIncidentServiceIntegrationTest {

    @Test
    void listIncidentsWithoutTokenReturnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/incidents"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void adminCanListAndCreateIncidents() throws Exception {
        String token = login("admin@voxops.dev", "admin123");

        mockMvc.perform(get("/api/incidents")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))));

        mockMvc.perform(post("/api/incidents")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Integration test incident",
                                  "severity": "SEV3"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title", is("Integration test incident")))
                .andExpect(jsonPath("$.severity", is("SEV3")))
                .andExpect(jsonPath("$.status", is("OPEN")))
                .andExpect(jsonPath("$.id", notNullValue()));
    }

    @Test
    void viewerCannotCreateIncidents() throws Exception {
        String token = login("viewer@voxops.dev", "viewer123");

        mockMvc.perform(post("/api/incidents")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Viewer should not create",
                                  "severity": "SEV4"
                                }
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    void viewerCanReadIncidents() throws Exception {
        String token = login("viewer@voxops.dev", "viewer123");

        mockMvc.perform(get("/api/incidents")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))));
    }
}

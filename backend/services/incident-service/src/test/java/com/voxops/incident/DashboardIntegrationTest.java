package com.voxops.incident;

import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class DashboardIntegrationTest extends AbstractIncidentServiceIntegrationTest {

    @Test
    void dashboardSummaryWithoutTokenReturnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/dashboard/summary"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void dashboardSummaryReturnsStatusCountsMetricsAndRecentEvents() throws Exception {
        String token = login("responder@voxops.dev", "responder123");

        mockMvc.perform(get("/api/dashboard/summary")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.incidentsByStatus", hasSize(3)))
                .andExpect(jsonPath("$.incidentsByStatus[0].status", notNullValue()))
                .andExpect(jsonPath("$.metrics.totalIncidents", greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.metrics.totalTimelineEvents", greaterThanOrEqualTo(0)))
                .andExpect(jsonPath("$.metrics.publishedKafkaEvents", greaterThanOrEqualTo(0)))
                .andExpect(jsonPath("$.recentTimelineEvents", notNullValue()))
                .andExpect(jsonPath("$.recentDomainEvents", notNullValue()));
    }

    @Test
    void viewerCanReadDashboardSummary() throws Exception {
        String token = login("viewer@voxops.dev", "viewer123");

        mockMvc.perform(get("/api/dashboard/summary")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.metrics.totalIncidents", greaterThanOrEqualTo(1)));
    }
}

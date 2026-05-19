package com.voxops.incident;

public record DashboardMetrics(
        long totalIncidents,
        long totalTimelineEvents,
        long totalTranscripts,
        long totalRecommendations,
        long totalPostmortems,
        long publishedKafkaEvents,
        long failedKafkaEvents
) {
}

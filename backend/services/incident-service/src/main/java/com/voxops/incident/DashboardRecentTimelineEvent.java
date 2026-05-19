package com.voxops.incident;

import java.time.Instant;
import java.util.UUID;

public record DashboardRecentTimelineEvent(
        UUID incidentId,
        String incidentTitle,
        TimelineEventType eventType,
        String summary,
        String source,
        Instant createdAt
) {
}

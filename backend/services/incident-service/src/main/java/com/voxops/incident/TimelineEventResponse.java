package com.voxops.incident;

import java.time.Instant;
import java.util.UUID;

public record TimelineEventResponse(
        UUID id,
        UUID incidentId,
        TimelineEventType eventType,
        String summary,
        String source,
        Instant createdAt
) {
    public static TimelineEventResponse from(TimelineEvent event) {
        return new TimelineEventResponse(
                event.getId(),
                event.getIncidentId(),
                event.getEventType(),
                event.getSummary(),
                event.getSource(),
                event.getCreatedAt()
        );
    }
}

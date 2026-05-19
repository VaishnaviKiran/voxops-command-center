package com.voxops.incident;

import java.time.Instant;
import java.util.UUID;

public record IncidentResponse(
        UUID id,
        String title,
        IncidentSeverity severity,
        IncidentStatus status,
        Instant startedAt
) {
    public static IncidentResponse from(Incident incident) {
        return new IncidentResponse(
                incident.getId(),
                incident.getTitle(),
                incident.getSeverity(),
                incident.getStatus(),
                incident.getStartedAt()
        );
    }
}

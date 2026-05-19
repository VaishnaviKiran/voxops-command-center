package com.voxops.incident;

import java.time.Instant;
import java.util.UUID;

public record PostmortemResponse(
        UUID id,
        UUID incidentId,
        String title,
        String content,
        String generatedFrom,
        Instant createdAt
) {
    public static PostmortemResponse from(Postmortem postmortem) {
        return new PostmortemResponse(
                postmortem.getId(),
                postmortem.getIncidentId(),
                postmortem.getTitle(),
                postmortem.getContent(),
                postmortem.getGeneratedFrom(),
                postmortem.getCreatedAt()
        );
    }
}

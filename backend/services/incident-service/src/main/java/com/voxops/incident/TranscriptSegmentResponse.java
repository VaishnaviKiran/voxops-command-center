package com.voxops.incident;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record TranscriptSegmentResponse(
        UUID id,
        UUID incidentId,
        String speakerLabel,
        String text,
        BigDecimal confidence,
        Instant createdAt
) {
    public static TranscriptSegmentResponse from(TranscriptSegment segment) {
        return new TranscriptSegmentResponse(
                segment.getId(),
                segment.getIncidentId(),
                segment.getSpeakerLabel(),
                segment.getText(),
                segment.getConfidence(),
                segment.getCreatedAt()
        );
    }
}

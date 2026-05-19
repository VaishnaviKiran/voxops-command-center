package com.voxops.incident;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record AiRecommendationResponse(
        UUID id,
        UUID incidentId,
        String prompt,
        String response,
        BigDecimal confidence,
        String citations,
        AiRecommendationStatus status,
        Instant createdAt
) {
    public static AiRecommendationResponse from(AiRecommendation recommendation) {
        return new AiRecommendationResponse(
                recommendation.getId(),
                recommendation.getIncidentId(),
                recommendation.getPrompt(),
                recommendation.getResponse(),
                recommendation.getConfidence(),
                recommendation.getCitations(),
                recommendation.getStatus(),
                recommendation.getCreatedAt()
        );
    }
}

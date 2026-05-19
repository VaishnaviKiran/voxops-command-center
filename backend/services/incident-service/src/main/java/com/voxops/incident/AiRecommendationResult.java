package com.voxops.incident;

import java.math.BigDecimal;

public record AiRecommendationResult(
        String provider,
        String prompt,
        String response,
        BigDecimal confidence,
        String citations
) {
}

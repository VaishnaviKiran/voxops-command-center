package com.voxops.incident;

public interface AiRecommendationProvider {
    AiProviderType providerType();

    AiRecommendationResult generate(AiRecommendationContext context);
}

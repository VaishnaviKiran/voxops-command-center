package com.voxops.incident;

import org.springframework.stereotype.Component;

@Component
public class AnthropicRecommendationProvider extends ExternalAiRecommendationProvider {

    @Override
    public AiProviderType providerType() {
        return AiProviderType.ANTHROPIC;
    }

    @Override
    public AiRecommendationResult generate(AiRecommendationContext context) {
        return notConfiguredResult(context, providerType().name());
    }
}

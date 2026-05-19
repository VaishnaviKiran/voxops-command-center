package com.voxops.incident;

import java.math.BigDecimal;

public abstract class ExternalAiRecommendationProvider implements AiRecommendationProvider {

    protected AiRecommendationResult notConfiguredResult(AiRecommendationContext context, String providerName) {
        String prompt = """
                Provider %s was selected for incident %s, but live provider integration is not configured yet.
                Timeline events: %d
                Transcript segments: %d
                Retrieved runbooks: %d
                """.formatted(
                providerName,
                context.incident().getId(),
                context.timeline().size(),
                context.transcripts().size(),
                context.runbooks().size()
        );

        String response = """
                %s provider configuration is present, but this project is currently running in scaffold mode.
                Next implementation step: add the provider SDK/client call, pass this prompt, require cited output,
                and map the response into AiRecommendationResult.
                """.formatted(providerName);

        return new AiRecommendationResult(providerName, prompt, response, BigDecimal.valueOf(0.25), "provider:scaffold");
    }
}

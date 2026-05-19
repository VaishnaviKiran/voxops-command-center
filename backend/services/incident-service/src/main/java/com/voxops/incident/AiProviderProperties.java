package com.voxops.incident;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "voxops.ai")
public record AiProviderProperties(
        AiProviderType provider,
        OpenAi openai,
        Anthropic anthropic,
        Ollama ollama
) {
    public AiProviderType activeProvider() {
        return provider == null ? AiProviderType.MOCK : provider;
    }

    public record OpenAi(String apiKey, String model) {
    }

    public record Anthropic(String apiKey, String model) {
    }

    public record Ollama(String baseUrl, String model) {
    }
}

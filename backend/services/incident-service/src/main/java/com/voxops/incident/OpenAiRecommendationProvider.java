package com.voxops.incident;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
public class OpenAiRecommendationProvider extends ExternalAiRecommendationProvider {

    private static final URI CHAT_COMPLETIONS_URI = URI.create("https://api.openai.com/v1/chat/completions");

    private final AiProviderProperties aiProviderProperties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public OpenAiRecommendationProvider(AiProviderProperties aiProviderProperties, ObjectMapper objectMapper) {
        this.aiProviderProperties = aiProviderProperties;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    @Override
    public AiProviderType providerType() {
        return AiProviderType.OPENAI;
    }

    @Override
    public AiRecommendationResult generate(AiRecommendationContext context) {
        AiProviderProperties.OpenAi openAi = aiProviderProperties.openai();
        if (openAi == null || openAi.apiKey() == null || openAi.apiKey().isBlank()) {
            return notConfiguredResult(context, providerType().name());
        }

        String prompt = buildPrompt(context);

        try {
            String requestBody = objectMapper.writeValueAsString(Map.of(
                    "model", openAi.model() == null || openAi.model().isBlank() ? "gpt-4o-mini" : openAi.model(),
                    "temperature", 0.2,
                    "messages", List.of(
                            Map.of(
                                    "role", "system",
                                    "content", """
                                            You are an expert SRE incident commander. Generate concise, practical,
                                            safety-aware recommendations grounded only in the provided incident context.
                                            Include concrete next actions and mention when more evidence is needed.
                                            """
                            ),
                            Map.of("role", "user", "content", prompt)
                    )
            ));

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(CHAT_COMPLETIONS_URI)
                    .timeout(Duration.ofSeconds(30))
                    .header("authorization", "Bearer " + openAi.apiKey())
                    .header("content-type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                return providerErrorResult(context, prompt, "OpenAI API returned HTTP " + response.statusCode());
            }

            JsonNode root = objectMapper.readTree(response.body());
            String content = root.path("choices").path(0).path("message").path("content").asText();
            if (content == null || content.isBlank()) {
                return providerErrorResult(context, prompt, "OpenAI response did not include message content.");
            }

            return new AiRecommendationResult(
                    providerType().name(),
                    prompt,
                    content,
                    calculateConfidence(context.timeline(), context.transcripts()),
                    buildCitations(context.timeline(), context.transcripts(), context.runbooks())
            );
        } catch (Exception error) {
            return providerErrorResult(context, prompt, "OpenAI request failed: " + error.getMessage());
        }
    }

    private String buildPrompt(AiRecommendationContext context) {
        Incident incident = context.incident();
        String timelineContext = context.timeline().stream()
                .sorted(Comparator.comparing(TimelineEvent::getCreatedAt).reversed())
                .limit(10)
                .map(event -> "- [%s] %s (%s)".formatted(event.getEventType(), event.getSummary(), event.getSource()))
                .collect(Collectors.joining("\n"));
        String transcriptContext = context.transcripts().stream()
                .sorted(Comparator.comparing(TranscriptSegment::getCreatedAt).reversed())
                .limit(10)
                .map(segment -> "- %s: %s".formatted(segment.getSpeakerLabel(), segment.getText()))
                .collect(Collectors.joining("\n"));
        String runbookContext = context.runbooks().stream()
                .map(runbook -> "- %s (%s, score=%s): %s".formatted(
                        runbook.title(),
                        runbook.citation(),
                        runbook.score(),
                        runbook.excerpt()
                ))
                .collect(Collectors.joining("\n"));

        return """
                Incident:
                id: %s
                title: %s
                severity: %s
                status: %s

                Timeline context:
                %s

                Transcript context:
                %s

                Retrieved runbook context:
                %s

                Return:
                1. Situation assessment
                2. Top 3 recommended next actions
                3. Runbook citations used
                4. Risks or missing evidence
                5. Suggested owner for the next action
                """.formatted(
                incident.getId(),
                incident.getTitle(),
                incident.getSeverity(),
                incident.getStatus(),
                timelineContext.isBlank() ? "No timeline events available." : timelineContext,
                transcriptContext.isBlank() ? "No transcript segments available." : transcriptContext,
                runbookContext.isBlank() ? "No runbook matches available." : runbookContext
        );
    }

    private AiRecommendationResult providerErrorResult(AiRecommendationContext context, String prompt, String errorMessage) {
        String response = """
                OpenAI recommendation could not be generated.
                Reason: %s

                Fallback next action: review the latest timeline and transcript entries manually, then retry once
                provider configuration and network access are confirmed.
                """.formatted(errorMessage);

        return new AiRecommendationResult(
                providerType().name(),
                prompt,
                response,
                BigDecimal.valueOf(0.10),
                buildCitations(context.timeline(), context.transcripts(), context.runbooks())
        );
    }

    private String buildCitations(List<TimelineEvent> timeline, List<TranscriptSegment> transcripts, List<RunbookMatch> runbooks) {
        String timelineCitation = timeline.stream()
                .findFirst()
                .map(event -> "timeline:" + event.getId())
                .orElse("timeline:none");
        String transcriptCitation = transcripts.stream()
                .findFirst()
                .map(segment -> "transcript:" + segment.getId())
                .orElse("transcript:none");
        String runbookCitation = runbooks.stream()
                .map(RunbookMatch::citation)
                .collect(Collectors.joining(","));
        if (runbookCitation.isBlank()) {
            runbookCitation = "runbook:none";
        }

        return String.join(",", Stream.of(timelineCitation, transcriptCitation, runbookCitation).toList());
    }

    private BigDecimal calculateConfidence(List<TimelineEvent> timeline, List<TranscriptSegment> transcripts) {
        double base = 0.55;
        double timelineBoost = Math.min(timeline.size(), 5) * 0.04;
        double transcriptBoost = Math.min(transcripts.size(), 5) * 0.03;
        return BigDecimal.valueOf(Math.min(0.90, base + timelineBoost + transcriptBoost));
    }
}

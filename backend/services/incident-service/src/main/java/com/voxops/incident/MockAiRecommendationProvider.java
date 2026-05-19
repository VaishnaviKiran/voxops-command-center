package com.voxops.incident;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
public class MockAiRecommendationProvider implements AiRecommendationProvider {

    @Override
    public AiProviderType providerType() {
        return AiProviderType.MOCK;
    }

    @Override
    public AiRecommendationResult generate(AiRecommendationContext context) {
        return new AiRecommendationResult(
                providerType().name(),
                buildPrompt(context.incident(), context.timeline(), context.transcripts(), context.runbooks()),
                buildRecommendation(context.incident(), context.timeline(), context.transcripts(), context.runbooks()),
                calculateConfidence(context.timeline(), context.transcripts()),
                buildCitations(context.timeline(), context.transcripts(), context.runbooks())
        );
    }

    private String buildPrompt(
            Incident incident,
            List<TimelineEvent> timeline,
            List<TranscriptSegment> transcripts,
            List<RunbookMatch> runbooks
    ) {
        return """
                Generate operational recommendations for incident:
                title=%s
                severity=%s
                status=%s
                timelineEvents=%d
                transcriptSegments=%d
                runbookContext=%s
                """.formatted(
                incident.getTitle(),
                incident.getSeverity(),
                incident.getStatus(),
                timeline.size(),
                transcripts.size(),
                formatRunbooks(runbooks)
        );
    }

    private String buildRecommendation(
            Incident incident,
            List<TimelineEvent> timeline,
            List<TranscriptSegment> transcripts,
            List<RunbookMatch> runbooks
    ) {
        String latestTimeline = timeline.stream()
                .max(Comparator.comparing(TimelineEvent::getCreatedAt))
                .map(TimelineEvent::getSummary)
                .orElse("No timeline events captured yet.");
        String latestTranscript = transcripts.stream()
                .max(Comparator.comparing(TranscriptSegment::getCreatedAt))
                .map(TranscriptSegment::getText)
                .orElse("No transcript context captured yet.");

        return """
                Recommended next actions:
                1. Assign an incident commander and confirm owner for %s.
                2. Validate customer impact and attach metrics before mitigation decisions.
                3. Review the latest timeline signal: %s
                4. Review the latest transcript signal: %s
                5. Apply the most relevant runbook guidance: %s
                6. If this remains %s, prepare rollback or traffic-shift options and document every decision.
                """.formatted(
                incident.getTitle(),
                latestTimeline,
                latestTranscript,
                runbooks.stream().findFirst().map(RunbookMatch::excerpt).orElse("No runbook match found."),
                incident.getSeverity()
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
        String runbookCitations = runbooks.stream()
                .map(RunbookMatch::citation)
                .collect(Collectors.joining(","));
        if (runbookCitations.isBlank()) {
            runbookCitations = "runbook:none";
        }

        return String.join(",", Stream.of(timelineCitation, transcriptCitation, runbookCitations).toList());
    }

    private String formatRunbooks(List<RunbookMatch> runbooks) {
        if (runbooks.isEmpty()) {
            return "No runbook matches found.";
        }
        return runbooks.stream()
                .map(runbook -> "%s (%s): %s".formatted(runbook.title(), runbook.citation(), runbook.excerpt()))
                .collect(Collectors.joining("\n"));
    }

    private BigDecimal calculateConfidence(List<TimelineEvent> timeline, List<TranscriptSegment> transcripts) {
        double base = 0.45;
        double timelineBoost = Math.min(timeline.size(), 5) * 0.05;
        double transcriptBoost = Math.min(transcripts.size(), 5) * 0.04;
        return BigDecimal.valueOf(Math.min(0.85, base + timelineBoost + transcriptBoost));
    }
}

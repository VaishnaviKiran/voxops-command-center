package com.voxops.incident;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class RunbookRetrievalService {

    private static final Set<String> STOP_WORDS = Set.of(
            "the", "and", "for", "with", "from", "this", "that", "then", "than", "into", "onto",
            "incident", "status", "voice", "stream", "created", "generated", "current", "latest"
    );

    private final IncidentRepository incidentRepository;
    private final TimelineEventRepository timelineEventRepository;
    private final TranscriptSegmentRepository transcriptSegmentRepository;
    private final AiRecommendationRepository aiRecommendationRepository;
    private final RunbookDocumentRepository runbookDocumentRepository;
    private final MeterRegistry meterRegistry;

    public RunbookRetrievalService(
            IncidentRepository incidentRepository,
            TimelineEventRepository timelineEventRepository,
            TranscriptSegmentRepository transcriptSegmentRepository,
            AiRecommendationRepository aiRecommendationRepository,
            RunbookDocumentRepository runbookDocumentRepository,
            MeterRegistry meterRegistry
    ) {
        this.incidentRepository = incidentRepository;
        this.timelineEventRepository = timelineEventRepository;
        this.transcriptSegmentRepository = transcriptSegmentRepository;
        this.aiRecommendationRepository = aiRecommendationRepository;
        this.runbookDocumentRepository = runbookDocumentRepository;
        this.meterRegistry = meterRegistry;
    }

    @Transactional(readOnly = true)
    public RunbookSearchResponse searchForIncident(UUID incidentId) {
        Incident incident = incidentRepository.findById(incidentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Incident not found"));
        String query = buildIncidentQuery(
                incident,
                timelineEventRepository.findByIncident_IdOrderByCreatedAtDesc(incidentId),
                transcriptSegmentRepository.findByIncident_IdOrderByCreatedAtDesc(incidentId),
                aiRecommendationRepository.findByIncident_IdOrderByCreatedAtDesc(incidentId)
        );
        List<RunbookMatch> matches = search(query, 3);
        meterRegistry.counter("voxops_runbook_retrievals_total", "source", "incident").increment();
        return new RunbookSearchResponse(incidentId, query, matches);
    }

    @Transactional(readOnly = true)
    public List<RunbookMatch> search(String query, int limit) {
        Set<String> queryTerms = tokenize(query);
        return runbookDocumentRepository.findAll().stream()
                .map(document -> score(document, queryTerms))
                .filter(scored -> scored.score() > 0)
                .sorted(Comparator.comparing(ScoredRunbook::score).reversed())
                .limit(limit)
                .map(scored -> RunbookMatch.from(scored.document(), scored.score(), excerpt(scored.document(), queryTerms)))
                .toList();
    }

    private String buildIncidentQuery(
            Incident incident,
            List<TimelineEvent> timeline,
            List<TranscriptSegment> transcripts,
            List<AiRecommendation> recommendations
    ) {
        String timelineContext = timeline.stream()
                .limit(8)
                .map(TimelineEvent::getSummary)
                .collect(Collectors.joining(" "));
        String transcriptContext = transcripts.stream()
                .limit(8)
                .map(TranscriptSegment::getText)
                .collect(Collectors.joining(" "));
        String recommendationContext = recommendations.stream()
                .limit(3)
                .map(AiRecommendation::getResponse)
                .collect(Collectors.joining(" "));

        return String.join(" ",
                incident.getTitle(),
                incident.getSeverity().name(),
                incident.getStatus().name(),
                timelineContext,
                transcriptContext,
                recommendationContext
        );
    }

    private ScoredRunbook score(RunbookDocument document, Set<String> queryTerms) {
        Set<String> titleTerms = tokenize(document.getTitle());
        Set<String> tagTerms = tokenize(document.getTags());
        Set<String> serviceTerms = tokenize(document.getServiceName());
        Set<String> contentTerms = tokenize(document.getContent());

        double score = 0;
        for (String term : queryTerms) {
            if (titleTerms.contains(term)) {
                score += 4;
            }
            if (tagTerms.contains(term)) {
                score += 3;
            }
            if (serviceTerms.contains(term)) {
                score += 3;
            }
            if (contentTerms.contains(term)) {
                score += 1;
            }
        }
        return new ScoredRunbook(document, score);
    }

    private String excerpt(RunbookDocument document, Set<String> queryTerms) {
        String[] sentences = document.getContent().split("(?<=\\.)\\s+");
        return Arrays.stream(sentences)
                .max(Comparator.comparingInt(sentence -> overlap(tokenize(sentence), queryTerms)))
                .orElse(document.getContent());
    }

    private int overlap(Set<String> left, Set<String> right) {
        Set<String> intersection = new HashSet<>(left);
        intersection.retainAll(right);
        return intersection.size();
    }

    private Set<String> tokenize(String value) {
        if (value == null || value.isBlank()) {
            return Set.of();
        }

        return Arrays.stream(value.toLowerCase(Locale.ROOT).split("[^a-z0-9]+"))
                .filter(token -> token.length() >= 3)
                .filter(token -> !STOP_WORDS.contains(token))
                .collect(Collectors.toSet());
    }

    private record ScoredRunbook(RunbookDocument document, double score) {
    }
}

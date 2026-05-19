package com.voxops.incident;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class PostmortemService {

    private final IncidentRepository incidentRepository;
    private final TimelineEventRepository timelineEventRepository;
    private final TranscriptSegmentRepository transcriptSegmentRepository;
    private final AiRecommendationRepository aiRecommendationRepository;
    private final RunbookRetrievalService runbookRetrievalService;
    private final PostmortemRepository postmortemRepository;
    private final DomainEventPublisher domainEventPublisher;
    private final EventTopicProperties eventTopicProperties;
    private final MeterRegistry meterRegistry;

    public PostmortemService(
            IncidentRepository incidentRepository,
            TimelineEventRepository timelineEventRepository,
            TranscriptSegmentRepository transcriptSegmentRepository,
            AiRecommendationRepository aiRecommendationRepository,
            RunbookRetrievalService runbookRetrievalService,
            PostmortemRepository postmortemRepository,
            DomainEventPublisher domainEventPublisher,
            EventTopicProperties eventTopicProperties,
            MeterRegistry meterRegistry
    ) {
        this.incidentRepository = incidentRepository;
        this.timelineEventRepository = timelineEventRepository;
        this.transcriptSegmentRepository = transcriptSegmentRepository;
        this.aiRecommendationRepository = aiRecommendationRepository;
        this.runbookRetrievalService = runbookRetrievalService;
        this.postmortemRepository = postmortemRepository;
        this.domainEventPublisher = domainEventPublisher;
        this.eventTopicProperties = eventTopicProperties;
        this.meterRegistry = meterRegistry;
    }

    @Transactional(readOnly = true)
    public List<PostmortemResponse> listPostmortems(UUID incidentId) {
        ensureIncidentExists(incidentId);
        return postmortemRepository.findByIncident_IdOrderByCreatedAtDesc(incidentId)
                .stream()
                .map(PostmortemResponse::from)
                .toList();
    }

    @Transactional
    public PostmortemResponse generatePostmortem(UUID incidentId) {
        Incident incident = incidentRepository.findById(incidentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Incident not found"));

        List<TimelineEvent> timeline = timelineEventRepository.findByIncident_IdOrderByCreatedAtDesc(incidentId);
        List<TranscriptSegment> transcripts = transcriptSegmentRepository.findByIncident_IdOrderByCreatedAtDesc(incidentId);
        List<AiRecommendation> recommendations = aiRecommendationRepository.findByIncident_IdOrderByCreatedAtDesc(incidentId);
        List<RunbookMatch> runbooks = runbookRetrievalService.searchForIncident(incidentId).matches();

        String title = "Postmortem: " + incident.getTitle();
        String content = buildContent(incident, timeline, transcripts, recommendations, runbooks);
        String generatedFrom = "timeline=%d,transcripts=%d,recommendations=%d,runbooks=%d,status=%s".formatted(
                timeline.size(),
                transcripts.size(),
                recommendations.size(),
                runbooks.size(),
                incident.getStatus()
        );

        Postmortem postmortem = postmortemRepository.save(new Postmortem(incident, title, content, generatedFrom));
        meterRegistry.counter("voxops_postmortems_created_total", "status", incident.getStatus().name()).increment();

        TimelineEvent timelineEvent = timelineEventRepository.save(new TimelineEvent(
                incident,
                TimelineEventType.NOTE,
                "Postmortem generated from incident timeline, transcripts, recommendations, and status changes.",
                "postmortem-service"
        ));

        publishPostmortemEvent(postmortem);
        publishTimelineEvent(timelineEvent);

        return PostmortemResponse.from(postmortem);
    }

    private String buildContent(
            Incident incident,
            List<TimelineEvent> timeline,
            List<TranscriptSegment> transcripts,
            List<AiRecommendation> recommendations,
            List<RunbookMatch> runbooks
    ) {
        List<TimelineEvent> chronologicalTimeline = timeline.stream()
                .sorted(Comparator.comparing(TimelineEvent::getCreatedAt))
                .toList();
        List<TimelineEvent> statusChanges = chronologicalTimeline.stream()
                .filter(event -> event.getEventType() == TimelineEventType.STATUS_CHANGE)
                .toList();

        return """
                # %s

                ## Executive Summary
                Incident `%s` was handled as a `%s` incident and currently sits in `%s`.
                This postmortem was generated from %d timeline events, %d transcript segments, %d AI recommendations,
                and %d status changes.

                ## Customer Impact
                Impact should be confirmed by service metrics and customer reports. Current incident evidence:
                %s

                ## Timeline
                %s

                ## Status Changes
                %s

                ## Transcript Evidence
                %s

                ## AI Recommendations Considered
                %s

                ## Operational Runbooks Retrieved
                %s

                ## Resolution
                %s

                ## Follow-Up Actions
                1. Confirm customer impact and incident duration with production metrics.
                2. Identify the owning team for the suspected root cause.
                3. Convert unresolved mitigation notes into tracked action items.
                4. Review cited runbooks and update any stale mitigation steps.
                """.formatted(
                incident.getTitle(),
                incident.getId(),
                incident.getSeverity(),
                incident.getStatus(),
                timeline.size(),
                transcripts.size(),
                recommendations.size(),
                statusChanges.size(),
                summarizeImpactEvidence(chronologicalTimeline, transcripts),
                formatTimeline(chronologicalTimeline),
                formatStatusChanges(statusChanges),
                formatTranscripts(transcripts),
                formatRecommendations(recommendations),
                formatRunbooks(runbooks),
                summarizeResolution(incident, statusChanges)
        );
    }

    private String summarizeImpactEvidence(List<TimelineEvent> timeline, List<TranscriptSegment> transcripts) {
        return timeline.stream()
                .filter(event -> event.getSummary().toLowerCase().contains("latency")
                        || event.getSummary().toLowerCase().contains("error")
                        || event.getSummary().toLowerCase().contains("customer")
                        || event.getSummary().toLowerCase().contains("checkout"))
                .findFirst()
                .map(TimelineEvent::getSummary)
                .or(() -> transcripts.stream().findFirst().map(TranscriptSegment::getText))
                .orElse("No explicit customer impact evidence was captured. Add impact details before final review.");
    }

    private String formatTimeline(List<TimelineEvent> timeline) {
        if (timeline.isEmpty()) {
            return "- No timeline events were captured.";
        }
        return timeline.stream()
                .map(event -> "- %s: [%s] %s (%s)".formatted(
                        event.getCreatedAt(),
                        event.getEventType(),
                        event.getSummary(),
                        event.getSource()
                ))
                .collect(Collectors.joining("\n"));
    }

    private String formatStatusChanges(List<TimelineEvent> statusChanges) {
        if (statusChanges.isEmpty()) {
            return "- No status changes were captured.";
        }
        return statusChanges.stream()
                .map(event -> "- %s: %s".formatted(event.getCreatedAt(), event.getSummary()))
                .collect(Collectors.joining("\n"));
    }

    private String formatTranscripts(List<TranscriptSegment> transcripts) {
        if (transcripts.isEmpty()) {
            return "- No transcript segments were captured.";
        }
        return transcripts.stream()
                .sorted(Comparator.comparing(TranscriptSegment::getCreatedAt))
                .limit(12)
                .map(segment -> "- %s: %s".formatted(segment.getSpeakerLabel(), segment.getText()))
                .collect(Collectors.joining("\n"));
    }

    private String formatRecommendations(List<AiRecommendation> recommendations) {
        if (recommendations.isEmpty()) {
            return "- No AI recommendations were generated.";
        }
        return recommendations.stream()
                .sorted(Comparator.comparing(AiRecommendation::getCreatedAt))
                .limit(5)
                .map(recommendation -> "- %s".formatted(recommendation.getResponse().replace("\n", " ")))
                .collect(Collectors.joining("\n"));
    }

    private String formatRunbooks(List<RunbookMatch> runbooks) {
        if (runbooks.isEmpty()) {
            return "- No operational runbooks were retrieved.";
        }
        return runbooks.stream()
                .map(runbook -> "- [%s] %s (%s, score %s): %s".formatted(
                        runbook.citation(),
                        runbook.title(),
                        runbook.serviceName(),
                        runbook.score(),
                        runbook.excerpt()
                ))
                .collect(Collectors.joining("\n"));
    }

    private String summarizeResolution(Incident incident, List<TimelineEvent> statusChanges) {
        if (incident.getStatus() != IncidentStatus.RESOLVED) {
            return "Incident is not yet resolved. Final resolution summary should be completed after closure.";
        }

        return statusChanges.stream()
                .reduce((first, second) -> second)
                .map(TimelineEvent::getSummary)
                .orElse("Incident was marked resolved, but no resolution note was captured.");
    }

    private void publishPostmortemEvent(Postmortem postmortem) {
        domainEventPublisher.publish(
                eventTopicProperties.activeTopics().postmortemCreatedTopic(),
                postmortem.getIncidentId().toString(),
                DomainEvent.of(
                        "postmortem.created",
                        postmortem.getIncidentId(),
                        "incident",
                        Map.of(
                                "postmortemId", postmortem.getId().toString(),
                                "title", postmortem.getTitle(),
                                "generatedFrom", postmortem.getGeneratedFrom(),
                                "createdAt", postmortem.getCreatedAt().toString()
                        )
                )
        );
    }

    private void publishTimelineEvent(TimelineEvent event) {
        domainEventPublisher.publish(
                eventTopicProperties.activeTopics().timelineCreatedTopic(),
                event.getIncidentId().toString(),
                DomainEvent.of(
                        "timeline.event.created",
                        event.getIncidentId(),
                        "incident",
                        Map.of(
                                "timelineEventId", event.getId().toString(),
                                "eventType", event.getEventType().name(),
                                "summary", event.getSummary(),
                                "source", event.getSource(),
                                "createdAt", event.getCreatedAt().toString()
                        )
                )
        );
    }

    private void ensureIncidentExists(UUID incidentId) {
        if (!incidentRepository.existsById(incidentId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Incident not found");
        }
    }
}

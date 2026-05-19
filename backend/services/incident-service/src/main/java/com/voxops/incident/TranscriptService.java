package com.voxops.incident;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class TranscriptService {

    private final IncidentRepository incidentRepository;
    private final TranscriptSegmentRepository transcriptSegmentRepository;
    private final TimelineEventRepository timelineEventRepository;
    private final DomainEventPublisher domainEventPublisher;
    private final EventTopicProperties eventTopicProperties;
    private final MeterRegistry meterRegistry;

    public TranscriptService(
            IncidentRepository incidentRepository,
            TranscriptSegmentRepository transcriptSegmentRepository,
            TimelineEventRepository timelineEventRepository,
            DomainEventPublisher domainEventPublisher,
            EventTopicProperties eventTopicProperties,
            MeterRegistry meterRegistry
    ) {
        this.incidentRepository = incidentRepository;
        this.transcriptSegmentRepository = transcriptSegmentRepository;
        this.timelineEventRepository = timelineEventRepository;
        this.domainEventPublisher = domainEventPublisher;
        this.eventTopicProperties = eventTopicProperties;
        this.meterRegistry = meterRegistry;
    }

    @Transactional(readOnly = true)
    public List<TranscriptSegmentResponse> listTranscripts(UUID incidentId) {
        ensureIncidentExists(incidentId);
        return transcriptSegmentRepository.findByIncident_IdOrderByCreatedAtDesc(incidentId)
                .stream()
                .map(TranscriptSegmentResponse::from)
                .toList();
    }

    @Transactional
    public TranscriptSegmentResponse createTranscript(UUID incidentId, CreateTranscriptSegmentRequest request) {
        Incident incident = incidentRepository.findById(incidentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Incident not found"));

        String speakerLabel = request.speakerLabel() == null || request.speakerLabel().isBlank()
                ? "Voice stream"
                : request.speakerLabel();
        BigDecimal confidence = request.confidence() == null ? BigDecimal.valueOf(0.5) : request.confidence();

        TranscriptSegment segment = transcriptSegmentRepository.save(
                new TranscriptSegment(incident, speakerLabel, request.text(), confidence)
        );
        meterRegistry.counter("voxops_transcript_segments_created_total", "speaker", speakerLabel).increment();

        String summary = "Transcript captured from %s: %s".formatted(speakerLabel, summarize(request.text()));
        TimelineEvent timelineEvent = timelineEventRepository.save(
                new TimelineEvent(incident, TimelineEventType.NOTE, summary, "transcript-service")
        );

        publishTranscriptEvent(segment);
        publishTimelineEvent(timelineEvent);

        return TranscriptSegmentResponse.from(segment);
    }

    private void publishTranscriptEvent(TranscriptSegment segment) {
        domainEventPublisher.publish(
                eventTopicProperties.activeTopics().transcriptCreatedTopic(),
                segment.getIncidentId().toString(),
                DomainEvent.of(
                        "transcript.segment.created",
                        segment.getIncidentId(),
                        "incident",
                        Map.of(
                                "transcriptSegmentId", segment.getId().toString(),
                                "speakerLabel", segment.getSpeakerLabel(),
                                "text", segment.getText(),
                                "confidence", segment.getConfidence(),
                                "createdAt", segment.getCreatedAt().toString()
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

    private String summarize(String text) {
        if (text.length() <= 180) {
            return text;
        }
        return text.substring(0, 177) + "...";
    }
}

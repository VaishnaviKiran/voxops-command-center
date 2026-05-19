package com.voxops.incident;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class TimelineService {

    private final IncidentRepository incidentRepository;
    private final TimelineEventRepository timelineEventRepository;
    private final DomainEventPublisher domainEventPublisher;
    private final EventTopicProperties eventTopicProperties;

    public TimelineService(
            IncidentRepository incidentRepository,
            TimelineEventRepository timelineEventRepository,
            DomainEventPublisher domainEventPublisher,
            EventTopicProperties eventTopicProperties
    ) {
        this.incidentRepository = incidentRepository;
        this.timelineEventRepository = timelineEventRepository;
        this.domainEventPublisher = domainEventPublisher;
        this.eventTopicProperties = eventTopicProperties;
    }

    @Transactional(readOnly = true)
    public List<TimelineEventResponse> listTimeline(UUID incidentId) {
        ensureIncidentExists(incidentId);
        return timelineEventRepository.findByIncident_IdOrderByCreatedAtDesc(incidentId)
                .stream()
                .map(TimelineEventResponse::from)
                .toList();
    }

    @Transactional
    public TimelineEventResponse createTimelineEvent(UUID incidentId, CreateTimelineEventRequest request) {
        Incident incident = incidentRepository.findById(incidentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Incident not found"));

        String source = request.source() == null || request.source().isBlank() ? "manual" : request.source();
        TimelineEvent event = timelineEventRepository.save(new TimelineEvent(incident, request.eventType(), request.summary(), source));
        TimelineEventResponse response = TimelineEventResponse.from(event);

        publishTimelineEvent(event);

        return response;
    }

    public void publishTimelineEvent(TimelineEvent event) {
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

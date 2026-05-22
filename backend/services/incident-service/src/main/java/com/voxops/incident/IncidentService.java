package com.voxops.incident;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class IncidentService {

    private final IncidentRepository incidentRepository;
    private final TimelineEventRepository timelineEventRepository;
    private final DomainEventPublisher domainEventPublisher;
    private final EventTopicProperties eventTopicProperties;
    private final MeterRegistry meterRegistry;

    public IncidentService(
            IncidentRepository incidentRepository,
            TimelineEventRepository timelineEventRepository,
            DomainEventPublisher domainEventPublisher,
            EventTopicProperties eventTopicProperties,
            MeterRegistry meterRegistry
    ) {
        this.incidentRepository = incidentRepository;
        this.timelineEventRepository = timelineEventRepository;
        this.domainEventPublisher = domainEventPublisher;
        this.eventTopicProperties = eventTopicProperties;
        this.meterRegistry = meterRegistry;
    }

    @Transactional(readOnly = true)
    public List<IncidentResponse> listIncidents() {
        return incidentRepository.findAll(Sort.by(Sort.Direction.DESC, "startedAt"))
                .stream()
                .map(IncidentResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public IncidentResponse getIncident(UUID incidentId) {
        return incidentRepository.findById(incidentId)
                .map(IncidentResponse::from)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Incident not found"));
    }

    @Transactional
    public IncidentResponse createIncident(CreateIncidentRequest request) {
        Incident incident = new Incident(request.title(), request.severity());
        Incident savedIncident = incidentRepository.save(incident);
        IncidentResponse response = IncidentResponse.from(savedIncident);
        meterRegistry.counter(
                "voxops_incidents_created_total",
                "severity", savedIncident.getSeverity().name(),
                "status", savedIncident.getStatus().name()
        ).increment();

        domainEventPublisher.publish(
                eventTopicProperties.activeTopics().incidentCreatedTopic(),
                savedIncident.getId().toString(),
                DomainEvent.of(
                        "incident.created",
                        savedIncident.getId(),
                        "incident",
                        Map.of(
                                "title", savedIncident.getTitle(),
                                "severity", savedIncident.getSeverity().name(),
                                "status", savedIncident.getStatus().name(),
                                "startedAt", savedIncident.getStartedAt().toString()
                        )
                )
        );

        return response;
    }

    @Transactional
    public IncidentResponse updateStatus(UUID incidentId, UpdateIncidentStatusRequest request) {
        Incident incident = incidentRepository.findById(incidentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Incident not found"));

        IncidentStatus previousStatus = incident.getStatus();
        IncidentStatus nextStatus = request.status();
        if (previousStatus == nextStatus) {
            return IncidentResponse.from(incident);
        }

        validateTransition(previousStatus, nextStatus);
        incident.transitionTo(nextStatus);

        TimelineEvent timelineEvent = timelineEventRepository.save(
                new TimelineEvent(
                        incident,
                        TimelineEventType.STATUS_CHANGE,
                        statusChangeSummary(previousStatus, nextStatus, request.note()),
                        "incident-status-workflow"
                )
        );

        meterRegistry.counter(
                "voxops_incident_status_changes_total",
                "from", previousStatus.name(),
                "to", nextStatus.name()
        ).increment();

        publishStatusChangedEvent(incident, previousStatus, nextStatus, timelineEvent);
        publishTimelineEvent(timelineEvent);

        return IncidentResponse.from(incident);
    }

    @Transactional
    public void deleteIncident(UUID incidentId) {
        if (!incidentRepository.existsById(incidentId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Incident not found");
        }
        eventAuditLogRepository.deleteByAggregateId(incidentId);
        incidentRepository.deleteById(incidentId);
    }

    private void validateTransition(IncidentStatus previousStatus, IncidentStatus nextStatus) {
        boolean isValid = switch (previousStatus) {
            case OPEN -> nextStatus == IncidentStatus.MITIGATING || nextStatus == IncidentStatus.RESOLVED;
            case MITIGATING -> nextStatus == IncidentStatus.RESOLVED || nextStatus == IncidentStatus.OPEN;
            case RESOLVED -> false;
        };

        if (!isValid) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid incident status transition from %s to %s".formatted(previousStatus, nextStatus)
            );
        }
    }

    private String statusChangeSummary(IncidentStatus previousStatus, IncidentStatus nextStatus, String note) {
        String summary = "Incident status changed from %s to %s.".formatted(previousStatus, nextStatus);
        if (note == null || note.isBlank()) {
            return summary;
        }
        return summary + " " + note;
    }

    private void publishStatusChangedEvent(
            Incident incident,
            IncidentStatus previousStatus,
            IncidentStatus nextStatus,
            TimelineEvent timelineEvent
    ) {
        domainEventPublisher.publish(
                eventTopicProperties.activeTopics().incidentStatusChangedTopic(),
                incident.getId().toString(),
                DomainEvent.of(
                        "incident.status.changed",
                        incident.getId(),
                        "incident",
                        Map.of(
                                "previousStatus", previousStatus.name(),
                                "status", nextStatus.name(),
                                "timelineEventId", timelineEvent.getId().toString(),
                                "changedAt", timelineEvent.getCreatedAt().toString()
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
}

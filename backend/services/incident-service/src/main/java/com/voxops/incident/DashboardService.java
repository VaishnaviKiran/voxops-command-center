package com.voxops.incident;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class DashboardService {

    private final IncidentRepository incidentRepository;
    private final TimelineEventRepository timelineEventRepository;
    private final TranscriptSegmentRepository transcriptSegmentRepository;
    private final AiRecommendationRepository aiRecommendationRepository;
    private final PostmortemRepository postmortemRepository;
    private final EventAuditLogRepository eventAuditLogRepository;

    public DashboardService(
            IncidentRepository incidentRepository,
            TimelineEventRepository timelineEventRepository,
            TranscriptSegmentRepository transcriptSegmentRepository,
            AiRecommendationRepository aiRecommendationRepository,
            PostmortemRepository postmortemRepository,
            EventAuditLogRepository eventAuditLogRepository
    ) {
        this.incidentRepository = incidentRepository;
        this.timelineEventRepository = timelineEventRepository;
        this.transcriptSegmentRepository = transcriptSegmentRepository;
        this.aiRecommendationRepository = aiRecommendationRepository;
        this.postmortemRepository = postmortemRepository;
        this.eventAuditLogRepository = eventAuditLogRepository;
    }

    @Transactional(readOnly = true)
    public DashboardSummaryResponse getSummary() {
        List<Incident> incidents = incidentRepository.findAll();
        Map<java.util.UUID, Incident> incidentsById = incidents.stream()
                .collect(Collectors.toMap(Incident::getId, Function.identity()));

        List<DashboardStatusCount> incidentsByStatus = Arrays.stream(IncidentStatus.values())
                .map(status -> new DashboardStatusCount(
                        status,
                        incidents.stream().filter(incident -> incident.getStatus() == status).count()
                ))
                .toList();

        List<DashboardRecentTimelineEvent> recentTimelineEvents = timelineEventRepository
                .findTop15ByOrderByCreatedAtDesc()
                .stream()
                .map(event -> toRecentTimelineEvent(event, incidentsById.get(event.getIncidentId())))
                .toList();

        List<DashboardRecentDomainEvent> recentDomainEvents = eventAuditLogRepository
                .findTop20ByOrderByCreatedAtDesc()
                .stream()
                .map(DashboardRecentDomainEvent::from)
                .toList();

        DashboardMetrics metrics = new DashboardMetrics(
                incidents.size(),
                timelineEventRepository.count(),
                transcriptSegmentRepository.count(),
                aiRecommendationRepository.count(),
                postmortemRepository.count(),
                eventAuditLogRepository.countByStatus(EventPublishStatus.PUBLISHED),
                eventAuditLogRepository.countByStatus(EventPublishStatus.FAILED)
        );

        return new DashboardSummaryResponse(
                incidentsByStatus,
                metrics,
                recentTimelineEvents,
                recentDomainEvents
        );
    }

    private DashboardRecentTimelineEvent toRecentTimelineEvent(TimelineEvent event, Incident incident) {
        String incidentTitle = incident == null ? "Unknown incident" : incident.getTitle();
        return new DashboardRecentTimelineEvent(
                event.getIncidentId(),
                incidentTitle,
                event.getEventType(),
                event.getSummary(),
                event.getSource(),
                event.getCreatedAt()
        );
    }
}

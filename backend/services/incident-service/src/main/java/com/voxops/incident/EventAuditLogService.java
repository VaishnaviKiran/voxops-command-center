package com.voxops.incident;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class EventAuditLogService {

    private final EventAuditLogRepository eventAuditLogRepository;
    private final ObjectMapper objectMapper;

    public EventAuditLogService(EventAuditLogRepository eventAuditLogRepository, ObjectMapper objectMapper) {
        this.eventAuditLogRepository = eventAuditLogRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public List<EventAuditLogResponse> listForIncident(UUID incidentId) {
        return eventAuditLogRepository.findByAggregateIdOrderByCreatedAtDesc(incidentId)
                .stream()
                .map(EventAuditLogResponse::from)
                .toList();
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public EventAuditLog record(String topic, String key, DomainEvent event, EventPublishStatus status) {
        return eventAuditLogRepository.save(new EventAuditLog(topic, key, event, toJson(event), status));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markPublished(UUID auditLogId, int partition, long offset) {
        eventAuditLogRepository.findById(auditLogId).ifPresent(log -> log.markPublished(partition, offset));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markFailed(UUID auditLogId, String errorMessage) {
        eventAuditLogRepository.findById(auditLogId).ifPresent(log -> log.markFailed(errorMessage));
    }

    private String toJson(DomainEvent event) {
        try {
            return objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException error) {
            return "{\"error\":\"failed to serialize domain event\"}";
        }
    }
}

package com.voxops.incident;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/incidents/{incidentId}/events")
public class EventAuditLogController {

    private final EventAuditLogService eventAuditLogService;

    public EventAuditLogController(EventAuditLogService eventAuditLogService) {
        this.eventAuditLogService = eventAuditLogService;
    }

    @GetMapping
    public List<EventAuditLogResponse> listEvents(@PathVariable UUID incidentId) {
        return eventAuditLogService.listForIncident(incidentId);
    }
}

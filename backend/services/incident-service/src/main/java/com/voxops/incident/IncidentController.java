package com.voxops.incident;

import com.voxops.common.ServiceHealthResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/incidents")
public class IncidentController {

    private final IncidentService incidentService;

    public IncidentController(IncidentService incidentService) {
        this.incidentService = incidentService;
    }

    @GetMapping("/health")
    public ServiceHealthResponse health() {
        return ServiceHealthResponse.up("incident-service");
    }

    @GetMapping
    public List<IncidentResponse> listIncidents() {
        return incidentService.listIncidents();
    }

    @GetMapping("/{incidentId}")
    public IncidentResponse getIncident(@PathVariable UUID incidentId) {
        return incidentService.getIncident(incidentId);
    }

    @PostMapping
    public IncidentResponse createIncident(@Valid @RequestBody CreateIncidentRequest request) {
        return incidentService.createIncident(request);
    }

    @PutMapping("/{incidentId}/status")
    public IncidentResponse updateStatus(
            @PathVariable UUID incidentId,
            @Valid @RequestBody UpdateIncidentStatusRequest request
    ) {
        return incidentService.updateStatus(incidentId, request);
    }
}

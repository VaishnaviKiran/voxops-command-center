package com.voxops.incident;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/incidents/{incidentId}/runbooks")
public class RunbookController {

    private final RunbookRetrievalService runbookRetrievalService;

    public RunbookController(RunbookRetrievalService runbookRetrievalService) {
        this.runbookRetrievalService = runbookRetrievalService;
    }

    @GetMapping
    public RunbookSearchResponse searchRunbooks(@PathVariable UUID incidentId) {
        return runbookRetrievalService.searchForIncident(incidentId);
    }
}

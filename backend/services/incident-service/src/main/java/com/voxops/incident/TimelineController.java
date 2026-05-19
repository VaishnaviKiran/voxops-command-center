package com.voxops.incident;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/incidents/{incidentId}/timeline")
public class TimelineController {

    private final TimelineService timelineService;

    public TimelineController(TimelineService timelineService) {
        this.timelineService = timelineService;
    }

    @GetMapping
    public List<TimelineEventResponse> listTimeline(@PathVariable UUID incidentId) {
        return timelineService.listTimeline(incidentId);
    }

    @PostMapping
    public TimelineEventResponse createTimelineEvent(
            @PathVariable UUID incidentId,
            @Valid @RequestBody CreateTimelineEventRequest request
    ) {
        return timelineService.createTimelineEvent(incidentId, request);
    }
}

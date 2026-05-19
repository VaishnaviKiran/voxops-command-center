package com.voxops.incident;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/incidents/{incidentId}/postmortems")
public class PostmortemController {

    private final PostmortemService postmortemService;

    public PostmortemController(PostmortemService postmortemService) {
        this.postmortemService = postmortemService;
    }

    @GetMapping
    public List<PostmortemResponse> listPostmortems(@PathVariable UUID incidentId) {
        return postmortemService.listPostmortems(incidentId);
    }

    @PostMapping("/generate")
    public PostmortemResponse generatePostmortem(@PathVariable UUID incidentId) {
        return postmortemService.generatePostmortem(incidentId);
    }
}

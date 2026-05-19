package com.voxops.incident;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/incidents/{incidentId}/recommendations")
public class AiRecommendationController {

    private final AiRecommendationService aiRecommendationService;

    public AiRecommendationController(AiRecommendationService aiRecommendationService) {
        this.aiRecommendationService = aiRecommendationService;
    }

    @GetMapping
    public List<AiRecommendationResponse> listRecommendations(@PathVariable UUID incidentId) {
        return aiRecommendationService.listRecommendations(incidentId);
    }

    @PostMapping("/generate")
    public AiRecommendationResponse generateRecommendation(@PathVariable UUID incidentId) {
        return aiRecommendationService.generateRecommendation(incidentId);
    }
}

package com.voxops.rag;

import com.voxops.common.ServiceHealthResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/knowledge")
public class RagController {

    @GetMapping("/health")
    public ServiceHealthResponse health() {
        return ServiceHealthResponse.up("rag-service");
    }

    @GetMapping("/search")
    public SearchResponse search(@RequestParam String query) {
        return new SearchResponse(query, List.of("RAG pipeline placeholder. Vector search will be added next."));
    }

    public record SearchResponse(String query, List<String> results) {
    }
}

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
@RequestMapping("/api/incidents/{incidentId}/transcripts")
public class TranscriptController {

    private final TranscriptService transcriptService;

    public TranscriptController(TranscriptService transcriptService) {
        this.transcriptService = transcriptService;
    }

    @GetMapping
    public List<TranscriptSegmentResponse> listTranscripts(@PathVariable UUID incidentId) {
        return transcriptService.listTranscripts(incidentId);
    }

    @PostMapping
    public TranscriptSegmentResponse createTranscript(
            @PathVariable UUID incidentId,
            @Valid @RequestBody CreateTranscriptSegmentRequest request
    ) {
        return transcriptService.createTranscript(incidentId, request);
    }
}

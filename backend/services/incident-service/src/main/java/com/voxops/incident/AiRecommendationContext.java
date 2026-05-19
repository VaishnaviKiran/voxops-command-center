package com.voxops.incident;

import java.util.List;

public record AiRecommendationContext(
        Incident incident,
        List<TimelineEvent> timeline,
        List<TranscriptSegment> transcripts,
        List<RunbookMatch> runbooks
) {
}

package com.voxops.incident;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateTimelineEventRequest(
        @NotNull
        TimelineEventType eventType,

        @NotBlank
        @Size(max = 2000)
        String summary,

        @Size(max = 80)
        String source
) {
}

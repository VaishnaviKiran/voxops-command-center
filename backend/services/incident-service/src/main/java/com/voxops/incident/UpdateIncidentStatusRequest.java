package com.voxops.incident;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateIncidentStatusRequest(
        @NotNull
        IncidentStatus status,

        @Size(max = 1000)
        String note
) {
}

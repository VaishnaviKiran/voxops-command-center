package com.voxops.incident;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateIncidentRequest(
        @NotBlank
        @Size(max = 255)
        String title,

        @NotNull
        IncidentSeverity severity
) {
}

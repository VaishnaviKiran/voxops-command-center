package com.voxops.incident;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record CreateTranscriptSegmentRequest(
        @Size(max = 120)
        String speakerLabel,

        @NotBlank
        @Size(max = 4000)
        String text,

        @DecimalMin("0.0")
        @DecimalMax("1.0")
        BigDecimal confidence
) {
}

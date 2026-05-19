package com.voxops.incident;

import java.math.BigDecimal;
import java.math.RoundingMode;

public record RunbookMatch(
        String id,
        String slug,
        String title,
        String serviceName,
        String tags,
        String excerpt,
        BigDecimal score,
        String citation
) {
    public static RunbookMatch from(RunbookDocument document, double score, String excerpt) {
        return new RunbookMatch(
                document.getId().toString(),
                document.getSlug(),
                document.getTitle(),
                document.getServiceName(),
                document.getTags(),
                excerpt,
                BigDecimal.valueOf(score).setScale(2, RoundingMode.HALF_UP),
                "runbook:" + document.getSlug()
        );
    }
}

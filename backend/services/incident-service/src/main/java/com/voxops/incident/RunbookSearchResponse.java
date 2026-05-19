package com.voxops.incident;

import java.util.List;
import java.util.UUID;

public record RunbookSearchResponse(
        UUID incidentId,
        String query,
        List<RunbookMatch> matches
) {
}

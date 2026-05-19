package com.voxops.common;

import java.time.Instant;

public record ServiceHealthResponse(
        String service,
        String status,
        Instant checkedAt
) {
    public static ServiceHealthResponse up(String service) {
        return new ServiceHealthResponse(service, "UP", Instant.now());
    }
}

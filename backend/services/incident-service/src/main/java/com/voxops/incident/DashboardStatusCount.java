package com.voxops.incident;

public record DashboardStatusCount(
        IncidentStatus status,
        long count
) {
}

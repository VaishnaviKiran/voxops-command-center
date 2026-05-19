package com.voxops.incident;

import java.util.List;

public record DashboardSummaryResponse(
        List<DashboardStatusCount> incidentsByStatus,
        DashboardMetrics metrics,
        List<DashboardRecentTimelineEvent> recentTimelineEvents,
        List<DashboardRecentDomainEvent> recentDomainEvents
) {
}

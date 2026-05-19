package com.voxops.incident;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "voxops.events")
public record EventTopicProperties(
        boolean enabled,
        Topics topics
) {
    public Topics activeTopics() {
        return topics == null ? new Topics(null, null, null, null, null, null) : topics;
    }

    public record Topics(
            String incidentCreated,
            String incidentStatusChanged,
            String timelineCreated,
            String transcriptCreated,
            String recommendationCreated,
            String postmortemCreated
    ) {
        public String incidentCreatedTopic() {
            return incidentCreated == null || incidentCreated.isBlank() ? "incident.created" : incidentCreated;
        }

        public String incidentStatusChangedTopic() {
            return incidentStatusChanged == null || incidentStatusChanged.isBlank()
                    ? "incident.status.changed"
                    : incidentStatusChanged;
        }

        public String timelineCreatedTopic() {
            return timelineCreated == null || timelineCreated.isBlank() ? "timeline.event.created" : timelineCreated;
        }

        public String transcriptCreatedTopic() {
            return transcriptCreated == null || transcriptCreated.isBlank() ? "transcript.segment.created" : transcriptCreated;
        }

        public String recommendationCreatedTopic() {
            return recommendationCreated == null || recommendationCreated.isBlank()
                    ? "ai.recommendation.created"
                    : recommendationCreated;
        }

        public String postmortemCreatedTopic() {
            return postmortemCreated == null || postmortemCreated.isBlank()
                    ? "postmortem.created"
                    : postmortemCreated;
        }
    }
}

package com.voxops.incident;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class DomainEventPublisher {

    private static final Logger LOGGER = LoggerFactory.getLogger(DomainEventPublisher.class);

    private final Optional<KafkaTemplate<String, DomainEvent>> kafkaTemplate;
    private final EventTopicProperties eventTopicProperties;
    private final EventAuditLogService eventAuditLogService;
    private final MeterRegistry meterRegistry;

    public DomainEventPublisher(
            Optional<KafkaTemplate<String, DomainEvent>> kafkaTemplate,
            EventTopicProperties eventTopicProperties,
            EventAuditLogService eventAuditLogService,
            MeterRegistry meterRegistry
    ) {
        this.kafkaTemplate = kafkaTemplate;
        this.eventTopicProperties = eventTopicProperties;
        this.eventAuditLogService = eventAuditLogService;
        this.meterRegistry = meterRegistry;
    }

    public void publish(String topic, String key, DomainEvent event) {
        Timer.Sample sample = Timer.start(meterRegistry);
        EventPublishStatus initialStatus = eventTopicProperties.enabled()
                ? EventPublishStatus.PENDING
                : EventPublishStatus.DISABLED;
        EventAuditLog auditLog = eventAuditLogService.record(topic, key, event, initialStatus);

        if (!eventTopicProperties.enabled()) {
            meterRegistry.counter(
                    "voxops_kafka_events_total",
                    "topic", topic,
                    "event_type", event.eventType(),
                    "status", EventPublishStatus.DISABLED.name()
            ).increment();
            LOGGER.info("Kafka event publishing disabled: eventType={}, aggregateId={}", event.eventType(), event.aggregateId());
            return;
        }

        if (kafkaTemplate.isEmpty()) {
            meterRegistry.counter(
                    "voxops_kafka_events_total",
                    "topic", topic,
                    "event_type", event.eventType(),
                    "status", EventPublishStatus.FAILED.name()
            ).increment();
            eventAuditLogService.markFailed(auditLog.getId(), "Kafka is not configured");
            LOGGER.warn(
                    "Kafka publishing enabled but broker client is unavailable: eventType={}, aggregateId={}",
                    event.eventType(),
                    event.aggregateId()
            );
            return;
        }

        kafkaTemplate.get().send(topic, key, event)
                .whenComplete((result, error) -> {
                    if (error != null) {
                        sample.stop(Timer.builder("voxops_kafka_publish_seconds")
                                .tag("topic", topic)
                                .tag("event_type", event.eventType())
                                .tag("status", EventPublishStatus.FAILED.name())
                                .register(meterRegistry));
                        meterRegistry.counter(
                                "voxops_kafka_events_total",
                                "topic", topic,
                                "event_type", event.eventType(),
                                "status", EventPublishStatus.FAILED.name()
                        ).increment();
                        eventAuditLogService.markFailed(auditLog.getId(), error.getMessage());
                        LOGGER.warn(
                                "Failed to publish Kafka event: topic={}, eventType={}, aggregateId={}",
                                topic,
                                event.eventType(),
                                event.aggregateId(),
                                error
                        );
                    } else {
                        sample.stop(Timer.builder("voxops_kafka_publish_seconds")
                                .tag("topic", topic)
                                .tag("event_type", event.eventType())
                                .tag("status", EventPublishStatus.PUBLISHED.name())
                                .register(meterRegistry));
                        meterRegistry.counter(
                                "voxops_kafka_events_total",
                                "topic", topic,
                                "event_type", event.eventType(),
                                "status", EventPublishStatus.PUBLISHED.name()
                        ).increment();
                        eventAuditLogService.markPublished(
                                auditLog.getId(),
                                result.getRecordMetadata().partition(),
                                result.getRecordMetadata().offset()
                        );
                        LOGGER.info(
                                "Published Kafka event: topic={}, partition={}, offset={}, eventType={}, aggregateId={}",
                                topic,
                                result.getRecordMetadata().partition(),
                                result.getRecordMetadata().offset(),
                                event.eventType(),
                                event.aggregateId()
                        );
                    }
                });
    }
}

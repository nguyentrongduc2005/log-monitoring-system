package com.vdt.log_monitoring.modules.processing.internal.publisher;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import com.vdt.log_monitoring.modules.processing.api.events.CriticalLogDetectedEvent;
import com.vdt.log_monitoring.modules.processing.internal.model.ProcessedLog;

@Component
public class CriticalLogDetectedPublisher extends AbstractKafkaProcessingEventPublisher<CriticalLogDetectedEvent> {

    public CriticalLogDetectedPublisher(
            KafkaTemplate<String, CriticalLogDetectedEvent> kafkaTemplate,
            @Value("${app.kafka.topics.alerts-critical}") String criticalAlertTopic,
            @Value("${app.kafka.publish-timeout}") Duration publishTimeout) {
        super(kafkaTemplate, criticalAlertTopic, publishTimeout);
    }

    public void publish(ProcessedLog log) {
        CriticalLogDetectedEvent event = new CriticalLogDetectedEvent(
                log.eventId(),
                log.ingestionId(),
                log.applicationId(),
                log.applicationName(),
                log.applicationDisplayName(),
                log.traceId(),
                log.level().name(),
                log.message(),
                log.fingerprint().value(),
                log.logTimestamp(),
                log.processedAt(),
                CriticalLogDetectedEvent.CURRENT_SCHEMA_VERSION);

        publish(log.applicationId().toString(), event);
    }
}

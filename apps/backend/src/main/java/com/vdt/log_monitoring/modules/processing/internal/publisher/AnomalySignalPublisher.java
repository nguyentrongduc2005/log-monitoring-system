package com.vdt.log_monitoring.modules.processing.internal.publisher;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import com.vdt.log_monitoring.modules.processing.api.events.AnomalySignalEvent;
import com.vdt.log_monitoring.modules.processing.internal.model.ProcessedLog;

@Component
public class AnomalySignalPublisher extends AbstractKafkaProcessingEventPublisher<AnomalySignalEvent> {

    public AnomalySignalPublisher(
            KafkaTemplate<String, AnomalySignalEvent> kafkaTemplate,
            @Value("${app.kafka.topics.anomaly-signals}") String anomalySignalsTopic,
            @Value("${app.kafka.publish-timeout}") Duration publishTimeout) {
        super(kafkaTemplate, anomalySignalsTopic, publishTimeout);
    }

    public void publish(ProcessedLog log, String matchedRule) {
        AnomalySignalEvent event = AnomalySignalEvent.builder()
            .applicationId(log.applicationId())
            .timestamp(log.logTimestamp())
            .logId(log.eventId())
            .level(log.level().name())
            .matchedRule(matchedRule)
            .serviceName(log.applicationName())
            .build();

        publish(log.applicationId().toString(), event);
    }
}

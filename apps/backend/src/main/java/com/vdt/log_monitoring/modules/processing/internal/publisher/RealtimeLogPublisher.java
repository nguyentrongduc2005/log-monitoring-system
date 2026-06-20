package com.vdt.log_monitoring.modules.processing.internal.publisher;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import com.vdt.log_monitoring.modules.processing.api.events.RealtimeLogEvent;
import com.vdt.log_monitoring.modules.processing.internal.model.ProcessedLog;

@Component
public class RealtimeLogPublisher extends AbstractKafkaProcessingEventPublisher<RealtimeLogEvent> {

    public RealtimeLogPublisher(
            KafkaTemplate<String, RealtimeLogEvent> kafkaTemplate,
            @Value("${app.kafka.topics.live}") String liveLogTopic,
            @Value("${app.kafka.publish-timeout}") Duration publishTimeout) {
        super(kafkaTemplate, liveLogTopic, publishTimeout);
    }

    public void publish(ProcessedLog log) {
        RealtimeLogEvent event = new RealtimeLogEvent(
                log.eventId(),
                log.ingestionId(),
                log.applicationId(),
                log.applicationName(),
                log.applicationDisplayName(),
                log.level().name(),
                log.message(),
                log.traceId(),
                log.logTimestamp(),
                log.processedAt(),
                RealtimeLogEvent.CURRENT_SCHEMA_VERSION);

        publish(log.applicationId().toString(), event);
    }
}

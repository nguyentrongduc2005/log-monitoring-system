package com.vdt.log_monitoring.modules.processing.internal.publisher;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import com.vdt.log_monitoring.modules.processing.api.events.LogProcessingFailedEvent;

@Component
public class ProcessingErrorPublisher extends AbstractKafkaProcessingEventPublisher<LogProcessingFailedEvent> {

    public ProcessingErrorPublisher(
            KafkaTemplate<String, LogProcessingFailedEvent> kafkaTemplate,
            @Value("${app.kafka.topics.processing-errors}") String processingErrorsTopic,
            @Value("${app.kafka.publish-timeout}") Duration publishTimeout) {
        super(kafkaTemplate, processingErrorsTopic, publishTimeout);
    }

    public void publish(LogProcessingFailedEvent event) {
        publish(event.applicationId().toString(), event);
    }
}

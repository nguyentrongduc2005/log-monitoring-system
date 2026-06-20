package com.vdt.log_monitoring.modules.realtime.internal.publisher;

import java.time.Duration;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import com.vdt.log_monitoring.modules.processing.api.events.LogProcessingFailedEvent;

@Component
public class RealtimeErrorPublisher {

    private final KafkaTemplate<String, LogProcessingFailedEvent> kafkaTemplate;
    private final String processingErrorsTopic;
    private final Duration publishTimeout;

    public RealtimeErrorPublisher(
            KafkaTemplate<String, LogProcessingFailedEvent> kafkaTemplate,
            @Value("${app.kafka.topics.processing-errors}") String processingErrorsTopic,
            @Value("${app.kafka.publish-timeout}") Duration publishTimeout) {
        this.kafkaTemplate = kafkaTemplate;
        this.processingErrorsTopic = processingErrorsTopic;
        this.publishTimeout = publishTimeout;
    }

    public void publish(LogProcessingFailedEvent event) {
        try {
            kafkaTemplate
                    .send(processingErrorsTopic, event.applicationId().toString(), event)
                    .get(publishTimeout.toMillis(), TimeUnit.MILLISECONDS);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while publishing realtime failure event", ex);
        } catch (ExecutionException | TimeoutException ex) {
            throw new IllegalStateException("Failed to publish realtime failure event", ex);
        }
    }
}

package com.vdt.log_monitoring.modules.ingestion.internal.impl;

import java.time.Duration;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import com.vdt.log_monitoring.modules.ingestion.api.IngestionException;
import com.vdt.log_monitoring.modules.ingestion.api.events.RawLogReceivedEvent;
import com.vdt.log_monitoring.modules.ingestion.internal.RawLogPublisher;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class KafkaRawLogPublisher implements RawLogPublisher {
    private final KafkaTemplate<String, RawLogReceivedEvent> kafkaTemplate;
    @Value("${app.kafka.topics.raw}")
    private String rawLogTopic;
    @Value("${app.kafka.publish-timeout}")
    private Duration publishTimeout;

    @Override
    public void publish(RawLogReceivedEvent event) {
        String key = event.applicationId().toString();

        try {
            kafkaTemplate
                    .send(rawLogTopic, key, event)
                    .get(publishTimeout.toMillis(), TimeUnit.MILLISECONDS);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw ingestionUnavailable("Interrupted while publishing raw log event", ex);
        } catch (ExecutionException | TimeoutException ex) {
            throw ingestionUnavailable("Failed to publish raw log event", ex);
        }
    }

    private IngestionException ingestionUnavailable(String message, Exception cause) {
        IngestionException exception = new IngestionException(
                IngestionException.ErrorCode.INGESTION_UNAVAILABLE,
                message);
        exception.initCause(cause);
        return exception;
    }
}

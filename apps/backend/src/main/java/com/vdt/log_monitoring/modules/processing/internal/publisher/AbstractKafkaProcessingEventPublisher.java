package com.vdt.log_monitoring.modules.processing.internal.publisher;

import java.time.Duration;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;

import com.vdt.log_monitoring.modules.processing.api.ProcessingException;

abstract class AbstractKafkaProcessingEventPublisher<T> {

    private static final Logger log = LoggerFactory.getLogger(AbstractKafkaProcessingEventPublisher.class);

    private final KafkaTemplate<String, T> kafkaTemplate;
    private final String topic;
    private final Duration publishTimeout;

    protected AbstractKafkaProcessingEventPublisher(
            KafkaTemplate<String, T> kafkaTemplate,
            String topic,
            Duration publishTimeout) {
        this.kafkaTemplate = kafkaTemplate;
        this.topic = topic;
        this.publishTimeout = publishTimeout;
    }

    protected void publish(String key, T event) {
        try {
            kafkaTemplate
                    .send(topic, key, event)
                    .get(publishTimeout.toMillis(), TimeUnit.MILLISECONDS);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw eventPublishFailed("Interrupted while publishing processing event to " + topic, ex);
        } catch (ExecutionException | TimeoutException ex) {
            throw eventPublishFailed("Failed to publish processing event to " + topic, ex);
        }
    }

    protected void publishAsync(String key, T event) {
        kafkaTemplate.send(topic, key, event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.warn("Failed to publish processing event asynchronously to topic={}", topic, ex);
                    }
                });
    }

    private ProcessingException eventPublishFailed(String message, Exception cause) {
        return new ProcessingException(
                ProcessingException.ErrorCode.DOWNSTREAM_EVENT_PUBLISH_FAILED,
                message,
                cause);
    }
}

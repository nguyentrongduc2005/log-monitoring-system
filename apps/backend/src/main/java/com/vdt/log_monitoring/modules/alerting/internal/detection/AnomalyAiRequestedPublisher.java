package com.vdt.log_monitoring.modules.alerting.internal.detection;

import java.time.Duration;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import com.vdt.log_monitoring.modules.anomaly.api.events.AnomalyAiRequestedEvent;

@Component
public class AnomalyAiRequestedPublisher {

	private final KafkaTemplate<String, AnomalyAiRequestedEvent> kafkaTemplate;
	private final String topic;
	private final Duration publishTimeout;

	public AnomalyAiRequestedPublisher(
		KafkaTemplate<String, AnomalyAiRequestedEvent> kafkaTemplate,
		@Value("${app.kafka.topics.anomaly-ai-requested}") String topic,
		@Value("${app.kafka.publish-timeout}") Duration publishTimeout
	) {
		this.kafkaTemplate = kafkaTemplate;
		this.topic = topic;
		this.publishTimeout = publishTimeout;
	}

	public void publish(AnomalyAiRequestedEvent event) {
		try {
			kafkaTemplate
				.send(topic, event.applicationId().toString(), event)
				.get(publishTimeout.toMillis(), TimeUnit.MILLISECONDS);
		} catch (InterruptedException exception) {
			Thread.currentThread().interrupt();
			throw new IllegalStateException("Interrupted while publishing anomaly AI request", exception);
		} catch (ExecutionException | TimeoutException exception) {
			throw new IllegalStateException("Failed to publish anomaly AI request", exception);
		}
	}
}

package com.vdt.log_monitoring.modules.anomaly.internal.publisher;

import java.time.Duration;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import com.vdt.log_monitoring.modules.anomaly.api.AnomalyException;
import com.vdt.log_monitoring.modules.anomaly.api.events.AnomalyDetectedEvent;

@Component
public class AnomalyDetectedPublisher {

	private final KafkaTemplate<String, AnomalyDetectedEvent> kafkaTemplate;
	private final String topic;
	private final Duration publishTimeout;

	public AnomalyDetectedPublisher(
		KafkaTemplate<String, AnomalyDetectedEvent> kafkaTemplate,
		@Value("${app.kafka.topics.anomaly-detected}") String topic,
		@Value("${app.kafka.publish-timeout}") Duration publishTimeout
	) {
		this.kafkaTemplate = kafkaTemplate;
		this.topic = topic;
		this.publishTimeout = publishTimeout;
	}

	public void publish(AnomalyDetectedEvent event) {
		publish(event.applicationId().toString(), event);
	}

	private void publish(String key, AnomalyDetectedEvent event) {
		try {
			kafkaTemplate.send(topic, key, event).get(publishTimeout.toMillis(), TimeUnit.MILLISECONDS);
		} catch (InterruptedException exception) {
			Thread.currentThread().interrupt();
			throw publishFailed(exception);
		} catch (ExecutionException | TimeoutException exception) {
			throw publishFailed(exception);
		}
	}

	private AnomalyException publishFailed(Exception cause) {
		return new AnomalyException(
			AnomalyException.ErrorCode.ANOMALY_PROCESSING_FAILED,
			"Failed to publish anomaly detected event",
			cause);
	}
}

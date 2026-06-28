package com.vdt.log_monitoring.modules.anomaly.internal.publisher;

import java.time.Duration;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import com.vdt.log_monitoring.modules.anomaly.api.AnomalyException;
import com.vdt.log_monitoring.modules.anomaly.api.events.AnomalyReportUpdatedEvent;

@Component
public class AnomalyReportUpdatedPublisher {

	private final KafkaTemplate<String, AnomalyReportUpdatedEvent> kafkaTemplate;
	private final String topic;
	private final Duration publishTimeout;

	public AnomalyReportUpdatedPublisher(
		KafkaTemplate<String, AnomalyReportUpdatedEvent> kafkaTemplate,
		@Value("${app.kafka.topics.anomaly-report-updated}") String topic,
		@Value("${app.kafka.publish-timeout}") Duration publishTimeout
	) {
		this.kafkaTemplate = kafkaTemplate;
		this.topic = topic;
		this.publishTimeout = publishTimeout;
	}

	public void publish(AnomalyReportUpdatedEvent event) {
		try {
			kafkaTemplate
				.send(topic, event.applicationId().toString(), event)
				.get(publishTimeout.toMillis(), TimeUnit.MILLISECONDS);
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
			"Failed to publish anomaly report update event",
			cause);
	}
}

package com.vdt.log_monitoring.modules.alerting.internal.detection;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.retry.annotation.Backoff;
import org.springframework.stereotype.Component;

import com.vdt.log_monitoring.modules.anomaly.api.events.AnomalyDetectedEvent;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class AnomalyDetectedConsumer {

	private final AnomalyAlertService anomalyAlertService;

	@RetryableTopic(
		attempts = "${app.kafka.retry.alerts-critical-delivery-attempts}",
		autoCreateTopics = "${app.kafka.retry.auto-create-topics}",
		backoff = @Backoff(delayExpression = "${app.kafka.retry.alerts-critical-delivery-backoff-ms}"),
		dltTopicSuffix = "${app.kafka.retry.dlt-topic-suffix}"
	)
	@KafkaListener(
		topics = "${app.kafka.topics.anomaly-detected}",
		groupId = "${app.kafka.consumer-groups.anomaly-detected-alerting}"
	)
	public void consume(AnomalyDetectedEvent event, Acknowledgment acknowledgment) {
		anomalyAlertService.createOrUpdateAlert(event);
		acknowledgment.acknowledge();
	}
}

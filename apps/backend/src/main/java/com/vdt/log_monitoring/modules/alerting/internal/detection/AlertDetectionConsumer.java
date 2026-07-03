package com.vdt.log_monitoring.modules.alerting.internal.detection;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.retry.annotation.Backoff;
import org.springframework.stereotype.Component;

import com.vdt.log_monitoring.modules.processing.api.events.CriticalLogDetectedEvent;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class AlertDetectionConsumer {

	private final AlertDetectionService alertDetectionService;

	@RetryableTopic(
		attempts = "${app.kafka.retry.alerts-critical-delivery-attempts}",
		autoCreateTopics = "${app.kafka.retry.auto-create-topics}",
		backoff = @Backoff(delayExpression = "${app.kafka.retry.alerts-critical-delivery-backoff-ms}"),
		dltTopicSuffix = "${app.kafka.retry.dlt-topic-suffix}"
	)
	@KafkaListener(
		topics = "${app.kafka.topics.alerts-critical}",
		groupId = "${app.kafka.consumer-groups.alerts-critical}"
	)
	public void consume(CriticalLogDetectedEvent event, Acknowledgment acknowledgment) {
		alertDetectionService.detect(event);
		acknowledgment.acknowledge();
	}
}

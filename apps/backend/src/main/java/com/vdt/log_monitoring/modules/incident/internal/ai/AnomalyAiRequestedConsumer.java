package com.vdt.log_monitoring.modules.incident.internal.ai;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import com.vdt.log_monitoring.modules.anomaly.api.events.AnomalyAiRequestedEvent;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class AnomalyAiRequestedConsumer {

	private final IncidentAnomalyReportAiService anomalyReportAiService;

	@KafkaListener(
		topics = "${app.kafka.topics.anomaly-ai-requested}",
		groupId = "${app.kafka.consumer-groups.anomaly-ai-incident}"
	)
	public void consume(AnomalyAiRequestedEvent event, Acknowledgment acknowledgment) {
		anomalyReportAiService.requestAnalysis(
			event.anomalyReportId(),
			event.alertId(),
			event.triggerReason());
		acknowledgment.acknowledge();
	}
}

package com.vdt.log_monitoring.modules.alerting.internal.detection;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import com.vdt.log_monitoring.modules.anomaly.api.events.AnomalyReportUpdatedEvent;
import com.vdt.log_monitoring.modules.realtime.api.RealtimeFacade;
import com.vdt.log_monitoring.modules.realtime.api.events.AnomalyReportNotificationMessage;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class AnomalyReportUpdatedConsumer {

	private final RealtimeFacade realtimeFacade;

	@KafkaListener(
		topics = "${app.kafka.topics.anomaly-report-updated}",
		groupId = "${app.kafka.consumer-groups.anomaly-detected-alerting}"
	)
	public void consume(AnomalyReportUpdatedEvent event, Acknowledgment acknowledgment) {
		realtimeFacade.publishAnomalyReportNotification(new AnomalyReportNotificationMessage(
			event.anomalyReportId(),
			event.applicationId(),
			event.sourceType(),
			event.updateType(),
			event.aiStatus(),
			event.updatedAt()));
		acknowledgment.acknowledge();
	}
}

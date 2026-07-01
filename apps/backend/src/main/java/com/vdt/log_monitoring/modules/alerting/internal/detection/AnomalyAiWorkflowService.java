package com.vdt.log_monitoring.modules.alerting.internal.detection;

import java.time.Instant;
import java.util.UUID;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.vdt.log_monitoring.modules.alerting.internal.notification.ChatRoomEntity;
import com.vdt.log_monitoring.modules.alerting.internal.notification.ChatRoomRepository;
import com.vdt.log_monitoring.modules.alerting.internal.notification.ChatRoomStatus;
import com.vdt.log_monitoring.modules.alerting.internal.notification.telegram.TelegramNotifier;
import com.vdt.log_monitoring.modules.alerting.internal.rule.AlertChannel;
import com.vdt.log_monitoring.modules.anomaly.api.AnomalyFacade;
import com.vdt.log_monitoring.modules.anomaly.api.events.AnomalyDetectedEvent;
import com.vdt.log_monitoring.modules.incident.api.IncidentFacade;
import com.vdt.log_monitoring.modules.realtime.api.RealtimeFacade;
import com.vdt.log_monitoring.modules.realtime.api.events.AnomalyReportNotificationMessage;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class AnomalyAiWorkflowService {

	private final AnomalyFacade anomalyFacade;
	private final IncidentFacade incidentFacade;
	private final RealtimeFacade realtimeFacade;
	private final ChatRoomRepository chatRoomRepository;
	private final TelegramNotifier telegramNotifier;

	@Async
	public void requestAnalysis(AnomalyDetectedEvent event, UUID alertId) {
		try {
			anomalyFacade.markAiPending(event.anomalyReportId(), event.aiTriggerReason());
			publishNotification(event, "AI_STARTED", "PENDING");

			incidentFacade.requestAnomalyReportAi(
				event.anomalyReportId(),
				alertId,
				event.aiTriggerReason());

			AnomalyFacade.AnomalyReportDto report = anomalyFacade.findReportById(event.anomalyReportId());
			publishNotification(report, updateType(report.aiStatus()));
			
			if ("SUCCEEDED".equals(report.aiStatus())) {
				ChatRoomEntity anomalyRoom = chatRoomRepository
					.findByNameContainingIgnoreCaseAndStatusAndChannel("anomaly", ChatRoomStatus.ACTIVE, AlertChannel.TELEGRAM)
					.stream()
					.findFirst()
					.orElse(null);
				if (anomalyRoom != null) {
					telegramNotifier.notifyAnomalyAiReport(report, anomalyRoom.getChatId());
				}
			}
		} catch (RuntimeException exception) {
			log.warn("Anomaly AI workflow failed reportId={}", event.anomalyReportId(), exception);
			try {
				anomalyFacade.updateAiFailure(event.anomalyReportId(), exception.getMessage());
				AnomalyFacade.AnomalyReportDto report = anomalyFacade.findReportById(event.anomalyReportId());
				publishNotification(report, "AI_FAILED");
			} catch (RuntimeException updateException) {
				log.error("Failed to persist anomaly AI failure reportId={}", event.anomalyReportId(), updateException);
			}
		}
	}

	private void publishNotification(AnomalyDetectedEvent event, String updateType, String aiStatus) {
		realtimeFacade.publishAnomalyReportNotification(new AnomalyReportNotificationMessage(
			event.anomalyReportId(),
			event.applicationId(),
			event.sourceType(),
			updateType,
			aiStatus,
			Instant.now()));
	}

	private void publishNotification(AnomalyFacade.AnomalyReportDto report, String updateType) {
		realtimeFacade.publishAnomalyReportNotification(new AnomalyReportNotificationMessage(
			report.id(),
			report.applicationId(),
			report.sourceType(),
			updateType,
			report.aiStatus(),
			report.updatedAt() == null ? Instant.now() : report.updatedAt()));
	}

	private String updateType(String aiStatus) {
		if ("FAILED".equals(aiStatus)) {
			return "AI_FAILED";
		}
		if ("SUCCEEDED".equals(aiStatus)) {
			return "AI_COMPLETED";
		}
		return "AI_UPDATED";
	}
}

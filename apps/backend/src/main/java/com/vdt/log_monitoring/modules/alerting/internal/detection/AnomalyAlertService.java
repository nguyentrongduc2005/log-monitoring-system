package com.vdt.log_monitoring.modules.alerting.internal.detection;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vdt.log_monitoring.modules.alerting.internal.alert.AlertLogSample;
import com.vdt.log_monitoring.modules.alerting.internal.alert.AlertEntity;
import com.vdt.log_monitoring.modules.alerting.internal.alert.AlertService;
import com.vdt.log_monitoring.modules.alerting.internal.notification.websocket.WebSocketAlertPublisher;
import com.vdt.log_monitoring.modules.alerting.internal.notification.ChatRoomEntity;
import com.vdt.log_monitoring.modules.alerting.internal.notification.ChatRoomRepository;
import com.vdt.log_monitoring.modules.alerting.internal.notification.ChatRoomStatus;
import com.vdt.log_monitoring.modules.alerting.internal.notification.telegram.TelegramNotifier;
import com.vdt.log_monitoring.modules.alerting.internal.rule.AlertChannel;
import com.vdt.log_monitoring.modules.alerting.internal.rule.AlertDeliveryTarget;
import com.vdt.log_monitoring.modules.anomaly.api.AnomalyFacade;
import com.vdt.log_monitoring.modules.anomaly.api.events.AnomalyDetectedEvent;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AnomalyAlertService {

	private static final String UNKNOWN_LOG_LEVEL = "UNKNOWN";

	private final AlertService alertService;
	private final WebSocketAlertPublisher webSocketAlertPublisher;
	private final AnomalyFacade anomalyFacade;
	private final AnomalyAiWorkflowService anomalyAiWorkflowService;
	private final ObjectMapper objectMapper;
	private final ChatRoomRepository chatRoomRepository;
	private final TelegramNotifier telegramNotifier;

	@Transactional
	public AlertEntity createOrUpdateAlert(AnomalyDetectedEvent event) {
		AnomalyFacade.AnomalyReportDto report = anomalyFacade.findReportById(event.anomalyReportId());
		
		Set<AlertDeliveryTarget> deliveryTargets = new java.util.HashSet<>();
		deliveryTargets.add(AlertDeliveryTarget.channelOnly(AlertChannel.WEBSOCKET));

		ChatRoomEntity anomalyRoom = chatRoomRepository
			.findByNameContainingIgnoreCaseAndStatusAndChannel("anomaly", ChatRoomStatus.ACTIVE, AlertChannel.TELEGRAM)
			.stream()
			.findFirst()
			.orElse(null);
			
		if (anomalyRoom != null) {
			deliveryTargets.add(AlertDeliveryTarget.of(AlertChannel.TELEGRAM, anomalyRoom.getId()));
		}

		AlertEntity alert = alertService.createFromAnomaly(
			event,
			deliveryTargets,
			logSamplesFromEvidence(report.evidencePayloadJson()));
		
		webSocketAlertPublisher.publish(alert);
		
		if (anomalyRoom != null) {
			telegramNotifier.notifyAnomalyAlert(alert, anomalyRoom.getChatId());
		}

		anomalyFacade.markAlerted(event.anomalyReportId(), alert.getId());
		if (event.aiTriggerRequested()) {
			anomalyAiWorkflowService.requestAnalysis(event, alert.getId());
		}
		return alert;
	}

	private List<AlertLogSample> logSamplesFromEvidence(String evidencePayloadJson) {
		if (evidencePayloadJson == null || evidencePayloadJson.isBlank()) {
			return List.of();
		}
		try {
			Map<String, Object> evidence = objectMapper.readValue(
				evidencePayloadJson,
				new TypeReference<Map<String, Object>>() {});
			List<AlertLogSample> structuredSamples = structuredLogSamples(evidence.get("logSamples"));
			if (!structuredSamples.isEmpty()) {
				return structuredSamples;
			}
			return legacySampleMessages(evidence.get("sampleMessages"));
		} catch (JsonProcessingException exception) {
			return List.of();
		}
	}

	private List<AlertLogSample> structuredLogSamples(Object logSamples) {
		if (!(logSamples instanceof List<?> samples)) {
			return List.of();
		}
		return samples.stream()
			.filter(Map.class::isInstance)
			.map(Map.class::cast)
			.map(this::toAlertLogSample)
			.filter(sample -> !sample.message().isBlank())
			.limit(5)
			.toList();
	}

	private AlertLogSample toAlertLogSample(Map<?, ?> sample) {
		String level = textOrDefault(sample.get("level"), UNKNOWN_LOG_LEVEL);
		String message = textOrDefault(sample.get("message"), "");
		return new AlertLogSample(level, message);
	}

	private List<AlertLogSample> legacySampleMessages(Object sampleMessages) {
		if (!(sampleMessages instanceof List<?> messages)) {
			return List.of();
		}
		return messages.stream()
			.filter(String.class::isInstance)
			.map(String.class::cast)
			.filter(message -> !message.isBlank())
			.limit(5)
			.map(message -> new AlertLogSample(UNKNOWN_LOG_LEVEL, message))
			.toList();
	}

	private String textOrDefault(Object value, String fallback) {
		if (!(value instanceof String text) || text.isBlank()) {
			return fallback;
		}
		return text;
	}
}

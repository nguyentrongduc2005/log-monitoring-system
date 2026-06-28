package com.vdt.log_monitoring.modules.alerting.internal.alert;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.vdt.log_monitoring.modules.alerting.api.AlertingException;
import com.vdt.log_monitoring.modules.anomaly.api.events.AnomalyDetectedEvent;
import com.vdt.log_monitoring.modules.alerting.internal.rule.AlertDeliveryTarget;
import com.vdt.log_monitoring.modules.alerting.internal.rule.AlertRuleDefinition;
import com.vdt.log_monitoring.modules.alerting.internal.rule.AlertSeverity;

@Service
@RequiredArgsConstructor
public class AlertService {
	private final AlertRepository alertRepository;

	@Transactional
	public AlertEntity trigger(
		AlertRuleDefinition rule,
		AlertOccurrenceData occurrence,
		long initialCount,
		Instant firstSeenAt,
		List<AlertLogSample> logSamples
	) {
		return findActiveOccurrence(rule.id(), occurrence.applicationId())
			.map(alert -> {
				alert.retrigger(occurrence.logTimestamp());
				return alert;
			})
			.orElseGet(() -> {
				AlertEntity newAlert = alertRepository.save(AlertEntity.create(
					rule.id(),
					occurrence.applicationId(),
					occurrence.applicationName(),
					occurrence.applicationDisplayName(),
					rule.name(),
					rule.severity(),
					logSamples,
					firstSeenAt,
					firstSeenAt,
					occurrence.logTimestamp(),
					rule.toDeliveryTargets(),
					initialCount));
				return newAlert;
			});
	}

	@Transactional
	public AlertEntity createFromAnomaly(AnomalyDetectedEvent event, Set<AlertDeliveryTarget> deliveryTargets) {
		return findActiveAnomalyOccurrence(event.sourceType(), event.anomalyReportId())
			.map(alert -> {
				alert.retrigger(event.detectedAt() == null ? Instant.now() : event.detectedAt());
				return alert;
			})
			.orElseGet(() -> alertRepository.save(AlertEntity.createFromAnomaly(
				event.applicationId(),
				event.applicationName(),
				event.applicationDisplayName(),
				event.sourceType(),
				event.anomalyReportId(),
				event.ruleName(),
				AlertSeverity.from(event.severity()),
				summary(event),
				metadata(event),
				event.detectedAt() == null ? Instant.now() : event.detectedAt(),
				event.windowStart(),
				event.windowEnd(),
				deliveryTargets)));
	}

	@Transactional
	public void recordOccurrence(UUID ruleId, UUID applicationId, Instant occurredAt) {
		findActiveOccurrence(ruleId, applicationId)
			.ifPresent(alert -> alert.recordOccurrence(occurredAt));
	}

	@Transactional
	public Optional<AlertEntity> recordOccurrenceOrTrigger(
		AlertRuleDefinition rule,
		AlertOccurrenceData occurrence,
		long initialCount,
		Instant firstSeenAt,
		List<AlertLogSample> logSamples
	) {
		Optional<AlertEntity> activeAlert = findActiveOccurrence(rule.id(), occurrence.applicationId());
		activeAlert.ifPresent(alert -> alert.recordOccurrence(occurrence.logTimestamp()));
		if (activeAlert.isPresent()) {
			return Optional.empty();
		}
		
		AlertEntity newAlert = alertRepository.save(AlertEntity.create(
			rule.id(),
			occurrence.applicationId(),
			occurrence.applicationName(),
			occurrence.applicationDisplayName(),
			rule.name(),
			rule.severity(),
			logSamples,
			firstSeenAt,
			firstSeenAt,
			occurrence.logTimestamp(),
			rule.toDeliveryTargets(),
			initialCount));
		return Optional.of(newAlert);
	}

	@Transactional
	public AlertEntity acknowledgeAlert(UUID alertId, UUID acknowledgedBy) {
		AlertEntity alert = getAlertById(alertId);
		alert.acknowledge(acknowledgedBy);
		return alert;
	}

	@Transactional(readOnly = true)
	public List<AlertEntity> listAlerts(List<UUID> applicationIds, String status, String severity) {
		if (applicationIds == null || applicationIds.isEmpty()) {
			return List.of();
		}
		AlertStatus parsedStatus = parseOptionalStatus(status);
		AlertSeverity parsedSeverity = parseOptionalSeverity(severity);
		return alertRepository.findByApplicationIdInOrderByTriggeredAtDesc(applicationIds).stream()
			.filter(alert -> parsedStatus == null || alert.getStatus() == parsedStatus)
			.filter(alert -> parsedSeverity == null || alert.getSeverity() == parsedSeverity)
			.toList();
	}

	@Transactional(readOnly = true)
	public List<AlertEntity> findAlertsInWindow(
		UUID applicationId,
		Instant windowStart,
		Instant windowEnd
	) {
		if (applicationId == null || windowStart == null || windowEnd == null || windowStart.isAfter(windowEnd)) {
			return List.of();
		}
		return alertRepository.findByApplicationIdAndFirstSeenAtBetweenOrderByTriggeredAtDesc(
			applicationId,
			windowStart,
			windowEnd);
	}

	@Transactional
	public AlertEntity resolveAlert(UUID alertId, UUID resolvedBy) {
		AlertEntity alert = getAlertById(alertId);
		alert.resolve(resolvedBy);
		return alert;
	}

	@Transactional(readOnly = true)
	public AlertEntity getAlertById(UUID alertId) {
		return alertRepository.findById(alertId)
			.orElseThrow(() -> new AlertingException(
				AlertingException.ErrorCode.ALERT_NOT_FOUND,
				"Alert not found"));
	}

	private Optional<AlertEntity> findActiveOccurrence(UUID ruleId, UUID applicationId) {
		return alertRepository.findFirstByRuleIdAndApplicationIdAndStatusNotOrderByTriggeredAtDesc(
			ruleId, applicationId, AlertStatus.RESOLVED);
	}

	private Optional<AlertEntity> findActiveAnomalyOccurrence(String triggerType, UUID sourceId) {
		return alertRepository.findFirstByTriggerTypeAndSourceIdAndStatusNotOrderByTriggeredAtDesc(
			triggerType,
			sourceId,
			AlertStatus.RESOLVED);
	}

	private String summary(AnomalyDetectedEvent event) {
		if (event.summary() == null || event.summary().isBlank()) {
			return event.title();
		}
		if (event.title() == null || event.title().isBlank()) {
			return event.summary();
		}
		return event.title().trim() + ": " + event.summary().trim();
	}

	private String metadata(AnomalyDetectedEvent event) {
		return "{"
			+ "\"kind\":\"ANOMALY_REPORT\","
			+ "\"anomalyReportId\":\"" + event.anomalyReportId() + "\","
			+ "\"sourceType\":\"" + escape(event.sourceType()) + "\","
			+ "\"ruleName\":\"" + escape(event.ruleName()) + "\","
			+ "\"windowStart\":\"" + event.windowStart() + "\","
			+ "\"windowEnd\":\"" + event.windowEnd() + "\","
			+ "\"aiTriggerRequested\":" + event.aiTriggerRequested() + ","
			+ "\"aiTriggerReason\":\"" + escape(event.aiTriggerReason()) + "\""
			+ "}";
	}

	private String escape(String value) {
		return value == null ? "" : value.replace("\\", "\\\\").replace("\"", "\\\"");
	}

	private AlertStatus parseOptionalStatus(String status) {
		if (status == null || status.isBlank()) {
			return null;
		}
		try {
			return AlertStatus.valueOf(status.trim().toUpperCase(Locale.ROOT));
		} catch (IllegalArgumentException exception) {
			throw new AlertingException(AlertingException.ErrorCode.INVALID_ALERT_STATUS, "Invalid alert status");
		}
	}

	private AlertSeverity parseOptionalSeverity(String severity) {
		if (severity == null || severity.isBlank()) {
			return null;
		}
		try {
			return AlertSeverity.from(severity);
		} catch (IllegalArgumentException exception) {
			throw new AlertingException(AlertingException.ErrorCode.INVALID_ALERT_SEVERITY, "Invalid alert severity");
		}
	}
}

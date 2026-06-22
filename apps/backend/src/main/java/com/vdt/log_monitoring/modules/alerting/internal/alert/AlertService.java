package com.vdt.log_monitoring.modules.alerting.internal.alert;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.vdt.log_monitoring.modules.alerting.api.AlertingException;
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
		AlertSeverity severity,
		long initialCount,
		Instant firstSeenAt
	) {
		return findActiveOccurrence(rule.id(), occurrence.fingerprint())
			.map(alert -> {
				alert.retrigger(occurrence.logTimestamp());
				return alert;
			})
			.orElseGet(() -> alertRepository.save(AlertEntity.create(
				rule.id(),
				occurrence.applicationId(),
				occurrence.eventId(),
				occurrence.ingestionId(),
				occurrence.applicationName(),
				occurrence.applicationDisplayName(),
				severity,
				occurrence.message(),
				occurrence.fingerprint(),
				occurrence.logTimestamp(),
				rule.toDeliveryTargets(),
				initialCount,
				firstSeenAt)));
	}

	@Transactional
	public void recordOccurrence(UUID ruleId, String fingerprint, Instant occurredAt) {
		findActiveOccurrence(ruleId, fingerprint)
			.ifPresent(alert -> alert.recordOccurrence(occurredAt));
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

	private Optional<AlertEntity> findActiveOccurrence(UUID ruleId, String fingerprint) {
		return alertRepository.findFirstByRuleIdAndFingerprintAndStatusNotOrderByTriggeredAtDesc(
			ruleId, fingerprint, AlertStatus.RESOLVED);
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

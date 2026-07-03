package com.vdt.log_monitoring.modules.anomaly.internal;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.vdt.log_monitoring.modules.anomaly.api.AnomalyFacade;
import com.vdt.log_monitoring.modules.anomaly.internal.report.AnomalyReportEntity;
import com.vdt.log_monitoring.modules.anomaly.internal.report.AnomalyReportService;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class AnomalyFacadeImpl implements AnomalyFacade {

	private final AnomalyReportService anomalyReportService;

	@Override
	@Transactional(readOnly = true)
	public AnomalyReportDto findReportById(UUID id) {
		return mapReport(anomalyReportService.getReportById(id));
	}

	@Override
	@Transactional(readOnly = true)
	public List<AnomalyReportDto> findReports(List<UUID> visibleApplicationIds) {
		return anomalyReportService.listReports(visibleApplicationIds).stream()
			.map(this::mapReport)
			.toList();
	}

	@Override
	@Transactional
	public AnomalyReportDto createReport(CreateAnomalyReportCommand command) {
		return mapReport(anomalyReportService.createReport(command));
	}

	@Override
	@Transactional
	public AnomalyReportDto createOrUpdateReport(CreateAnomalyReportCommand command) {
		return mapReport(anomalyReportService.createOrUpdateReport(command));
	}

	@Override
	@Transactional
	public void markAlerted(UUID reportId, UUID alertId) {
		anomalyReportService.markAlerted(reportId, alertId);
	}

	@Override
	@Transactional
	public void markAiPending(UUID reportId, String reason) {
		anomalyReportService.markAiPending(reportId, reason);
	}

	@Override
	@Transactional
	public void updateAiResult(UUID reportId, AnomalyAiResult result) {
		anomalyReportService.updateAiResult(reportId, result);
	}

	@Override
	@Transactional
	public void updateAiFailure(UUID reportId, String error) {
		anomalyReportService.updateAiFailure(reportId, error);
	}

	@Override
	@Transactional
	public AnomalyReportDto resolveReport(UUID reportId, UUID resolvedBy) {
		return mapReport(anomalyReportService.resolveReport(reportId, resolvedBy));
	}

	private AnomalyReportDto mapReport(AnomalyReportEntity report) {
		return new AnomalyReportDto(
			report.getId(),
			report.getApplicationId(),
			report.getAlertId(),
			report.getSourceType(),
			report.getRuleName(),
			report.getFingerprint(),
			report.getSeverity(),
			report.getStatus(),
			report.getTitle(),
			report.getSummary(),
			report.getHypothesis(),
			report.getConfidenceScore(),
			report.getWindowStart(),
			report.getWindowEnd(),
			report.getOccurrenceCount(),
			report.getFirstSeenAt(),
			report.getLastSeenAt(),
			report.getEvidencePayloadJson(),
			report.isAiTriggerRequested(),
			report.getAiTriggerReason(),
			report.getAiStatus(),
			report.getAiStartedAt(),
			report.getAiCompletedAt(),
			report.getAiResultJson(),
			report.getAiError(),
			report.getResolvedBy(),
			report.getResolvedAt(),
			report.getCreatedAt(),
			report.getUpdatedAt());
	}
}

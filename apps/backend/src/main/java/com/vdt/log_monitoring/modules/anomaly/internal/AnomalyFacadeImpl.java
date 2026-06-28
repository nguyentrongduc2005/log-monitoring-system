package com.vdt.log_monitoring.modules.anomaly.internal;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.vdt.log_monitoring.modules.anomaly.api.AnomalyFacade;
import com.vdt.log_monitoring.modules.anomaly.api.events.AnomalyReportUpdatedEvent;
import com.vdt.log_monitoring.modules.anomaly.internal.publisher.AnomalyReportUpdatedPublisher;
import com.vdt.log_monitoring.modules.anomaly.internal.report.AnomalyReportEntity;
import com.vdt.log_monitoring.modules.anomaly.internal.report.AnomalyReportService;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class AnomalyFacadeImpl implements AnomalyFacade {

	private final AnomalyReportService anomalyReportService;
	private final AnomalyReportUpdatedPublisher anomalyReportUpdatedPublisher;

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
	public void markAlerted(UUID reportId, UUID alertId) {
		anomalyReportService.markAlerted(reportId, alertId);
	}

	@Override
	@Transactional
	public void markAiPending(UUID reportId, String reason) {
		publishUpdate(anomalyReportService.markAiPending(reportId, reason), "AI_STARTED");
	}

	@Override
	@Transactional
	public void updateAiResult(UUID reportId, AnomalyAiResult result) {
		publishUpdate(anomalyReportService.updateAiResult(reportId, result), "AI_COMPLETED");
	}

	@Override
	@Transactional
	public void updateAiFailure(UUID reportId, String error) {
		publishUpdate(anomalyReportService.updateAiFailure(reportId, error), "AI_FAILED");
	}

	private void publishUpdate(AnomalyReportEntity report, String updateType) {
		anomalyReportUpdatedPublisher.publish(new AnomalyReportUpdatedEvent(
			report.getId(),
			report.getApplicationId(),
			report.getSourceType(),
			updateType,
			report.getAiStatus(),
			report.getUpdatedAt() == null ? java.time.Instant.now() : report.getUpdatedAt()));
	}

	private AnomalyReportDto mapReport(AnomalyReportEntity report) {
		return new AnomalyReportDto(
			report.getId(),
			report.getApplicationId(),
			report.getAlertId(),
			report.getSourceType(),
			report.getRuleName(),
			report.getSeverity(),
			report.getStatus(),
			report.getTitle(),
			report.getSummary(),
			report.getHypothesis(),
			report.getConfidenceScore(),
			report.getLikelihoodLabel(),
			report.getImpactSummary(),
			report.getInvestigationStepsJson(),
			report.getRecommendedActionsJson(),
			report.getDimensionType(),
			report.getDimensionValue(),
			report.getMetricGroup(),
			report.getObservedValue(),
			report.getThresholdValue(),
			report.getObservedCount(),
			report.getThresholdCount(),
			report.getWindowStart(),
			report.getWindowEnd(),
			report.getEvidencePayloadJson(),
			report.isAiTriggerRequested(),
			report.getAiTriggerReason(),
			report.getAiStatus(),
			report.getAiModel(),
			report.getAiPromptVersion(),
			report.getAiStartedAt(),
			report.getAiCompletedAt(),
			report.getAiSummary(),
			report.getAiConfidenceScore(),
			report.getAiLikelihoodLabel(),
			report.getAiRootCauseCandidatesJson(),
			report.getAiRecommendedActionsJson(),
			report.getAiInvestigationStepsJson(),
			report.getAiResultJson(),
			report.getAiRawResponseJson(),
			report.getAiError(),
			report.getCreatedAt(),
			report.getUpdatedAt());
	}
}

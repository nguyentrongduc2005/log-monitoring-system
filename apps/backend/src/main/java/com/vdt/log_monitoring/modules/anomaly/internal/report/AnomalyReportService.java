package com.vdt.log_monitoring.modules.anomaly.internal.report;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.vdt.log_monitoring.modules.anomaly.api.AnomalyException;
import com.vdt.log_monitoring.modules.anomaly.api.AnomalyFacade;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AnomalyReportService {

	private final AnomalyReportRepository anomalyReportRepository;

	@Transactional
	public AnomalyReportEntity createReport(AnomalyFacade.CreateAnomalyReportCommand command) {
		return anomalyReportRepository.save(AnomalyReportEntity.create(command));
	}

	@Transactional
	public AnomalyReportEntity markAlerted(UUID reportId, UUID alertId) {
		AnomalyReportEntity report = getReportById(reportId);
		report.markAlerted(alertId);
		return report;
	}

	@Transactional
	public AnomalyReportEntity markAiPending(UUID reportId, String reason) {
		AnomalyReportEntity report = getReportById(reportId);
		report.markAiPending(reason);
		return report;
	}

	@Transactional
	public AnomalyReportEntity updateAiResult(UUID reportId, AnomalyFacade.AnomalyAiResult result) {
		AnomalyReportEntity report = getReportById(reportId);
		report.updateAiResult(result);
		return report;
	}

	@Transactional
	public AnomalyReportEntity updateAiFailure(UUID reportId, String error) {
		AnomalyReportEntity report = getReportById(reportId);
		report.updateAiFailure(error);
		return report;
	}

	@Transactional(readOnly = true)
	public AnomalyReportEntity getReportById(UUID id) {
		return anomalyReportRepository.findById(id)
			.orElseThrow(() -> new AnomalyException(
				AnomalyException.ErrorCode.ANOMALY_REPORT_NOT_FOUND,
				"Anomaly report not found"));
	}

	@Transactional(readOnly = true)
	public List<AnomalyReportEntity> listReports(List<UUID> visibleApplicationIds) {
		if (visibleApplicationIds == null || visibleApplicationIds.isEmpty()) {
			return List.of();
		}
		return anomalyReportRepository.findByApplicationIdInOrderByCreatedAtDesc(visibleApplicationIds);
	}
}

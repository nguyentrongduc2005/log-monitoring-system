package com.vdt.log_monitoring.modules.anomaly.internal.report;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.vdt.log_monitoring.modules.anomaly.api.AnomalyFacade;

@ExtendWith(MockitoExtension.class)
class AnomalyReportServiceTest {

	private static final UUID APP_ID = UUID.fromString("00000000-0000-0000-0000-000000000101");
	private static final Instant NOW = Instant.parse("2026-06-29T00:00:00Z");

	@Mock
	private AnomalyReportRepository repository;

	@Test
	void updatesOpenReportWithSameIdentity() {
		AnomalyReportService service = new AnomalyReportService(repository);
		AnomalyFacade.CreateAnomalyReportCommand first = command("ERROR", 0.70, NOW.minusSeconds(60), NOW);
		AnomalyReportEntity existing = AnomalyReportEntity.create(first);
		AnomalyFacade.CreateAnomalyReportCommand second = command("CRITICAL", 0.90, NOW.minusSeconds(120), NOW.plusSeconds(60));
		when(repository.findFirstByApplicationIdAndSourceTypeAndRuleNameAndFingerprintAndStatusNotOrderByUpdatedAtDesc(
			APP_ID,
			"ANOMALY_LOG",
			"SECURITY_AUTH_FAILURE",
			"user:john",
			"RESOLVED"))
			.thenReturn(Optional.of(existing));

		AnomalyReportEntity report = service.createOrUpdateReport(second);

		assertThat(report).isSameAs(existing);
		assertThat(report.getOccurrenceCount()).isEqualTo(2);
		assertThat(report.getSeverity()).isEqualTo("CRITICAL");
		assertThat(report.getConfidenceScore()).isEqualTo(0.90);
		assertThat(report.getWindowStart()).isEqualTo(NOW.minusSeconds(120));
		assertThat(report.getWindowEnd()).isEqualTo(NOW.plusSeconds(60));
		assertThat(report.getFirstSeenAt()).isEqualTo(NOW.minusSeconds(120));
		assertThat(report.getLastSeenAt()).isEqualTo(NOW.plusSeconds(60));
	}

	@Test
	void createsNewReportWhenNoOpenReportExists() {
		AnomalyReportService service = new AnomalyReportService(repository);
		AnomalyFacade.CreateAnomalyReportCommand command = command("ERROR", 0.70, NOW.minusSeconds(60), NOW);
		when(repository.findFirstByApplicationIdAndSourceTypeAndRuleNameAndFingerprintAndStatusNotOrderByUpdatedAtDesc(
			APP_ID,
			"ANOMALY_LOG",
			"SECURITY_AUTH_FAILURE",
			"user:john",
			"RESOLVED"))
			.thenReturn(Optional.empty());
		when(repository.save(org.mockito.ArgumentMatchers.any(AnomalyReportEntity.class)))
			.thenAnswer(invocation -> invocation.getArgument(0));

		AnomalyReportEntity report = service.createOrUpdateReport(command);

		assertThat(report.getOccurrenceCount()).isEqualTo(1);
		assertThat(report.getFingerprint()).isEqualTo("user:john");
		verify(repository).save(report);
	}

	@Test
	void doesNotReopenResolvedReportWhenAiPendingArrivesLate() {
		AnomalyReportEntity report = AnomalyReportEntity.create(
			command("ERROR", 0.70, NOW.minusSeconds(60), NOW));
		UUID userId = UUID.fromString("00000000-0000-0000-0000-000000000001");
		report.resolve(userId);

		report.markAiPending("RULE_REQUIRES_AI");

		assertThat(report.getStatus()).isEqualTo("RESOLVED");
		assertThat(report.getAiStatus()).isEqualTo("NOT_REQUESTED");
	}

	@Test
	void doesNotRequestAiAgainWhenSameSeverityAlreadyHasAnalysis() {
		AnomalyReportEntity report = AnomalyReportEntity.create(
			command("ERROR", 0.70, NOW.minusSeconds(60), NOW));
		report.updateAiResult(new AnomalyFacade.AnomalyAiResult("{\"summary\":\"checked\"}"));

		report.recordOccurrence(command("ERROR", 0.75, NOW.minusSeconds(30), NOW.plusSeconds(30)));

		assertThat(report.getSeverity()).isEqualTo("ERROR");
		assertThat(report.getAiStatus()).isEqualTo("SUCCEEDED");
		assertThat(report.getAiResultJson()).contains("checked");
	}

	@Test
	void allowsAiAgainOnceWhenSeverityEscalatesToCritical() {
		AnomalyReportEntity report = AnomalyReportEntity.create(
			command("ERROR", 0.70, NOW.minusSeconds(60), NOW));
		report.updateAiResult(new AnomalyFacade.AnomalyAiResult("{\"summary\":\"checked\"}"));

		report.recordOccurrence(command("CRITICAL", 0.95, NOW.minusSeconds(30), NOW.plusSeconds(30)));

		assertThat(report.getSeverity()).isEqualTo("CRITICAL");
		assertThat(report.getAiStatus()).isEqualTo("NOT_REQUESTED");
		assertThat(report.isAiTriggerRequested()).isTrue();
	}

	@Test
	void doesNotRequestAiAgainWhenCriticalRemainsCritical() {
		AnomalyReportEntity report = AnomalyReportEntity.create(
			command("CRITICAL", 0.95, NOW.minusSeconds(60), NOW));
		report.updateAiResult(new AnomalyFacade.AnomalyAiResult("{\"summary\":\"critical checked\"}"));

		report.recordOccurrence(command("CRITICAL", 0.98, NOW.minusSeconds(30), NOW.plusSeconds(30)));

		assertThat(report.getSeverity()).isEqualTo("CRITICAL");
		assertThat(report.getAiStatus()).isEqualTo("SUCCEEDED");
		assertThat(report.getAiResultJson()).contains("critical checked");
	}

	@Test
	void doesNotRetryFailedAiWithoutCriticalEscalation() {
		AnomalyReportEntity report = AnomalyReportEntity.create(
			command("ERROR", 0.70, NOW.minusSeconds(60), NOW));
		report.updateAiFailure("AI unavailable");

		report.recordOccurrence(command("ERROR", 0.75, NOW.minusSeconds(30), NOW.plusSeconds(30)));

		assertThat(report.getSeverity()).isEqualTo("ERROR");
		assertThat(report.getAiStatus()).isEqualTo("FAILED");
		assertThat(report.getAiError()).isEqualTo("AI unavailable");
	}

	@Test
	void allowsFailedAiToRunAgainWhenSeverityEscalatesToCritical() {
		AnomalyReportEntity report = AnomalyReportEntity.create(
			command("ERROR", 0.70, NOW.minusSeconds(60), NOW));
		report.updateAiFailure("AI unavailable");

		report.recordOccurrence(command("CRITICAL", 0.95, NOW.minusSeconds(30), NOW.plusSeconds(30)));

		assertThat(report.getSeverity()).isEqualTo("CRITICAL");
		assertThat(report.getAiStatus()).isEqualTo("NOT_REQUESTED");
		assertThat(report.getAiError()).isNull();
	}

	@Test
	void listReportsExcludesResolvedReports() {
		AnomalyReportService service = new AnomalyReportService(repository);
		AnomalyReportEntity openReport = AnomalyReportEntity.create(
			command("ERROR", 0.70, NOW.minusSeconds(60), NOW));
		AnomalyReportEntity resolvedReport = AnomalyReportEntity.create(
			command("ERROR", 0.70, NOW.minusSeconds(120), NOW.minusSeconds(60)));
		resolvedReport.resolve(UUID.fromString("00000000-0000-0000-0000-000000000001"));
		when(repository.findByApplicationIdInOrderByCreatedAtDesc(List.of(APP_ID)))
			.thenReturn(List.of(openReport, resolvedReport));

		List<AnomalyReportEntity> reports = service.listReports(List.of(APP_ID));

		assertThat(reports).containsExactly(openReport);
	}

	private AnomalyFacade.CreateAnomalyReportCommand command(
		String severity,
		double confidence,
		Instant windowStart,
		Instant windowEnd
	) {
		return new AnomalyFacade.CreateAnomalyReportCommand(
			APP_ID,
			"ANOMALY_LOG",
			"SECURITY_AUTH_FAILURE",
			"user:john",
			severity,
			"Authentication failures exceeded threshold.",
			"Authentication failures exceeded threshold.",
			"Possible brute-force attempt.",
			confidence,
			windowStart,
			windowEnd,
			"{}",
			true,
			"RULE_REQUIRES_AI");
	}
}

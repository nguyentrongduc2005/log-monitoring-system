package com.vdt.log_monitoring.modules.anomaly.internal.report;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class AnomalyReportMigrationTest {

	@Test
	void statusConstraintAllowsResolvedReports() throws Exception {
		String migration = Files.readString(Path.of(
			"src/main/resources/db/migration/postgresql/V23__allow_resolved_anomaly_report_status.sql"));

		assertThat(migration)
			.contains("DROP CONSTRAINT IF EXISTS ck_anomaly_reports_status")
			.contains("ADD CONSTRAINT ck_anomaly_reports_status")
			.contains("'RESOLVED'");
	}
}

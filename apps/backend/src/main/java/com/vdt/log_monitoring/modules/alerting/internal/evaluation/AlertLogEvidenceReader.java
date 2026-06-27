package com.vdt.log_monitoring.modules.alerting.internal.evaluation;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import com.vdt.log_monitoring.modules.alerting.internal.alert.AlertLogSample;

import javax.sql.DataSource;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;

@Repository
@Slf4j
public class AlertLogEvidenceReader {

	private static final int TOP_FINGERPRINT_LIMIT = 5;

	private final DataSource clickHouseDataSource;

	public AlertLogEvidenceReader(@Qualifier("clickHouseDataSource") DataSource clickHouseDataSource) {
		this.clickHouseDataSource = clickHouseDataSource;
	}

	public List<AlertLogSample> findTopErrorLogSamples(
		UUID applicationId,
		Instant windowStart,
		Instant windowEnd
	) {
		if (applicationId == null) {
			return new ArrayList<>();
		}
		String sql = """
			SELECT any(level) AS sample_level, any(message) AS sample_message
			FROM processed_logs
			WHERE application_id = ?
			  AND log_timestamp >= ?
			  AND log_timestamp <= ?
			  AND level IN ('ERROR', 'CRITICAL')
			  AND fingerprint IS NOT NULL
			  AND fingerprint != ''
			GROUP BY fingerprint
			ORDER BY count() DESC, min(log_timestamp) ASC
			LIMIT %d
			""".formatted(TOP_FINGERPRINT_LIMIT);
		try (
			var connection = clickHouseDataSource.getConnection();
			var statement = connection.prepareStatement(sql)
		) {
			statement.setObject(1, applicationId);
			statement.setTimestamp(2, Timestamp.from(windowStart));
			statement.setTimestamp(3, Timestamp.from(windowEnd));
			try (var resultSet = statement.executeQuery()) {
				List<AlertLogSample> samples = new ArrayList<>();
				while (resultSet.next()) {
					samples.add(new AlertLogSample(
						resultSet.getString("sample_level"),
						resultSet.getString("sample_message")
					));
				}
				if (samples.isEmpty()) {
					samples.add(new AlertLogSample("INFO", "No representative log samples available in ClickHouse at trigger time"));
				}
				return samples;
			}
		} catch (Exception exception) {
			log.warn("Failed to collect top error log samples for alert rule evaluation", exception);
			return List.of(new AlertLogSample("ERROR", "Failed to fetch log samples due to a ClickHouse error"));
		}
	}
}

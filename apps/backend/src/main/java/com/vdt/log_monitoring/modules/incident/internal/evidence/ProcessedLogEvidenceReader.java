package com.vdt.log_monitoring.modules.incident.internal.evidence;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.sql.DataSource;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;

@Repository
@Slf4j
public class ProcessedLogEvidenceReader {

	private static final int TOP_FINGERPRINT_LIMIT = 5;
	private static final int RELATED_LOG_LIMIT = 20;

	private final DataSource clickHouseDataSource;

	public ProcessedLogEvidenceReader(@Qualifier("clickHouseDataSource") DataSource clickHouseDataSource) {
		this.clickHouseDataSource = clickHouseDataSource;
	}

	public List<FingerprintSummary> findTopErrorFingerprints(
		UUID applicationId,
		Instant windowStart,
		Instant windowEnd
	) {
		if (applicationId == null) {
			return List.of();
		}
		String sql = """
			SELECT fingerprint,
			       count() AS occurrence_count,
			       min(log_timestamp) AS first_seen_at,
			       max(log_timestamp) AS last_seen_at,
			       any(level) AS severity,
			       any(message) AS sample_message
			FROM processed_logs
			WHERE application_id = ?
			  AND log_timestamp >= ?
			  AND log_timestamp <= ?
			  AND level IN ('ERROR', 'CRITICAL')
			  AND fingerprint IS NOT NULL
			  AND fingerprint != ''
			GROUP BY fingerprint
			ORDER BY occurrence_count DESC, first_seen_at ASC
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
				List<FingerprintSummary> results = new ArrayList<>();
				while (resultSet.next()) {
					results.add(summaryFrom(resultSet));
				}
				return results;
			}
		} catch (Exception exception) {
			log.warn("Failed to collect top error fingerprints for incident investigation", exception);
			return List.of();
		}
	}

	public List<FingerprintSummary> findTopSignalFingerprints(
		UUID applicationId,
		Instant windowStart,
		Instant windowEnd
	) {
		if (applicationId == null) {
			return List.of();
		}
		String sql = """
			SELECT fingerprint,
			       count() AS occurrence_count,
			       min(log_timestamp) AS first_seen_at,
			       max(log_timestamp) AS last_seen_at,
			       any(level) AS severity,
			       any(message) AS sample_message
			FROM processed_logs
			WHERE application_id = ?
			  AND log_timestamp >= ?
			  AND log_timestamp <= ?
			  AND level IN ('WARN', 'ERROR', 'CRITICAL')
			  AND fingerprint IS NOT NULL
			  AND fingerprint != ''
			GROUP BY fingerprint
			ORDER BY occurrence_count DESC, first_seen_at ASC
			LIMIT %d
			""".formatted(TOP_FINGERPRINT_LIMIT);
		return fingerprintSummaries(sql, applicationId, windowStart, windowEnd);
	}

	public List<LogSample> findRelatedLogs(
		UUID applicationId,
		Instant windowStart,
		Instant windowEnd,
		List<String> traceIds,
		List<String> fingerprints
	) {
		if (applicationId == null || ((traceIds == null || traceIds.isEmpty()) && (fingerprints == null || fingerprints.isEmpty()))) {
			return List.of();
		}
		String sql = """
			SELECT event_id, level, message, trace_id, fingerprint, log_timestamp
			FROM processed_logs
			WHERE application_id = ?
			  AND log_timestamp >= ?
			  AND log_timestamp <= ?
			  AND (
			    (%s)
			    OR (%s)
			  )
			ORDER BY log_timestamp DESC
			LIMIT %d
			""".formatted(inClause("trace_id", traceIds), inClause("fingerprint", fingerprints), RELATED_LOG_LIMIT);
		try (
			var connection = clickHouseDataSource.getConnection();
			var statement = connection.prepareStatement(sql)
		) {
			statement.setObject(1, applicationId);
			statement.setTimestamp(2, Timestamp.from(windowStart));
			statement.setTimestamp(3, Timestamp.from(windowEnd));
			try (var resultSet = statement.executeQuery()) {
				List<LogSample> results = new ArrayList<>();
				while (resultSet.next()) {
					results.add(new LogSample(
						resultSet.getString("event_id"),
						resultSet.getString("level"),
						resultSet.getString("message"),
						resultSet.getString("trace_id"),
						resultSet.getString("fingerprint"),
						resultSet.getTimestamp("log_timestamp").toInstant()));
				}
				return results;
			}
		} catch (Exception exception) {
			log.warn("Failed to collect related logs for incident investigation", exception);
			return List.of();
		}
	}

	private List<FingerprintSummary> fingerprintSummaries(
		String sql,
		UUID applicationId,
		Instant windowStart,
		Instant windowEnd
	) {
		try (
			var connection = clickHouseDataSource.getConnection();
			var statement = connection.prepareStatement(sql)
		) {
			statement.setObject(1, applicationId);
			statement.setTimestamp(2, Timestamp.from(windowStart));
			statement.setTimestamp(3, Timestamp.from(windowEnd));
			try (var resultSet = statement.executeQuery()) {
				List<FingerprintSummary> results = new ArrayList<>();
				while (resultSet.next()) {
					results.add(summaryFrom(resultSet));
				}
				return results;
			}
		} catch (Exception exception) {
			log.warn("Failed to collect fingerprint summaries for incident investigation", exception);
			return List.of();
		}
	}

	private FingerprintSummary summaryFrom(java.sql.ResultSet resultSet) throws java.sql.SQLException {
		return new FingerprintSummary(
			resultSet.getString("fingerprint"),
			resultSet.getLong("occurrence_count"),
			resultSet.getTimestamp("first_seen_at").toInstant(),
			resultSet.getTimestamp("last_seen_at").toInstant(),
			resultSet.getString("severity"),
			resultSet.getString("sample_message"));
	}

	private String inClause(String column, List<String> values) {
		List<String> filtered = values == null ? List.of() : values.stream()
			.filter(value -> value != null && !value.isBlank())
			.distinct()
			.limit(20)
			.toList();
		if (filtered.isEmpty()) {
			return "1 = 0";
		}
		return column + " IN (" + filtered.stream()
			.map(this::quoted)
			.collect(java.util.stream.Collectors.joining(", ")) + ")";
	}

	private String quoted(String value) {
		return "'" + value.replace("\\", "\\\\").replace("'", "''") + "'";
	}

	public record FingerprintSummary(
		String fingerprint,
		long occurrenceCount,
		Instant firstSeenAt,
		Instant lastSeenAt,
		String severity,
		String sampleMessage
	) {}

	public record LogSample(
		String eventId,
		String severity,
		String message,
		String traceId,
		String fingerprint,
		Instant occurredAt
	) {}
}

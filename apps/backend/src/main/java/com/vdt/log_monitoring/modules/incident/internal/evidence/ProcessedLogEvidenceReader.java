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

import com.vdt.log_monitoring.modules.incident.internal.incident.EvidenceType;

@Repository
@Slf4j
public class ProcessedLogEvidenceReader {

	private static final int LIMIT = 20;
	private static final int INCIDENT_ERROR_LOG_LIMIT = 100;
	private static final int TOP_FINGERPRINT_LIMIT = 5;

	private final DataSource clickHouseDataSource;

	public ProcessedLogEvidenceReader(@Qualifier("clickHouseDataSource") DataSource clickHouseDataSource) {
		this.clickHouseDataSource = clickHouseDataSource;
	}

	public List<IncidentEvidenceCandidate> findErrorSamples(
		List<UUID> applicationIds,
		Instant windowStart,
		Instant windowEnd
	) {
		if (applicationIds == null || applicationIds.isEmpty()) {
			return List.of();
		}
		String placeholders = String.join(",", applicationIds.stream().map(ignored -> "?").toList());
		String sql = """
			SELECT event_id, application_id, fingerprint, trace_id, level, message, log_timestamp
			FROM processed_logs
			WHERE application_id IN (%s)
			  AND log_timestamp >= ?
			  AND log_timestamp <= ?
			  AND level IN ('ERROR', 'CRITICAL')
			ORDER BY log_timestamp DESC
			LIMIT %d
			""".formatted(placeholders, LIMIT);
		try (
			var connection = clickHouseDataSource.getConnection();
			var statement = connection.prepareStatement(sql)
		) {
			int index = 1;
			for (UUID applicationId : applicationIds) {
				statement.setObject(index++, applicationId);
			}
			statement.setTimestamp(index++, Timestamp.from(windowStart));
			statement.setTimestamp(index, Timestamp.from(windowEnd));
			try (var resultSet = statement.executeQuery()) {
				List<IncidentEvidenceCandidate> results = new ArrayList<>();
				while (resultSet.next()) {
					String level = resultSet.getString("level");
					String message = resultSet.getString("message");
					UUID applicationId = UUID.fromString(resultSet.getString("application_id"));
					String eventId = String.valueOf(resultSet.getObject("event_id"));
					String fingerprint = resultSet.getString("fingerprint");
					String traceId = resultSet.getString("trace_id");
					Instant occurredAt = resultSet.getTimestamp("log_timestamp").toInstant();
					results.add(new IncidentEvidenceCandidate(
						EvidenceType.LOG,
						eventId,
						applicationId,
						fingerprint,
						traceId,
						level,
						"Log " + level + ": " + message,
						message,
						occurredAt,
						metadata(traceId)));
				}
				return results;
			}
		} catch (Exception exception) {
			log.warn("Failed to collect ClickHouse log evidence for incident investigation", exception);
			return List.of();
		}
	}

	public List<ErrorLogSample> findIncidentErrorLogs(
		List<UUID> applicationIds,
		Instant windowStart,
		Instant windowEnd
	) {
		if (applicationIds == null || applicationIds.isEmpty()) {
			return List.of();
		}
		String placeholders = String.join(",", applicationIds.stream().map(ignored -> "?").toList());
		String sql = """
			SELECT event_id,
			       application_id,
			       application_name,
			       application_display_name,
			       level,
			       message,
			       fingerprint,
			       trace_id,
			       log_timestamp
			FROM processed_logs
			WHERE application_id IN (%s)
			  AND log_timestamp >= ?
			  AND log_timestamp <= ?
			  AND level IN ('ERROR', 'CRITICAL')
			ORDER BY log_timestamp DESC
			LIMIT %d
			""".formatted(placeholders, INCIDENT_ERROR_LOG_LIMIT);
		try (
			var connection = clickHouseDataSource.getConnection();
			var statement = connection.prepareStatement(sql)
		) {
			int index = 1;
			for (UUID applicationId : applicationIds) {
				statement.setObject(index++, applicationId);
			}
			statement.setTimestamp(index++, Timestamp.from(windowStart));
			statement.setTimestamp(index, Timestamp.from(windowEnd));
			try (var resultSet = statement.executeQuery()) {
				List<ErrorLogSample> results = new ArrayList<>();
				while (resultSet.next()) {
					results.add(new ErrorLogSample(
						UUID.fromString(String.valueOf(resultSet.getObject("event_id"))),
						UUID.fromString(resultSet.getString("application_id")),
						resultSet.getString("application_name"),
						resultSet.getString("application_display_name"),
						resultSet.getString("level"),
						resultSet.getString("message"),
						resultSet.getString("fingerprint"),
						resultSet.getString("trace_id"),
						resultSet.getTimestamp("log_timestamp").toInstant()));
				}
				return results;
			}
		} catch (Exception exception) {
			log.warn("Failed to collect incident error logs", exception);
			return List.of();
		}
	}

	public FingerprintSummary findFingerprintSummary(
		UUID applicationId,
		String fingerprint,
		Instant windowStart,
		Instant windowEnd
	) {
		if (applicationId == null || fingerprint == null || fingerprint.isBlank()) {
			return null;
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
			  AND fingerprint = ?
			GROUP BY fingerprint
			LIMIT 1
			""";
		try (
			var connection = clickHouseDataSource.getConnection();
			var statement = connection.prepareStatement(sql)
		) {
			statement.setObject(1, applicationId);
			statement.setTimestamp(2, Timestamp.from(windowStart));
			statement.setTimestamp(3, Timestamp.from(windowEnd));
			statement.setString(4, fingerprint);
			try (var resultSet = statement.executeQuery()) {
				if (!resultSet.next()) {
					return null;
				}
				return summaryFrom(resultSet);
			}
		} catch (Exception exception) {
			log.warn("Failed to collect trigger fingerprint summary for incident investigation", exception);
			return null;
		}
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

	public TraceContext findTraceContext(
		UUID applicationId,
		UUID triggerEventId,
		Instant windowStart,
		Instant windowEnd
	) {
		if (applicationId == null || triggerEventId == null) {
			return null;
		}
		String traceId = findTraceId(applicationId, triggerEventId);
		if (traceId == null || traceId.isBlank()) {
			return null;
		}
		String sql = """
			SELECT event_id, level, message, log_timestamp
			FROM processed_logs
			WHERE application_id = ?
			  AND trace_id = ?
			  AND log_timestamp >= ?
			  AND log_timestamp <= ?
			ORDER BY log_timestamp ASC
			LIMIT %d
			""".formatted(LIMIT);
		try (
			var connection = clickHouseDataSource.getConnection();
			var statement = connection.prepareStatement(sql)
		) {
			statement.setObject(1, applicationId);
			statement.setString(2, traceId);
			statement.setTimestamp(3, Timestamp.from(windowStart));
			statement.setTimestamp(4, Timestamp.from(windowEnd));
			try (var resultSet = statement.executeQuery()) {
				List<TraceEvent> events = new ArrayList<>();
				while (resultSet.next()) {
					events.add(new TraceEvent(
						String.valueOf(resultSet.getObject("event_id")),
						resultSet.getString("level"),
						resultSet.getString("message"),
						resultSet.getTimestamp("log_timestamp").toInstant()));
				}
				return events.isEmpty() ? null : new TraceContext(traceId, events);
			}
		} catch (Exception exception) {
			log.warn("Failed to collect trace context for incident investigation", exception);
			return null;
		}
	}

	private String findTraceId(UUID applicationId, UUID eventId) {
		String sql = """
			SELECT trace_id
			FROM processed_logs
			WHERE application_id = ?
			  AND event_id = ?
			  AND trace_id IS NOT NULL
			  AND trace_id != ''
			LIMIT 1
			""";
		try (
			var connection = clickHouseDataSource.getConnection();
			var statement = connection.prepareStatement(sql)
		) {
			statement.setObject(1, applicationId);
			statement.setObject(2, eventId);
			try (var resultSet = statement.executeQuery()) {
				return resultSet.next() ? resultSet.getString("trace_id") : null;
			}
		} catch (Exception exception) {
			log.warn("Failed to find trace id for incident trigger event", exception);
			return null;
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

	private String metadata(String traceId) {
		if (traceId == null || traceId.isBlank()) {
			return null;
		}
		return "{\"traceId\":\"" + traceId.replace("\\", "\\\\").replace("\"", "\\\"") + "\"}";
	}

	public record FingerprintSummary(
		String fingerprint,
		long occurrenceCount,
		Instant firstSeenAt,
		Instant lastSeenAt,
		String severity,
		String sampleMessage
	) {}

	public record TraceContext(String traceId, List<TraceEvent> events) {}

	public record TraceEvent(String eventId, String level, String message, Instant occurredAt) {}

	public record ErrorLogSample(
		UUID eventId,
		UUID applicationId,
		String applicationName,
		String applicationDisplayName,
		String level,
		String message,
		String fingerprint,
		String traceId,
		Instant logTimestamp
	) {}
}

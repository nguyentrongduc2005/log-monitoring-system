package com.vdt.log_monitoring.modules.analytics.internal;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;

import com.vdt.log_monitoring.modules.analytics.api.DashboardFacade.LogSearchEntryDto;
import com.vdt.log_monitoring.modules.analytics.api.DashboardFacade.LogSearchSummaryDto;
import com.vdt.log_monitoring.modules.analytics.api.DashboardFacade.LogVolumePointDto;

import lombok.extern.slf4j.Slf4j;

@Repository
@Slf4j
public class ClickHouseAnalyticsRepository {

	private final DataSource clickHouseDataSource;
	private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm").withZone(ZoneOffset.UTC);

	public ClickHouseAnalyticsRepository(@Qualifier("clickHouseDataSource") DataSource clickHouseDataSource) {
		this.clickHouseDataSource = clickHouseDataSource;
	}

	public long getTotalLogsCount(Instant windowStart, Instant windowEnd, List<java.util.UUID> visibleApplicationIds) {
		String inClause = visibleApplicationIds.stream().map(id -> "?").collect(java.util.stream.Collectors.joining(","));
		String sql = "SELECT count() FROM processed_logs WHERE log_timestamp >= ? AND log_timestamp <= ? AND application_id IN (" + inClause + ")";
		try (
			var connection = clickHouseDataSource.getConnection();
			var statement = connection.prepareStatement(sql)
		) {
			statement.setTimestamp(1, Timestamp.from(windowStart));
			statement.setTimestamp(2, Timestamp.from(windowEnd));
			int index = 3;
			for (java.util.UUID id : visibleApplicationIds) {
				statement.setObject(index++, id);
			}
			try (var resultSet = statement.executeQuery()) {
				if (resultSet.next()) {
					return resultSet.getLong(1);
				}
			}
		} catch (Exception e) {
			log.warn("Failed to get total logs count from ClickHouse", e);
		}
		return 0;
	}

	public long getErrorLogsCount(Instant windowStart, Instant windowEnd, List<java.util.UUID> visibleApplicationIds) {
		String inClause = visibleApplicationIds.stream().map(id -> "?").collect(java.util.stream.Collectors.joining(","));
		String sql = "SELECT count() FROM processed_logs WHERE log_timestamp >= ? AND log_timestamp <= ? AND level IN ('ERROR', 'CRITICAL') AND application_id IN (" + inClause + ")";
		try (
			var connection = clickHouseDataSource.getConnection();
			var statement = connection.prepareStatement(sql)
		) {
			statement.setTimestamp(1, Timestamp.from(windowStart));
			statement.setTimestamp(2, Timestamp.from(windowEnd));
			int index = 3;
			for (java.util.UUID id : visibleApplicationIds) {
				statement.setObject(index++, id);
			}
			try (var resultSet = statement.executeQuery()) {
				if (resultSet.next()) {
					return resultSet.getLong(1);
				}
			}
		} catch (Exception e) {
			log.warn("Failed to get error logs count from ClickHouse", e);
		}
		return 0;
	}

	public record LogVolumeRow(Instant bucket, long infoCount, long warnCount, long errorCount, long criticalCount) {}

	public List<LogVolumeRow> getLogVolume(Instant windowStart, Instant windowEnd, List<java.util.UUID> visibleApplicationIds) {
		String inClause = visibleApplicationIds.stream().map(id -> "?").collect(java.util.stream.Collectors.joining(","));
		String sql = """
			SELECT 
				toStartOfHour(log_timestamp) as bucket,
				countIf(level = 'INFO') as info_count,
				countIf(level IN ('WARN', 'WARNING')) as warn_count,
				countIf(level = 'ERROR') as error_count,
				countIf(level = 'CRITICAL') as critical_count
			FROM processed_logs
			WHERE log_timestamp >= ? AND log_timestamp <= ? AND application_id IN (%s)
			GROUP BY bucket
			ORDER BY bucket ASC
			""".formatted(inClause);
		
		List<LogVolumeRow> result = new ArrayList<>();
		try (
			var connection = clickHouseDataSource.getConnection();
			var statement = connection.prepareStatement(sql)
		) {
			statement.setTimestamp(1, Timestamp.from(windowStart));
			statement.setTimestamp(2, Timestamp.from(windowEnd));
			int index = 3;
			for (java.util.UUID id : visibleApplicationIds) {
				statement.setObject(index++, id);
			}
			try (var resultSet = statement.executeQuery()) {
				while (resultSet.next()) {
					Instant bucket = resultSet.getTimestamp("bucket").toInstant();
					long info = resultSet.getLong("info_count");
					long warn = resultSet.getLong("warn_count");
					long error = resultSet.getLong("error_count");
					long crit = resultSet.getLong("critical_count");
					result.add(new LogVolumeRow(bucket, info, warn, error, crit));
				}
			}
		} catch (Exception e) {
			log.warn("Failed to get log volume from ClickHouse", e);
		}
		return result;
	}

	private record FilteredQueryBuilder(String sql, List<Object> params) {}

	private FilteredQueryBuilder buildFilteredQuery(
		String selectClause,
		String tailClause,
		Instant start,
		Instant end,
		List<UUID> allowedApplicationIds,
		String query,
		UUID singleApplicationId,
		String level
	) {
		StringBuilder sql = new StringBuilder(selectClause).append(" FROM processed_logs WHERE log_timestamp >= ? AND log_timestamp <= ?");
		List<Object> params = new ArrayList<>();
		params.add(Timestamp.from(start));
		params.add(Timestamp.from(end));

		if (singleApplicationId != null) {
			if (allowedApplicationIds.contains(singleApplicationId)) {
				sql.append(" AND application_id = ?");
				params.add(singleApplicationId);
			} else {
				sql.append(" AND 1 = 0");
			}
		} else {
			if (allowedApplicationIds.isEmpty()) {
				sql.append(" AND 1 = 0");
			} else {
				String placeholders = allowedApplicationIds.stream().map(id -> "?").collect(Collectors.joining(","));
				sql.append(" AND application_id IN (").append(placeholders).append(")");
				params.addAll(allowedApplicationIds);
			}
		}

		if (level != null && !"ALL".equalsIgnoreCase(level)) {
			sql.append(" AND level = ?");
			params.add(level.toUpperCase());
		}

		if (query != null && !query.isBlank()) {
			sql.append(" AND (message ILIKE ? OR trace_id ILIKE ? OR toString(event_id) ILIKE ?)");
			String likeVal = "%" + query.trim() + "%";
			params.add(likeVal);
			params.add(likeVal);
			params.add(likeVal);
		}

		if (tailClause != null) {
			sql.append(" ").append(tailClause);
		}

		return new FilteredQueryBuilder(sql.toString(), params);
	}

	private void bindParams(java.sql.PreparedStatement statement, List<Object> params) throws java.sql.SQLException {
		int index = 1;
		for (Object param : params) {
			if (param instanceof Timestamp) {
				statement.setTimestamp(index++, (Timestamp) param);
			} else {
				statement.setObject(index++, param);
			}
		}
	}

	public LogSearchSummaryDto getLogSearchSummary(
		Instant start,
		Instant end,
		List<UUID> allowedApplicationIds,
		String query,
		UUID singleApplicationId,
		String level
	) {
		String select = """
			SELECT 
				count() as total_count,
				countIf(level = 'ERROR') as error_count,
				countIf(level = 'CRITICAL') as critical_count,
				uniq(trace_id) as unique_traces
			""";
		FilteredQueryBuilder builder = buildFilteredQuery(select, null, start, end, allowedApplicationIds, query, singleApplicationId, level);

		try (
			var connection = clickHouseDataSource.getConnection();
			var statement = connection.prepareStatement(builder.sql())
		) {
			bindParams(statement, builder.params());
			try (var resultSet = statement.executeQuery()) {
				if (resultSet.next()) {
					return new LogSearchSummaryDto(
						resultSet.getLong("total_count"),
						resultSet.getLong("error_count"),
						resultSet.getLong("critical_count"),
						resultSet.getLong("unique_traces"),
						0L
					);
				}
			}
		} catch (Exception e) {
			log.warn("Failed to get log search summary from ClickHouse", e);
		}
		return new LogSearchSummaryDto(0, 0, 0, 0, 0);
	}

	public List<LogVolumePointDto> getLogSearchVolume(
		Instant start,
		Instant end,
		List<UUID> allowedApplicationIds,
		String query,
		UUID singleApplicationId,
		String level,
		String range
	) {
		String intervalStr = switch (range == null ? "24h" : range) {
			case "15m" -> "INTERVAL 1 MINUTE";
			case "1h" -> "INTERVAL 5 MINUTE";
			case "6h" -> "INTERVAL 30 MINUTE";
			default -> "INTERVAL 1 HOUR";
		};

		String select = """
			SELECT 
				toStartOfInterval(log_timestamp, %s) as bucket,
				count() as total_count,
				countIf(level = 'INFO') as info_count,
				countIf(level IN ('WARN', 'WARNING')) as warn_count,
				countIf(level = 'ERROR') as error_count,
				countIf(level = 'CRITICAL') as critical_count
			""".formatted(intervalStr);

		String tail = "GROUP BY bucket ORDER BY bucket ASC";
		FilteredQueryBuilder builder = buildFilteredQuery(select, tail, start, end, allowedApplicationIds, query, singleApplicationId, level);

		List<LogVolumePointDto> result = new ArrayList<>();
		try (
			var connection = clickHouseDataSource.getConnection();
			var statement = connection.prepareStatement(builder.sql())
		) {
			bindParams(statement, builder.params());
			try (var resultSet = statement.executeQuery()) {
				while (resultSet.next()) {
					java.sql.Timestamp bucketTs = resultSet.getTimestamp("bucket");
					String timeStr = bucketTs != null ? TIME_FORMATTER.format(bucketTs.toInstant()) : "";
					result.add(new LogVolumePointDto(
						timeStr,
						resultSet.getLong("info_count"),
						resultSet.getLong("warn_count"),
						resultSet.getLong("error_count"),
						resultSet.getLong("critical_count")
					));
				}
			}
		} catch (Exception e) {
			log.warn("Failed to get log search volume from ClickHouse", e);
		}
		return result;
	}

	public List<LogSearchEntryDto> searchLogs(
		Instant start,
		Instant end,
		List<UUID> allowedApplicationIds,
		String query,
		UUID singleApplicationId,
		String level,
		int limit,
		int offset
	) {
		String select = """
			SELECT 
				event_id,
				application_id,
				application_name,
				level,
				message,
				trace_id,
				log_timestamp
			""";
		String tail = "ORDER BY log_timestamp DESC LIMIT ? OFFSET ?";
		FilteredQueryBuilder builder = buildFilteredQuery(select, tail, start, end, allowedApplicationIds, query, singleApplicationId, level);
		
		List<Object> paramsWithPagination = new ArrayList<>(builder.params());
		paramsWithPagination.add(limit);
		paramsWithPagination.add(offset);

		List<LogSearchEntryDto> result = new ArrayList<>();
		try (
			var connection = clickHouseDataSource.getConnection();
			var statement = connection.prepareStatement(builder.sql())
		) {
			bindParams(statement, paramsWithPagination);
			try (var resultSet = statement.executeQuery()) {
				while (resultSet.next()) {
					UUID eventId = resultSet.getObject("event_id", UUID.class);
					UUID appId = resultSet.getObject("application_id", UUID.class);
					Timestamp logTs = resultSet.getTimestamp("log_timestamp");
					String traceId = resultSet.getString("trace_id");

					result.add(new LogSearchEntryDto(
						eventId != null ? eventId.toString() : "",
						logTs != null ? logTs.toString() : "",
						appId,
						resultSet.getString("application_name"),
						resultSet.getString("level"),
						resultSet.getString("message"),
						traceId != null ? traceId : "",
						"",
						eventId != null ? eventId.toString() : "",
						"",
						"",
						0L,
						0,
						java.util.Map.of(),
						List.of()
					));
				}
			}
		} catch (Exception e) {
			log.warn("Failed to search logs from ClickHouse", e);
		}
		return result;
	}

	public List<LogSearchEntryDto> getRelatedTraceLogs(
		String traceId,
		List<UUID> allowedApplicationIds
	) {
		if (traceId == null || traceId.isBlank() || allowedApplicationIds.isEmpty()) {
			return List.of();
		}
		String placeholders = allowedApplicationIds.stream().map(id -> "?").collect(Collectors.joining(","));
		String sql = """
			SELECT 
				event_id,
				application_id,
				application_name,
				level,
				message,
				trace_id,
				log_timestamp
			FROM processed_logs
			WHERE trace_id = ? AND application_id IN (%s)
			ORDER BY log_timestamp ASC
			LIMIT 100
			""".formatted(placeholders);

		List<LogSearchEntryDto> result = new ArrayList<>();
		try (
			var connection = clickHouseDataSource.getConnection();
			var statement = connection.prepareStatement(sql)
		) {
			statement.setString(1, traceId);
			int index = 2;
			for (UUID id : allowedApplicationIds) {
				statement.setObject(index++, id);
			}
			try (var resultSet = statement.executeQuery()) {
				while (resultSet.next()) {
					UUID eventId = resultSet.getObject("event_id", UUID.class);
					UUID appId = resultSet.getObject("application_id", UUID.class);
					Timestamp logTs = resultSet.getTimestamp("log_timestamp");

					result.add(new LogSearchEntryDto(
						eventId != null ? eventId.toString() : "",
						logTs != null ? logTs.toString() : "",
						appId,
						resultSet.getString("application_name"),
						resultSet.getString("level"),
						resultSet.getString("message"),
						traceId,
						"",
						eventId != null ? eventId.toString() : "",
						"",
						"",
						0L,
						0,
						java.util.Map.of(),
						List.of()
					));
				}
			}
		} catch (Exception e) {
			log.warn("Failed to get related trace logs from ClickHouse", e);
		}
		return result;
	}
}

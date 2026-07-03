package com.vdt.log_monitoring.modules.analytics.internal;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;

import lombok.extern.slf4j.Slf4j;

@Repository
@Slf4j
public class ClickHouseAnalyticsRepository {

	private final DataSource clickHouseDataSource;

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
}

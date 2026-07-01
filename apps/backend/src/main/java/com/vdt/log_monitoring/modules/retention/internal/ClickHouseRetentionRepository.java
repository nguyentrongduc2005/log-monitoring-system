package com.vdt.log_monitoring.modules.retention.internal;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;

import com.vdt.log_monitoring.modules.retention.api.RetentionException;

@Repository
public class ClickHouseRetentionRepository {

	private static final String COUNT_SQL = """
		SELECT count()
		FROM processed_logs
		WHERE level = ?
		  AND log_timestamp < ?
		""";

	private static final String DELETE_SQL = """
		ALTER TABLE processed_logs
		DELETE WHERE level = ?
		  AND log_timestamp < ?
		""";

	private final DataSource clickHouseDataSource;

	public ClickHouseRetentionRepository(@Qualifier("clickHouseDataSource") DataSource clickHouseDataSource) {
		this.clickHouseDataSource = clickHouseDataSource;
	}

	public long deleteExpiredLogs(String level, Instant cutoff) {
		long affectedRows = countExpiredLogs(level, cutoff);

		try (
			var connection = clickHouseDataSource.getConnection();
			var statement = connection.prepareStatement(DELETE_SQL)
		) {
			statement.setString(1, level);
			statement.setTimestamp(2, Timestamp.from(cutoff));
			statement.executeUpdate();
			return affectedRows;
		} catch (SQLException exception) {
			throw new RetentionException(
				RetentionException.ErrorCode.RETENTION_DELETE_FAILED,
				"Failed to delete expired " + level + " logs from ClickHouse",
				exception
			);
		}
	}

	private long countExpiredLogs(String level, Instant cutoff) {
		try (
			var connection = clickHouseDataSource.getConnection();
			var statement = connection.prepareStatement(COUNT_SQL)
		) {
			statement.setString(1, level);
			statement.setTimestamp(2, Timestamp.from(cutoff));
			try (var resultSet = statement.executeQuery()) {
				return resultSet.next() ? resultSet.getLong(1) : 0;
			}
		} catch (SQLException exception) {
			throw new RetentionException(
				RetentionException.ErrorCode.RETENTION_DELETE_FAILED,
				"Failed to count expired " + level + " logs in ClickHouse",
				exception
			);
		}
	}
}

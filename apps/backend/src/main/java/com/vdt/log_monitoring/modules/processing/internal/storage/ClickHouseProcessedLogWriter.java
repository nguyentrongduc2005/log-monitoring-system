package com.vdt.log_monitoring.modules.processing.internal.storage;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.List;
import java.util.Objects;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.vdt.log_monitoring.modules.processing.api.ProcessingException;
import com.vdt.log_monitoring.modules.processing.internal.model.ProcessedLog;

@Component
public class ClickHouseProcessedLogWriter implements LogWriter {

    private static final String INSERT_SQL_PREFIX = """
            INSERT INTO processed_logs
            (
                event_id,
                ingestion_id,
                application_id,
                application_name,
                application_display_name,
                level,
                message,
                trace_id,
                log_timestamp,
                received_at,
                processed_at,
                fingerprint,
                status
            )
            VALUES
            """;
    private static final String INSERT_VALUE_PLACEHOLDERS = "(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

    private final DataSource clickHouseDataSource;

    public ClickHouseProcessedLogWriter(@Qualifier("clickHouseDataSource") DataSource clickHouseDataSource) {
        this.clickHouseDataSource = clickHouseDataSource;
    }

    private final List<ProcessedLog> buffer = java.util.Collections.synchronizedList(new java.util.ArrayList<>());

    @Override
    public void write(ProcessedLog log) {
        buffer.add(Objects.requireNonNull(log, "log must not be null"));
        if (buffer.size() >= 100) {
            flush();
        }
    }

    @org.springframework.scheduling.annotation.Scheduled(fixedDelay = 500)
    public void flush() {
        List<ProcessedLog> toWrite;
        synchronized (buffer) {
            if (buffer.isEmpty()) return;
            toWrite = new java.util.ArrayList<>(buffer);
            buffer.clear();
        }
        writeBatch(toWrite);
    }

    @Override
    public void writeBatch(List<ProcessedLog> logs) {
        if (logs == null || logs.isEmpty()) {
            return;
        }

        try (
                var connection = clickHouseDataSource.getConnection();
                var statement = connection.prepareStatement(buildInsertSql(logs.size()))) {
            int parameterIndex = 1;
            for (ProcessedLog log : logs) {
                parameterIndex = bind(statement, Objects.requireNonNull(log, "log must not be null"), parameterIndex);
            }

            statement.executeUpdate();
        } catch (SQLException ex) {
            throw new ProcessingException(
                    ProcessingException.ErrorCode.LOG_STORAGE_FAILED,
                    "Failed to write processed logs to ClickHouse",
                    ex);
        }
    }

    private String buildInsertSql(int logCount) {
        return INSERT_SQL_PREFIX + String.join(", ", java.util.Collections.nCopies(logCount, INSERT_VALUE_PLACEHOLDERS));
    }

    private int bind(PreparedStatement statement, ProcessedLog log, int index) throws SQLException {
        statement.setObject(index++, log.eventId());
        statement.setObject(index++, log.ingestionId());
        statement.setObject(index++, log.applicationId());
        statement.setString(index++, log.applicationName());
        setNullableString(statement, index++, log.applicationDisplayName());
        statement.setString(index++, log.level().name());
        statement.setString(index++, log.message());
        setNullableString(statement, index++, log.traceId());
        statement.setTimestamp(index++, Timestamp.from(log.logTimestamp()));
        statement.setTimestamp(index++, Timestamp.from(log.receivedAt()));
        statement.setTimestamp(index++, Timestamp.from(log.processedAt()));
        setNullableString(statement, index++, log.fingerprint() == null ? null : log.fingerprint().value());
        statement.setString(index++, log.status().name());
        return index;
    }

    private void setNullableString(PreparedStatement statement, int index, String value) throws SQLException {
        if (value == null) {
            statement.setNull(index, Types.VARCHAR);
            return;
        }

        statement.setString(index, value);
    }
}

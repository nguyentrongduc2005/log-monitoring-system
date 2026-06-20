package com.vdt.log_monitoring.modules.processing.internal.storage;

import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import javax.sql.DataSource;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.util.StreamUtils;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class ClickHouseMigrationRunner implements ApplicationRunner {

    private static final String HISTORY_TABLE = "clickhouse_schema_history";
    private static final String CREATE_HISTORY_TABLE_SQL = """
            CREATE TABLE IF NOT EXISTS clickhouse_schema_history
            (
                version String,
                description String,
                script String,
                checksum Int32,
                installed_at DateTime64(3, 'UTC')
            )
            ENGINE = ReplacingMergeTree(installed_at)
            ORDER BY version
            """;

    private final DataSource clickHouseDataSource;
    private final ClickHouseMigrationProperties properties;
    private final ResourcePatternResolver resourcePatternResolver;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        Resource[] migrationResources = resolveMigrationResources();
        if (migrationResources.length == 0) {
            return;
        }

        try (var connection = clickHouseDataSource.getConnection()) {
            try (var statement = connection.createStatement()) {
                statement.execute(CREATE_HISTORY_TABLE_SQL);
            }

            for (Resource resource : migrationResources) {
                MigrationScript script = MigrationScript.from(resource);
                if (isApplied(connection, script.version())) {
                    continue;
                }

                executeScript(connection, script);
                recordApplied(connection, script);
            }
        }
    }

    private Resource[] resolveMigrationResources() throws Exception {
        List<Resource> resources = new ArrayList<>();
        for (String location : properties.locations()) {
            String pattern = location.contains("*") ? location : location + "/*.sql";
            resources.addAll(Arrays.asList(resourcePatternResolver.getResources(pattern)));
        }

        return resources.stream()
                .filter(Resource::exists)
                .sorted(Comparator.comparing(resource -> {
                    try {
                        return resource.getFilename();
                    } catch (Exception ex) {
                        return "";
                    }
                }))
                .toArray(Resource[]::new);
    }

    private boolean isApplied(java.sql.Connection connection, String version) throws SQLException {
        String sql = "SELECT count() FROM " + HISTORY_TABLE + " WHERE version = ?";
        try (var statement = connection.prepareStatement(sql)) {
            statement.setString(1, version);
            try (var resultSet = statement.executeQuery()) {
                return resultSet.next() && resultSet.getLong(1) > 0;
            }
        }
    }

    private void executeScript(java.sql.Connection connection, MigrationScript script) throws SQLException {
        for (String statementSql : splitStatements(script.sql())) {
            if (statementSql.isBlank()) {
                continue;
            }

            try (var statement = connection.createStatement()) {
                statement.execute(statementSql);
            }
        }
    }

    private void recordApplied(java.sql.Connection connection, MigrationScript script) throws SQLException {
        String sql = "INSERT INTO " + HISTORY_TABLE
                + " (version, description, script, checksum, installed_at) VALUES (?, ?, ?, ?, ?)";
        try (var statement = connection.prepareStatement(sql)) {
            statement.setString(1, script.version());
            statement.setString(2, script.description());
            statement.setString(3, script.filename());
            statement.setInt(4, script.sql().hashCode());
            statement.setTimestamp(5, Timestamp.from(Instant.now()));
            statement.executeUpdate();
        }
    }

    private List<String> splitStatements(String sql) {
        return Arrays.stream(sql.split(";"))
                .map(String::trim)
                .filter(statement -> !statement.isBlank())
                .toList();
    }

    private record MigrationScript(
            String version,
            String description,
            String filename,
            String sql) {

        private static MigrationScript from(Resource resource) throws Exception {
            String filename = resource.getFilename();
            String sql = StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);
            String version = filename;
            String description = "";

            if (filename != null && filename.startsWith("V") && filename.contains("__")) {
                int separatorIndex = filename.indexOf("__");
                version = filename.substring(1, separatorIndex);
                description = filename.substring(separatorIndex + 2).replace(".sql", "").replace('_', ' ');
            }

            return new MigrationScript(version, description, filename, sql);
        }
    }
}

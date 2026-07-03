package com.vdt.log_monitoring.modules.processing.internal.storage;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.clickhouse.migration")
public record ClickHouseMigrationProperties(
                boolean enabled,
                String[] locations) {
}

package com.vdt.log_monitoring.modules.processing.internal.storage;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.datasource.clickhouse")
public record ClickHouseDataSourceProperties(
                String url,
                String username,
                String password) {
}

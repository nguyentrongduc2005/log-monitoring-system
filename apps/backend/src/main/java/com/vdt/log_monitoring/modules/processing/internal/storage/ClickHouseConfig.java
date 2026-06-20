package com.vdt.log_monitoring.modules.processing.internal.storage;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.support.ResourcePatternResolver;

import com.zaxxer.hikari.HikariDataSource;

@Configuration
@EnableConfigurationProperties({
        ClickHouseDataSourceProperties.class,
        ClickHouseMigrationProperties.class
})
public class ClickHouseConfig {
    @Bean
    @Primary
    @ConfigurationProperties("spring.datasource")
    DataSourceProperties dataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean
    @Primary
    DataSource dataSource(DataSourceProperties dataSourceProperties) {
        return dataSourceProperties.initializeDataSourceBuilder()
                .type(HikariDataSource.class)
                .build();
    }

    @Bean("clickHouseDataSource")
    DataSource clickHouseDataSource(ClickHouseDataSourceProperties properties) {
        HikariDataSource dataSource = new HikariDataSource();
        dataSource.setDriverClassName("com.clickhouse.jdbc.Driver");
        dataSource.setJdbcUrl(properties.url());
        dataSource.setUsername(properties.username());
        dataSource.setPassword(properties.password());
        dataSource.setMaximumPoolSize(10);
        dataSource.setMinimumIdle(1);
        dataSource.setPoolName("clickhouse-pool");
        return dataSource;
    }

    @Bean
    @ConditionalOnProperty(prefix = "app.clickhouse.migration", name = "enabled", havingValue = "true")
    ApplicationRunner clickHouseMigrationRunner(
            @Qualifier("clickHouseDataSource") DataSource clickHouseDataSource,
            ClickHouseMigrationProperties properties,
            ResourcePatternResolver resourcePatternResolver) {
        return new ClickHouseMigrationRunner(clickHouseDataSource, properties, resourcePatternResolver);
    }
}

package com.vdt.log_monitoring.api.identity.dto;

import java.time.Instant;
import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

public class MetricSourceDto {
    
    @Builder
    public record MetricSourceResponse(
        UUID id,
        UUID applicationId,
        String targetHost,
        Integer targetPort,
        String metricsPath,
        String scrapeInterval,
        boolean enabled,
        Instant createdAt,
        Instant updatedAt
    ) {}

    public record MetricSourceRequest(
        @NotBlank String targetHost,
        @NotNull Integer targetPort,
        @NotBlank String metricsPath,
        @NotBlank String scrapeInterval,
        @NotNull Boolean enabled
    ) {}
}

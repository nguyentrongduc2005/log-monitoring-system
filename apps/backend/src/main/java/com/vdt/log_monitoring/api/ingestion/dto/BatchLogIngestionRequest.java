package com.vdt.log_monitoring.api.ingestion.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class BatchLogIngestionRequest {
    @NotBlank(message = "Application name is required")
    @Size(max = 100, message = "Application name cannot exceed 100 characters")
    private String applicationName;
    @NotEmpty(message = "Raw logs are required")
    @Size(max = 500, message = "Batch cannot exceed 500 raw logs")
    private String @NotBlank(message = "Raw log cannot be blank") [] rawLogs;
}

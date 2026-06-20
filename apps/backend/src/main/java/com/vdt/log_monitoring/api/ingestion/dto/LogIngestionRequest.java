package com.vdt.log_monitoring.api.ingestion.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class LogIngestionRequest {
    @NotBlank(message = "Application name is required")
    @Size(max = 100, message = "Application name cannot exceed 100 characters")
    private String applicationName;
    @NotBlank(message = "Raw log is required")
    private String rawLog;
}

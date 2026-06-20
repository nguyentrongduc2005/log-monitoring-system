package com.vdt.log_monitoring.api.ingestion.dto;

import java.time.Instant;
import java.util.UUID;

import com.vdt.log_monitoring.modules.ingestion.api.LogIngestionFacade;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LogIngestionResponse {
    private UUID eventId;
    private UUID ingestionId;
    private String applicationName;
    private String applicationDisplayName;
    private Instant receivedAt;

    public static LogIngestionResponse from(LogIngestionFacade.IngestLogResult result) {
        return LogIngestionResponse.builder()
                .eventId(result.eventId())
                .ingestionId(result.ingestionId())
                .applicationName(result.applicationName())
                .applicationDisplayName(result.applicationDisplayName())
                .receivedAt(result.receivedAt())
                .build();
    }
}

package com.vdt.log_monitoring.api.logs.dto;

import java.time.Instant;
import java.util.UUID;

import com.vdt.log_monitoring.modules.logs.api.LogsIngestionFacade;

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

    public static LogIngestionResponse from(LogsIngestionFacade.IngestLogResult result) {
        return LogIngestionResponse.builder()
                .eventId(result.eventId())
                .ingestionId(result.ingestionId())
                .applicationName(result.applicationName())
                .applicationDisplayName(result.applicationDisplayName())
                .receivedAt(result.receivedAt())
                .build();
    }
}

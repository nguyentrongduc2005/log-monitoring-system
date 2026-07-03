package com.vdt.log_monitoring.api.ingestion.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.vdt.log_monitoring.modules.ingestion.api.LogIngestionFacade;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class BatchLogIngestionResponse {
        private List<IngestedLogItem> items;
        private int ingestedCount;
        private UUID ingestionId;
        private String applicationName;
        private String applicationDisplayName;
        private Instant receivedAt;

        public static BatchLogIngestionResponse from(LogIngestionFacade.BatchIngestLogResult result) {
                return BatchLogIngestionResponse.builder()
                                .items(
                                                result.items().stream()
                                                                .map(item -> new IngestedLogItem(item.index(),
                                                                                item.eventId()))
                                                                .toList())
                                .ingestedCount(result.ingestedCount())
                                .ingestionId(result.ingestionId())
                                .applicationName(result.applicationName())
                                .applicationDisplayName(result.applicationDisplayName())
                                .receivedAt(result.receivedAt())
                                .build();
        }

        public record IngestedLogItem(
                        int index,
                        UUID eventId) {
        }
}

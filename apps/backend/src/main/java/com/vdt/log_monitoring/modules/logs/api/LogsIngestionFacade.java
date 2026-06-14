package com.vdt.log_monitoring.modules.logs.api;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface LogsIngestionFacade {

        IngestLogResult ingest(IngestLogCommand command);

        BatchIngestLogResult batchIngest(BatchIngestLogCommand command);

        public record IngestLogCommand(
                        String apiKey,
                        String applicationName,
                        String rawLog,
                        String idempotencyKey) {
        }

        public record IngestLogResult(
                        UUID eventId,
                        UUID ingestionId,
                        String applicationName,
                        String applicationDisplayName,
                        Instant receivedAt) {
        }

        public record BatchIngestLogResult(
                        UUID ingestionId,
                        String applicationName,
                        String applicationDisplayName,
                        int ingestedCount,
                        Instant receivedAt,
                        List<IngestedLogItem> items) {

        }

        public record IngestedLogItem(
                        int index,
                        UUID eventId) {
        }

        public record BatchIngestLogCommand(
                        String apiKey,
                        String applicationName,
                        String[] rawLogs,
                        String idempotencyKey) {
        }
}

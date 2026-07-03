package com.vdt.log_monitoring.modules.ingestion.internal;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.IntStream;

import org.springframework.stereotype.Service;

import com.vdt.log_monitoring.modules.identity.api.ApplicationAccessFacade;
import com.vdt.log_monitoring.modules.ingestion.api.IngestionException;
import com.vdt.log_monitoring.modules.ingestion.api.LogIngestionFacade;
import com.vdt.log_monitoring.modules.ingestion.api.LogIngestionFacade.BatchIngestLogCommand;
import com.vdt.log_monitoring.modules.ingestion.api.LogIngestionFacade.BatchIngestLogResult;
import com.vdt.log_monitoring.modules.ingestion.api.LogIngestionFacade.IngestLogCommand;
import com.vdt.log_monitoring.modules.ingestion.api.LogIngestionFacade.IngestLogResult;
import com.vdt.log_monitoring.modules.ingestion.api.events.RawLogReceivedEvent;
import com.vdt.log_monitoring.modules.ingestion.internal.storage.LogIngestionIdempotencyStore;

import com.vdt.log_monitoring.modules.ingestion.internal.storage.IngestionMetricsStore;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class LogIngestionService {
        private static final int SINGLE_LOG_INDEX = 0;
        private static final int MAX_BATCH_SIZE = 500;

        private final ApplicationAccessFacade applicationAccessFacade;
        private final RawLogPublisher rawLogPublisher;
        private final LogIngestionIdempotencyStore idempotencyStore;
        private final RawLogSanitizer rawLogSanitizer;
        private final IngestionMetricsStore metricsStore;

        public IngestLogResult ingest(IngestLogCommand command) {
                validateRawLog(command.rawLog());

                Instant receivedAt = Instant.now();
                String idempotencyKey = normalizeIdempotencyKey(command.idempotencyKey());
                ApplicationAccessFacade.ApiKeyVerificationDto application = verifyApplicationAccess(
                                command.apiKey(),
                                command.applicationName());
                UUID ingestionId = prepareIngestionId(
                                application.applicationId(),
                                idempotencyKey,
                                new String[] { command.rawLog() });
                UUID eventId = publishRawLog(
                                application,
                                idempotencyKey,
                                SINGLE_LOG_INDEX,
                                ingestionId,
                                command.rawLog(),
                                receivedAt);

                metricsStore.increment(application.applicationId(), 1);

                return new IngestLogResult(
                                eventId,
                                ingestionId,
                                application.applicationName(),
                                application.applicationDisplayName(),
                                receivedAt);
        }

        public BatchIngestLogResult batchIngest(BatchIngestLogCommand command) {
                validateBatch(command.rawLogs());

                Instant receivedAt = Instant.now();
                String idempotencyKey = normalizeIdempotencyKey(command.idempotencyKey());
                ApplicationAccessFacade.ApiKeyVerificationDto application = verifyApplicationAccess(
                                command.apiKey(),
                                command.applicationName());
                UUID ingestionId = prepareIngestionId(application.applicationId(), idempotencyKey, command.rawLogs());

                List<LogIngestionFacade.IngestedLogItem> items = IntStream.range(0, command.rawLogs().length)
                                .mapToObj(index -> ingestBatchItem(
                                                application,
                                                idempotencyKey,
                                                ingestionId,
                                                command.rawLogs()[index],
                                                index,
                                                receivedAt))
                                .toList();

                metricsStore.increment(application.applicationId(), command.rawLogs().length);

                return new BatchIngestLogResult(
                                ingestionId,
                                application.applicationName(),
                                application.applicationDisplayName(),
                                items.size(),
                                receivedAt,
                                items);
        }

        private LogIngestionFacade.IngestedLogItem ingestBatchItem(
                        ApplicationAccessFacade.ApiKeyVerificationDto application,
                        String idempotencyKey,
                        UUID ingestionId,
                        String rawLog,
                        int index,
                        Instant receivedAt) {
                UUID eventId = publishRawLog(
                                application,
                                idempotencyKey,
                                index,
                                ingestionId,
                                rawLog,
                                receivedAt);

                return new LogIngestionFacade.IngestedLogItem(index, eventId);
        }

        private ApplicationAccessFacade.ApiKeyVerificationDto verifyApplicationAccess(
                        String apiKey,
                        String requestedApplicationName) {
                ApplicationAccessFacade.ApiKeyVerificationDto verification = applicationAccessFacade
                                .verifyApplicationApiKey(apiKey);

                if (!verification.valid()) {
                        throw new IngestionException(
                                        IngestionException.ErrorCode.INVALID_API_KEY,
                                        verification.failureReason());
                }

                if (!Objects.equals(verification.applicationName(), requestedApplicationName)) {
                        throw new IngestionException(
                                        IngestionException.ErrorCode.API_KEY_NOT_ALLOWED_FOR_APPLICATION,
                                        "API key is not allowed to ingest logs for this application");
                }

                return verification;
        }

        private UUID prepareIngestionId(UUID applicationId, String idempotencyKey, String[] rawLogs) {
                if (!hasIdempotencyKey(idempotencyKey)) {
                        return UUID.randomUUID();
                }

                return idempotencyStore
                                .prepareBatch(applicationId, idempotencyKey, rawLogs)
                                .ingestionId();
        }

        private UUID publishRawLog(
                        ApplicationAccessFacade.ApiKeyVerificationDto application,
                        String idempotencyKey,
                        int index,
                        UUID ingestionId,
                        String rawLog,
                        Instant receivedAt) {
                if (hasIdempotencyKey(idempotencyKey)) {
                        var publishedEventId = idempotencyStore.findPublishedEventId(
                                        application.applicationId(),
                                        idempotencyKey,
                                        index);
                        if (publishedEventId.isPresent()) {
                                return publishedEventId.get();
                        }
                }

                UUID eventId = UUID.randomUUID();
                String redactedRawLog = rawLogSanitizer.redact(rawLog);
                rawLogPublisher.publish(buildRawLogReceivedEvent(
                                eventId,
                                ingestionId,
                                application,
                                redactedRawLog,
                                receivedAt));

                if (hasIdempotencyKey(idempotencyKey)) {
                        idempotencyStore.markPublished(
                                        application.applicationId(),
                                        idempotencyKey,
                                        index,
                                        eventId);
                }

                return eventId;
        }

        private RawLogReceivedEvent buildRawLogReceivedEvent(
                        UUID eventId,
                        UUID ingestionId,
                        ApplicationAccessFacade.ApiKeyVerificationDto application,
                        String rawLog,
                        Instant receivedAt) {
                return new RawLogReceivedEvent(
                                eventId,
                                ingestionId,
                                application.applicationId(),
                                application.applicationName(),
                                application.applicationDisplayName(),
                                rawLog,
                                receivedAt,
                                RawLogReceivedEvent.CURRENT_SCHEMA_VERSION);
        }

        private void validateBatch(String[] rawLogs) {
                if (rawLogs == null || rawLogs.length == 0) {
                        throw new IngestionException(
                                        IngestionException.ErrorCode.INVALID_BATCH,
                                        "Raw logs are required");
                }

                if (rawLogs.length > MAX_BATCH_SIZE) {
                        throw new IngestionException(
                                        IngestionException.ErrorCode.INVALID_BATCH,
                                        "Batch cannot exceed 500 raw logs");
                }

                for (String rawLog : rawLogs) {
                        validateRawLog(rawLog);
                }
        }

        private void validateRawLog(String rawLog) {
                if (rawLog == null || rawLog.isBlank()) {
                        throw new IngestionException(
                                        IngestionException.ErrorCode.INVALID_RAW_LOG,
                                        "Raw log cannot be blank");
                }
        }

        private String normalizeIdempotencyKey(String idempotencyKey) {
                if (!hasIdempotencyKey(idempotencyKey)) {
                        return null;
                }

                return idempotencyKey.trim();
        }

        private boolean hasIdempotencyKey(String idempotencyKey) {
                return idempotencyKey != null && !idempotencyKey.isBlank();
        }
}

package com.vdt.log_monitoring.modules.logs.internal.storage;

import java.util.Optional;
import java.util.UUID;

public interface LogIngestionIdempotencyStore {
    BatchIdempotencyContext prepareBatch(UUID applicationId, String idempotencyKey, String[] rawLogs);

    Optional<UUID> findPublishedEventId(UUID applicationId, String idempotencyKey, int index);

    void markPublished(UUID applicationId, String idempotencyKey, int index, UUID eventId);

    record BatchIdempotencyContext(UUID ingestionId) {
    }
}

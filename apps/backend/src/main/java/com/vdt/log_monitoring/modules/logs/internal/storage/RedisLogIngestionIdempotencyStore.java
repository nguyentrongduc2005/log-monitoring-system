package com.vdt.log_monitoring.modules.logs.internal.storage;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import com.vdt.log_monitoring.modules.logs.api.LogsException;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class RedisLogIngestionIdempotencyStore implements LogIngestionIdempotencyStore {
    private final StringRedisTemplate redisTemplate;

    @Value("${app.logs.idempotency.key-prefix:logs:ingestion:idempotency}")
    private String keyPrefix;

    @Value("${app.logs.idempotency.ttl:24h}")
    private Duration ttl;

    @Override
    public BatchIdempotencyContext prepareBatch(UUID applicationId, String idempotencyKey, String[] rawLogs) {
        String payloadHash = hashRawLogs(rawLogs);
        String hashKey = batchHashKey(applicationId, idempotencyKey);
        String ingestionIdKey = batchIngestionIdKey(applicationId, idempotencyKey);

        try {
            Boolean hashCreated = redisTemplate.opsForValue().setIfAbsent(hashKey, payloadHash, ttl);
            String existingHash = Boolean.TRUE.equals(hashCreated)
                    ? payloadHash
                    : redisTemplate.opsForValue().get(hashKey);

            if (!payloadHash.equals(existingHash)) {
                throw new LogsException(
                        LogsException.ErrorCode.IDEMPOTENCY_CONFLICT,
                        "Idempotency-Key was already used with a different batch payload");
            }

            UUID generatedIngestionId = UUID.randomUUID();
            redisTemplate.opsForValue().setIfAbsent(ingestionIdKey, generatedIngestionId.toString(), ttl);
            String storedIngestionId = redisTemplate.opsForValue().get(ingestionIdKey);

            return new BatchIdempotencyContext(UUID.fromString(storedIngestionId));
        } catch (DataAccessException ex) {
            throw ingestionUnavailable("Failed to access ingestion idempotency store", ex);
        }
    }

    @Override
    public Optional<UUID> findPublishedEventId(UUID applicationId, String idempotencyKey, int index) {
        try {
            String value = redisTemplate.opsForValue().get(itemKey(applicationId, idempotencyKey, index));
            return value == null ? Optional.empty() : Optional.of(UUID.fromString(value));
        } catch (DataAccessException ex) {
            throw ingestionUnavailable("Failed to access ingestion idempotency store", ex);
        }
    }

    @Override
    public void markPublished(UUID applicationId, String idempotencyKey, int index, UUID eventId) {
        try {
            redisTemplate.opsForValue().set(itemKey(applicationId, idempotencyKey, index), eventId.toString(), ttl);
        } catch (DataAccessException ex) {
            throw ingestionUnavailable("Failed to update ingestion idempotency store", ex);
        }
    }

    private String batchHashKey(UUID applicationId, String idempotencyKey) {
        return "%s:batch:%s:%s:hash".formatted(keyPrefix, applicationId, idempotencyKey);
    }

    private String batchIngestionIdKey(UUID applicationId, String idempotencyKey) {
        return "%s:batch:%s:%s:ingestion-id".formatted(keyPrefix, applicationId, idempotencyKey);
    }

    private String itemKey(UUID applicationId, String idempotencyKey, int index) {
        return "%s:item:%s:%s:%d".formatted(keyPrefix, applicationId, idempotencyKey, index);
    }

    private String hashRawLogs(String[] rawLogs) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            for (String rawLog : rawLogs) {
                byte[] bytes = rawLog.getBytes(StandardCharsets.UTF_8);
                digest.update(ByteBuffer.allocate(Integer.BYTES).putInt(bytes.length).array());
                digest.update(bytes);
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 algorithm is not available", ex);
        }
    }

    private LogsException ingestionUnavailable(String message, Exception cause) {
        LogsException exception = new LogsException(
                LogsException.ErrorCode.INGESTION_UNAVAILABLE,
                message);
        exception.initCause(cause);
        return exception;
    }
}

package com.vdt.log_monitoring.modules.alerting.api.events;

import java.time.Instant;
import java.util.UUID;

import com.vdt.log_monitoring.modules.alerting.internal.rule.AlertSeverity;

public record AlertCreatedEvent(
        UUID alertId,

        UUID ruleId,

        UUID applicationId,

        UUID eventId,

        UUID ingestionId,

        String applicationName,

        String applicationDisplayName,

        AlertSeverity severity,

        String message,

        String fingerprint,

        Instant logTimestamp,

        Instant triggeredAt,

        int schemaVersion) {
    public static final int CURRENT_SCHEMA_VERSION = 1;
}

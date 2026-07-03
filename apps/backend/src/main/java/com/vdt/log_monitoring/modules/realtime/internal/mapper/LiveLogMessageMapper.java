package com.vdt.log_monitoring.modules.realtime.internal.mapper;

import org.springframework.stereotype.Component;

import com.vdt.log_monitoring.modules.processing.api.events.RealtimeLogEvent;
import com.vdt.log_monitoring.modules.realtime.api.events.LiveLogMessage;

@Component
public class LiveLogMessageMapper {

    public LiveLogMessage toMessage(RealtimeLogEvent event) {
        if (event.schemaVersion() != RealtimeLogEvent.CURRENT_SCHEMA_VERSION) {
            throw new IllegalArgumentException("Unsupported realtime log schema version: " + event.schemaVersion());
        }

        return new LiveLogMessage(
                event.eventId(),
                event.ingestionId(),
                event.applicationId(),
                event.applicationName(),
                event.applicationDisplayName(),
                event.level(),
                event.message(),
                event.traceId(),
                event.logTimestamp(),
                event.processedAt());
    }
}

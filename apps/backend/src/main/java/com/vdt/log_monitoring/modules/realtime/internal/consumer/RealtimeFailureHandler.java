package com.vdt.log_monitoring.modules.realtime.internal.consumer;

import java.time.Instant;

import org.springframework.stereotype.Component;

import com.vdt.log_monitoring.modules.processing.api.events.LogProcessingFailedEvent;
import com.vdt.log_monitoring.modules.processing.api.events.RealtimeLogEvent;
import com.vdt.log_monitoring.modules.realtime.internal.publisher.RealtimeErrorPublisher;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class RealtimeFailureHandler {

    private static final String DEFAULT_FAILURE_CODE = "REALTIME_DELIVERY_FAILED";

    private final RealtimeErrorPublisher realtimeErrorPublisher;

    public void handle(RealtimeLogEvent event, String exceptionClass, String exceptionMessage) {
        LogProcessingFailedEvent failureEvent = new LogProcessingFailedEvent(
                event.eventId(),
                event.ingestionId(),
                event.applicationId(),
                event.applicationName(),
                failureCode(exceptionClass),
                failureMessage(exceptionMessage),
                Instant.now(),
                LogProcessingFailedEvent.CURRENT_SCHEMA_VERSION);

        realtimeErrorPublisher.publish(failureEvent);
    }

    private String failureCode(String exceptionClass) {
        if (exceptionClass != null && !exceptionClass.isBlank()) {
            return exceptionClass;
        }
        return DEFAULT_FAILURE_CODE;
    }

    private String failureMessage(String exceptionMessage) {
        if (exceptionMessage != null && !exceptionMessage.isBlank()) {
            return exceptionMessage;
        }
        return "Realtime log event moved to DLT after delivery failure";
    }
}

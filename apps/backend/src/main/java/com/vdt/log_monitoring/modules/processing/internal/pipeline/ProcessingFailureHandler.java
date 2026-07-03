package com.vdt.log_monitoring.modules.processing.internal.pipeline;

import java.time.Instant;

import org.springframework.stereotype.Component;

import com.vdt.log_monitoring.modules.ingestion.api.events.RawLogReceivedEvent;
import com.vdt.log_monitoring.modules.processing.api.ProcessingException;
import com.vdt.log_monitoring.modules.processing.api.events.LogProcessingFailedEvent;
import com.vdt.log_monitoring.modules.processing.internal.publisher.ProcessingErrorPublisher;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ProcessingFailureHandler {

    private final ProcessingErrorPublisher processingErrorPublisher;

    public void handle(RawLogReceivedEvent rawEvent, String exceptionClass, String exceptionMessage) {
        LogProcessingFailedEvent event = new LogProcessingFailedEvent(
                rawEvent.eventId(),
                rawEvent.ingestionId(),
                rawEvent.applicationId(),
                rawEvent.applicationName(),
                failureCode(exceptionClass),
                failureMessage(exceptionMessage),
                Instant.now(),
                LogProcessingFailedEvent.CURRENT_SCHEMA_VERSION);

        processingErrorPublisher.publish(event);
    }

    private String failureCode(String exceptionClass) {
        if (exceptionClass != null && !exceptionClass.isBlank()) {
            return exceptionClass;
        }
        return ProcessingException.ErrorCode.PROCESSING_UNAVAILABLE.name();
    }

    private String failureMessage(String exceptionMessage) {
        return exceptionMessage == null || exceptionMessage.isBlank()
                ? "Raw log moved to DLT after processing failure"
                : exceptionMessage;
    }

}

package com.vdt.log_monitoring.modules.realtime.internal.consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.vdt.log_monitoring.modules.processing.api.events.LogProcessingFailedEvent;
import com.vdt.log_monitoring.modules.processing.api.events.RealtimeLogEvent;
import com.vdt.log_monitoring.modules.realtime.internal.publisher.RealtimeErrorPublisher;

class RealtimeFailureHandlerTest {

    private static final UUID EVENT_ID = UUID.fromString("00000000-0000-0000-0000-000000000201");
    private static final UUID INGESTION_ID = UUID.fromString("00000000-0000-0000-0000-000000000202");
    private static final UUID APPLICATION_ID = UUID.fromString("00000000-0000-0000-0000-000000000203");

    @Test
    void handlePublishesRealtimeFailureToErrorTopic() {
        RealtimeErrorPublisher errorPublisher = mock();
        RealtimeFailureHandler handler = new RealtimeFailureHandler(errorPublisher);
        RealtimeLogEvent event = realtimeLogEvent();

        handler.handle(event, "java.lang.IllegalStateException", "websocket unavailable");

        ArgumentCaptor<LogProcessingFailedEvent> eventCaptor = ArgumentCaptor.forClass(LogProcessingFailedEvent.class);
        verify(errorPublisher).publish(eventCaptor.capture());

        LogProcessingFailedEvent failureEvent = eventCaptor.getValue();
        assertThat(failureEvent.eventId()).isEqualTo(EVENT_ID);
        assertThat(failureEvent.ingestionId()).isEqualTo(INGESTION_ID);
        assertThat(failureEvent.applicationId()).isEqualTo(APPLICATION_ID);
        assertThat(failureEvent.applicationName()).isEqualTo("payments");
        assertThat(failureEvent.failureCode()).isEqualTo("java.lang.IllegalStateException");
        assertThat(failureEvent.failureMessage()).isEqualTo("websocket unavailable");
        assertThat(failureEvent.schemaVersion()).isEqualTo(LogProcessingFailedEvent.CURRENT_SCHEMA_VERSION);
    }

    private RealtimeLogEvent realtimeLogEvent() {
        Instant timestamp = Instant.parse("2026-06-16T08:00:00Z");
        return new RealtimeLogEvent(
                EVENT_ID,
                INGESTION_ID,
                APPLICATION_ID,
                "payments",
                "Payments",
                "ERROR",
                "payment failed",
                "trace-1",
                timestamp,
                timestamp.plusSeconds(1),
                RealtimeLogEvent.CURRENT_SCHEMA_VERSION);
    }
}

package com.vdt.log_monitoring.modules.realtime.internal.publisher;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.KafkaTemplate;

import com.vdt.log_monitoring.modules.processing.api.events.LogProcessingFailedEvent;

class RealtimeErrorPublisherTest {

    private static final UUID EVENT_ID = UUID.fromString("00000000-0000-0000-0000-000000000301");
    private static final UUID INGESTION_ID = UUID.fromString("00000000-0000-0000-0000-000000000302");
    private static final UUID APPLICATION_ID = UUID.fromString("00000000-0000-0000-0000-000000000303");

    @Test
    void publishSendsFailureEventToProcessingErrorsTopic() {
        KafkaTemplate<String, LogProcessingFailedEvent> kafkaTemplate = mock();
        LogProcessingFailedEvent event = failureEvent();
        when(kafkaTemplate.send(eq("processing.errors"), eq(APPLICATION_ID.toString()), eq(event)))
                .thenReturn(CompletableFuture.completedFuture(null));
        RealtimeErrorPublisher publisher = new RealtimeErrorPublisher(
                kafkaTemplate,
                "processing.errors",
                Duration.ofSeconds(1));

        publisher.publish(event);

        verify(kafkaTemplate).send("processing.errors", APPLICATION_ID.toString(), event);
    }

    private LogProcessingFailedEvent failureEvent() {
        return new LogProcessingFailedEvent(
                EVENT_ID,
                INGESTION_ID,
                APPLICATION_ID,
                "payments",
                "REALTIME_DELIVERY_FAILED",
                "websocket unavailable",
                Instant.parse("2026-06-16T08:00:00Z"),
                LogProcessingFailedEvent.CURRENT_SCHEMA_VERSION);
    }
}

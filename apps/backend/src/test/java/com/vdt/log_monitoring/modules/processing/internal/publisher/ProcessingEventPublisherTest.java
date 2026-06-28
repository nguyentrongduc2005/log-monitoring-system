package com.vdt.log_monitoring.modules.processing.internal.publisher;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.kafka.core.KafkaTemplate;

import com.vdt.log_monitoring.modules.processing.api.events.AnomalySignalEvent;
import com.vdt.log_monitoring.modules.processing.api.events.CriticalLogDetectedEvent;
import com.vdt.log_monitoring.modules.processing.api.events.LogProcessingFailedEvent;
import com.vdt.log_monitoring.modules.processing.api.events.RealtimeLogEvent;
import com.vdt.log_monitoring.modules.processing.internal.model.LogFingerprint;
import com.vdt.log_monitoring.modules.processing.internal.model.LogLevel;
import com.vdt.log_monitoring.modules.processing.internal.model.LogMetadata;
import com.vdt.log_monitoring.modules.processing.internal.model.LogProcessingStatus;
import com.vdt.log_monitoring.modules.processing.internal.model.ProcessedLog;

class ProcessingEventPublisherTest {

    private static final UUID EVENT_ID = UUID.fromString("00000000-0000-0000-0000-000000000101");
    private static final UUID INGESTION_ID = UUID.fromString("00000000-0000-0000-0000-000000000102");
    private static final UUID APPLICATION_ID = UUID.fromString("00000000-0000-0000-0000-000000000103");
    private static final Instant LOG_TIMESTAMP = Instant.parse("2026-06-16T05:00:00Z");
    private static final Instant PROCESSED_AT = Instant.parse("2026-06-16T05:00:01Z");

    @Test
    void realtimePublisherSendsRealtimeEventToLiveTopic() {
        KafkaTemplate<String, RealtimeLogEvent> kafkaTemplate = mock();
        when(kafkaTemplate.send(eq("logs.live"), eq(APPLICATION_ID.toString()), org.mockito.ArgumentMatchers.any()))
                .thenReturn(CompletableFuture.completedFuture(null));
        RealtimeLogPublisher publisher = new RealtimeLogPublisher(kafkaTemplate, "logs.live", Duration.ofSeconds(1));

        publisher.publish(processedLog(LogLevel.INFO));

        ArgumentCaptor<RealtimeLogEvent> eventCaptor = ArgumentCaptor.forClass(RealtimeLogEvent.class);
        verify(kafkaTemplate).send(eq("logs.live"), eq(APPLICATION_ID.toString()), eventCaptor.capture());
        assertThat(eventCaptor.getValue().eventId()).isEqualTo(EVENT_ID);
        assertThat(eventCaptor.getValue().level()).isEqualTo("INFO");
        assertThat(eventCaptor.getValue().schemaVersion()).isEqualTo(RealtimeLogEvent.CURRENT_SCHEMA_VERSION);
    }

    @Test
    void criticalPublisherSendsCriticalEventToCriticalAlertsTopic() {
        KafkaTemplate<String, CriticalLogDetectedEvent> kafkaTemplate = mock();
        when(kafkaTemplate.send(eq("alerts.critical"), eq(APPLICATION_ID.toString()), org.mockito.ArgumentMatchers.any()))
                .thenReturn(CompletableFuture.completedFuture(null));
        CriticalLogDetectedPublisher publisher = new CriticalLogDetectedPublisher(
                kafkaTemplate,
                "alerts.critical",
                Duration.ofSeconds(1));

        publisher.publish(processedLog(LogLevel.CRITICAL));

        ArgumentCaptor<CriticalLogDetectedEvent> eventCaptor = ArgumentCaptor.forClass(CriticalLogDetectedEvent.class);
        verify(kafkaTemplate).send(eq("alerts.critical"), eq(APPLICATION_ID.toString()), eventCaptor.capture());
        assertThat(eventCaptor.getValue().eventId()).isEqualTo(EVENT_ID);
        assertThat(eventCaptor.getValue().traceId()).isEqualTo("trace-1");
        assertThat(eventCaptor.getValue().fingerprint()).isEqualTo("fingerprint-1");
        assertThat(eventCaptor.getValue().schemaVersion()).isEqualTo(CriticalLogDetectedEvent.CURRENT_SCHEMA_VERSION);
    }

    @Test
    void processingErrorPublisherSendsFailureEventToProcessingErrorsTopic() {
        KafkaTemplate<String, LogProcessingFailedEvent> kafkaTemplate = mock();
        when(kafkaTemplate.send(eq("processing.errors"), eq(APPLICATION_ID.toString()), org.mockito.ArgumentMatchers.any()))
                .thenReturn(CompletableFuture.completedFuture(null));
        ProcessingErrorPublisher publisher = new ProcessingErrorPublisher(
                kafkaTemplate,
                "processing.errors",
                Duration.ofSeconds(1));
        LogProcessingFailedEvent event = new LogProcessingFailedEvent(
                EVENT_ID,
                INGESTION_ID,
                APPLICATION_ID,
                "payments",
                "PROCESSING_UNAVAILABLE",
                "parse failed",
                PROCESSED_AT,
                LogProcessingFailedEvent.CURRENT_SCHEMA_VERSION);

        publisher.publish(event);

        verify(kafkaTemplate).send("processing.errors", APPLICATION_ID.toString(), event);
    }

    @Test
    void anomalyPublisherSendsSignalEventWithClassificationInputs() {
        KafkaTemplate<String, AnomalySignalEvent> kafkaTemplate = mock();
        when(kafkaTemplate.send(eq("logs.anomaly.signals"), eq(APPLICATION_ID.toString()), org.mockito.ArgumentMatchers.any()))
                .thenReturn(CompletableFuture.completedFuture(null));
        AnomalySignalPublisher publisher = new AnomalySignalPublisher(
                kafkaTemplate,
                "logs.anomaly.signals",
                Duration.ofSeconds(1));

        publisher.publish(processedLog(LogLevel.ERROR), "KEYWORD_MATCH_FAILED");

        ArgumentCaptor<AnomalySignalEvent> eventCaptor = ArgumentCaptor.forClass(AnomalySignalEvent.class);
        verify(kafkaTemplate).send(eq("logs.anomaly.signals"), eq(APPLICATION_ID.toString()), eventCaptor.capture());
        assertThat(eventCaptor.getValue().applicationId()).isEqualTo(APPLICATION_ID);
        assertThat(eventCaptor.getValue().logId()).isEqualTo(EVENT_ID);
        assertThat(eventCaptor.getValue().level()).isEqualTo("ERROR");
        assertThat(eventCaptor.getValue().message()).isEqualTo("payment failed");
        assertThat(eventCaptor.getValue().traceId()).isEqualTo("trace-1");
        assertThat(eventCaptor.getValue().matchedRule()).isEqualTo("KEYWORD_MATCH_FAILED");
        assertThat(eventCaptor.getValue().serviceName()).isEqualTo("payments");
    }

    private ProcessedLog processedLog(LogLevel level) {
        return new ProcessedLog(
                EVENT_ID,
                INGESTION_ID,
                APPLICATION_ID,
                "payments",
                "Payments",
                level,
                "payment failed",
                "trace-1",
                LOG_TIMESTAMP,
                LOG_TIMESTAMP,
                PROCESSED_AT,
                LogFingerprint.of("fingerprint-1"),
                LogProcessingStatus.STORED,
                LogMetadata.empty());
    }

}

package com.vdt.log_monitoring.modules.processing.internal.pipeline;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.vdt.log_monitoring.modules.processing.internal.model.RawLogEnvelope;
import com.vdt.log_monitoring.modules.processing.internal.publisher.AnomalySignalPublisher;
import com.vdt.log_monitoring.modules.processing.internal.publisher.CriticalLogDetectedPublisher;
import com.vdt.log_monitoring.modules.processing.internal.publisher.RealtimeLogPublisher;
import com.vdt.log_monitoring.modules.processing.internal.storage.LogWriter;

class LogProcessingServiceTest {

	private static final UUID EVENT_ID = UUID.fromString("00000000-0000-0000-0000-000000000101");
	private static final UUID INGESTION_ID = UUID.fromString("00000000-0000-0000-0000-000000000102");
	private static final UUID APPLICATION_ID = UUID.fromString("00000000-0000-0000-0000-000000000103");
	private static final Instant RECEIVED_AT = Instant.parse("2026-06-30T00:00:00Z");

	private final LogWriter logWriter = mock();
	private final RealtimeLogPublisher realtimeLogPublisher = mock();
	private final CriticalLogDetectedPublisher criticalLogDetectedPublisher = mock();
	private final AnomalySignalPublisher anomalySignalPublisher = mock();

	private final LogProcessingService service = new LogProcessingService(
		new LogParser(),
		new LogNormalizer(),
		new LogEnricher(),
		new LogFingerprinter(),
		logWriter,
		realtimeLogPublisher,
		criticalLogDetectedPublisher,
		anomalySignalPublisher);

	@Test
	void publishesAnomalySignalForWarnSuspiciousKeyword() {
		RawLogEnvelope envelope = envelope(
			"2026-06-30T00:00:00.000Z WARN service degraded for payment-service dependency=payment-gateway traceId=demo-suspicious-001");

		service.process(envelope);

		verify(anomalySignalPublisher).publish(any(), eq("KEYWORD_MATCH_DEGRADED"));
		verify(criticalLogDetectedPublisher, never()).publish(any());
	}

	private RawLogEnvelope envelope(String rawLog) {
		return new RawLogEnvelope(
			EVENT_ID,
			INGESTION_ID,
			APPLICATION_ID,
			"payment-service",
			"Payment Service",
			rawLog,
			RECEIVED_AT,
			1);
	}
}

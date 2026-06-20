package com.vdt.log_monitoring.modules.processing.internal.pipeline;

import org.springframework.stereotype.Service;

import com.vdt.log_monitoring.modules.processing.internal.model.LogProcessingStatus;
import com.vdt.log_monitoring.modules.processing.internal.model.ProcessedLog;
import com.vdt.log_monitoring.modules.processing.internal.model.RawLogEnvelope;
import com.vdt.log_monitoring.modules.processing.internal.publisher.CriticalLogDetectedPublisher;
import com.vdt.log_monitoring.modules.processing.internal.publisher.RealtimeLogPublisher;
import com.vdt.log_monitoring.modules.processing.internal.storage.LogWriter;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class LogProcessingService {

    private final LogParser logParser;
    private final LogNormalizer logNormalizer;
    private final LogEnricher logEnricher;
    private final LogFingerprinter logFingerprinter;
    private final LogWriter logWriter;
    private final RealtimeLogPublisher realtimeLogPublisher;
    private final CriticalLogDetectedPublisher criticalLogDetectedPublisher;

    public void process(RawLogEnvelope envelope) {
        ProcessedLog normalizedLog = normalize(envelope);
        ProcessedLog storedLog = store(normalizedLog);
        publishRealtimeLog(storedLog);

        if (storedLog.shouldPublishCriticalAlert()) {
            publishCriticalAlert(storedLog);
        }
    }

    private ProcessedLog normalize(RawLogEnvelope envelope) {
        var parsedLog = logParser.parse(envelope);
        ProcessedLog normalizedLog = logNormalizer.normalize(envelope, parsedLog);
        ProcessedLog enrichedLog = logEnricher.enrich(normalizedLog);
        return logFingerprinter.addFingerprint(enrichedLog);
    }

    private ProcessedLog store(ProcessedLog normalizedLog) {
        logWriter.write(normalizedLog);
        return normalizedLog.withStatus(LogProcessingStatus.STORED);
    }

    private void publishRealtimeLog(ProcessedLog log) {
        realtimeLogPublisher.publish(log);
    }

    private void publishCriticalAlert(ProcessedLog log) {
        criticalLogDetectedPublisher.publish(log);
    }
}

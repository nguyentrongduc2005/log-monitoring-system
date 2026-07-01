package com.vdt.log_monitoring.modules.ingestion.internal.impl;

import org.springframework.stereotype.Component;

import com.vdt.log_monitoring.modules.ingestion.api.LogIngestionFacade;
import com.vdt.log_monitoring.modules.ingestion.internal.LogIngestionService;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class LogIngestionFacadeImpl implements LogIngestionFacade {

    private final LogIngestionService ingestionService;
    private final com.vdt.log_monitoring.modules.ingestion.internal.storage.IngestionMetricsStore metricsStore;

    @Override
    public IngestLogResult ingest(IngestLogCommand command) {
        return ingestionService.ingest(command);
    }

    @Override
    public BatchIngestLogResult batchIngest(BatchIngestLogCommand command) {
        return ingestionService.batchIngest(command);
    }

    @Override
    public long getIngestedLogsPerMinute(java.util.List<java.util.UUID> applicationIds) {
        return metricsStore.getLogsCountLastMinute(applicationIds);
    }

}

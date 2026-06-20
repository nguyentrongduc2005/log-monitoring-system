package com.vdt.log_monitoring.modules.ingestion.internal.impl;

import org.springframework.stereotype.Component;

import com.vdt.log_monitoring.modules.ingestion.api.LogIngestionFacade;
import com.vdt.log_monitoring.modules.ingestion.internal.LogIngestionService;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class LogIngestionFacadeImpl implements LogIngestionFacade {

    private final LogIngestionService ingestionService;

    @Override
    public IngestLogResult ingest(IngestLogCommand command) {
        return ingestionService.ingest(command);
    }

    @Override
    public BatchIngestLogResult batchIngest(BatchIngestLogCommand command) {
        return ingestionService.batchIngest(command);
    }

}

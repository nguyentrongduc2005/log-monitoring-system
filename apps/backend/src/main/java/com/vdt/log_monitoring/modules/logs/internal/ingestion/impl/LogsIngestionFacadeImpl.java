package com.vdt.log_monitoring.modules.logs.internal.ingestion.impl;

import org.springframework.stereotype.Component;

import com.vdt.log_monitoring.modules.logs.api.LogsIngestionFacade;
import com.vdt.log_monitoring.modules.logs.internal.ingestion.LogIngestionService;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class LogsIngestionFacadeImpl implements LogsIngestionFacade {

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

package com.vdt.log_monitoring.modules.processing.internal.pipeline;

import org.springframework.stereotype.Component;

import com.vdt.log_monitoring.modules.processing.internal.model.ProcessedLog;

@Component
public class LogEnricher {

    public ProcessedLog enrich(ProcessedLog log) {
        return log;
    }
}

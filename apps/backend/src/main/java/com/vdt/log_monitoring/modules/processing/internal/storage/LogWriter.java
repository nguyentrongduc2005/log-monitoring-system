package com.vdt.log_monitoring.modules.processing.internal.storage;

import java.util.List;

import com.vdt.log_monitoring.modules.processing.internal.model.ProcessedLog;

public interface LogWriter {
    void write(ProcessedLog log);

    void writeBatch(List<ProcessedLog> logs);
}

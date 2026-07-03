package com.vdt.log_monitoring.modules.ingestion.internal;

import com.vdt.log_monitoring.modules.ingestion.api.events.RawLogReceivedEvent;

public interface RawLogPublisher {
    void publish(RawLogReceivedEvent event);
}

package com.vdt.log_monitoring.modules.logs.internal.ingestion;

import com.vdt.log_monitoring.modules.logs.api.events.RawLogReceivedEvent;

public interface RawLogPublisher {
    void publish(RawLogReceivedEvent event);
}

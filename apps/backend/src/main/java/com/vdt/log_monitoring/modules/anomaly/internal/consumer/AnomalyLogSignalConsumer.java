package com.vdt.log_monitoring.modules.anomaly.internal.consumer;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.vdt.log_monitoring.shared.event.AnomalySignalEvent;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class AnomalyLogSignalConsumer {

    @KafkaListener(
        topics = "${app.kafka.topics.anomaly-signals}", 
        groupId = "${app.kafka.consumer-groups.anomaly-signals}"
    )
    public void consume(AnomalySignalEvent event) {
        log.info("Received Anomaly Signal: logId={}, level={}, matchedRule={}, serviceName={}", 
            event.logId(), event.level(), event.matchedRule(), event.serviceName());
        
        // TODO: In Phase 2, this will save to Redis for count aggregation and scoring.
    }
}

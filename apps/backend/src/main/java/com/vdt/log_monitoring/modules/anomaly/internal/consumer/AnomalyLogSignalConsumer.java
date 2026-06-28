package com.vdt.log_monitoring.modules.anomaly.internal.consumer;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.vdt.log_monitoring.modules.anomaly.internal.log.AnomalyLogRuleHandler;
import com.vdt.log_monitoring.modules.processing.api.events.AnomalySignalEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class AnomalyLogSignalConsumer {

    private final AnomalyLogRuleHandler anomalyLogRuleHandler;

    @KafkaListener(
        topics = "${app.kafka.topics.anomaly-signals}", 
        groupId = "${app.kafka.consumer-groups.anomaly-signals}"
    )
    public void consume(AnomalySignalEvent event) {
        log.debug("Received Anomaly Signal: logId={}, level={}, matchedRule={}, serviceName={}",
            event.logId(), event.level(), event.matchedRule(), event.serviceName());
        anomalyLogRuleHandler.handle(event);
    }
}

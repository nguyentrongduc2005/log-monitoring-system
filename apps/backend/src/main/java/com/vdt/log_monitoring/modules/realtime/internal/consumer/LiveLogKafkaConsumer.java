package com.vdt.log_monitoring.modules.realtime.internal.consumer;

import org.springframework.kafka.annotation.DltHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.retry.annotation.Backoff;
import org.springframework.stereotype.Component;

import com.vdt.log_monitoring.modules.processing.api.events.RealtimeLogEvent;
import com.vdt.log_monitoring.modules.realtime.api.RealtimeFacade;
import com.vdt.log_monitoring.modules.realtime.internal.mapper.LiveLogMessageMapper;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class LiveLogKafkaConsumer {

    private final LiveLogMessageMapper liveLogMessageMapper;
    private final RealtimeFacade realtimeFacade;
    private final RealtimeFailureHandler failureHandler;

    @RetryableTopic(
            attempts = "${app.kafka.retry.realtime-delivery-attempts}",
            autoCreateTopics = "${app.kafka.retry.auto-create-topics}",
            backoff = @Backoff(delayExpression = "${app.kafka.retry.realtime-delivery-backoff-ms}"),
            dltTopicSuffix = "${app.kafka.retry.dlt-topic-suffix}")
    @KafkaListener(
            topics = "${app.kafka.topics.live}",
            groupId = "${app.kafka.consumer-groups.realtime}",
            properties = "spring.json.value.default.type=com.vdt.log_monitoring.modules.processing.api.events.RealtimeLogEvent")
    public void consume(RealtimeLogEvent event, Acknowledgment acknowledgment) {
        realtimeFacade.publishLiveLog(liveLogMessageMapper.toMessage(event));
        acknowledgment.acknowledge();
    }

    @DltHandler
    public void consumeDlt(
            RealtimeLogEvent event,
            @Header(name = KafkaHeaders.DLT_EXCEPTION_FQCN, required = false) String exceptionClass,
            @Header(name = KafkaHeaders.DLT_EXCEPTION_MESSAGE, required = false) String exceptionMessage,
            Acknowledgment acknowledgment) {
        failureHandler.handle(event, exceptionClass, exceptionMessage);
        acknowledgment.acknowledge();
    }
}

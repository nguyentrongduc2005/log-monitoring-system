package com.vdt.log_monitoring.modules.processing.internal.consumer;

import org.springframework.kafka.annotation.DltHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.retry.annotation.Backoff;
import org.springframework.stereotype.Component;

import com.vdt.log_monitoring.modules.ingestion.api.events.RawLogReceivedEvent;
import com.vdt.log_monitoring.modules.processing.internal.model.RawLogEnvelope;
import com.vdt.log_monitoring.modules.processing.internal.pipeline.ProcessingFailureHandler;
import com.vdt.log_monitoring.modules.processing.internal.pipeline.LogProcessingService;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class KafkaRawLogConsumer {

    private final LogProcessingService logProcessingService;
    private final ProcessingFailureHandler failureHandler;

    @RetryableTopic(
            attempts = "${app.kafka.retry.raw-processing-attempts}",
            autoCreateTopics = "${app.kafka.retry.auto-create-topics}",
            backoff = @Backoff(delayExpression = "${app.kafka.retry.raw-processing-backoff-ms}"),
            dltTopicSuffix = "${app.kafka.retry.dlt-topic-suffix}")
    @KafkaListener(topics = "${app.kafka.topics.raw}", groupId = "${spring.kafka.consumer.group-id}")
    public void consume(RawLogReceivedEvent event, Acknowledgment acknowledgment) {
        logProcessingService.process(RawLogEnvelope.from(event));
        acknowledgment.acknowledge();
    }

    @DltHandler
    public void consumeDlt(
            RawLogReceivedEvent event,
            @Header(name = KafkaHeaders.DLT_EXCEPTION_FQCN, required = false) String exceptionClass,
            @Header(name = KafkaHeaders.DLT_EXCEPTION_MESSAGE, required = false) String exceptionMessage,
            Acknowledgment acknowledgment) {
        failureHandler.handle(event, exceptionClass, exceptionMessage);
        acknowledgment.acknowledge();
    }
}

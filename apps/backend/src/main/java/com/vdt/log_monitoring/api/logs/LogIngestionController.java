package com.vdt.log_monitoring.api.logs;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import com.vdt.log_monitoring.api.logs.dto.BatchLogIngestionRequest;
import com.vdt.log_monitoring.api.logs.dto.BatchLogIngestionResponse;
import com.vdt.log_monitoring.api.logs.dto.LogIngestionRequest;
import com.vdt.log_monitoring.api.logs.dto.LogIngestionResponse;

import com.vdt.log_monitoring.modules.logs.api.LogsIngestionFacade;
import com.vdt.log_monitoring.shared.dto.ApiResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/logs")
@RequiredArgsConstructor
public class LogIngestionController {
    private static final String API_KEY_HEADER = "X-API-Key";
    private static final String IDEMPOTENCY_KEY_HEADER = "Idempotency-Key";

    private final LogsIngestionFacade logsIngestionFacade;

    @PostMapping
    public ResponseEntity<ApiResponse<LogIngestionResponse>> ingestLog(
            @RequestHeader(API_KEY_HEADER) String apiKey,
            @RequestHeader(value = IDEMPOTENCY_KEY_HEADER, required = false) String idempotencyKey,
            @Valid @RequestBody LogIngestionRequest request) {
        LogsIngestionFacade.IngestLogResult ingestLogResult = logsIngestionFacade
                .ingest(new LogsIngestionFacade.IngestLogCommand(
                        apiKey,
                        request.getApplicationName(),
                        request.getRawLog(),
                        idempotencyKey));
        LogIngestionResponse logResponse = LogIngestionResponse.from(ingestLogResult);
        return ResponseEntity
                .accepted()
                .body(ApiResponse.success(logResponse, "ACCEPTED"));
    }

    @PostMapping("/batch")
    public ResponseEntity<ApiResponse<BatchLogIngestionResponse>> ingestBatchLogs(
            @RequestHeader(API_KEY_HEADER) String apiKey,
            @RequestHeader(value = IDEMPOTENCY_KEY_HEADER, required = false) String idempotencyKey,
            @Valid @RequestBody BatchLogIngestionRequest request) {
        LogsIngestionFacade.BatchIngestLogResult ingestLogResult = logsIngestionFacade
                .batchIngest(new LogsIngestionFacade.BatchIngestLogCommand(
                        apiKey,
                        request.getApplicationName(),
                        request.getRawLogs(),
                        idempotencyKey));
        BatchLogIngestionResponse logResponse = BatchLogIngestionResponse.from(ingestLogResult);
        return ResponseEntity
                .accepted()
                .body(ApiResponse.success(logResponse, "ACCEPTED"));
    }
}

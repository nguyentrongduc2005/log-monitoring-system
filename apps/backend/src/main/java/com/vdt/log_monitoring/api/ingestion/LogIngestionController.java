package com.vdt.log_monitoring.api.ingestion;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import com.vdt.log_monitoring.api.ingestion.dto.BatchLogIngestionRequest;
import com.vdt.log_monitoring.api.ingestion.dto.BatchLogIngestionResponse;
import com.vdt.log_monitoring.api.ingestion.dto.LogIngestionRequest;
import com.vdt.log_monitoring.api.ingestion.dto.LogIngestionResponse;
import com.vdt.log_monitoring.modules.ingestion.api.LogIngestionFacade;
import com.vdt.log_monitoring.shared.dto.ApiResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/logs")
@RequiredArgsConstructor
public class LogIngestionController {
        private static final String API_KEY_HEADER = "X-API-Key";
        private static final String IDEMPOTENCY_KEY_HEADER = "Idempotency-Key";

        private final LogIngestionFacade logIngestionFacade;

        @PostMapping
        public ResponseEntity<ApiResponse<LogIngestionResponse>> ingestLog(
                        @RequestHeader(API_KEY_HEADER) String apiKey,
                        @RequestHeader(value = IDEMPOTENCY_KEY_HEADER, required = false) String idempotencyKey,
                        @Valid @RequestBody LogIngestionRequest request) {
                LogIngestionFacade.IngestLogResult ingestLogResult = logIngestionFacade
                                .ingest(new LogIngestionFacade.IngestLogCommand(
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
                LogIngestionFacade.BatchIngestLogResult ingestLogResult = logIngestionFacade
                                .batchIngest(new LogIngestionFacade.BatchIngestLogCommand(
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

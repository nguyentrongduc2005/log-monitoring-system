```text
modules/logs
├── api
│   ├── LogsIngestionFacade.java
│   ├── LogsQueryFacade.java
│   ├── LogsException.java
│   └── events
│       ├── RawLogReceivedEvent.java
│       ├── RealtimeLogEvent.java
│       ├── CriticalLogDetectedEvent.java
│       └── LogProcessingFailedEvent.java
│
└── internal
    ├── ingestion
    │   ├── LogsIngestionFacadeImpl.java
    │   ├── LogIngestionService.java
    │   ├── RawLogPublisher.java
    │   └── KafkaRawLogPublisher.java
    │
    ├── processing
    │   ├── RawLogConsumer.java
    │   ├── LogProcessingService.java
    │   ├── LogParser.java
    │   ├── LogNormalizer.java
    │   ├── LogSanitizer.java
    │   ├── LogEnricher.java
    │   ├── LogFingerprinter.java
    │   └── ProcessingFailureHandler.java
    │
    ├── storage
    │   ├── LogWriter.java
    │   ├── LogReader.java
    │   └── ClickHouseLogRepository.java
    │
    ├── query
    │   ├── LogsQueryFacadeImpl.java
    │   ├── LogSearchService.java
    │   ├── LogSearchCriteria.java
    │   └── LogSearchPolicy.java
    │
    └── model
        ├── RawLog.java
        ├── ProcessedLog.java
        ├── LogLevel.java
        ├── LogMetadata.java
        ├── LogFingerprint.java
        └── LogProcessingStatus.java
```

Ingestion
Nhận log từ bên ngoài, kiểm tra kích thước, batch size, schema, publish queue.

Processing
Chuẩn hóa log, làm sạch dữ liệu nhạy cảm, enrich metadata, tạo fingerprint, phát hiện lỗi.

Storage
Ghi/đọc log với storage chuyên dụng như ClickHouse.

Query
Tìm kiếm, lọc, phân trang, bắt buộc time range, giới hạn result.

Vì sao mình tách storage và query riêng?
storage là adapter đọc/ghi dữ liệu.
query là use case phục vụ người dùng: search log theo điều kiện.
Sau này query có rule riêng như bắt buộc from/to, limit <= 500, filter theo quyền application. Không nên nhét hết vào repository.

logs -> realtime
logs -> alerting
logs -> incidents

RawLogReceivedEvent -> logs.raw
RealtimeLogEvent -> logs.live
CriticalLogDetectedEvent -> alerts.critical
LogProcessingFailedEvent -> logs.raw.DLT

Hoàn thiện raw log processing worker.
Ghi log chuẩn hóa vào ClickHouse.
Tạo alert event cho ERROR/CRITICAL.
Thêm incident/fingerprint để gom lỗi.
Thêm severity rule trước, AI sau.
Thêm metrics source: Prometheus/node exporter hoặc OpenTelemetry.
AI phân tích incident đã gom, không phân tích từng raw log.

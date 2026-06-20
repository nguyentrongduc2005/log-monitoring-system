# Ingestion Module Summary

## Mục tiêu module

Module `ingestion` chịu trách nhiệm nhận log từ external application và đưa log
thô vào pipeline bất đồng bộ. Module này là boundary đầu vào của hệ thống log,
không xử lý nghiệp vụ processing sâu.

## Phạm vi hiện tại

- Nhận log đơn lẻ và batch thông qua `LogIngestionFacade`.
- Xác thực API key và application bằng `ApplicationAccessFacade` của module
  `identity`.
- Kiểm tra raw log, batch size và idempotency key.
- Redact dữ liệu nhạy cảm ở raw log trước khi publish.
- Publish `RawLogReceivedEvent` vào Kafka topic `logs.raw`.
- Lưu trạng thái idempotency bằng Redis để tránh publish trùng event.

## Ngoài phạm vi

Các trách nhiệm sau không thuộc module `ingestion`:

- Parse log.
- Normalize level, timestamp, trace id hoặc message.
- Ghi log chuẩn hóa vào ClickHouse.
- Phát hiện alert/incident.
- Publish realtime event sau xử lý.
- Query log lịch sử.

Các phần này nên được tách sang module/worker riêng, ví dụ `processing` hoặc
`log-query`, và giao tiếp với `ingestion` qua Kafka contract thay vì gọi trực
tiếp implementation nội bộ.

## Cấu trúc module

```text
modules/ingestion
├── api
│   ├── LogIngestionFacade.java
│   ├── IngestionException.java
│   └── events
│       └── RawLogReceivedEvent.java
└── internal
    ├── LogIngestionService.java
    ├── RawLogPublisher.java
    ├── RawLogSanitizer.java
    ├── impl
    │   ├── KafkaRawLogPublisher.java
    │   ├── LogIngestionFacadeImpl.java
    │   └── RegexRawLogSanitizer.java
    └── storage
        ├── LogIngestionIdempotencyStore.java
        └── RedisLogIngestionIdempotencyStore.java
```

## Boundary rule

Code bên ngoài `modules.ingestion` chỉ được phụ thuộc vào package
`modules.ingestion.api`. Không import trực tiếp `modules.ingestion.internal`.
Quy tắc này được kiểm tra bởi `IngestionModuleBoundaryTest`.

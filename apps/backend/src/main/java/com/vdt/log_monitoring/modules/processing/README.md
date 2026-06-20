# Processing Module Summary

## Mục tiêu module

Module `processing` là background worker xử lý log thô sau khi `ingestion`
publish event vào Kafka topic `logs.raw`.

Module này không nhận HTTP request từ external application. Boundary chính của
module là Kafka:

```text
ingestion
  -> Kafka logs.raw
  -> processing
  -> ClickHouse
  -> Kafka logs.live / alerts.critical
```

## Phạm vi nên triển khai

- Consume `RawLogReceivedEvent` từ Kafka topic `logs.raw`.
- Kiểm tra `schemaVersion` của raw event.
- Parse raw log thành cấu trúc trung gian.
- Normalize `level`, `message`, `timestamp`, `traceId`.
- Enrich metadata nếu có source, host, environment hoặc attributes.
- Tạo fingerprint cho log lỗi.
- Ghi log chuẩn hóa vào ClickHouse.
- Publish `RealtimeLogEvent` cho dashboard live log.
- Publish `CriticalLogDetectedEvent` khi level là `ERROR` hoặc `CRITICAL`.
- Để Kafka listener route raw event gốc vào DLT và publish
  `LogProcessingFailedEvent` vào topic audit lỗi khi xử lý thất bại.

## Luồng biến đổi log

Module `processing` không nhận trực tiếp plain text log từ HTTP client. Plain
text log đi vào hệ thống qua module `ingestion`, sau đó `ingestion` publish
`RawLogReceivedEvent` vào Kafka topic `logs.raw`. Consumer của processing nhận
event này và biến đổi qua các bước sau:

```text
RawLogReceivedEvent
  -> RawLogEnvelope
  -> ParsedLog
  -> ProcessedLog(status=NORMALIZED)
  -> ProcessedLog(status=STORED)
  -> RealtimeLogEvent / CriticalLogDetectedEvent
```

### 1. Consume raw event

`KafkaRawLogConsumer` subscribe topic `logs.raw` bằng `@KafkaListener`.
Consumer không chứa business logic; nó chỉ convert `RawLogReceivedEvent` thành
`RawLogEnvelope` rồi gọi `LogProcessingService`.

`RawLogEnvelope` là input nội bộ của worker, chứa:

- `eventId`, `ingestionId`
- `applicationId`, `applicationName`, `applicationDisplayName`
- `rawLog`
- `receivedAt`
- `schemaVersion`

Về mặt trạng thái pipeline, log ở bước này tương ứng với `RECEIVED`.

### 2. Parse raw log

`LogParser` kiểm tra `schemaVersion`. Hiện worker hỗ trợ schema version `1`.
Sau đó parser đọc dòng log theo format dạng:

```text
2026-06-15T09:01:12.368Z INFO Payment authorized traceId=... orderId=...
```

Parser tách ra các trường:

- `level`: `INFO`, `WARN`, `ERROR`, `CRITICAL`
- `message`
- `traceId` nếu raw log có `traceId=...`, `trace_id=...` hoặc `trace-id=...`
- `logTimestamp`; nếu raw log không có timestamp thì dùng `receivedAt`

Kết quả bước này là `ParsedLog`, chưa ghi storage.

### 3. Normalize log

`LogNormalizer` nhận `RawLogEnvelope` và `ParsedLog`, sau đó tạo
`ProcessedLog` chuẩn hóa:

- trim `applicationName`, `applicationDisplayName`, `message`, `traceId`
- set `processedAt` theo thời điểm worker xử lý
- set `metadata` mặc định là `LogMetadata.empty()`
- set trạng thái `LogProcessingStatus.NORMALIZED`

Đây là thời điểm raw log đã được biến thành model chuẩn nội bộ.

### 4. Enrich và fingerprint

`LogEnricher` hiện là điểm mở rộng để bổ sung metadata như `source`, `host`,
`environment` hoặc attributes. Hiện implementation chưa enrich thêm dữ liệu.

`LogFingerprinter` tạo fingerprint SHA-256 từ:

```text
applicationId + level + normalizedMessage
```

Trong đó `normalizedMessage` được chuẩn hóa bằng cách thay UUID và số bằng
placeholder để các lỗi cùng dạng có fingerprint ổn định hơn.

### 5. Store vào ClickHouse

`LogProcessingService` gọi `LogWriter.write(...)`. Implementation hiện tại là
`ClickHouseProcessedLogWriter`.

Writer insert vào bảng `processed_logs`. Với batch, writer build một câu:

```sql
INSERT INTO processed_logs (...) VALUES (...), (...), (...)
```

Sau khi ghi ClickHouse thành công, service tạo bản `ProcessedLog` mới với
trạng thái `LogProcessingStatus.STORED`.

### 6. Publish output events

Sau khi log đã lưu thành công:

- Service publish `RealtimeLogEvent` vào topic `logs.live` để dashboard realtime
  consume.
- Nếu level là `ERROR` hoặc `CRITICAL`, service publish thêm
  `CriticalLogDetectedEvent` vào topic `alerts.critical`.

Nếu một bước trong pipeline lỗi, exception được để thoát ra khỏi
`KafkaRawLogConsumer`. Annotation `@RetryableTopic` retry theo cấu hình
`app.kafka.retry.raw-processing-*`, rồi route raw event gốc vào topic
`logs.raw.DLT` để replay khi đã terminal failure. `@DltHandler` sau đó publish
`LogProcessingFailedEvent` vào topic `processing.errors` để audit/monitoring.

## Quản lý trạng thái xử lý

Trạng thái xử lý nằm trong enum `LogProcessingStatus`:

```text
RECEIVED -> NORMALIZED -> STORED
                    \-> FAILED
```

- `RECEIVED`: raw event đã được consumer nhận từ Kafka. Trạng thái này đại diện
  bởi `RawLogEnvelope`.
- `NORMALIZED`: raw log đã parse và chuẩn hóa thành `ProcessedLog`.
- `STORED`: `ProcessedLog` đã được ghi thành công xuống ClickHouse.
- `FAILED`: pipeline gặp lỗi parse, normalize, storage hoặc publish downstream
  event; Kafka listener route raw event gốc vào DLT và DLT handler publish
  failure event vào `processing.errors`.

## Ngoài phạm vi

- Verify API key.
- Kiểm tra quyền application của client gửi log.
- Quản lý idempotency của HTTP ingestion request.
- Nhận request `/api/v1/logs`.
- Query log lịch sử.

Các trách nhiệm trên thuộc `ingestion`, `identity` hoặc `log-query`.

## Cấu trúc hiện tại

```text
modules/processing
├── api
│   ├── ProcessingException.java
│   └── events
│       ├── RealtimeLogEvent.java
│       ├── CriticalLogDetectedEvent.java
│       └── LogProcessingFailedEvent.java
└── internal
    ├── consumer
    │   ├── RawLogConsumer.java
    │   └── KafkaRawLogConsumer.java
    ├── model
    │   ├── RawLogEnvelope.java
    │   ├── ProcessedLog.java
    │   ├── LogLevel.java
    │   ├── LogProcessingStatus.java
    │   ├── LogFingerprint.java
    │   └── LogMetadata.java
    ├── pipeline
    │   ├── LogProcessingService.java
    │   ├── LogParser.java
    │   ├── LogNormalizer.java
    │   ├── LogEnricher.java
    │   ├── LogFingerprinter.java
    │   └── ProcessingFailureHandler.java
    └── storage
        ├── LogWriter.java
        ├── ClickHouseProcessedLogWriter.java
        ├── ClickHouseConfig.java
        ├── ClickHouseDataSourceProperties.java
        ├── ClickHouseMigrationProperties.java
        └── ClickHouseMigrationRunner.java
```

## Gợi ý thiết kế tốt hơn

- Không tạo `ProcessingFacade` nếu chưa có module nào gọi trực tiếp processing
  bằng Java method. Worker giao tiếp qua Kafka contract là đủ.
- Đặt `RawLogConsumer` thật mỏng: nhận event, gọi `LogProcessingService`, commit
  offset sau khi storage và downstream event bắt buộc đã acknowledge.
- Tách parse, normalize, enrich, fingerprint thành component nhỏ để dễ test
  từng bước và thay rule sau này.
- `LogProcessingService` nên orchestrate flow, không chứa logic regex/parser
  chi tiết.
- `ProcessedLogWriter` là port ghi log; ClickHouse chỉ là implementation.
- Event output nằm trong `api/events` vì realtime, alerting hoặc incident có thể
  consume contract này.
- Lỗi terminal nên có đường DLT rõ ràng, không silently discard raw log.
- Worker phải idempotent theo `eventId` hoặc khóa tương đương vì Kafka có thể
  giao lại message.

## Boundary rule

Code bên ngoài `modules.processing` chỉ được phụ thuộc vào
`modules.processing.api`. Không import trực tiếp `modules.processing.internal`.

## Checklist triển khai

### Đã làm trong module processing

- [x] Tách module `processing` riêng khỏi `ingestion`.
- [x] Tạo public event contract trong `modules.processing.api.events`:
      `RealtimeLogEvent`, `CriticalLogDetectedEvent`, `LogProcessingFailedEvent`.
- [x] Tạo exception riêng `ProcessingException` với error code có HTTP status
      tương ứng.
- [x] Tạo model nội bộ cho pipeline:
      `RawLogEnvelope`, `ProcessedLog`, `LogLevel`, `LogProcessingStatus`,
      `LogFingerprint`, `LogMetadata`.
- [x] Tạo Kafka consumer `KafkaRawLogConsumer` consume `RawLogReceivedEvent`
      từ topic `logs.raw`.
- [x] Tạo pipeline service `LogProcessingService` để orchestrate parse,
      normalize, enrich, fingerprint, store và publish event downstream.
- [x] Implement `LogParser` cho format log cơ bản:
      `timestamp level message traceId=...`.
- [x] Implement `LogNormalizer` để tạo `ProcessedLog` với status
      `NORMALIZED`.
- [x] Implement `LogFingerprinter` bằng SHA-256 từ
      `applicationId + level + normalizedMessage`.
- [x] Tạo extension point `LogEnricher` cho metadata enrichment.
- [x] Implement `ClickHouseProcessedLogWriter` ghi vào ClickHouse bằng
      multi-row `INSERT ... VALUES (...), (...), (...)`.
- [x] Tạo migration SQL cho bảng `processed_logs`.
- [x] Tạo `ClickHouseMigrationRunner` riêng vì Flyway core hiện không support
      `ClickHouse 25.3` trong project này.
- [x] Cấu hình datasource riêng cho ClickHouse và giữ PostgreSQL datasource là
      primary datasource.
- [x] Publish `RealtimeLogEvent` vào topic `logs.live` sau khi storage thành
      công.
- [x] Publish `CriticalLogDetectedEvent` vào topic `alerts.critical` khi level
      là `ERROR` hoặc `CRITICAL`.
- [x] Dùng Kafka listener DLT để route raw event gốc vào topic `logs.raw.DLT`
      và publish `LogProcessingFailedEvent` vào topic `processing.errors` khi
      pipeline gặp lỗi.
- [x] Thêm default type cho Kafka `JsonDeserializer` để consumer đọc được
      `RawLogReceivedEvent`.
- [x] Thêm boundary test đảm bảo code ngoài module không import
      `processing.internal`.
- [x] Full backend test hiện pass với Spring context, Kafka listener và module
      boundary.

### Đã có nhưng còn đơn giản

- [ ] Parser mới hỗ trợ format log text cơ bản, chưa hỗ trợ JSON log, stack
      trace nhiều dòng, logfmt đầy đủ hoặc format tùy biến theo application.
- [ ] `LogEnricher` mới là placeholder, chưa enrich `source`, `host`,
      `environment` hoặc attributes từ raw log.
- [ ] Fingerprint đã ổn cho MVP nhưng chưa có rule riêng cho từng loại lỗi hoặc
      stack trace.
- [ ] Failure handling đã đưa event vào `logs.raw.DLT`, nhưng chưa lưu đầy đủ
      `failureStage`, raw payload snapshot hoặc retry count.
- [ ] ClickHouse writer đã insert batch theo một statement nhiều values, nhưng
      `LogProcessingService` vẫn gọi `write(...)` từng event; chưa có buffer theo
      batch size / flush interval.
- [ ] Trạng thái `RECEIVED`, `NORMALIZED`, `STORED`, `FAILED` đang là trạng
      thái vận hành nội bộ, chưa có projection/audit table append-only cho từng
      transition.

### Còn thiếu so với requirement dự án

- [ ] Redaction secret trước khi storage/downstream event. Requirement yêu cầu
      lọc secret trước Kafka và redaction lại trước ClickHouse/downstream.
- [ ] Retry có giới hạn cho từng bước processing: parse/store/publish live/
      publish alert. Hiện code chưa có retry policy riêng cho worker.
- [x] Offset commit sau khi ClickHouse và downstream Kafka events đã broker
      acknowledgment. Listener dùng `manual_immediate` và chỉ `acknowledge()` sau
      khi xử lý thành công; failure flow được chuyển qua retry/DLT.
- [ ] Idempotency/dedup theo `eventId`. Requirement yêu cầu khi Kafka giao lại
      cùng raw event thì không tạo thêm logical log hoặc notification trùng.
- [ ] DLT event cần có failure stage rõ ràng, ví dụ `PARSE`, `NORMALIZE`,
      `STORE_CLICKHOUSE`, `PUBLISH_LIVE`, `PUBLISH_ALERT`.
- [ ] Schema ClickHouse hiện tại chưa khớp hoàn toàn với store requirement:
      requirement đề xuất `event_timestamp`, `normalized_at`, `stored_at`,
      `environment`, `host_name`, `source`, `attributes` và `level Enum8`; migration
      hiện dùng `log_timestamp`, `processed_at`, `status` và thiếu một số metadata.
- [ ] Retention/TTL cho ClickHouse partition chưa cấu hình.
- [ ] Metrics/observability cho worker chưa có: consumer lag, processing
      latency, ClickHouse insert latency, DLT count, publish latency.
- [ ] Alerting consumer riêng cho `alerts.critical` chưa có trong module
      `alerting`; processing mới publish event.
- [ ] Realtime/WebSocket consumer cho `logs.live` chưa có trong module
      `realtime`; processing mới publish event.
- [ ] `logs.live.DLT` và `alerts.critical.DLT` chưa được xử lý ở downstream
      consumer.
- [ ] Kafka topic provisioning, partition count, retention và ACL chưa được mô
      tả/áp dụng bằng config hoặc script vận hành.
- [ ] Integration test thật với Kafka + ClickHouse chưa có; hiện chủ yếu mới
      pass compile/context và module boundary test.
- [ ] Query module đọc ClickHouse và API search log chưa có; processing mới là
      writer.
- [ ] Batch backpressure và giới hạn payload/attributes cho processing chưa có.

ncident Là Gì
Incident trả lời câu hỏi:
Có sự cố thật không?
Các log nào liên quan?
Mức độ nghiêm trọng là gì?
Nguyên nhân có thể là gì?
Ảnh hưởng app nào?
Bắt đầu lúc nào?
Có còn tiếp diễn không?

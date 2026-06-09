# Module Requirements

> **Draft planning note:** File này chỉ dùng để định hướng trong quá trình
> thiết kế. Đây không phải nguồn kiến trúc chính thức và agent chỉ được sử dụng
> khi người dùng yêu cầu trực tiếp file này. `DETAI.md`, source code và cấu hình
> thực thi có độ ưu tiên cao hơn.

## 1. Mục tiêu và phạm vi

Tài liệu này mô tả ranh giới module cho backend Log Monitoring System theo
Modular Monolith. Hệ thống được build và deploy thành một Spring Boot
application, không phải microservices.

Phạm vi bắt buộc ban đầu:

- `identity`
- `logs`
- `alerting`
- `realtime`

Phạm vi điểm cộng/tương lai:

- `incidents`
- `analytics`
- `retention`
- `ai`

Không tạo module rỗng trước khi bắt đầu use case tương ứng. Module `logs` là
data owner duy nhất và có ba component tách biệt:

```text
logs.ingestion
    -> Kafka logs.raw
    -> logs.processing
    -> ClickHouse logs
    <- logs.query
```

Ba component có entry point và failure path riêng nhưng cùng thuộc business
module `logs` và chạy trong một Spring Boot application. `notification` thuộc
`alerting`; `livestream` thuộc `realtime`.

Kiến trúc frontend hiện tại không thay đổi.

## 2. Nguyên tắc

- Chia module theo business capability, không theo technical layer toàn cục.
- Mỗi module sở hữu use case, model, repository, storage adapter và public
  contract của mình.
- HTTP/WebSocket controller nằm trong top-level `api.<module>`, chỉ validate
  transport, authorize, gọi public module facade và map response.
- `modules.<module>.api` là public Java contract/facade nhỏ của module, không
  chứa Spring transport controller.
- Module không import entity, repository hoặc infrastructure của module khác.
- Giao tiếp đồng bộ dùng public module facade nhỏ.
- Kafka bắt buộc cho pipeline tiếp nhận và xử lý log.
- Redis bắt buộc cho alert deduplication.
- DDD chỉ dùng khi có invariant, state transition hoặc workflow phức tạp.
- `shared` chỉ chứa technical primitive thực sự dùng chung.

Cross-cutting concern được thực hiện nhất quán bằng Spring filter,
interceptor/decorator hoặc shared technical utility:

- Authentication và authorization.
- Request validation và exception mapping.
- Transaction boundary.
- Correlation ID, structured logging và metrics.
- Idempotency/retry tại các boundary cần thiết.

Cross-cutting code không được chứa business rule của module.

Backend source root:

```text
apps/backend/src/main/java/com/vdt/log_monitoring/
```

## 3. Cấu trúc backend

```text
com.vdt.log_monitoring
├── api
│   ├── identity       # HTTP controller và transport DTO
│   ├── logs           # ingestion/search HTTP controller và transport DTO
│   ├── alerting       # alert-management HTTP controller và transport DTO
│   └── realtime       # WebSocket/STOMP endpoint và transport DTO
├── modules
│   ├── identity
│   │   ├── api        # public module facade/contract
│   │   ├── application
│   │   ├── model
│   │   └── infrastructure
│   ├── logs
│   │   ├── api        # public module facade/contract
│   │   ├── application
│   │   │   ├── ingestion
│   │   │   ├── processing
│   │   │   └── search
│   │   ├── domain
│   │   ├── infrastructure
│   │   │   ├── kafka
│   │   │   │   ├── producer
│   │   │   │   └── consumer
│   │   │   └── clickhouse
│   │   └── integrationevents
│   ├── alerting
│   │   ├── api        # public module facade/contract
│   │   ├── application
│   │   ├── domain
│   │   ├── infrastructure
│   │   └── integrationevents
│   └── realtime
│       ├── api        # public module facade/contract
│       ├── application
│       ├── model
│       └── infrastructure
└── shared
    ├── eventbus
    ├── security
    ├── exceptions
    ├── observability
    └── common
```

Đây là cấu trúc định hướng, không phải yêu cầu tạo mọi package ngay lập tức.

Phân biệt hai boundary:

- Top-level `api`: adapter vào hệ thống, phụ thuộc public facade của module.
- `modules/<module>/api`: primary port được module công khai; không phụ thuộc
  controller, transport DTO hoặc infrastructure.

Application root chỉ wiring/khởi tạo module configuration. Mỗi module tự đăng
ký handler/service, repository và adapter bằng configuration của chính module.

## 4. Module bắt buộc

### 4.1. `identity`

Sở hữu:

- User, role và trạng thái tài khoản.
- Application nguồn gửi log.
- API key của application.
- Quyền `VIEW`/`MANAGE` application của kỹ sư.
- Authentication và authorization policy.

Storage:

- PostgreSQL schema `identity`.
- Redis có thể cache access/application metadata, nhưng PostgreSQL vẫn là
  source of truth.

Public contracts:

- Xác thực API key và trả về application identity.
- Kiểm tra application đang active.
- Kiểm tra user có quyền xem/quản lý application.
- Contract chỉ trả DTO/value ổn định, không trả entity hoặc repository.

Quy tắc:

- Chỉ lưu password hash và API-key hash.
- API key thô chỉ hiển thị một lần khi tạo.
- Không để pipeline log truy cập trực tiếp repository của `identity`.

### 4.2. `logs`

Sở hữu toàn bộ pipeline log:

- API nhận single/batch log.
- Validate request, API key, kích thước và timestamp.
- Gán `event_id`, `ingestion_id`, `received_at`.
- Publish toàn bộ log thô đã chấp nhận vào Kafka `logs.raw`.
- Worker consume, parse, normalize, redact và fingerprint.
- Batch write log chuẩn hóa vào ClickHouse.
- Search log theo application, level, time range và trace ID.
- Publish live log, critical alert và failed event.

Các component bắt buộc phải tách:

#### Ingestion component

- Chạy tại HTTP boundary.
- Xác thực API key và validate request.
- Không parse sâu, không chuẩn hóa và không ghi ClickHouse.
- Publish raw event vào `logs.raw`.
- Trả accepted chỉ sau Kafka acknowledgment.

#### Kafka buffer

- Tách tốc độ HTTP ingestion khỏi tốc độ xử lý và ghi ClickHouse.
- Giữ raw event theo retention đã cấu hình.
- Partition theo `application_id` để giữ ordering trong phạm vi application.
- Áp dụng producer retry, acknowledgment và backpressure.

#### Processing worker

- Là Kafka consumer riêng, không chạy trong request thread.
- Consume `logs.raw`, parse, normalize, redact và fingerprint.
- Batch write ClickHouse.
- Publish `logs.live` và, với `ERROR`/`CRITICAL`, `alerts.critical`.
- Chỉ commit offset `logs.raw` sau khi ClickHouse batch và mọi Kafka event
  downstream bắt buộc đã được broker acknowledgment.
- Nếu process chết sau khi ghi ClickHouse nhưng trước khi commit offset, raw
  event được xử lý lại với cùng `event_id`; ClickHouse writer và downstream
  consumer phải idempotent.
- Nếu publish downstream vẫn thất bại sau retry có giới hạn, không silently
  commit raw offset. Chuyển event kèm failure stage vào `logs.dlq` để replay và
  phát metric/cảnh báo vận hành.

Ingestion API, processing worker và query chạy trong cùng Spring Boot
application nhưng là các component tách biệt. Ingestion không gọi trực tiếp
worker; Kafka là ranh giới bắt buộc. Query không tham gia consumer group xử lý
log.

Kafka flow:

```text
LogIngestionController
    -> RawLogProducer
    -> Kafka logs.raw

RawLogProcessingWorker
    <- Kafka logs.raw
    -> normalize/redact/fingerprint
    -> ClickHouseBatchWriter
    -> Kafka logs.live
    -> Kafka alerts.critical      # ERROR/CRITICAL
    -> Kafka logs.dlq             # terminal processing failure
    -> commit logs.raw offset     # after required acknowledgments
```

Storage:

- ClickHouse dataset do `logs` sở hữu.
- `logs.processing` là writer duy nhất của ClickHouse `logs`.
- `logs.query` chỉ được đọc dataset thông qua query adapter của module.
- `logs.ingestion` không được truy cập ClickHouse.
- Không ghi log trực tiếp vào PostgreSQL.
- Không ghi trực tiếp ClickHouse từ HTTP request.

Các nhóm code bên trong `logs` có thể gồm:

```text
logs
├── api                  # LogsModuleFacade và public contract DTO
├── application
│   ├── ingestion       # validate và publish raw log
│   ├── processing      # worker use case
│   └── search          # query use case
├── domain              # LogRecord, LogLevel, normalizer, fingerprint
├── infrastructure
│   ├── config           # module-owned Spring configuration
│   ├── kafka
│   │   ├── producer    # logs.raw, logs.live, alerts.critical, logs.dlq
│   │   └── consumer    # RawLogProcessingWorker
│   └── clickhouse      # batch writer và query repository
└── integrationevents
```

Transport tương ứng nằm ngoài module:

```text
api/logs
├── LogIngestionController
├── LogSearchController
└── dto
```

Quy tắc bắt buộc:

- API chỉ trả accepted sau khi Kafka xác nhận publish.
- Consumer phải idempotent vì Kafka có thể giao lại event.
- ClickHouse retry phải có chiến lược chống/truy vấn loại duplicate cụ thể;
  giữ nguyên `event_id` là cần thiết nhưng chưa đủ.
- Không silently discard log lỗi; terminal failure đi vào `logs.dlq`.
- Search luôn có time range và result limit.
- Client không được gửi raw SQL hoặc ClickHouse expression.

Ingestion contract MVP:

- Maximum request body mặc định `1 MiB`.
- Tối đa `500` events mỗi batch.
- Rate limit mặc định `1,000 events/second/application`, cho phép burst
  `2,000`; các giá trị phải cấu hình được.
- Canonical fields: `applicationName`, `level`, `message`, `timestamp`,
  `traceId`; server gán `event_id`, `ingestion_id`, `received_at`.
- Chấp nhận timestamp từ `7 ngày trước` đến `5 phút trong tương lai`.
- MVP dùng whole-batch validation: một event không hợp lệ làm request bị từ
  chối và không event nào được publish.
- Kafka publish timeout mặc định `3 giây`; rate limit trả `429`, quá tải hoặc
  broker không acknowledge trả `503` cùng retry guidance.
- Hỗ trợ `Idempotency-Key`; retry phải giữ nguyên logical event ID.
- Redaction secret nhận diện được trước Kafka và redaction lại trước
  ClickHouse/downstream topic.

### 4.3. `alerting`

Sở hữu:

- Alert rules và threshold.
- Consume Kafka `alerts.critical`.
- Redis alert deduplication.
- Alert occurrence state phục vụ dedup và delivery.
- Notification channel configuration.
- Telegram delivery, retry và delivery history.
- Publish alert event cho realtime.

Storage:

- PostgreSQL schema `alerting`.
- Redis key TTL cho deduplication.

Luồng:

```text
Kafka alerts.critical
    -> rule evaluation
    -> Redis atomic dedup lock
    -> create/update alert occurrence in PostgreSQL
    -> Telegram delivery
    -> publish realtime alert event

realtime alert event
    -> realtime module
    -> authorized WebSocket subscribers
```

Quy tắc:

- `ERROR`/`CRITICAL` được đưa vào `alerts.critical` ngay sau khi worker xử lý.
- Rule mặc định có threshold `1`, nên lỗi đầu tiên đủ điều kiện gửi alert;
  Admin có thể cấu hình threshold khác.
- Cùng fingerprint trong cửa sổ dedup 60 giây chỉ phát một notification, kể cả
  khi xuất hiện 100 lần.
- Số lần xuất hiện vẫn được ghi nhận dù notification bị khóa trùng.
- Redis không phải source of truth cho rule hoặc alert occurrence.
- Notification failure không rollback alert occurrence.
- Retry notification phải idempotent.
- Không log Telegram token hoặc payload nhạy cảm.
- `alerting` không thao tác WebSocket session và không ghi WebSocket delivery;
  nó chỉ phát alert event cho `realtime`.
- `alerts.critical` dùng consumer group và executor riêng, không chia worker
  pool với live stream; mục tiêu MVP là p95 từ khi topic nhận event đến khi bắt
  đầu delivery không quá `2 giây`.

`alerting` không sở hữu acknowledge/resolve/assignment workflow. Khi cần các
use case đó, module tương lai `incidents` tham chiếu alert occurrence qua public
contract/event; không tạo module trước khi bắt đầu use case.

### 4.4. `realtime`

Sở hữu:

- Consume Kafka `logs.live`.
- Nhận alert event từ `alerting`.
- WebSocket/STOMP connection và subscription.
- Route live log/alert theo application.
- Filter live subscription theo `applicationIds` và `levels`.
- Cho phép client cập nhật filter mà không reload trang.
- Authorize subscription bằng public contract của `identity`.
- Buffer/backpressure cho client chậm.

Quy tắc:

- Không query database cho từng live event.
- Client chậm không được làm nghẽn Kafka consumer.
- Không gửi dữ liệu application mà user không có quyền xem.
- Level filter chỉ chấp nhận `INFO`, `WARN`, `ERROR`, `CRITICAL`.
- WebSocket delivery không thay thế lưu trữ ClickHouse hoặc alert occurrence.

## 5. Module tương lai/điểm cộng

### `incidents`

Tách từ `alerting` khi cần assignment, acknowledge/resolve workflow, timeline
và ownership riêng. Sở hữu PostgreSQL schema `incidents`.

### `analytics`

Đọc projection/materialized view do mình sở hữu trong ClickHouse để tính error
rate, critical rate và application health theo giờ. Đây là read model đơn
giản, không bắt buộc full DDD.

### `retention`

Sở hữu policy và lịch chạy trong PostgreSQL. Không trực tiếp xóa dữ liệu module
khác; gọi public contract của `logs`/`analytics`, và module sở hữu thực hiện
ClickHouse TTL hoặc partition operation.

### `ai`

Phân tích incident/fingerprint bất đồng bộ sau alert. AI không nằm trên hot
path ingestion, live log hoặc notification. Input phải redaction; output chỉ
là gợi ý và không tự resolve incident.

## 6. Kafka topic ownership

| Topic | Producer | Consumer | Phạm vi |
| --- | --- | --- | --- |
| `logs.raw` | `logs` ingestion | `logs` processing worker | Bắt buộc |
| `logs.live` | `logs` processing worker | `realtime` | Bắt buộc |
| `logs.dlq` | `logs` processing worker | Operations/admin tooling | Bắt buộc |
| `alerts.critical` | `logs` processing worker | `alerting` | Bắt buộc |
| `incidents.ai` | `incidents`/`alerting` | `ai` | Điểm cộng |

Mỗi event phải có event ID, schema version và UTC timestamp. Producer xác định
acknowledgment/retry; consumer xác định idempotency, ordering, retry và DLQ.
`alerts.critical` là topic ưu tiên về vận hành: có consumer group, executor,
concurrency và metric latency riêng. Kafka không tự cung cấp message priority,
vì vậy không được chỉ dựa vào việc đặt tên topic.

Quy tắc tải:

- Kafka tách request ingestion khỏi tốc độ xử lý worker trong cùng application.
- Worker concurrency không vượt số partition `logs.raw`.
- Dùng thread pool/connection pool riêng cho HTTP ingestion, Kafka worker và
  ClickHouse query để tránh một workload làm cạn tài nguyên workload khác.
- Partition `logs.raw` theo `application_id` để giữ ordering trong application.

## 7. Data ownership

| Dữ liệu | Owner | Storage |
| --- | --- | --- |
| Users, applications, API keys, permissions | `identity` | PostgreSQL `identity` |
| Normalized logs và log search | `logs` | ClickHouse |
| Alert rules, channels, occurrences, deliveries | `alerting` | PostgreSQL `alerting` |
| Alert dedup lock | `alerting` | Redis |
| Live subscriptions/buffer | `realtime` | Memory/transient |
| Incident lifecycle | `incidents` | PostgreSQL `incidents` |
| Retention policies/runs | `retention` | PostgreSQL `retention` |
| AI insights | `ai` | ClickHouse theo storage blueprint hiện tại |
| Hourly health projection | `analytics` | ClickHouse |

Không tạo cross-module ORM relationship hoặc repository. ID của module khác là
logical reference và được xác minh qua public contract/event.

`logs.ingestion`, `logs.processing`, `logs.query` không phải ba business module
và không có ownership riêng. Chúng là component nội bộ của cùng data owner
`logs`.

## 8. Frontend

Giữ nguyên feature-based architecture:

```text
apps/frontend/src
├── app
├── features
├── shared
└── api
```

- `src/app`: router, provider và wiring.
- `src/features`: behavior theo feature.
- `src/shared`: frontend code tái sử dụng.
- `src/api`: transport client và generated types.
- Frontend authorization không thay thế kiểm tra quyền ở backend.

## 9. Thứ tự triển khai

1. `identity`: application, API key và access policy.
2. `logs`: ingestion API và Kafka `logs.raw`.
3. `logs`: worker normalize, DLQ và batch write ClickHouse.
4. `logs`: search API.
5. `realtime`: Kafka `logs.live` và WebSocket.
6. `alerting`: `alerts.critical`, Redis dedup và Telegram.
7. Architecture tests, observability và load test 500 logs/2 seconds.
8. `analytics`, `retention`, `incidents`, `ai` theo phạm vi điểm cộng.

## 10. Tiêu chí review

- Capability và owner có rõ không?
- Kafka có nằm trên mọi đường tiếp nhận raw log không?
- Module có truy cập storage/internal type của module khác không?
- Top-level controller có chỉ gọi public module facade không?
- Retry, idempotency, DLQ và backpressure đã rõ chưa?
- Redis dedup có atomic và có TTL không?
- ClickHouse insert có batch và query có time bound không?
- Dữ liệu nhạy cảm đã được redaction chưa?
- Metrics cho ingestion, Kafka lag, ClickHouse, Redis và delivery đã có chưa?
- Test có chứng minh mục tiêu 500 logs trong 2 giây không?

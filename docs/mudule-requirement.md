# Module Requirements

## 1. Mục tiêu

Tài liệu này mô tả cách chia module cho Log Monitoring System và kiến trúc phù
hợp với từng module.

Kiến trúc tổng thể:

```text
Modular Monolith
├── Capability-first modules
├── Event-driven processing qua Kafka
├── Pragmatic DDD cho domain phức tạp
├── Feature module gọn cho nghiệp vụ đơn giản
├── Ports and adapters cho external integration
├── CQRS read-side cho search và analytics
└── Feature-based frontend
```

Đây là kiến trúc mục tiêu. Chỉ tạo module khi bắt đầu triển khai capability
tương ứng; không scaffold toàn bộ module trước khi cần.

## 2. Nguyên tắc chia module

- Module được chia theo business capability, không chia toàn dự án thành các
  package kỹ thuật global như `controller`, `service`, `repository`.
- Mỗi module sở hữu nghiệp vụ, dữ liệu và public contract của chính nó.
- Module khác không truy cập trực tiếp repository hoặc internal class.
- Module đơn giản dùng cấu trúc feature-local gọn.
- Module có invariant, state transition, retry, idempotency hoặc nhiều
  integration mới áp dụng DDD rõ hơn.
- Kafka được dùng cho luồng bất đồng bộ và tải cao; không dùng event nội bộ
  cho mọi lời gọi đơn giản.
- `shared` chỉ chứa khái niệm thực sự dùng chung, không trở thành package chứa
  đồ linh tinh.
- Bắt đầu bằng modular monolith; chỉ tách microservice khi tải, deployment hoặc
  ownership thực tế yêu cầu.

Backend source root:

```text
apps/backend/src/main/java/com/vdt/log_monitoring/
```

## 3. Tổng quan backend module

| Module | Trách nhiệm chính | Kiến trúc |
| --- | --- | --- |
| `identity` | User, đăng nhập, role và trạng thái tài khoản | Feature module |
| `applicationregistry` | Application và API key | Feature module |
| `accesscontrol` | Quyền user trên từng application | Policy/application service |
| `ingestion` | Nhận log tốc độ cao và publish Kafka | Hexagonal, event-driven |
| `processing` | Parse, normalize, fingerprint và route log | Pragmatic DDD, pipeline |
| `logstorage` | Batch write ClickHouse | Infrastructure adapter |
| `logsearch` | Tìm kiếm và lọc log | CQRS read module |
| `livestream` | Đẩy log realtime đến frontend | Event adapter |
| `alerting` | Rule, deduplication và incident lifecycle | Pragmatic DDD |
| `notification` | WebSocket/Telegram delivery và retry | Ports and adapters |
| `retention` | Policy, background execution và run history | Job orchestration |
| `aianalysis` | Phân tích incident bất đồng bộ | Async integration |
| `analytics` | Health metrics và error rate theo giờ | CQRS read model |
| `shared` | Kernel và technical primitive tối thiểu | Shared kernel |

## 4. Backend modules

### 4.1. `identity`

Trách nhiệm:

- Quản lý `users`.
- Đăng nhập và xác thực người dùng.
- Quản lý role `ADMIN`, `ENGINEER`.
- Khóa hoặc vô hiệu hóa tài khoản.

Kiến trúc:

- Feature module đơn giản.
- Không cần DDD đầy đủ nếu chỉ có CRUD, authentication và account status.
- Có thể dùng Spring Security và JPA repository trực tiếp trong module.

Cấu trúc gợi ý:

```text
identity
├── UserController
├── AuthenticationController
├── UserService
├── UserRepository
├── UserEntity
└── dto
```

Lưu ý:

- Không trả `password_hash` qua API.
- Authentication principal phải do backend xác minh.
- Nếu sau này có nhiều cơ chế identity hoặc policy phức tạp, mới tách
  `application` và `infrastructure`.

### 4.2. `applicationregistry`

Trách nhiệm:

- Quản lý application nguồn.
- Tạo, revoke và kiểm tra API key.
- Quản lý trạng thái application.
- Cung cấp metadata application cho module khác.

Kiến trúc:

- Feature module CRUD gọn.
- Không cần aggregate hoặc repository port khi chỉ dùng PostgreSQL.

Cấu trúc gợi ý:

```text
applicationregistry
├── ApplicationController
├── ApiKeyController
├── ApplicationService
├── ApiKeyService
├── ApplicationRepository
├── ApiKeyRepository
└── dto
```

Public contract:

- Kiểm tra application có hoạt động hay không.
- Xác thực API key và trả về `application_id`.
- Không cho module khác truy cập trực tiếp bảng API key.

### 4.3. `accesscontrol`

Trách nhiệm:

- Kiểm tra user được `VIEW` hoặc `MANAGE` application nào.
- Cho phép admin quản lý quyền kỹ sư.
- Cung cấp danh sách application user được truy cập.

Kiến trúc:

- Policy/application service đơn giản.
- Không cần DDD nếu policy chỉ dựa trên role và
  `user_application_access`.

Cấu trúc gợi ý:

```text
accesscontrol
├── AccessController
├── AccessPolicy
├── AccessService
└── UserApplicationAccessRepository
```

Lưu ý:

- Authorization luôn được kiểm tra server-side.
- Redis chỉ cache kết quả; PostgreSQL vẫn là source of truth.
- Xóa cache khi user, role hoặc quyền application thay đổi.

### 4.4. `ingestion`

Trách nhiệm:

- Cung cấp API nhận single và batch log.
- Xác thực API key.
- Validate request size và các field tối thiểu.
- Gán `event_id`, `ingestion_id`, `received_at`.
- Publish log thô vào `logs.raw`.
- Trả response sau khi Kafka chấp nhận event.

Kiến trúc:

- Hexagonal/ports and adapters nhẹ.
- Event-driven.
- Không chứa nghiệp vụ chuẩn hóa hoặc lưu ClickHouse.

Cấu trúc gợi ý:

```text
ingestion
├── api
│   ├── LogIngestionController
│   └── dto
├── application
│   └── IngestLogService
├── model
│   └── RawLog
└── infrastructure
    ├── ApplicationAuthenticator
    └── KafkaRawLogPublisher
```

Luồng:

```text
HTTP request
    -> API key validation
    -> request validation
    -> logs.raw
    -> accepted response
```

Lưu ý:

- Không `INSERT` log trực tiếp vào PostgreSQL hoặc ClickHouse.
- Publisher contract cần hỗ trợ retry và acknowledgment.
- Batch phải có giới hạn số lượng và kích thước.
- Mục tiêu tải tối thiểu là 500 log trong 2 giây.

### 4.5. `processing`

Trách nhiệm:

- Consume `logs.raw`.
- Parse và làm sạch dữ liệu.
- Chuẩn hóa field, timestamp và log level.
- Tạo fingerprint cho lỗi.
- Điều phối lưu ClickHouse, live stream, alert hoặc DLQ.

Kiến trúc:

- Pragmatic DDD kết hợp processing pipeline.
- Đây là module phức tạp vì có invariant, error handling, idempotency và nhiều
  output integration.

Cấu trúc gợi ý:

```text
processing
├── application
│   └── ProcessRawLogService
├── domain
│   ├── LogRecord
│   ├── LogLevel
│   ├── LogNormalizer
│   ├── ErrorFingerprint
│   └── ProcessingResult
└── infrastructure
    ├── KafkaRawLogConsumer
    ├── LogStoragePort
    ├── LiveLogPublisher
    ├── CriticalAlertPublisher
    └── DeadLetterPublisher
```

Domain rules:

- Level hợp lệ: `INFO`, `WARN`, `ERROR`, `CRITICAL`.
- Log chuẩn hóa có `applicationName`, `level`, `message`, `timestamp` và
  `traceId`.
- `ERROR` và `CRITICAL` tạo critical event.
- Kafka retry không được tạo log trùng trong ClickHouse.
- Log không xử lý được phải vào `logs.dlq`, không silently discard.

### 4.6. `logstorage`

Trách nhiệm:

- Ghi batch log đã chuẩn hóa vào ClickHouse.
- Ánh xạ storage model.
- Xử lý timeout, retry và idempotent insert.
- Cung cấp metric insert rate, batch size và failure.

Kiến trúc:

- Infrastructure-oriented adapter.
- Không cần DDD vì không sở hữu business rule.
- Có thể được triển khai như adapter của `processing` thay vì một domain
  module độc lập.

Cấu trúc gợi ý:

```text
logstorage
├── ClickHouseLogWriter
├── ClickHouseLogRow
└── ClickHouseBatchConfiguration
```

Lưu ý:

- Không để model ClickHouse lan vào processing domain.
- Batch size và flush interval phải benchmark.
- Chỉ xác nhận bước `STORED` sau khi ClickHouse ghi thành công.

### 4.7. `logsearch`

Trách nhiệm:

- Tìm log theo application, level, time range và trace ID.
- Phân trang hoặc cursor.
- Kiểm tra quyền application trước khi query.
- Trả dữ liệu tối ưu cho frontend.

Kiến trúc:

- CQRS read module.
- Query service và ClickHouse query repository trực tiếp.
- Không cần domain model giàu hành vi.

Cấu trúc gợi ý:

```text
logsearch
├── LogSearchController
├── LogSearchQueryService
├── ClickHouseLogQueryRepository
└── dto
```

Lưu ý:

- Mọi query phải có time range và result limit.
- Không cho frontend gửi raw SQL hoặc ClickHouse expression.
- Query phải được giới hạn theo danh sách application user được phép xem.

### 4.8. `livestream`

Trách nhiệm:

- Consume `logs.live`.
- Quản lý subscription WebSocket/STOMP.
- Route log theo application và quyền user.
- Áp dụng backpressure hoặc giới hạn buffer cho client chậm.

Kiến trúc:

- Event adapter.
- Không cần DDD.

Cấu trúc gợi ý:

```text
livestream
├── LiveLogConsumer
├── LiveLogBroker
├── SubscriptionAuthorizer
└── WebSocketConfiguration
```

Lưu ý:

- Live stream không được query lại database cho từng event.
- Client chậm không được làm nghẽn Kafka consumer.
- Không gửi log của application mà user không có quyền truy cập.

### 4.9. `alerting`

Trách nhiệm:

- Quản lý `alert_rules`.
- Consume `alerts.critical`.
- Áp dụng threshold và deduplication.
- Tạo hoặc cập nhật `alert_incidents`.
- Quản lý lifecycle incident.
- Gửi yêu cầu notification và AI analysis.

Kiến trúc:

- Pragmatic DDD.
- Đây là module domain phức tạp nhất vì có rule, state transition,
  fingerprint, deduplication và consistency.

Cấu trúc gợi ý:

```text
alerting
├── api
│   ├── AlertRuleController
│   └── IncidentController
├── application
│   ├── EvaluateCriticalEventService
│   ├── AcknowledgeIncidentService
│   └── ResolveIncidentService
├── domain
│   ├── AlertRule
│   ├── AlertIncident
│   ├── IncidentStatus
│   ├── AlertFingerprint
│   └── DeduplicationPolicy
└── infrastructure
    ├── CriticalEventConsumer
    ├── RedisDeduplicationStore
    ├── PostgresAlertRuleRepository
    └── PostgresIncidentRepository
```

Domain rules:

```text
OPEN -> ACKNOWLEDGED -> RESOLVED
```

- Cùng fingerprint xuất hiện 100 lần trong một phút chỉ phát một notification.
- `occurrence_count` và `last_seen_at` vẫn phải được cập nhật.
- AI không tạo incident hoặc alert riêng.
- Incident đã resolve chỉ được mở lại theo rule đã định nghĩa rõ.

### 4.10. `notification`

Trách nhiệm:

- Gửi incident qua WebSocket và Telegram.
- Ghi `alert_deliveries`.
- Retry delivery thất bại.
- Chuẩn hóa provider error.

Kiến trúc:

- Ports and adapters.
- Provider bên ngoài được cô lập sau notification port.

Cấu trúc gợi ý:

```text
notification
├── NotificationService
├── NotificationPort
├── WebSocketNotificationAdapter
├── TelegramNotificationAdapter
├── AlertDeliveryRepository
└── DeliveryRetryWorker
```

Lưu ý:

- Notification failure không rollback incident.
- Retry phải idempotent.
- Không log Telegram token hoặc nội dung nhạy cảm.
- AI insight hoàn thành không gửi thêm alert hoặc Telegram message.

### 4.11. `retention`

Trách nhiệm:

- Quản lý `retention_policies`.
- Chạy background scheduler.
- Điều phối ClickHouse TTL hoặc partition operation.
- Ghi và theo dõi `retention_runs`.
- Cảnh báo run bị treo hoặc thất bại.

Kiến trúc:

- Job orchestration module.
- Feature module gọn; không cần DDD đầy đủ.
- ClickHouse operation nằm sau adapter riêng.

Cấu trúc gợi ý:

```text
retention
├── RetentionPolicyController
├── RetentionPolicyService
├── RetentionScheduler
├── RetentionExecutor
├── ClickHouseRetentionAdapter
├── RetentionPolicyRepository
└── RetentionRunRepository
```

Lưu ý:

- Không chạy Java job `DELETE` từng row.
- INFO quá 7 ngày phải được nén hoặc xóa theo policy.
- Scheduler phải tránh hai instance thực thi cùng một policy đồng thời.
- `retention_runs` phải phản ánh `RUNNING`, `SUCCEEDED`, `FAILED` hoặc
  `PARTIALLY_SUCCEEDED`.

### 4.12. `aianalysis`

Trách nhiệm:

- Consume `incidents.ai`.
- Lấy dữ liệu incident và các log đại diện.
- Redact dữ liệu trước khi gọi AI.
- Phân loại, tóm tắt và đề xuất hướng xử lý.
- Lưu `incident_ai_insights`.
- Cập nhật `ai_status` của incident.

Kiến trúc:

- Async integration module.
- Ports and adapters cho AI provider.
- Không nằm trên hot path ingestion, live log hoặc alert.

Cấu trúc gợi ý:

```text
aianalysis
├── IncidentAiConsumer
├── IncidentAnalysisService
├── AiAnalysisPort
├── AiProviderAdapter
├── IncidentContextReader
└── IncidentInsightRepository
```

Luồng:

```text
Incident created
    -> notification sent immediately
    -> incidents.ai
    -> AI worker
    -> incident_ai_insights
    -> update incident detail
```

Lưu ý:

- Phân tích theo incident/fingerprint, không theo từng log.
- AI failure không ảnh hưởng alert hoặc trạng thái lưu log.
- AI output là gợi ý, không tự động resolve incident.

### 4.13. `analytics`

Trách nhiệm:

- Query `application_health_hourly`.
- Tính error rate, critical rate và health score.
- So sánh độ ổn định giữa các application theo giờ.
- Cung cấp dữ liệu chart cho frontend.

Kiến trúc:

- CQRS read model.
- Materialized view trong ClickHouse tạo dữ liệu tổng hợp.
- Không cần DDD.

Cấu trúc gợi ý:

```text
analytics
├── AnalyticsController
├── ApplicationHealthQueryService
├── ClickHouseHealthRepository
└── dto
```

Lưu ý:

- Dashboard không scan toàn bộ bảng `logs`.
- Health score phải có version nếu công thức thay đổi.
- Xử lý trường hợp không có log trong một khoảng thời gian.

### 4.14. `shared`

Trách nhiệm:

- Chứa primitive hoặc kernel thực sự dùng chung.

Có thể gồm:

```text
shared
├── time
├── error
├── event
└── observability
```

Không được chứa:

- Business service thuộc module cụ thể.
- Repository dùng chung để bỏ qua ownership.
- DTO chung chỉ vì có field giống nhau.
- Utility không rõ owner.

Chỉ chuyển code vào `shared` khi có ít nhất hai consumer thực tế và việc dùng
chung không làm mất ranh giới domain.

## 5. Module dependency

Dependency đồng bộ cho phép:

```text
ingestion -> applicationregistry
accesscontrol -> identity, applicationregistry
logsearch -> accesscontrol
livestream -> accesscontrol
alerting -> applicationregistry
notification -> alerting contract
retention -> applicationregistry
analytics -> accesscontrol
aianalysis -> alerting contract
```

Dependency bất đồng bộ:

```text
ingestion
    -> logs.raw
    -> processing

processing
    -> logs.live
    -> livestream

processing
    -> alerts.critical
    -> alerting

processing failure
    -> logs.dlq

alerting
    -> notification
    -> incidents.ai
    -> aianalysis
```

Quy tắc:

- Không tạo circular dependency.
- Ưu tiên public service/port nhỏ thay vì gọi internal repository.
- Không publish event nếu một lời gọi module trực tiếp đơn giản và nhất quán
  hơn.
- Event public phải có schema version và idempotency key.

## 6. Data ownership

| Dữ liệu | Module sở hữu | Storage |
| --- | --- | --- |
| `users` | `identity` | PostgreSQL |
| `applications`, API keys | `applicationregistry` | PostgreSQL |
| User/application permissions | `accesscontrol` | PostgreSQL, Redis cache |
| Normalized logs | `processing`/`logstorage` | ClickHouse |
| Log search projection | `logsearch` | ClickHouse read-only |
| Alert rules và incidents | `alerting` | PostgreSQL, Redis dedup |
| Alert deliveries/channels | `notification` | PostgreSQL |
| Retention policies/runs | `retention` | PostgreSQL |
| AI insights | `aianalysis` | ClickHouse |
| Hourly health metrics | `analytics` | ClickHouse |

Chi tiết bảng, cột và kiểu dữ liệu nằm tại:

```text
docs/store-requirement.md
```

## 7. Kafka topic ownership

| Topic | Producer | Consumer |
| --- | --- | --- |
| `logs.raw` | `ingestion` | `processing` |
| `logs.live` | `processing` | `livestream` |
| `logs.dlq` | `processing` | Operations/admin tooling |
| `alerts.critical` | `processing` | `alerting` |
| `incidents.ai` | `alerting` | `aianalysis` |

## 8. Frontend modules

Frontend sử dụng feature-based architecture:

```text
apps/frontend/src/features
├── auth
├── applications
├── live-logs
├── log-search
├── alerts
├── analytics
├── retention
└── administration
```

Mỗi feature có thể dùng cấu trúc:

```text
<feature>
├── api
├── components
├── hooks
├── pages
├── types
└── utils
```

Chỉ tạo thư mục khi có nội dung thực tế.

Frontend rules:

- `src/app` chỉ chứa router, provider và application wiring.
- Server state dùng TanStack Query.
- HTTP dùng shared Axios client.
- Log realtime dùng WebSocket/STOMP client.
- Feature không import internal file của feature khác; dùng public export hoặc
  shared component phù hợp.
- Quyền hiển thị trên frontend không thay thế authorization phía backend.

## 9. Phân loại kiến trúc

### Module đơn giản

Các module dự kiến bắt đầu với feature structure gọn:

- `identity`
- `applicationregistry`
- `accesscontrol`
- `livestream`
- `retention`

### Module có domain phức tạp

Áp dụng pragmatic DDD:

- `processing`
- `alerting`

### Module external integration

Áp dụng ports and adapters:

- `ingestion`
- `logstorage`
- `notification`
- `aianalysis`

### Module read-side

Áp dụng CQRS nhẹ:

- `logsearch`
- `analytics`

Phân loại này có thể thay đổi theo độ phức tạp thực tế. Không refactor sang DDD
chỉ để đồng nhất tên package.

## 10. Thứ tự triển khai đề xuất

1. `applicationregistry` và API key.
2. `ingestion` và `logs.raw`.
3. `processing` và `logstorage`.
4. `logsearch`.
5. `livestream`.
6. `identity` và `accesscontrol`.
7. `alerting` và Redis deduplication.
8. `notification`.
9. `analytics`.
10. `retention`.
11. `aianalysis`.

Thứ tự này ưu tiên hoàn thiện luồng:

```text
500 logs/2 seconds
    -> Kafka
    -> normalize
    -> ClickHouse
    -> live viewer
```

trước khi bổ sung alert, analytics, retention và AI.

## 11. Tiêu chí review module

Trước khi tạo hoặc thay đổi module, kiểm tra:

- Capability và owner có rõ ràng không?
- Module có đang truy cập storage của module khác không?
- Có cần DDD thật sự hay feature structure đã đủ?
- Có abstraction hoặc interface không có consumer thực tế không?
- Event có cần thiết hay gọi trực tiếp đơn giản hơn?
- Retry, idempotency và failure path đã rõ chưa?
- Dữ liệu nhạy cảm có được redaction không?
- Focused test và integration test nào chứng minh module hoạt động?
- Documentation và storage ownership có còn đồng bộ không?

# Storage Requirements

## 1. Mục tiêu

Tài liệu này mô tả thiết kế lưu trữ đã chốt cho Log Monitoring System.
Thiết kế gồm:

- 10 bảng PostgreSQL cho dữ liệu quản trị, phân quyền và cấu hình.
- 3 bảng ClickHouse cho log, AI insight và thống kê sức khỏe ứng dụng.
- Redis cho khóa chống cảnh báo trùng và cache dữ liệu API.
- 5 Kafka topic cho luồng xử lý bất đồng bộ.

Đây là yêu cầu thiết kế, chưa phải mô tả schema đã được triển khai. Mọi thay
đổi schema sau này phải được thực hiện bằng migration và đồng bộ lại tài liệu.

## 2. Nguyên tắc lưu trữ

- PostgreSQL không được nằm trên đường ghi log tốc độ cao.
- API tiếp nhận log phải đẩy log thô vào Kafka thay vì `INSERT` trực tiếp.
- Worker ghi log đã chuẩn hóa vào ClickHouse theo batch.
- Redis chỉ giữ dữ liệu tạm thời có TTL, không phải nguồn dữ liệu chính.
- Kafka là lớp truyền sự kiện, không thay thế database truy vấn.
- Tất cả timestamp lưu theo UTC.
- Không lưu mật khẩu, API key, token Telegram hoặc secret ở dạng thô.
- Query log phải có giới hạn thời gian, phân trang hoặc giới hạn số bản ghi.
- Dữ liệu log có thể chứa thông tin nhạy cảm và phải được lọc hoặc che trước
  khi lưu.

## 3. Phân chia trách nhiệm

| Hệ thống | Trách nhiệm |
| --- | --- |
| PostgreSQL 17 | User, application, quyền truy cập, API key, alert, retention |
| ClickHouse 25.3 | Log đã chuẩn hóa, AI insight, health analytics |
| Redis 8 | Alert deduplication và cache API |
| Kafka 4 | Buffer log thô và truyền event giữa các worker |

## 4. Quy ước chung

### PostgreSQL

- Tên bảng và cột dùng `snake_case`.
- Primary key sử dụng `UUID`.
- UUID được sinh tại application hoặc bằng cơ chế UUID đã thống nhất trong
  migration.
- Thời gian dùng `TIMESTAMPTZ`.
- Trạng thái dùng `VARCHAR` kết hợp `CHECK`, tránh PostgreSQL enum khó thay đổi.
- Schema được quản lý bằng Flyway tại:
  `apps/backend/src/main/resources/db/migration/postgresql/`.
- Migration đã chạy không được chỉnh sửa.
- Foreign key phải chỉ rõ hành vi `ON DELETE`.

### ClickHouse

- Thời gian dùng `DateTime64(3, 'UTC')`.
- Giá trị ít thay đổi dùng `LowCardinality(String)` hoặc `Enum8`.
- Dữ liệu log được ghi theo batch.
- Không cập nhật trạng thái từng log bằng mutation liên tục.
- Partition, sorting key và TTL phải được kiểm tra bằng dữ liệu tải thực tế.

## 5. PostgreSQL

### 5.1. `users`

Quản lý tài khoản đăng nhập và vai trò hệ thống.

| Cột | Kiểu dữ liệu | Null | Ràng buộc/Ghi chú |
| --- | --- | --- | --- |
| `id` | `UUID` | Không | Primary key |
| `email` | `VARCHAR(320)` | Không | Email đã normalize về lowercase |
| `password_hash` | `VARCHAR(255)` | Không | Chỉ lưu password hash |
| `display_name` | `VARCHAR(150)` | Không | Tên hiển thị |
| `role` | `VARCHAR(32)` | Không | `ADMIN` hoặc `ENGINEER` |
| `status` | `VARCHAR(32)` | Không | `ACTIVE`, `DISABLED`, `LOCKED` |
| `last_login_at` | `TIMESTAMPTZ` | Có | Lần đăng nhập gần nhất |
| `created_at` | `TIMESTAMPTZ` | Không | Mặc định thời gian hiện tại |
| `updated_at` | `TIMESTAMPTZ` | Không | Cập nhật khi thay đổi |

Index và ràng buộc:

- Unique index trên `LOWER(email)`.
- `CHECK` cho `role` và `status`.
- Không xóa cứng user đã có lịch sử thao tác; chuyển `status`.

### 5.2. `applications`

Quản lý các ứng dụng nguồn gửi log.

| Cột | Kiểu dữ liệu | Null | Ràng buộc/Ghi chú |
| --- | --- | --- | --- |
| `id` | `UUID` | Không | Primary key |
| `name` | `VARCHAR(100)` | Không | Tên kỹ thuật duy nhất |
| `display_name` | `VARCHAR(150)` | Không | Tên hiển thị |
| `description` | `TEXT` | Có | Mô tả ứng dụng |
| `status` | `VARCHAR(32)` | Không | `ACTIVE`, `INACTIVE` |
| `created_by` | `UUID` | Không | FK đến `users.id` |
| `created_at` | `TIMESTAMPTZ` | Không | |
| `updated_at` | `TIMESTAMPTZ` | Không | |

Index và ràng buộc:

- Unique index trên `LOWER(name)`.
- Index trên `status`.
- `created_by` dùng `ON DELETE RESTRICT`.
- Không tái sử dụng `id` hoặc `name` cho một ứng dụng khác.

### 5.3. `user_application_access`

Xác định kỹ sư được xem hoặc quản lý ứng dụng nào.

| Cột | Kiểu dữ liệu | Null | Ràng buộc/Ghi chú |
| --- | --- | --- | --- |
| `user_id` | `UUID` | Không | FK đến `users.id` |
| `application_id` | `UUID` | Không | FK đến `applications.id` |
| `access_level` | `VARCHAR(32)` | Không | `VIEW` hoặc `MANAGE` |
| `granted_by` | `UUID` | Không | FK đến `users.id` |
| `created_at` | `TIMESTAMPTZ` | Không | |
| `updated_at` | `TIMESTAMPTZ` | Không | |

Index và ràng buộc:

- Primary key gồm `(user_id, application_id)`.
- Index đảo trên `(application_id, user_id)` để tìm user theo ứng dụng.
- User có role `ADMIN` được quyền toàn hệ thống, không cần sinh bản ghi cho
  từng application.
- Xóa user hoặc application phải xóa các quyền liên quan bằng cascade.

### 5.4. `application_api_keys`

Quản lý API key dùng để xác thực ứng dụng gửi log.

| Cột | Kiểu dữ liệu | Null | Ràng buộc/Ghi chú |
| --- | --- | --- | --- |
| `id` | `UUID` | Không | Primary key |
| `application_id` | `UUID` | Không | FK đến `applications.id` |
| `name` | `VARCHAR(100)` | Không | Tên gợi nhớ của key |
| `key_prefix` | `VARCHAR(32)` | Không | Phần công khai để nhận diện |
| `key_hash` | `VARCHAR(255)` | Không | Hash của API key |
| `status` | `VARCHAR(32)` | Không | `ACTIVE`, `REVOKED`, `EXPIRED` |
| `expires_at` | `TIMESTAMPTZ` | Có | Null nếu không hết hạn |
| `last_used_at` | `TIMESTAMPTZ` | Có | Cập nhật có kiểm soát |
| `created_by` | `UUID` | Không | FK đến `users.id` |
| `created_at` | `TIMESTAMPTZ` | Không | |
| `revoked_at` | `TIMESTAMPTZ` | Có | |

Index và ràng buộc:

- Unique index trên `key_prefix`.
- Index trên `(application_id, status)`.
- Chỉ hiển thị API key thô một lần khi tạo.
- Không log hoặc trả lại `key_hash`.
- Việc cập nhật `last_used_at` không được tạo write load cho mỗi log; nên cập
  nhật theo khoảng thời gian hoặc bất đồng bộ.

### 5.5. `alert_rules`

Cấu hình điều kiện tạo cảnh báo.

| Cột | Kiểu dữ liệu | Null | Ràng buộc/Ghi chú |
| --- | --- | --- | --- |
| `id` | `UUID` | Không | Primary key |
| `application_id` | `UUID` | Có | Null nếu áp dụng toàn hệ thống |
| `name` | `VARCHAR(150)` | Không | |
| `levels` | `VARCHAR(16)[]` | Không | Tập `ERROR`, `CRITICAL` hoặc level khác |
| `threshold_count` | `INTEGER` | Không | Số lần xuất hiện để tạo incident |
| `window_seconds` | `INTEGER` | Không | Cửa sổ đếm sự kiện |
| `dedup_seconds` | `INTEGER` | Không | TTL chống cảnh báo trùng |
| `enabled` | `BOOLEAN` | Không | |
| `created_by` | `UUID` | Không | FK đến `users.id` |
| `created_at` | `TIMESTAMPTZ` | Không | |
| `updated_at` | `TIMESTAMPTZ` | Không | |

Index và ràng buộc:

- Index trên `(application_id, enabled)`.
- `threshold_count`, `window_seconds`, `dedup_seconds` phải lớn hơn `0`.
- Mặc định có thể cảnh báo ngay từ lỗi đầu tiên và dedup trong 60 giây.
- Quy tắc fingerprint phải được xác định ổn định để gom lỗi giống nhau.

### 5.6. `notification_channels`

Quản lý kênh gửi thông báo. Phiên bản đầu hỗ trợ Telegram.

| Cột | Kiểu dữ liệu | Null | Ràng buộc/Ghi chú |
| --- | --- | --- | --- |
| `id` | `UUID` | Không | Primary key |
| `application_id` | `UUID` | Có | Null nếu dùng chung toàn hệ thống |
| `name` | `VARCHAR(100)` | Không | |
| `type` | `VARCHAR(32)` | Không | Hiện tại là `TELEGRAM` |
| `configuration` | `JSONB` | Không | Ví dụ `chat_id`, template, options |
| `secret_reference` | `VARCHAR(255)` | Có | Tên biến môi trường/secret |
| `enabled` | `BOOLEAN` | Không | |
| `created_at` | `TIMESTAMPTZ` | Không | |
| `updated_at` | `TIMESTAMPTZ` | Không | |

Lưu ý:

- Không lưu Telegram bot token trong `configuration`.
- JSON phải được validate theo `type`.
- Nếu sau này một rule cần nhiều channel, có thể bổ sung bảng liên kết. Trong
  thiết kế hiện tại, channel được chọn theo application hoặc cấu hình mặc định.

### 5.7. `alert_incidents`

Gom các lỗi trùng thành một sự cố để theo dõi và phân tích.

| Cột | Kiểu dữ liệu | Null | Ràng buộc/Ghi chú |
| --- | --- | --- | --- |
| `id` | `UUID` | Không | Primary key |
| `application_id` | `UUID` | Không | FK đến `applications.id` |
| `alert_rule_id` | `UUID` | Có | FK đến `alert_rules.id` |
| `fingerprint` | `VARCHAR(128)` | Không | Hash ổn định của nhóm lỗi |
| `level` | `VARCHAR(16)` | Không | `ERROR` hoặc `CRITICAL` |
| `title` | `VARCHAR(255)` | Không | Tiêu đề ngắn |
| `message_sample` | `TEXT` | Không | Nội dung mẫu đã redaction |
| `representative_event_id` | `UUID` | Có | Event ClickHouse đại diện |
| `occurrence_count` | `BIGINT` | Không | Tổng số lần xuất hiện |
| `first_seen_at` | `TIMESTAMPTZ` | Không | |
| `last_seen_at` | `TIMESTAMPTZ` | Không | |
| `status` | `VARCHAR(32)` | Không | `OPEN`, `ACKNOWLEDGED`, `RESOLVED` |
| `ai_status` | `VARCHAR(32)` | Không | `NOT_REQUESTED`, `PENDING`, `PROCESSING`, `COMPLETED`, `FAILED` |
| `acknowledged_by` | `UUID` | Có | FK đến `users.id` |
| `acknowledged_at` | `TIMESTAMPTZ` | Có | |
| `resolved_at` | `TIMESTAMPTZ` | Có | |
| `created_at` | `TIMESTAMPTZ` | Không | |
| `updated_at` | `TIMESTAMPTZ` | Không | |

Index và ràng buộc:

- Index trên `(application_id, status, last_seen_at DESC)`.
- Index trên `(application_id, fingerprint, last_seen_at DESC)`.
- `occurrence_count` phải lớn hơn hoặc bằng `1`.
- AI chỉ bổ sung insight vào incident, không phát thêm một alert độc lập.
- ClickHouse không có foreign key; `representative_event_id` được kiểm tra ở
  application level.

### 5.8. `alert_deliveries`

Theo dõi kết quả gửi cảnh báo qua WebSocket hoặc Telegram.

| Cột | Kiểu dữ liệu | Null | Ràng buộc/Ghi chú |
| --- | --- | --- | --- |
| `id` | `UUID` | Không | Primary key |
| `incident_id` | `UUID` | Không | FK đến `alert_incidents.id` |
| `channel_id` | `UUID` | Có | FK đến `notification_channels.id` |
| `delivery_type` | `VARCHAR(32)` | Không | `WEBSOCKET`, `TELEGRAM` |
| `status` | `VARCHAR(32)` | Không | `PENDING`, `SENT`, `FAILED`, `RETRYING` |
| `attempt_count` | `INTEGER` | Không | Mặc định `0` |
| `provider_message_id` | `VARCHAR(255)` | Có | ID phản hồi từ provider |
| `last_error_code` | `VARCHAR(100)` | Có | Không chứa secret |
| `last_error_message` | `TEXT` | Có | Nội dung đã lọc |
| `created_at` | `TIMESTAMPTZ` | Không | |
| `sent_at` | `TIMESTAMPTZ` | Có | |
| `updated_at` | `TIMESTAMPTZ` | Không | |

Index và ràng buộc:

- Index trên `(incident_id, delivery_type)`.
- Index trên `(status, updated_at)` để worker tìm bản ghi cần retry.
- Dùng idempotency để tránh gửi lại cùng một notification ngoài ý muốn.

### 5.9. `retention_policies`

Cấu hình nén và xóa log theo level hoặc application.

| Cột | Kiểu dữ liệu | Null | Ràng buộc/Ghi chú |
| --- | --- | --- | --- |
| `id` | `UUID` | Không | Primary key |
| `application_id` | `UUID` | Có | Null là policy mặc định |
| `log_level` | `VARCHAR(16)` | Không | `INFO`, `WARN`, `ERROR`, `CRITICAL` |
| `compress_after_days` | `INTEGER` | Có | Null nếu không nén |
| `delete_after_days` | `INTEGER` | Có | Null nếu không tự xóa |
| `enabled` | `BOOLEAN` | Không | |
| `created_by` | `UUID` | Không | FK đến `users.id` |
| `created_at` | `TIMESTAMPTZ` | Không | |
| `updated_at` | `TIMESTAMPTZ` | Không | |

Yêu cầu mặc định:

- Log `INFO` phải được nén hoặc xóa khi quá 7 ngày.
- Khuyến nghị nén `INFO` sau 7 ngày và xóa sau 30 ngày.
- `delete_after_days` phải lớn hơn `compress_after_days` nếu cả hai tồn tại.
- Chỉ có một policy đang bật cho mỗi cặp application/level.
- Policy riêng của application ưu tiên hơn policy mặc định.

### 5.10. `retention_runs`

Theo dõi tiến trình và kết quả của retention background job.

| Cột | Kiểu dữ liệu | Null | Ràng buộc/Ghi chú |
| --- | --- | --- | --- |
| `id` | `UUID` | Không | Primary key |
| `policy_id` | `UUID` | Không | FK đến `retention_policies.id` |
| `run_type` | `VARCHAR(32)` | Không | `COMPRESS`, `DELETE`, `VERIFY` |
| `status` | `VARCHAR(32)` | Không | `RUNNING`, `SUCCEEDED`, `FAILED`, `PARTIALLY_SUCCEEDED` |
| `worker_instance` | `VARCHAR(150)` | Có | Instance thực thi job |
| `started_at` | `TIMESTAMPTZ` | Không | |
| `completed_at` | `TIMESTAMPTZ` | Có | |
| `affected_rows` | `BIGINT` | Không | Mặc định `0` |
| `affected_partitions` | `INTEGER` | Không | Mặc định `0` |
| `reclaimed_bytes` | `BIGINT` | Không | Mặc định `0` |
| `error_code` | `VARCHAR(100)` | Có | |
| `error_message` | `TEXT` | Có | Nội dung đã lọc |
| `metadata` | `JSONB` | Không | Chi tiết quan sát, mặc định `{}` |

Index và ràng buộc:

- Index trên `(status, started_at DESC)`.
- Index trên `(policy_id, started_at DESC)`.
- Các số liệu đếm không được âm.
- Job phải phát hiện lần chạy `RUNNING` quá thời gian cho phép.
- Không ghi secret hoặc toàn bộ câu lệnh chứa credential vào `metadata`.

## 6. ClickHouse

### 6.1. `logs`

Lưu log đã chuẩn hóa. Đây là bảng có lưu lượng ghi và dung lượng lớn nhất.

| Cột | Kiểu dữ liệu | Ghi chú |
| --- | --- | --- |
| `event_id` | `UUID` | ID duy nhất của log |
| `ingestion_id` | `UUID` | ID của request/batch tiếp nhận |
| `application_id` | `UUID` | Liên kết logic đến PostgreSQL |
| `application_name` | `LowCardinality(String)` | Denormalize để truy vấn nhanh |
| `level` | `Enum8` | `INFO=1`, `WARN=2`, `ERROR=3`, `CRITICAL=4` |
| `message` | `String` | Nội dung đã normalize/redaction |
| `trace_id` | `Nullable(String)` | ID truy vết giữa các service |
| `event_timestamp` | `DateTime64(3, 'UTC')` | Thời gian ứng dụng phát log |
| `received_at` | `DateTime64(3, 'UTC')` | Thời gian ingestion nhận |
| `normalized_at` | `DateTime64(3, 'UTC')` | Thời gian worker chuẩn hóa |
| `stored_at` | `DateTime64(3, 'UTC')` | Thời gian ghi ClickHouse |
| `environment` | `LowCardinality(String)` | Ví dụ `prod`, `staging` |
| `host_name` | `LowCardinality(String)` | Host/container phát log |
| `source` | `LowCardinality(String)` | Nguồn hoặc logger |
| `fingerprint` | `Nullable(String)` | Fingerprint cho nhóm lỗi |
| `attributes` | `Map(String, String)` | Metadata mở rộng đã giới hạn |

Engine và organization key đề xuất:

```text
ENGINE: MergeTree
PARTITION BY: toYYYYMM(event_timestamp)
ORDER BY: (application_id, level, event_timestamp, event_id)
```

Lưu ý:

- `application_name` là snapshot phục vụ tìm kiếm; `application_id` vẫn là
  định danh chính.
- `event_id` phải giữ nguyên khi Kafka retry để hỗ trợ idempotency.
- Worker ghi theo batch, không ghi từng row.
- `attributes` phải giới hạn số key, chiều dài key/value và tổng kích thước.
- Không dùng `Nullable` tùy tiện vì làm tăng chi phí lưu và truy vấn.
- Query bắt buộc có time range và limit.
- Có thể bổ sung bloom filter index cho `trace_id` sau khi benchmark.

#### Trạng thái xử lý log

Không tạo bảng trạng thái riêng và không update liên tục trong ClickHouse:

| Trạng thái | Dấu hiệu trong pipeline |
| --- | --- |
| `RAW_RECEIVED` | Event đã được ghi vào `logs.raw` |
| `NORMALIZED` | Worker đã parse và validate thành công |
| `STORED` | Bản ghi tồn tại trong ClickHouse `logs` |
| `FAILED` | Event được chuyển vào `logs.dlq` |

Nếu sau này cần audit từng transition, phải bổ sung một append-only event table
thay vì mutation trên `logs`.

#### Retention

- ClickHouse native TTL/background merge thực hiện nén hoặc xóa dữ liệu.
- Retention scheduler đọc `retention_policies`, áp dụng/kiểm tra policy và ghi
  kết quả vào `retention_runs`.
- Không chạy job Java xóa từng row.
- INFO quá 7 ngày phải được nén hoặc xóa.
- Delete mutation theo application chỉ dùng khi thực sự cần vì chi phí cao.
- Policy theo level toàn hệ thống nên được ưu tiên; override theo application
  cần được benchmark trước.

### 6.2. `incident_ai_insights`

Lưu kết quả AI phân tích incident, không phân tích và lưu kết quả cho từng log.

| Cột | Kiểu dữ liệu | Ghi chú |
| --- | --- | --- |
| `incident_id` | `UUID` | Liên kết logic đến PostgreSQL |
| `application_id` | `UUID` | Phục vụ filter |
| `analysis_version` | `UInt32` | Phiên bản phân tích |
| `provider` | `LowCardinality(String)` | Nhà cung cấp AI |
| `model_name` | `LowCardinality(String)` | Model đã dùng |
| `category` | `LowCardinality(String)` | Nhóm nguyên nhân |
| `summary` | `String` | Tóm tắt sự cố |
| `possible_root_cause` | `String` | Nguyên nhân khả dĩ |
| `suggested_action` | `String` | Hướng xử lý đề xuất |
| `confidence` | `Float32` | Từ `0.0` đến `1.0` |
| `source_fingerprint` | `String` | Fingerprint của incident |
| `analyzed_at` | `DateTime64(3, 'UTC')` | |

Engine đề xuất:

```text
ENGINE: ReplacingMergeTree(analyzed_at)
PARTITION BY: toYYYYMM(analyzed_at)
ORDER BY: (application_id, incident_id, analysis_version)
```

Lưu ý:

- AI không nằm trên hot path ingestion, live log hoặc alert.
- AI chỉ chạy sau khi incident đã được tạo và alert đã được gửi.
- Kết quả AI bổ sung vào incident hiện tại, không tạo alert thứ hai.
- Nên phân tích nhóm lỗi theo fingerprint, không phân tích từng log.
- Prompt và dữ liệu gửi AI phải được redaction.
- Kết quả AI chỉ là gợi ý, không tự động thay đổi trạng thái incident.

### 6.3. `application_health_hourly`

Lưu số liệu tổng hợp theo ứng dụng và giờ cho dashboard.

| Cột | Kiểu dữ liệu | Ghi chú |
| --- | --- | --- |
| `hour` | `DateTime('UTC')` | Bắt đầu của giờ |
| `application_id` | `UUID` | |
| `application_name` | `LowCardinality(String)` | Snapshot hiển thị |
| `total_count` | `UInt64` | Tổng số log |
| `info_count` | `UInt64` | |
| `warn_count` | `UInt64` | |
| `error_count` | `UInt64` | |
| `critical_count` | `UInt64` | |

Engine đề xuất:

```text
ENGINE: SummingMergeTree
PARTITION BY: toYYYYMM(hour)
ORDER BY: (application_id, hour)
```

Materialized view tổng hợp dữ liệu từ `logs` theo `toStartOfHour`.

Các chỉ số được tính khi query:

```text
error_rate = (error_count + critical_count) / total_count
critical_rate = critical_count / total_count
```

`health_score` được tính từ các count/rate theo công thức đã version hóa ở
application layer hoặc query layer. Không lưu cứng score nếu công thức còn có
thể thay đổi.

Lưu ý:

- Tránh scan bảng `logs` để vẽ dashboard theo giờ.
- Query phải xử lý trường hợp `total_count = 0`.
- Khi cần thay đổi công thức health score, API phải trả về phiên bản công thức.
- Không dùng tỷ lệ lỗi đơn lẻ để tự động kết luận nguyên nhân sự cố.

## 7. Redis

Redis chỉ chứa dữ liệu có thể tái tạo từ PostgreSQL, ClickHouse hoặc event
pipeline.

### 7.1. Alert deduplication

Key:

```text
alert:dedup:{application_id}:{fingerprint}
```

Giá trị đề xuất:

```json
{
  "incidentId": "uuid",
  "count": 100,
  "firstSeenAt": "UTC timestamp",
  "lastSeenAt": "UTC timestamp"
}
```

Yêu cầu:

- TTL mặc định 60 giây hoặc lấy từ `alert_rules.dedup_seconds`.
- Thao tác tạo key và tăng count phải atomic.
- Không đưa `timestamp` hoặc `trace_id` vào fingerprint vì sẽ phá dedup.
- Khi 100 lỗi giống nhau xuất hiện trong một phút, chỉ gửi một cảnh báo nhưng
  vẫn cập nhật `occurrence_count`.

### 7.2. Application cache

```text
cache:application:{application_id}
cache:application:list
```

- TTL đề xuất: 5-15 phút.
- Xóa cache khi application thay đổi trạng thái hoặc thông tin.
- Danh sách application phải được lọc theo quyền trước khi trả cho client.

### 7.3. User và access cache

```text
cache:user:{user_id}
cache:user:{user_id}:applications
cache:access:{user_id}:{application_id}
```

- TTL đề xuất: 5-15 phút.
- Xóa cache khi user, role hoặc `user_application_access` thay đổi.
- Không cache password hash, API key hash hoặc secret.
- Cache miss phải đọc lại PostgreSQL; cache không được quyết định quyền khi dữ
  liệu đã hết hạn.

### 7.4. Quy ước Redis

- Key phải có namespace rõ ràng.
- Mọi cache key phải có TTL.
- Payload lớn cần giới hạn kích thước.
- Không dùng lệnh quét blocking như `KEYS` trong production.
- Cần metric cho hit rate, miss rate, eviction và memory usage.

## 8. Kafka topics

Kafka topic không phải bảng database nhưng là một phần của yêu cầu lưu chuyển
dữ liệu.

| Topic | Key đề xuất | Nội dung | Lưu ý |
| --- | --- | --- | --- |
| `logs.raw` | `application_id` | Log thô đã được ingestion chấp nhận | Buffer chính, không ghi DB trực tiếp |
| `logs.live` | `application_id` | Log đã chuẩn hóa cho live viewer | Retention ngắn |
| `logs.dlq` | `application_id` | Log không parse hoặc xử lý được | Giữ lỗi và lý do đã lọc |
| `alerts.critical` | `fingerprint` | Event `ERROR`/`CRITICAL` | Dùng cho dedup và tạo incident |
| `incidents.ai` | `incident_id` | Yêu cầu AI phân tích incident | Không chặn alert hoặc live log |

Yêu cầu:

- Event có `event_id`, schema version và timestamp UTC.
- Producer phải xác định acknowledgment và retry.
- Consumer phải idempotent vì Kafka có thể giao event nhiều lần.
- Topic phải có retention, partition count và DLQ strategy rõ ràng.
- Không đặt payload bí mật hoặc dữ liệu chưa redaction vào event AI.

## 9. Luồng lưu trữ

```text
External Application
    -> Ingestion API
    -> Kafka logs.raw
    -> Parsing Worker
        -> ClickHouse logs
        -> Kafka logs.live
        -> Kafka alerts.critical (ERROR/CRITICAL)
        -> Kafka logs.dlq (parse/store failure)

alerts.critical
    -> Redis dedup
    -> PostgreSQL alert_incidents
    -> PostgreSQL alert_deliveries
    -> WebSocket + Telegram
    -> Kafka incidents.ai
    -> ClickHouse incident_ai_insights

ClickHouse logs
    -> Materialized View
    -> ClickHouse application_health_hourly

Retention Scheduler
    -> PostgreSQL retention_policies
    -> ClickHouse TTL/background operations
    -> PostgreSQL retention_runs
```

## 10. Tính nhất quán và lỗi

- PostgreSQL và ClickHouse không có transaction chung.
- Các liên kết `application_id`, `incident_id`, `event_id` giữa hai database
  là logical reference.
- Worker phải hỗ trợ retry và idempotency.
- Event chỉ được xem là xử lý xong sau khi bước lưu tương ứng thành công.
- Nếu ghi ClickHouse thất bại sau số lần retry cho phép, event chuyển
  `logs.dlq`.
- Nếu Telegram thất bại, incident vẫn tồn tại và `alert_deliveries` ghi trạng
  thái để retry.
- Nếu AI thất bại, alert và incident không bị ảnh hưởng; `ai_status` chuyển
  `FAILED`.

## 11. Backup và quan sát

### PostgreSQL

- Backup định kỳ và kiểm thử restore.
- Theo dõi connection pool, slow query, lock và database size.
- Audit thay đổi API key, quyền truy cập, alert rule và retention policy.

### ClickHouse

- Theo dõi insert rate, part count, merge backlog, disk usage và query latency.
- Cảnh báo khi retention không giải phóng dung lượng đúng hạn.
- Không tạo quá nhiều partition nhỏ.

### Redis

- Theo dõi memory, eviction, hit rate và expired keys.
- Cấu hình persistence chỉ phục vụ khả năng phục hồi cache/dedup; không coi
  Redis là nguồn dữ liệu chính.

### Kafka

- Theo dõi consumer lag, throughput, under-replicated partition và DLQ rate.
- Cảnh báo khi worker không theo kịp tốc độ ingestion.

## 12. Quyết định cần benchmark trước production

- Batch size và flush interval khi ghi ClickHouse.
- Số partition của từng Kafka topic.
- Sorting key và partition strategy của bảng `logs`.
- TTL nén/xóa thực tế theo dung lượng ổ đĩa.
- Giới hạn kích thước message và `attributes`.
- Công thức fingerprint và health score.
- TTL cache và chiến lược invalidation.
- Tải mục tiêu tối thiểu: tiếp nhận 500 log trong 2 giây, không lỗi và live
  viewer vẫn hoạt động mượt.

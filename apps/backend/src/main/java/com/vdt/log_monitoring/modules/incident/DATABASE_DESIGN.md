# Incident database design

## Mục tiêu

Database của module `incident` lưu hồ sơ điều tra lỗi, bằng chứng đã chọn và kết
quả phân tích AI. Module này không lưu lại toàn bộ log lịch sử, không thay thế
`alerting`, và không thay thế ClickHouse.

Định nghĩa ngắn:

```text
PostgreSQL incident schema = hồ sơ điều tra, trạng thái workflow, AI output.
ClickHouse processed_logs = nguồn log evidence để truy vấn theo time window.
Alerting schema = nguồn alert evidence.
```

## Nguyên tắc thiết kế

- Tạo schema PostgreSQL riêng: `incident`.
- Incident là aggregate chính, có thể liên kết nhiều alert, nhiều log, nhiều
  trace và nhiều application.
- Không giả định `1 alert = 1 incident`.
- Evidence lưu dữ liệu tóm tắt và tham chiếu, không copy raw log số lượng lớn.
- AI output phải có cấu trúc ổn định để frontend hiển thị và backend audit.
- Mọi dữ liệu đưa vào incident và AI analysis phải tôn trọng quyền application
  của user.
- Provider AI là implementation detail; database chỉ lưu provider/model để audit,
  không phụ thuộc vendor cụ thể.

## Schema tổng quan

```text
incident.incidents
  ├── incident.incident_applications
  ├── incident.incident_alerts
  ├── incident.incident_evidence
  ├── incident.incident_ai_analyses
  └── incident.incident_timeline_events
```

Quan hệ ngoài schema:

```text
incident.incidents.created_by -> identity.users.id
incident.incident_applications.application_id -> identity.applications.id
incident.incident_alerts.alert_id -> alerting.alerts.id
```

## Bảng `incident.incidents`

Lưu hồ sơ điều tra chính.

| Column | Type | Null | Ghi chú |
| --- | --- | --- | --- |
| `id` | `UUID` | no | Primary key |
| `title` | `VARCHAR(180)` | no | Tên incident, có thể do user hoặc AI đề xuất |
| `description` | `TEXT` | yes | Mô tả thủ công hoặc mô tả ban đầu |
| `status` | `VARCHAR(32)` | no | `INVESTIGATING`, `MITIGATED`, `RESOLVED` |
| `severity` | `VARCHAR(32)` | no | `SEV1`, `SEV2`, `SEV3`, `UNKNOWN` |
| `scope` | `VARCHAR(32)` | no | `APPLICATION`, `MULTI_APPLICATION`, `SYSTEM_WIDE` |
| `trigger_type` | `VARCHAR(32)` | no | `MANUAL`, `AUTO_ALERT`, `AUTO_HEALTH` |
| `started_at` | `TIMESTAMPTZ` | no | Thời điểm bắt đầu điều tra |
| `window_start` | `TIMESTAMPTZ` | no | Bắt đầu time window thu thập evidence |
| `window_end` | `TIMESTAMPTZ` | no | Kết thúc time window thu thập evidence |
| `last_evidence_collected_at` | `TIMESTAMPTZ` | yes | Mốc cuối cùng đã collect evidence, dùng cho manual refresh |
| `created_by` | `UUID` | yes | User tạo incident; null nếu system auto-trigger |
| `resolved_by` | `UUID` | yes | User resolve incident |
| `resolved_at` | `TIMESTAMPTZ` | yes | Thời điểm resolve |
| `created_at` | `TIMESTAMPTZ` | no | Default current timestamp |
| `updated_at` | `TIMESTAMPTZ` | no | Cập nhật khi workflow thay đổi |

Ràng buộc gợi ý:

- `status IN ('INVESTIGATING', 'MITIGATED', 'RESOLVED')`
- `severity IN ('SEV1', 'SEV2', 'SEV3', 'UNKNOWN')`
- `scope IN ('APPLICATION', 'MULTI_APPLICATION', 'SYSTEM_WIDE')`
- `trigger_type IN ('MANUAL', 'AUTO_ALERT', 'AUTO_HEALTH')`
- `window_start <= window_end`
- `resolved_at IS NOT NULL` khi `status = 'RESOLVED'`

Index gợi ý:

- `(status, severity, started_at DESC)`
- `(created_by, started_at DESC)`
- `(window_start, window_end)`
- `(last_evidence_collected_at)`

## Bảng `incident.incident_applications`

Lưu application bị ảnh hưởng bởi incident. Bảng này giúp query incident theo app
và kiểm tra quyền truy cập.

| Column | Type | Null | Ghi chú |
| --- | --- | --- | --- |
| `incident_id` | `UUID` | no | FK đến `incident.incidents.id` |
| `application_id` | `UUID` | no | FK đến `identity.applications.id` |
| `impact_role` | `VARCHAR(32)` | no | `PRIMARY`, `RELATED`, `SUSPECTED` |
| `created_at` | `TIMESTAMPTZ` | no | Default current timestamp |

Primary key:

```text
(incident_id, application_id)
```

Index gợi ý:

- `(application_id, incident_id)`

## Bảng `incident.incident_alerts`

Liên kết incident với alert đã có trong module `alerting`.

| Column | Type | Null | Ghi chú |
| --- | --- | --- | --- |
| `incident_id` | `UUID` | no | FK đến `incident.incidents.id` |
| `alert_id` | `UUID` | no | FK đến `alerting.alerts.id` |
| `relation_type` | `VARCHAR(32)` | no | `TRIGGER`, `RELATED`, `EVIDENCE` |
| `created_at` | `TIMESTAMPTZ` | no | Default current timestamp |

Primary key:

```text
(incident_id, alert_id)
```

Index gợi ý:

- `(alert_id)`

## Bảng `incident.incident_evidence`

Lưu bằng chứng đã được chọn vào hồ sơ incident. Evidence có thể đến từ alert,
log, trace, health snapshot hoặc deployment metadata sau này.

| Column | Type | Null | Ghi chú |
| --- | --- | --- | --- |
| `id` | `UUID` | no | Primary key |
| `incident_id` | `UUID` | no | FK đến `incident.incidents.id` |
| `type` | `VARCHAR(32)` | no | `ALERT`, `LOG`, `TRACE`, `HEALTH`, `DEPLOYMENT` |
| `source_id` | `VARCHAR(128)` | yes | ID nguồn nếu có; ví dụ alert id, event id, trace id |
| `application_id` | `UUID` | yes | App liên quan đến evidence |
| `fingerprint` | `VARCHAR(128)` | yes | Fingerprint lỗi/log |
| `trace_id` | `VARCHAR(128)` | yes | Trace ID nếu có |
| `severity` | `VARCHAR(32)` | yes | Severity tại thời điểm evidence |
| `summary` | `TEXT` | no | Tóm tắt ngắn để hiển thị và đưa vào prompt |
| `sample_message` | `TEXT` | yes | Một message đại diện, đã sanitize |
| `occurred_at` | `TIMESTAMPTZ` | yes | Thời điểm evidence xảy ra |
| `metadata_json` | `TEXT` | yes | Chuỗi JSON chứa count, status code, host, source, attributes |
| `created_at` | `TIMESTAMPTZ` | no | Default current timestamp |

Ràng buộc gợi ý:

- `type IN ('ALERT', 'LOG', 'TRACE', 'HEALTH', 'DEPLOYMENT')`
- `summary` không blank

Index gợi ý:

- `(incident_id, type)`
- `(application_id, occurred_at DESC)`
- `(fingerprint)`
- `(trace_id)`
- Có thể đổi `metadata_json` sang `JSONB` và thêm GIN index nếu cần query sâu.

Ghi chú:

- Với log evidence từ ClickHouse `processed_logs`, `source_id` nên là
  `event_id`.
- Không lưu batch raw log lớn trong bảng này. Nếu cần nhiều log mẫu, lưu mỗi
  mẫu là một evidence riêng hoặc lưu thống kê trong `metadata_json`.

## Bảng `incident.incident_ai_analyses`

Lưu từng lần chạy AI analysis. Một incident có thể chạy lại AI nhiều lần khi có
thêm evidence.

| Column | Type | Null | Ghi chú |
| --- | --- | --- | --- |
| `id` | `UUID` | no | Primary key |
| `incident_id` | `UUID` | no | FK đến `incident.incidents.id` |
| `status` | `VARCHAR(32)` | no | `PENDING`, `RUNNING`, `SUCCEEDED`, `FAILED` |
| `provider` | `VARCHAR(64)` | yes | Ví dụ `openai`, `gemini`, `stub` |
| `model` | `VARCHAR(120)` | yes | Model đã dùng |
| `prompt_version` | `VARCHAR(32)` | no | Version prompt/schema output |
| `summary` | `TEXT` | yes | Tổng quan lỗi |
| `likely_cause` | `TEXT` | yes | Nguyên nhân nghi ngờ |
| `severity` | `VARCHAR(32)` | yes | Severity AI đề xuất |
| `severity_reason` | `TEXT` | yes | Lý do chọn severity |
| `confidence` | `VARCHAR(32)` | yes | `LOW`, `MEDIUM`, `HIGH` |
| `suggested_actions_json` | `TEXT` | yes | Chuỗi JSON chứa danh sách hành động đề xuất |
| `evidence_refs_json` | `TEXT` | yes | Chuỗi JSON chứa evidence id/source id AI dùng |
| `raw_response_json` | `TEXT` | yes | Chuỗi JSON chứa structured response từ provider |
| `error_message` | `TEXT` | yes | Lỗi khi gọi provider hoặc parse output |
| `requested_by` | `UUID` | yes | User yêu cầu chạy analysis |
| `started_at` | `TIMESTAMPTZ` | yes | Bắt đầu gọi AI |
| `completed_at` | `TIMESTAMPTZ` | yes | Hoàn tất gọi AI |
| `created_at` | `TIMESTAMPTZ` | no | Default current timestamp |

Ràng buộc gợi ý:

- `status IN ('PENDING', 'RUNNING', 'SUCCEEDED', 'FAILED')`
- `confidence IN ('LOW', 'MEDIUM', 'HIGH')` nếu không null
- `completed_at >= started_at` nếu cả hai không null

Index gợi ý:

- `(incident_id, created_at DESC)`
- `(status, created_at)`

Ghi chú:

- Không nên lưu API key hoặc secret provider trong bảng này.
- `raw_response_json` chỉ lưu structured response đã qua parse/sanitize, không
  lưu prompt đầy đủ nếu prompt có thể chứa dữ liệu nhạy cảm.

## Bảng `incident.incident_timeline_events`

Lưu timeline thao tác và thay đổi trạng thái để frontend hiển thị lịch sử điều
tra.

| Column | Type | Null | Ghi chú |
| --- | --- | --- | --- |
| `id` | `UUID` | no | Primary key |
| `incident_id` | `UUID` | no | FK đến `incident.incidents.id` |
| `event_type` | `VARCHAR(64)` | no | `CREATED`, `AI_STARTED`, `AI_COMPLETED`, `STATUS_CHANGED`, `EVIDENCE_ADDED`, `NOTE_ADDED` |
| `message` | `TEXT` | no | Nội dung timeline |
| `actor_user_id` | `UUID` | yes | User thực hiện; null nếu system |
| `metadata_json` | `TEXT` | yes | Chuỗi JSON chứa trạng thái cũ/mới, analysis id, evidence id |
| `created_at` | `TIMESTAMPTZ` | no | Default current timestamp |

Index gợi ý:

- `(incident_id, created_at ASC)`

## Nguồn dữ liệu ngoài module

### Alert evidence

Nguồn chính là `alerting.alerts`. Incident chỉ lưu liên kết trong
`incident_alerts` và snapshot/tóm tắt trong `incident_evidence` khi cần đưa vào
AI prompt.

### Log evidence

Nguồn chính là ClickHouse `processed_logs`.

Truy vấn thường dùng:

```text
application_id IN (...)
AND log_timestamp BETWEEN window_start AND window_end
AND level IN ('ERROR', 'CRITICAL')
```

Nên ưu tiên lấy:

- top fingerprints theo số lượng
- log mẫu mới nhất theo fingerprint
- trace IDs xuất hiện nhiều hoặc liên quan alert
- count theo level/application/fingerprint

### Identity và phân quyền

Mọi query incident list/detail phải lọc qua application mà user có quyền xem.
`incident_applications` là bảng chính để enforce access ở tầng service.

Gợi ý:

- `ENGINEER`: chỉ thấy incident có ít nhất một application được cấp quyền.
- `ADMIN`: có thể thấy toàn bộ incident, bao gồm `SYSTEM_WIDE`.

## Lifecycle dữ liệu

### Manual investigation

```text
1. User tạo incident với app/time window.
2. Insert incidents.
3. Insert incident_applications.
4. Thu thập alert/log evidence.
5. Insert incident_alerts và incident_evidence.
6. Insert incident_ai_analyses status=PENDING/RUNNING.
7. AI hoàn tất, update incident_ai_analyses status=SUCCEEDED.
8. Update last_evidence_collected_at theo window_end.
9. Ghi timeline events.
```

### Manual refresh

```text
1. User bấm Refresh trên incident detail.
2. Service lấy from = last_evidence_collected_at hoặc window_end nếu chưa có.
3. Service lấy to = now.
4. Thu thập thêm alert/log evidence trong khoảng from -> to.
5. Append evidence mới vào incident_evidence.
6. Update window_end = to và last_evidence_collected_at = to.
7. Tạo incident_ai_analyses mới và chạy AI analysis trên toàn bộ evidence hiện có.
8. Ghi timeline EVIDENCE_REFRESHED và AI_COMPLETED/AI_FAILED.
```

### Auto-trigger từ alert

```text
1. Alerting tạo hoặc reopen alert nghiêm trọng.
2. Incident service kiểm tra incident đang mở cùng app/fingerprint/window.
3. Nếu chưa có, tạo incident trigger_type=AUTO_ALERT.
4. Gắn alert vào incident_alerts với relation_type=TRIGGER.
5. Chạy evidence collection và AI analysis async.
```

### Resolve incident

```text
1. User resolve incident.
2. Update incidents.status=RESOLVED, resolved_by, resolved_at.
3. Ghi timeline STATUS_CHANGED.
4. Không tự resolve alert liên quan trừ khi workflow nghiệp vụ quy định riêng.
```

## Migration MVP gợi ý

Migration đầu tiên của module có thể là:

```text
V8__create_incident_schema.sql
```

Nên tạo theo thứ tự:

```text
CREATE SCHEMA incident;
CREATE TABLE incident.incidents;
CREATE TABLE incident.incident_applications;
CREATE TABLE incident.incident_alerts;
CREATE TABLE incident.incident_evidence;
CREATE TABLE incident.incident_ai_analyses;
CREATE TABLE incident.incident_timeline_events;
CREATE INDEX ...
```

## MVP tối thiểu

Nếu muốn triển khai nhanh, MVP có thể bắt đầu với bốn bảng:

```text
incident.incidents
incident.incident_applications
incident.incident_evidence
incident.incident_ai_analyses
```

Sau đó thêm:

```text
incident.incident_alerts
incident.incident_timeline_events
```

khi cần liên kết alert rõ ràng và hiển thị timeline điều tra.

## Những điểm chưa nên làm ngay

- Chưa cần lưu vector embedding cho incident trong MVP.
- Chưa cần bảng riêng cho prompt nếu chưa có yêu cầu audit prompt đầy đủ.
- Chưa cần copy toàn bộ log liên quan từ ClickHouse sang PostgreSQL.
- Chưa cần hard-code provider AI vào schema.
- Chưa cần auto-resolve alert khi incident resolved.

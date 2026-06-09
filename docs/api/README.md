# API Contract Overview

Tài liệu này mô tả contract mục tiêu của API dự án. Trạng thái triển khai thực
tế phải được kiểm tra từ Spring Controller và OpenAPI sinh tại
`docs/api/openapi.json`; không xem các endpoint dưới đây là đã tồn tại khi code
chưa cung cấp chúng.

## Log ingestion

Target namespace:

```text
POST /api/v1/logs
POST /api/v1/logs/batch
```

Canonical fields:

| Field | Required | Ghi chú |
| --- | --- | --- |
| `applicationName` | Có | Phải khớp application của API key |
| `level` | Có | `INFO`, `WARN`, `ERROR`, `CRITICAL` |
| `message` | Có | Giới hạn bởi request body và field validation |
| `timestamp` | Có | ISO 8601 có timezone |
| `traceId` | Không | Chuỗi correlation từ ứng dụng nguồn |
| `eventId` | Không | Nếu có phải hợp lệ; retry giữ nguyên logical ID |

Server sinh `event_id`, `ingestion_id`, `received_at` khi cần. API chỉ trả
accepted sau khi Kafka acknowledge việc publish vào `logs.raw`; request thread
không parse sâu và không ghi PostgreSQL/ClickHouse.

## MVP limits

| Hạng mục | Mặc định |
| --- | --- |
| Request body | Tối đa `1 MiB` |
| Batch | Tối đa `500` events |
| Rate limit | `1,000 events/second/application`, burst `2,000` |
| Timestamp | Từ `7 ngày trước` đến `5 phút trong tương lai` |
| Validation | Whole-batch |
| Kafka timeout | `3 giây` |
| Idempotency | Header `Idempotency-Key` và stable event IDs |

Các giá trị phải cấu hình được. Thay đổi baseline cần có load-test evidence và
phải đồng bộ OpenAPI, module requirements và storage requirements.

## Responses

| Trường hợp | HTTP |
| --- | --- |
| Kafka đã acknowledge toàn bộ request hợp lệ | `202 Accepted` |
| Request hoặc một event không hợp lệ | `400 Bad Request` |
| API key không hợp lệ | `401 Unauthorized` |
| Không có quyền với application | `403 Forbidden` |
| Vượt rate limit | `429 Too Many Requests` |
| Kafka không acknowledge/quá tải | `503 Service Unavailable` |

Với whole-batch validation, một event lỗi làm toàn bộ batch bị từ chối và không
event nào được publish. Error response chỉ ra field hoặc index lỗi nhưng không
phản chiếu secret hay toàn bộ raw message.

## Search and realtime

Target log search:

```text
GET /api/v1/logs
```

Query bắt buộc có `from`, `to` và `limit`. Có thể filter theo
`applicationId`, `level`, `traceId` và cursor. Backend kiểm tra quyền application
và không nhận raw SQL hoặc ClickHouse expression.

Target realtime transport:

```text
WebSocket endpoint: /ws
Subscribe: /topic/logs
Subscribe: /topic/alerts
Client filter message: /app/subscriptions/logs
```

Live filter:

```json
{
  "applicationIds": ["uuid"],
  "levels": ["ERROR", "CRITICAL"]
}
```

Client có thể cập nhật filter mà không reload trang. Backend loại application
không được phép xem và chỉ chấp nhận `INFO`, `WARN`, `ERROR`, `CRITICAL`.
Client chậm không được làm nghẽn Kafka consumer. Reconnect, buffer limit và
dropped-event behavior phải được chốt khi triển khai transport.

## Identity and alert administration

Target management contracts:

```text
POST /api/v1/auth/login
GET  /api/v1/applications
POST /api/v1/applications/{id}/access
GET  /api/v1/alert-rules
POST /api/v1/alert-rules
PUT  /api/v1/alert-rules/{id}
POST /api/v1/notification-channels/telegram
```

Engineer chỉ được xem log và subscription của application được cấp quyền.
Admin quản lý application access, alert threshold/window/dedup và Telegram
channel. Secret hoặc Telegram bot token không được trả lại qua API.

Các route trên là target contract định hướng. OpenAPI sinh từ controller vẫn
là nguồn xác nhận endpoint nào đã được triển khai.

# Log Monitoring API

## Tổng Quan

Tài liệu này mô tả REST API và WebSocket API của Log Monitoring System theo cấu trúc: endpoint, method, authentication, path/query/header parameters, request payload, success response, error response và sample call. Cấu trúc trình bày được tham khảo từ hướng dẫn API document của Riverlee (`https://riverlee.vn/blog/api-document`), còn nội dung contract được xác minh từ controller, DTO, security config và exception handler trong source code hiện tại.

Backend expose:

| Loại API | Base |
| --- | --- |
| REST API | `http://localhost:8080/api/v1` |
| Internal Prometheus API | `http://localhost:8080/internal/prometheus` |
| OpenAPI runtime | `http://localhost:8080/v3/api-docs` |
| Swagger UI | `http://localhost:8080/swagger-ui` |
| STOMP WebSocket | `ws://localhost:8080/ws` |

`docs/api/openapi.json` đang là artifact cũ: file này chỉ có identity/application paths và thiếu ingestion, dashboard, alerting, incident, anomaly, retention. Vì vậy tài liệu này ưu tiên source code hiện tại thay vì artifact đó.

## Quy Ước Tài Liệu

Các endpoint REST nghiệp vụ được ghi theo path tương đối dưới base `/api/v1`. Endpoint nội bộ và WebSocket giữ nguyên path đầy đủ để tránh nhầm base.

Mỗi nhóm endpoint dùng cùng một format để dễ tra cứu:

| Thành phần | Cách đọc trong tài liệu |
| --- | --- |
| Endpoint và method | Dòng như `GET /users` hoặc bảng index theo nhóm |
| Authentication | Public, JWT, API key, ADMIN hoặc điều kiện visibility theo application |
| Path/query/header parameters | Ghi riêng ở bảng params hoặc cột `Request/Params` |
| Request payload | Tên DTO và bảng field/validation khi endpoint có body |
| Success response | HTTP status + schema response |
| Error response | Status/error code đã xác minh từ handler hoặc module exception |
| Sample call | `curl` dùng giá trị mẫu an toàn, không chứa secret thật |

## Versioning

| Item | Value |
| --- | --- |
| API version prefix | `/api/v1` |
| Runtime OpenAPI path | `/v3/api-docs` |
| Swagger UI path | `/swagger-ui` |
| Default backend port | `8080` |
| Default response content type | `application/json` |

## Authentication

### JWT Bearer

Các endpoint nghiệp vụ của dashboard dùng JWT:

```http
Authorization: Bearer <accessToken>
Content-Type: application/json
```

`JwtAuthenticationFilter` đọc token từ `Authorization`, validate JWT, kiểm tra Redis blacklist nếu Redis khả dụng, và set Spring Security principal bằng email + role.

### API Key Ingestion

Log ingestion endpoint public ở Spring Security layer nhưng controller yêu cầu header:

```http
X-API-Key: <application-api-key>
Idempotency-Key: <optional-idempotency-key>
Content-Type: application/json
```

`Idempotency-Key` là optional, dùng để chống gửi trùng log/batch trong ingestion flow.

### Public/Internal

| Khu vực | Auth |
| --- | --- |
| `/api/v1/auth/**` | Public |
| `POST /api/v1/logs`, `POST /api/v1/logs/batch` | `X-API-Key` |
| `/internal/prometheus/**` | Public |
| `/api/v1/users/me`, `/api/v1/applications/me` | JWT |
| `/api/v1/users/**`, `/api/v1/applications/**` | `ROLE_ADMIN`, trừ các route `/me` đã match trước |
| Controller có `@PreAuthorize("hasRole('ADMIN')")` | `ROLE_ADMIN` |
| Các `/api/v1/**` còn lại | JWT |

## Response Envelope

Phần lớn REST endpoints trả về `ApiResponse<T>`:

| Field | Type | Mô tả |
| --- | --- | --- |
| `success` | boolean | `true` nếu request xử lý thành công |
| `message` | string | Message mặc định là `Success` hoặc message do controller/handler set |
| `data` | object/null | Payload theo từng endpoint |
| `timestamp` | instant | Thời điểm tạo response |

Ví dụ:

```json
{
  "success": true,
  "message": "Success",
  "data": {
    "id": "00000000-0000-0000-0000-000000000001"
  },
  "timestamp": "2026-07-03T00:00:00Z"
}
```

Ngoại lệ đã xác minh:

| Endpoint | Response |
| --- | --- |
| `GET /api/v1/dashboard/overview` | Trả `OverviewSnapshotDto` trực tiếp |
| `DELETE /api/v1/metric-sources/{applicationId}` | `204 No Content` |
| `GET /internal/prometheus/targets` | Trả `List<PrometheusTargetDto>` trực tiếp |

## Endpoint Index

### Authentication

| Method | Endpoint | Auth | Success |
| --- | --- | --- | --- |
| `POST` | `/auth/login` | Public | `200 ApiResponse<LoginResponse>` |
| `POST` | `/auth/refresh` | Public | `200 ApiResponse<LoginResponse>` |
| `POST` | `/auth/logout` | Public; optional bearer header | `200 ApiResponse<Void>` |
| `POST` | `/auth/register` | Public | `200 ApiResponse<UserResponse>` |

### Users

| Method | Endpoint | Auth | Success |
| --- | --- | --- | --- |
| `GET` | `/users/me` | JWT | `200 ApiResponse<UserResponse>` |
| `PUT` | `/users/me` | JWT | `200 ApiResponse<UserResponse>` |
| `PUT` | `/users/me/password` | JWT | `200 ApiResponse<Void>` |
| `POST` | `/users` | ADMIN | `200 ApiResponse<UserResponse>` |
| `GET` | `/users` | ADMIN | `200 ApiResponse<UserPageResponse>` |
| `GET` | `/users/{id}` | ADMIN | `200 ApiResponse<UserResponse>` |
| `PUT` | `/users/{id}` | ADMIN | `200 ApiResponse<UserResponse>` |
| `PUT` | `/users/{id}/role` | ADMIN | `200 ApiResponse<UserResponse>` |
| `PUT` | `/users/{id}/status` | ADMIN | `200 ApiResponse<UserResponse>` |
| `DELETE` | `/users/{id}` | ADMIN | `200 ApiResponse<UserResponse>` |

### Applications, Access, API Keys

| Method | Endpoint | Auth | Success |
| --- | --- | --- | --- |
| `POST` | `/applications` | ADMIN | `200 ApiResponse<ApplicationResponse>` |
| `GET` | `/applications` | ADMIN | `200 ApiResponse<List<ApplicationResponse>>` |
| `GET` | `/applications/me` | JWT | `200 ApiResponse<List<ApplicationResponse>>` |
| `GET` | `/applications/{id}` | ADMIN | `200 ApiResponse<ApplicationResponse>` |
| `PUT` | `/applications/{id}` | ADMIN | `200 ApiResponse<ApplicationResponse>` |
| `PUT` | `/applications/{id}/status` | ADMIN | `200 ApiResponse<ApplicationResponse>` |
| `GET` | `/users/{userId}/applications` | ADMIN | `200 ApiResponse<List<ApplicationAccessResponse>>` |
| `PUT` | `/users/{userId}/applications` | ADMIN | `200 ApiResponse<List<ApplicationAccessResponse>>` |
| `POST` | `/users/{userId}/applications/{applicationId}` | ADMIN | `200 ApiResponse<ApplicationAccessResponse>` |
| `DELETE` | `/users/{userId}/applications/{applicationId}` | ADMIN | `200 ApiResponse<Void>` |
| `POST` | `/applications/{applicationId}/api-keys` | ADMIN | `200 ApiResponse<ApiKeyCreationResponse>` |
| `GET` | `/applications/{applicationId}/api-keys` | ADMIN | `200 ApiResponse<List<ApiKeyResponse>>` |
| `POST` | `/applications/{applicationId}/api-keys/{apiKeyId}/rotate` | ADMIN | `200 ApiResponse<ApiKeyCreationResponse>` |
| `POST` | `/applications/{applicationId}/api-keys/{apiKeyId}/revoke` | ADMIN | `200 ApiResponse<Void>` |

### Metric Sources and Prometheus

| Method | Endpoint | Auth | Success |
| --- | --- | --- | --- |
| `GET` | `/applications/{applicationId}/metric-sources` | JWT | `200 ApiResponse<MetricSourceResponse>` hoặc `204` |
| `POST` | `/applications/{applicationId}/metric-sources` | JWT | `200 ApiResponse<MetricSourceResponse>` |
| `PUT` | `/metric-sources/{applicationId}` | JWT | `200 ApiResponse<MetricSourceResponse>` |
| `DELETE` | `/metric-sources/{applicationId}` | JWT | `204 No Content` |
| `POST` | `/metric-sources/test-connection` | JWT | `200 ApiResponse<Boolean>` |
| `GET` | `/internal/prometheus/targets` | Public | `200 List<PrometheusTargetDto>` |

### Logs, Dashboard, Alerting

| Method | Endpoint | Auth | Success |
| --- | --- | --- | --- |
| `POST` | `/logs` | API key | `202 ApiResponse<LogIngestionResponse>` |
| `POST` | `/logs/batch` | API key | `202 ApiResponse<BatchLogIngestionResponse>` |
| `GET` | `/dashboard/overview` | JWT | `200 OverviewSnapshotDto` |
| `POST` | `/alert-rules` | ADMIN | `200 ApiResponse<AlertRuleResponse>` |
| `GET` | `/alert-rules` | ADMIN | `200 ApiResponse<List<AlertRuleResponse>>` |
| `GET` | `/alert-rules/{id}` | ADMIN | `200 ApiResponse<AlertRuleResponse>` |
| `PUT` | `/alert-rules/{id}` | ADMIN | `200 ApiResponse<AlertRuleResponse>` |
| `PUT` | `/alert-rules/{id}/status` | ADMIN | `200 ApiResponse<AlertRuleResponse>` |
| `DELETE` | `/alert-rules/{id}` | ADMIN | `200 ApiResponse<Void>` |
| `GET` | `/alerts` | JWT | `200 ApiResponse<List<AlertResponse>>` |
| `PUT` | `/alerts/{id}/acknowledge` | JWT + visible application | `200 ApiResponse<AlertResponse>` |
| `PUT` | `/alerts/{id}/resolve` | JWT + visible application | `200 ApiResponse<AlertResponse>` |
| `POST` | `/alert-chat-rooms` | ADMIN | `200 ApiResponse<ChatRoomResponse>` |
| `GET` | `/alert-chat-rooms` | ADMIN | `200 ApiResponse<List<ChatRoomResponse>>` |
| `GET` | `/alert-chat-rooms/telegram/discover` | ADMIN | `200 ApiResponse<List<TelegramChatResponse>>` |
| `PUT` | `/alert-chat-rooms/{id}/status` | ADMIN | `200 ApiResponse<ChatRoomResponse>` |

### Incidents, Anomaly, Retention

| Method | Endpoint | Auth | Success |
| --- | --- | --- | --- |
| `GET` | `/incidents` | JWT | `200 ApiResponse<List<IncidentSummaryDto>>` |
| `GET` | `/incidents/{id}` | JWT + visible application | `200 ApiResponse<IncidentDto>` |
| `POST` | `/incidents/from-alert/{alertId}` | JWT + visible alert application | `200 ApiResponse<IncidentDto>` |
| `PUT` | `/incidents/{id}/resolve` | JWT + visible application | `200 ApiResponse<IncidentDto>` |
| `GET` | `/incidents/anomaly-reports` | JWT | `200 ApiResponse<List<IncidentAnomalyReportDto>>` |
| `GET` | `/incidents/anomaly-reports/{id}` | JWT | `200 ApiResponse<IncidentAnomalyReportDto>` |
| `GET` | `/anomaly/reports` | JWT | `200 ApiResponse<List<AnomalyReportDto>>` |
| `GET` | `/anomaly/reports/{id}` | JWT + visible application | `200 ApiResponse<AnomalyReportDto>` |
| `PUT` | `/anomaly/reports/{id}/resolve` | JWT + visible application | `200 ApiResponse<AnomalyReportDto>` |
| `GET` | `/retention/policies` | ADMIN | `200 ApiResponse<List<RetentionPolicyResponse>>` |
| `PUT` | `/retention/policies` | ADMIN | `200 ApiResponse<List<RetentionPolicyResponse>>` |
| `POST` | `/retention/policies/{id}/run` | ADMIN | `200 ApiResponse<RetentionRunResponse>` |

## Chi Tiết Endpoint Theo Nhóm

### Authentication API

#### `POST /auth/login`

**Mục đích:** Đăng nhập bằng email/password và nhận access token + refresh token.

**Authentication:** Public.

**Request body:** `LoginRequest`

| Field | Type | Required | Validation |
| --- | --- | --- | --- |
| `email` | string | yes | `@NotBlank`, `@Email` |
| `password` | string | yes | `@NotBlank` |

**Success response:** `200 ApiResponse<LoginResponse>`

`LoginResponse`: `accessToken`, `refreshToken`, `user`.

**Error responses:** `400` validation failed; `401 INVALID_CREDENTIALS`; `403 ACCOUNT_DISABLED` hoặc `ACCOUNT_LOCKED`; `500` unhandled error.

**Sample call:**

```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@example.local","password":"change-me"}'
```

#### `POST /auth/refresh`

**Mục đích:** Đổi refresh token lấy token pair mới.

**Authentication:** Public.

**Request body:** `RefreshTokenRequest`

| Field | Type | Required | Validation |
| --- | --- | --- | --- |
| `refreshToken` | string | yes | `@NotBlank` |

**Success response:** `200 ApiResponse<LoginResponse>`

**Error responses:** `400`, `401`, `403`, `500`.

#### `POST /auth/logout`

**Mục đích:** Logout refresh token và optional access token hiện tại.

**Authentication:** Public; nếu có `Authorization` header thì controller trích bearer token để logout.

**Headers:**

| Header | Required | Mô tả |
| --- | --- | --- |
| `Authorization` | no | Optional bearer access token |

**Request body:** `LogoutRequest`

| Field | Type | Required | Validation |
| --- | --- | --- | --- |
| `refreshToken` | string | yes | `@NotBlank` |

**Success response:** `200 ApiResponse<Void>` với message `Logged out successfully`.

#### `POST /auth/register`

**Mục đích:** Tạo user mới qua auth controller.

**Authentication:** Public theo `SecurityConfig`.

**Request body:** `CreateUserRequest`

| Field | Type | Required | Validation |
| --- | --- | --- | --- |
| `email` | string | yes | `@NotBlank`, `@Email`, max 320 |
| `password` | string | yes | min 6, max 100 |
| `displayName` | string | yes | max 150 |
| `role` | string | yes | service validate role |

**Success response:** `200 ApiResponse<UserResponse>`

**Error responses:** `400`, `409 EMAIL_ALREADY_EXISTS`, `500`.

### User API

#### Current User

| Endpoint | Mục đích | Request | Success |
| --- | --- | --- | --- |
| `GET /users/me` | Lấy profile user hiện tại | none | `ApiResponse<UserResponse>` |
| `PUT /users/me` | Cập nhật email/display name | `UpdateUserRequest` | `ApiResponse<UserResponse>` |
| `PUT /users/me/password` | Đổi password | `ChangePasswordRequest` | `ApiResponse<Void>` |

`UpdateUserRequest`:

| Field | Type | Required | Validation |
| --- | --- | --- | --- |
| `email` | string | yes | `@NotBlank`, `@Email`, max 320 |
| `displayName` | string | yes | `@NotBlank`, max 150 |

`ChangePasswordRequest`:

| Field | Type | Required | Validation |
| --- | --- | --- | --- |
| `oldPassword` | string | yes | `@NotBlank` |
| `newPassword` | string | yes | min 6, max 100 |

#### User Administration

| Endpoint | Mục đích | Params/Body |
| --- | --- | --- |
| `POST /users` | Tạo user | `CreateUserRequest` |
| `GET /users` | Liệt kê user có phân trang/lọc | query params bên dưới |
| `GET /users/{id}` | Lấy user theo UUID | path `id` |
| `PUT /users/{id}` | Cập nhật user | `UpdateUserRequest` |
| `PUT /users/{id}/role` | Đổi role | raw string body |
| `PUT /users/{id}/status` | Đổi status | raw string body |
| `DELETE /users/{id}` | Soft delete user | path `id` |

`GET /users` query parameters:

| Param | Type | Required | Default |
| --- | --- | --- | --- |
| `page` | int | no | `0` |
| `size` | int | no | `20` |
| `search` | string | no | none |
| `role` | string | no | none |
| `status` | string | no | none |
| `includeDeleted` | boolean | no | `false` |

### Application API

#### Applications

| Endpoint | Mục đích | Request |
| --- | --- | --- |
| `POST /applications` | Tạo application | `ApplicationRequest` |
| `GET /applications` | Liệt kê tất cả applications | none |
| `GET /applications/me` | Liệt kê applications user hiện tại được xem | none |
| `GET /applications/{id}` | Lấy application theo UUID | path `id` |
| `PUT /applications/{id}` | Cập nhật application | `ApplicationRequest` |
| `PUT /applications/{id}/status` | Đổi trạng thái application | `ApplicationStatusRequest` |

`ApplicationRequest`:

| Field | Type | Required | Validation |
| --- | --- | --- | --- |
| `name` | string | yes | max 100 |
| `displayName` | string | yes | max 150 |
| `description` | string | no | max 2000 |

`ApplicationStatusRequest`:

| Field | Type | Required |
| --- | --- | --- |
| `status` | string | yes |

#### Application Access

| Endpoint | Mục đích | Request |
| --- | --- | --- |
| `GET /users/{userId}/applications` | Lấy grants của user | none |
| `PUT /users/{userId}/applications` | Replace toàn bộ grants | `ReplaceApplicationAccessRequest` |
| `POST /users/{userId}/applications/{applicationId}` | Grant một application | `ApplicationAccessGrantRequest` |
| `DELETE /users/{userId}/applications/{applicationId}` | Gỡ grant | none |

`ReplaceApplicationAccessRequest`:

| Field | Type | Required |
| --- | --- | --- |
| `grants` | `ApplicationAccessGrantRequest[]` | yes |

`ApplicationAccessGrantRequest`:

| Field | Type | Required | Ghi chú |
| --- | --- | --- | --- |
| `applicationId` | UUID | required trong replace body; path param trong grant endpoint | Body field không được controller dùng ở `POST /users/{userId}/applications/{applicationId}` |
| `accessLevel` | string | yes | service validate access level |

#### API Keys

| Endpoint | Mục đích | Request |
| --- | --- | --- |
| `POST /applications/{applicationId}/api-keys` | Tạo API key | `CreateApiKeyRequest` |
| `GET /applications/{applicationId}/api-keys` | Liệt kê API keys | none |
| `POST /applications/{applicationId}/api-keys/{apiKeyId}/rotate` | Rotate key | none |
| `POST /applications/{applicationId}/api-keys/{apiKeyId}/revoke` | Revoke key | none |

`CreateApiKeyRequest`:

| Field | Type | Required | Validation |
| --- | --- | --- | --- |
| `name` | string | yes | max 100 |
| `expiresAt` | instant | no | none |

`ApiKeyCreationResponse` có `rawApiKey`; giá trị này chỉ xuất hiện trong response tạo/rotate.

### Metric Source API

| Endpoint | Mục đích | Request/Params |
| --- | --- | --- |
| `GET /applications/{applicationId}/metric-sources` | Lấy metric source của application | path `applicationId` |
| `POST /applications/{applicationId}/metric-sources` | Tạo/cập nhật metric source | `MetricSourceRequest` |
| `PUT /metric-sources/{applicationId}` | Cập nhật metric source | `MetricSourceRequest` |
| `DELETE /metric-sources/{applicationId}` | Xóa metric source | path `applicationId` |
| `POST /metric-sources/test-connection` | Test kết nối target | `MetricSourceRequest` |
| `GET /internal/prometheus/targets` | Trả target list cho Prometheus file_sd/http_sd style | none |

`MetricSourceRequest`:

| Field | Type | Required |
| --- | --- | --- |
| `targetHost` | string | yes |
| `targetPort` | integer | yes |
| `metricsPath` | string | yes |
| `scrapeInterval` | string | yes |
| `enabled` | boolean | yes |

### Log Ingestion API

#### `POST /logs`

**Mục đích:** Nhận một raw log, xác thực application API key, publish vào Kafka, trả `202 Accepted`.

**Authentication:** `X-API-Key`.

**Headers:**

| Header | Required | Mô tả |
| --- | --- | --- |
| `X-API-Key` | yes | Application API key |
| `Idempotency-Key` | no | Chống gửi trùng request |

**Request body:** `LogIngestionRequest`

| Field | Type | Required | Validation |
| --- | --- | --- | --- |
| `applicationName` | string | yes | max 100 |
| `rawLog` | string | yes | `@NotBlank` |

**Success response:** `202 ApiResponse<LogIngestionResponse>`

`LogIngestionResponse`: `eventId`, `ingestionId`, `applicationName`, `applicationDisplayName`, `receivedAt`.

**Error responses:** `400 INVALID_RAW_LOG`; `401 INVALID_API_KEY`; `403 API_KEY_NOT_ALLOWED_FOR_APPLICATION`; `409 IDEMPOTENCY_CONFLICT`; `503 INGESTION_UNAVAILABLE`.

**Sample call:**

```bash
curl -X POST http://localhost:8080/api/v1/logs \
  -H "Content-Type: application/json" \
  -H "X-API-Key: <application-api-key>" \
  -H "Idempotency-Key: demo-log-0001" \
  -d '{"applicationName":"payment-service","rawLog":"2026-07-03T00:00:00Z ERROR payment failed orderId=42"}'
```

#### `POST /logs/batch`

**Mục đích:** Nhận batch raw logs, publish cùng ingestion id.

**Authentication:** `X-API-Key`.

**Request body:** `BatchLogIngestionRequest`

| Field | Type | Required | Validation |
| --- | --- | --- | --- |
| `applicationName` | string | yes | max 100 |
| `rawLogs` | string[] | yes | not empty, max 500, từng item not blank |

**Success response:** `202 ApiResponse<BatchLogIngestionResponse>`

`BatchLogIngestionResponse`: `items`, `ingestedCount`, `ingestionId`, `applicationName`, `applicationDisplayName`, `receivedAt`.

### Dashboard API

#### `GET /dashboard/overview`

**Mục đích:** Lấy snapshot dashboard gồm metrics, log volume, recent critical alerts.

**Authentication:** JWT.

**Query parameters:**

| Param | Type | Required | Default |
| --- | --- | --- | --- |
| `window` | string | no | `24h` |

**Success response:** `200 OverviewSnapshotDto`

Response không bọc `ApiResponse`.

### Alerting API

#### Alert Rules

| Endpoint | Mục đích | Request/Params |
| --- | --- | --- |
| `POST /alert-rules` | Tạo alert rule | `AlertRuleRequest` |
| `GET /alert-rules` | Liệt kê rules | optional query `applicationId` |
| `GET /alert-rules/{id}` | Lấy rule theo UUID | path `id` |
| `PUT /alert-rules/{id}` | Cập nhật rule | `UpdateAlertRuleRequest` |
| `PUT /alert-rules/{id}/status` | Đổi trạng thái rule | `AlertRuleStatusRequest` |
| `DELETE /alert-rules/{id}` | Xóa rule | path `id` |

`AlertRuleRequest` và `UpdateAlertRuleRequest`:

| Field | Type | Required | Validation |
| --- | --- | --- | --- |
| `applicationId` | UUID | create only | required khi create |
| `name` | string | yes | max 120 |
| `description` | string | no | max 2000 |
| `minSeverity` | string | yes | service validate severity |
| `severity` | string | yes | service validate severity |
| `keywordPattern` | string | no | max 255 |
| `thresholdCount` | int | yes | min 1 |
| `thresholdWindowSeconds` | int | yes | min 1 |
| `cooldownSeconds` | int | yes | min 1 |
| `activeStartTime` | string | no | parsed by service |
| `activeEndTime` | string | no | parsed by service |
| `channels` | string[] | no | service validate channel |
| `deliveryTargets` | `AlertDeliveryTargetRequest[]` | no | nested validation |

`AlertDeliveryTargetRequest`: `channel` required, optional `chatRoomId`.

`AlertRuleStatusRequest`: required `status`.

#### Alerts

| Endpoint | Mục đích | Request/Params |
| --- | --- | --- |
| `GET /alerts` | Liệt kê alerts user được xem | optional `applicationId`, `status`, `severity` |
| `PUT /alerts/{id}/acknowledge` | Acknowledge alert | path `id` |
| `PUT /alerts/{id}/resolve` | Resolve alert | path `id` |

Controller kiểm tra application visibility trước khi acknowledge/resolve.

#### Alert Chat Rooms

| Endpoint | Mục đích | Request/Params |
| --- | --- | --- |
| `POST /alert-chat-rooms` | Tạo chat room | `CreateChatRoomRequest` |
| `GET /alert-chat-rooms` | Liệt kê chat rooms | optional `channel`, `status` |
| `GET /alert-chat-rooms/telegram/discover` | Discover Telegram chats | none |
| `PUT /alert-chat-rooms/{id}/status` | Đổi trạng thái chat room | `ChatRoomStatusRequest` |

`CreateChatRoomRequest`:

| Field | Type | Required | Validation |
| --- | --- | --- | --- |
| `channel` | string | yes | `@NotBlank` |
| `name` | string | yes | max 120 |
| `chatId` | string | yes | max 128 |
| `description` | string | no | max 2000 |

### Incident API

| Endpoint | Mục đích | Request/Params |
| --- | --- | --- |
| `GET /incidents` | Liệt kê incidents user được xem | optional `applicationId`, `status`, `severity` |
| `GET /incidents/{id}` | Lấy incident detail | path `id` |
| `POST /incidents/from-alert/{alertId}` | Tạo incident từ alert | path `alertId` |
| `PUT /incidents/{id}/resolve` | Resolve incident | path `id` |
| `GET /incidents/anomaly-reports` | Liệt kê anomaly reports theo incident facade | none |
| `GET /incidents/anomaly-reports/{id}` | Lấy anomaly report theo incident facade | path `id` |

Các endpoint incident dùng `ApplicationAccessFacade` để giới hạn dữ liệu theo application user được xem.

### Anomaly API

| Endpoint | Mục đích | Request/Params |
| --- | --- | --- |
| `GET /anomaly/reports` | Liệt kê anomaly reports user được xem | optional `applicationId` |
| `GET /anomaly/reports/{id}` | Lấy anomaly report detail | path `id` |
| `PUT /anomaly/reports/{id}/resolve` | Resolve anomaly report; nếu report có linked alert thì resolve alert tương ứng | path `id` |

### Retention API

| Endpoint | Mục đích | Request/Params |
| --- | --- | --- |
| `GET /retention/policies` | Liệt kê retention policies | none |
| `PUT /retention/policies` | Cập nhật policies | `RetentionPolicyUpdateRequest[]` |
| `POST /retention/policies/{id}/run` | Chạy policy ngay | path `id` |

`RetentionPolicyUpdateRequest`:

| Field | Type | Required | Validation |
| --- | --- | --- | --- |
| `id` | UUID | yes | `@NotNull` |
| `retentionDays` | int | yes | min 1, max 730 |
| `enabled` | boolean | yes | none |

## Response Schemas Chính

### Identity

| Schema | Fields |
| --- | --- |
| `UserResponse` | `id`, `email`, `displayName`, `role`, `status`, `lastLoginAt`, `createdAt`, `updatedAt` |
| `UserPageResponse` | `users`, `page`, `size`, `totalElements`, `totalPages` |
| `ApplicationResponse` | `id`, `name`, `displayName`, `description`, `status`, `createdAt`, `updatedAt` |
| `ApplicationAccessResponse` | `userId`, `applicationId`, `applicationName`, `applicationDisplayName`, `accessLevel`, `grantedBy`, `createdAt`, `updatedAt` |
| `ApiKeyResponse` | `id`, `applicationId`, `name`, `keyPrefix`, `status`, `expiresAt`, `lastUsedAt`, `createdAt`, `revokedAt` |
| `ApiKeyCreationResponse` | `id`, `applicationId`, `name`, `keyPrefix`, `rawApiKey`, `status`, `expiresAt`, `createdAt` |

### Log Ingestion

| Schema | Fields |
| --- | --- |
| `LogIngestionResponse` | `eventId`, `ingestionId`, `applicationName`, `applicationDisplayName`, `receivedAt` |
| `BatchLogIngestionResponse` | `items`, `ingestedCount`, `ingestionId`, `applicationName`, `applicationDisplayName`, `receivedAt` |
| `BatchLogIngestionResponse.IngestedLogItem` | `eventId`, `rawLog` |

### Alerting

| Schema | Fields |
| --- | --- |
| `AlertRuleResponse` | `id`, `applicationId`, `name`, `description`, `minSeverity`, `severity`, `keywordPattern`, `thresholdCount`, `thresholdWindowSeconds`, `cooldownSeconds`, `activeStartTime`, `activeEndTime`, `status`, `channels`, `deliveryTargets`, `createdBy`, `createdAt`, `updatedAt` |
| `AlertResponse` | `id`, `ruleId`, `ruleName`, `applicationId`, `applicationName`, `applicationDisplayName`, `severity`, `triggerType`, `sourceType`, `sourceId`, `summary`, `metadataJson`, `logSamples`, `triggeredAt`, `occurrenceCount`, `firstSeenAt`, `lastSeenAt`, `status`, `dispatchedChannels`, `deliveryTargets`, `acknowledgedBy`, `acknowledgedAt`, `resolvedBy`, `resolvedAt`, `createdAt`, `updatedAt` |
| `ChatRoomResponse` | `id`, `channel`, `name`, `chatId`, `description`, `status`, `createdBy`, `createdAt`, `updatedAt` |
| `TelegramChatResponse` | `chatId`, `name`, `type`, `username` |

### Dashboard, Incident, Anomaly, Retention

| Schema | Fields |
| --- | --- |
| `OverviewSnapshotDto` | `window`, `generatedAt`, `metrics`, `volume`, `criticalAlerts` |
| `OverviewMetricDto` | `id`, `label`, `value`, `trend`, `helper`, `tone` |
| `LogVolumePointDto` | `time`, `INFO`, `WARN`, `ERROR`, `CRITICAL` |
| `CriticalAlertSummaryDto` | `id`, `severity`, `application`, `logSamples`, `occurrences`, `lastSeen`, `deliveryState` |
| `IncidentSummaryDto` | `id`, `title`, `description`, `shortSummary`, `impact`, `status`, `severity`, `scope`, `triggerType`, `startedAt`, `windowStart`, `windowEnd`, `lastEvidenceCollectedAt`, `applicationIds`, `createdBy`, `resolvedBy`, `resolvedAt`, `createdAt`, `updatedAt` |
| `IncidentDto` | `id`, `title`, `description`, `shortSummary`, `impact`, `possibleCause`, `recommendedActions`, `status`, `severity`, `scope`, `triggerType`, `startedAt`, `windowStart`, `windowEnd`, `lastEvidenceCollectedAt`, `applications`, `evidence`, `timeline`, `createdBy`, `resolvedBy`, `resolvedAt`, `createdAt`, `updatedAt` |
| `IncidentAnomalyReportDto` | `id`, `alertId`, `status`, `evidencePayload`, `createdAt`, `updatedAt` |
| `AnomalyReportDto` | `id`, `applicationId`, `alertId`, `sourceType`, `ruleName`, `fingerprint`, `severity`, `status`, `title`, `summary`, `hypothesis`, `confidenceScore`, `windowStart`, `windowEnd`, `occurrenceCount`, `firstSeenAt`, `lastSeenAt`, `evidencePayloadJson`, `aiTriggerRequested`, `aiTriggerReason`, `aiStatus`, `aiStartedAt`, `aiCompletedAt`, `aiResultJson`, `aiError`, `resolvedBy`, `resolvedAt`, `createdAt`, `updatedAt` |
| `RetentionPolicyResponse` | `id`, `logLevel`, `label`, `description`, `retentionDays`, `minDays`, `maxDays`, `enabled`, `nextRunAt`, `recentOperation` |
| `RetentionRunResponse` | `id`, `policyId`, `status`, `startedAt`, `finishedAt`, `affectedRows`, `message` |

## WebSocket API

| Item | Value |
| --- | --- |
| Endpoint | `/ws` |
| Protocol | STOMP over WebSocket |
| Broker prefix | `/topic` |
| Application destination prefix | `/app` |
| CONNECT auth | `Authorization: Bearer <accessToken>` |

Destinations:

| Destination | Payload | Access |
| --- | --- | --- |
| `/topic/applications/{applicationId}/logs` | `LiveLogMessage` | JWT + `canViewApplication` |
| `/topic/applications/{applicationId}/alerts` | `AlertNotificationMessage` | JWT + `canViewApplication` |
| `/topic/applications/{applicationId}/anomaly-reports` | `AnomalyReportNotificationMessage` | JWT CONNECT required; current interceptor pattern does not enforce application check for this suffix |
| `/topic/incidents` | `IncidentNotificationMessage` | JWT CONNECT required |

## Error Contract

### Validation Error

`GlobalExceptionHandler` maps `MethodArgumentNotValidException` to `400`:

```json
{
  "success": false,
  "message": "Validation failed",
  "data": {
    "fieldName": "Validation message"
  },
  "timestamp": "2026-07-03T00:00:00Z"
}
```

### JWT Filter Errors

JWT errors are returned as `401` with error code in `data`:

| Case | Message | Data |
| --- | --- | --- |
| Expired token | `JWT token has expired` | `JWT_EXPIRED` |
| Invalid token | `Invalid JWT token` | `INVALID_JWT` |
| Revoked token | `Access token has been revoked` | `ACCESS_TOKEN_REVOKED` |
| Other auth failure | `JWT authentication failed` | `JWT_AUTHENTICATION_FAILED` |

### Module Error Codes

| Module | HTTP statuses from code | Error codes |
| --- | --- | --- |
| Identity | `400`, `401`, `403`, `404`, `409` | `USER_NOT_FOUND`, `EMAIL_ALREADY_EXISTS`, `INVALID_CREDENTIALS`, `ACCOUNT_DISABLED`, `ACCOUNT_LOCKED`, `UNAUTHORIZED`, `APPLICATION_NOT_FOUND`, `APPLICATION_NAME_ALREADY_EXISTS`, `APPLICATION_INACTIVE`, `APPLICATION_ACCESS_NOT_FOUND`, `INVALID_APPLICATION_STATUS`, `INVALID_APPLICATION_ACCESS_LEVEL`, `INVALID_APPLICATION_ACCESS_GRANT`, `API_KEY_NOT_FOUND`, `INVALID_API_KEY`, `API_KEY_REVOKED`, `API_KEY_EXPIRED` |
| Ingestion | `400`, `401`, `403`, `409`, `503` | `INVALID_API_KEY`, `API_KEY_NOT_ALLOWED_FOR_APPLICATION`, `INVALID_RAW_LOG`, `INVALID_BATCH`, `IDEMPOTENCY_CONFLICT`, `INGESTION_UNAVAILABLE` |
| Alerting | `400`, `404`, `409`, `502`, `503` | `ALERT_RULE_NOT_FOUND`, `ALERT_NOT_FOUND`, `ALERT_RULE_NAME_ALREADY_EXISTS`, `CHAT_ROOM_NOT_FOUND`, `CHAT_ROOM_ALREADY_EXISTS`, `INVALID_ALERT_RULE`, `INVALID_ALERT_SEVERITY`, `INVALID_ALERT_CHANNEL`, `INVALID_CHAT_ROOM`, `INVALID_ALERT_STATUS`, `TELEGRAM_NOT_CONFIGURED`, `TELEGRAM_DISCOVERY_FAILED` |
| Anomaly | `400`, `404`, `500` | `ANOMALY_RULE_NOT_FOUND`, `ANOMALY_REPORT_NOT_FOUND`, `INVALID_ANOMALY_CONFIGURATION`, `ANOMALY_PROCESSING_FAILED`, `METRIC_COLLECTION_FAILED` |
| Incident | `400`, `404`, `502` | `INCIDENT_NOT_FOUND`, `INVALID_INCIDENT_REQUEST`, `INVALID_INCIDENT_STATUS`, `INCIDENT_ANALYSIS_FAILED` |
| Retention | `400`, `404`, `502` | `POLICY_NOT_FOUND`, `INVALID_POLICY`, `RETENTION_DELETE_FAILED` |

Module exception handlers return:

```json
{
  "success": false,
  "message": "Error message from exception",
  "data": "ERROR_CODE",
  "timestamp": "2026-07-03T00:00:00Z"
}
```

Unhandled exceptions are mapped to `500 ApiResponse.error(ex.getMessage())`.

## Verification Notes

- Runtime OpenAPI fetch from `localhost:8080/v3/api-docs` was not available during documentation because the backend was not running.
- Checked-in `docs/api/openapi.json` is stale relative to source controllers.
- Endpoint paths, methods, headers, request fields and status codes in this document were verified from Spring controllers, DTO validation annotations, `SecurityConfig`, `JwtAuthenticationFilter`, WebSocket config/interceptor, and module exception classes.

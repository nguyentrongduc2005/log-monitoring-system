# Incident module

## Mục tiêu module

`incident` là module dành cho hồ sơ điều tra lỗi của hệ thống hoặc ứng dụng.
Module này không thay thế `alerting`, `processing` hay log search.

Định nghĩa ngắn gọn:

```text
Alert = tín hiệu cảnh báo.
Log Search = công cụ tra log.
Incident = hồ sơ điều tra hiện trạng lỗi, có AI tổng hợp và phân tích.
```

Incident là nơi AI tổng hợp alert, log và trace để giúp người dùng hiểu:

- lỗi đang xảy ra là gì
- mức độ nghiêm trọng
- phạm vi ảnh hưởng
- nguyên nhân nghi ngờ
- bằng chứng từ alert/log/trace
- hướng xử lý đề xuất

## Vai trò trong hệ thống

Flow tổng quát:

```text
Logs -> Rules -> Alerts -> Notification
                         -> Incident investigation
```

Khi hệ thống phát hiện lỗi, `alerting` vẫn là module tạo alert và gửi thông báo.
Người dùng sau đó có thể vào trang Incident để xem hoặc mở một cuộc điều tra.
Nếu tình trạng lỗi đủ nặng, hệ thống cũng có thể tự bật incident và chạy AI
investigation.

Incident không nên được hiểu là `1 alert = 1 incident`. Một incident có thể gom
nhiều alert, nhiều log và nhiều trace liên quan trong cùng một khoảng thời gian.

Ví dụ:

```text
Incident: Checkout payment degradation

Related alerts:
- Checkout API error rate high
- Payment gateway timeout
- Circuit breaker opened

Related logs/traces:
- traceId=trc-pay-8842
- fingerprint=PAYMENT_GATEWAY_TIMEOUT
- statusCode=504

Affected apps:
- Checkout API
- Billing Worker, nếu bị ảnh hưởng dây chuyền
```

## Khi nào tạo incident

### Manual investigation

Người dùng chủ động mở một cuộc điều tra khi thấy hệ thống có dấu hiệu bất
thường.

Ví dụ:

```text
User nhận alert -> vào Incident -> Start investigation
```

AI sẽ lấy dữ liệu trong phạm vi người dùng có quyền xem, gồm alert, log, trace
và trạng thái sức khỏe app trong time window được chọn.

### Auto-triggered incident

Hệ thống tự tạo incident khi tình trạng lỗi nghiêm trọng.

Điều kiện gợi ý cho MVP hoặc sau MVP:

- `CRITICAL` logs vượt ngưỡng trong 5-15 phút
- `ERROR` logs vượt ngưỡng trong 5-15 phút
- error rate của app vượt ngưỡng
- nhiều alert `CRITICAL` cùng application/fingerprint
- nhiều app cùng bị lỗi trong một time window
- pipeline health bất thường làm ảnh hưởng khả năng xử lý log

Khi auto-trigger, incident nên được tạo ở trạng thái `INVESTIGATING` và AI
analysis được chạy tự động.

## Phân quyền

Incident phải tôn trọng quyền truy cập application.

- `ENGINEER`: chỉ được thấy, tạo và điều tra incident trong phạm vi các app mà
  người đó quản lý hoặc được cấp quyền xem.
- `ADMIN`: được thấy, tạo và điều tra incident trên toàn bộ app, bao gồm
  incident cross-app hoặc system-wide.

AI investigation cũng phải dùng cùng phạm vi quyền:

- Với `ENGINEER`, AI chỉ được đọc alert/log/trace thuộc các app được cấp quyền.
- Với `ADMIN`, AI có thể tổng hợp dữ liệu toàn hệ thống.

## Dữ liệu AI cần tổng hợp

Khi chạy investigation, AI nên nhận các nhóm dữ liệu sau:

- Alert liên quan trong time window
- Log `ERROR` và `CRITICAL`
- Trace ID liên quan
- Fingerprint/message pattern
- Affected applications
- Error rate hoặc health snapshot tại thời điểm lỗi
- Recent deployment metadata, nếu sau này có
- Pipeline health, nếu sau này có Kafka/Redis/actuator metrics

## Nội dung AI investigation

AI output nên có cấu trúc ổn định để frontend hiển thị rõ ràng:

```text
AI Investigation

Summary:
Checkout API is experiencing payment gateway timeouts.

Severity:
SEV2 - Major degradation

Reason:
Error rate increased from 3.4% to 5.9% within 15 minutes.
Critical logs appeared several times in the latest window.
The issue affects payment authorization, but the whole system is not down.

Likely cause:
External payment provider timeout or unstable network between Checkout API and
the provider.

Evidence:
- PAYMENT_GATEWAY_TIMEOUT
- Circuit breaker opened for provider=stripe
- traceId=trc-pay-8842
- statusCode=504

Suggested actions:
- Check payment provider status
- Inspect traces with traceId=trc-pay-8842
- Review recent Checkout API deployment
- Monitor retry count and circuit breaker state
```

## UI/UX dự kiến

Trang Incident nên có:

- danh sách incident đang mở
- nút `Start investigation`
- bộ lọc theo status, severity, scope, affected app
- incident detail
- AI investigation summary
- likely root cause
- severity reasoning
- evidence alerts
- evidence logs
- trace IDs
- suggested actions
- investigation timeline

Các trạng thái gợi ý:

```text
INVESTIGATING
MITIGATED
RESOLVED
```

Severity gợi ý:

```text
SEV1 - Critical outage
SEV2 - Major degradation
SEV3 - Minor degradation
```

Scope gợi ý:

```text
APPLICATION
MULTI_APPLICATION
SYSTEM_WIDE
```

## Ranh giới module

Cấu trúc module dự kiến:

```text
modules/incident
├── api
│   └── public facade, commands, DTOs, module events
├── internal
│   ├── incident aggregate/service/repository
│   ├── investigation orchestration
│   ├── AI analysis adapter
│   └── evidence collection
└── README.md
```

Code bên ngoài `modules.incident` chỉ nên phụ thuộc vào package
`modules.incident.api`. Không import trực tiếp `modules.incident.internal`.

## Quan hệ với module khác

`incident` dự kiến sẽ đọc hoặc gọi qua facade/public contract của các module:

- `alerting`: lấy alert liên quan và nhận tín hiệu alert nghiêm trọng.
- `processing` hoặc query/log-search module sau này: lấy log, trace, fingerprint.
- `identity`: kiểm tra quyền application theo user.
- `realtime`: publish notification khi incident được tạo/cập nhật, nếu cần.

Module này chưa implement business logic trong MVP hiện tại. README này ghi lại
quyết định thiết kế để dùng khi bắt đầu implement Incident sau này.

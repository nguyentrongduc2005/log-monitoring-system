# Alert Rule Daily Time Window Plan

## 1. Bối cảnh

Alert rule hiện tại đã có các thông tin chính:

```text
applicationId
name
description
minSeverity
severity
keywordPattern
thresholdCount
thresholdWindowSeconds
cooldownSeconds
status
deliveryTargets
```

Alerting cũng đã có cache active rule trong Redis:

```text
key   = alerting:rules:active:{applicationId}
value = JSON list AlertRuleDefinition
ttl   = app.alerting.rule-cache.ttl
```

Vấn đề chính:

- Processing hiện chủ yếu gửi alert candidate cho `ERROR` và `CRITICAL`.
- Rule custom cho `WARN` hoặc `INFO` ít hiệu quả vì alerting không nhận đủ candidate.
- Không cần mở rộng rule quá phức tạp như `matchMode`, nhiều time window, day-of-week, timezone, override threshold/cooldown/severity.
- Chỉ cần thêm **khung giờ trong ngày** để rule chỉ hoạt động trong khoảng giờ đó, hoặc hoạt động cả ngày.

## 2. Mục tiêu MVP

Thêm 2 field vào rule:

```text
activeStartTime
activeEndTime
```

Ý nghĩa:

```text
activeStartTime = null
activeEndTime   = null
=> rule hoạt động cả ngày như hiện tại.

activeStartTime = 00:00
activeEndTime   = 06:00
=> rule chỉ hoạt động từ 00:00 đến 06:00 mỗi ngày.
```

Không làm trong MVP:

- Không thêm ngày trong tuần.
- Không thêm timezone riêng cho từng rule.
- Không thêm enabled riêng cho time window.
- Không thêm threshold override.
- Không thêm cooldown override.
- Không thêm severity override.
- Không thêm `matchMode`.
- Không thêm regex.
- Không tạo nhiều time window trong một rule.

## 3. Thiết kế hành vi

Mỗi rule có một khung giờ tùy chọn:

```text
activeStartTime
activeEndTime
```

Khi alerting evaluate rule:

```text
1. Nhận log candidate.
2. Kiểm tra rule có đang ACTIVE không.
3. Kiểm tra thời điểm hiện tại hoặc logTimestamp có nằm trong khung giờ rule không.
4. Nếu nằm trong khung giờ:
     evaluate rule như hiện tại.
5. Nếu không nằm trong khung giờ:
     bỏ qua rule.
```

Nếu rule không cấu hình khung giờ:

```text
activeStartTime = null
activeEndTime = null
```

thì rule match cả ngày, giữ nguyên hành vi cũ.

## 4. Ví dụ sử dụng

### Rule chạy cả ngày

```text
activeStartTime = null
activeEndTime = null
```

Rule chạy 24/7.

### Rule chỉ chạy ban đêm

```text
activeStartTime = 00:00
activeEndTime = 06:00
```

Rule chỉ hoạt động từ nửa đêm đến 6 giờ sáng.

### Rule chạy ngoài giờ hành chính

Cho phép khung giờ qua ngày:

```text
activeStartTime = 18:00
activeEndTime = 08:00
```

Ý nghĩa:

```text
18:00 -> 23:59
00:00 -> 08:00
```

## 5. Database

Thêm 2 cột vào bảng `alerting.alert_rules`:

```sql
ALTER TABLE alerting.alert_rules
    ADD COLUMN active_start_time TIME,
    ADD COLUMN active_end_time TIME;
```

Không cần tạo bảng mới.

Validation ở backend:

- Nếu cả 2 field đều null: hợp lệ, rule chạy cả ngày.
- Nếu cấu hình khung giờ thì cả 2 field phải có giá trị.
- `activeStartTime == activeEndTime` không nên cho phép, vì dễ gây mơ hồ.
- Cho phép khung giờ qua ngày, ví dụ `18:00 -> 08:00`.

## 6. API Contract

Mở rộng create/update rule request:

```json
{
  "applicationId": "...",
  "name": "Night payment warnings",
  "description": "Only active at night",
  "minSeverity": "WARN",
  "severity": "HIGH",
  "keywordPattern": "payment_failed",
  "thresholdCount": 3,
  "thresholdWindowSeconds": 300,
  "cooldownSeconds": 600,
  "activeStartTime": "00:00",
  "activeEndTime": "06:00",
  "deliveryTargets": []
}
```

Rule chạy cả ngày:

```json
{
  "activeStartTime": null,
  "activeEndTime": null
}
```

Response rule trả thêm:

```json
{
  "activeStartTime": "00:00",
  "activeEndTime": "06:00"
}
```

## 7. Redis Cache

Giữ nguyên key Redis hiện tại:

```text
alerting:rules:active:{applicationId}
```

Chỉ cần thêm 2 field vào `AlertRuleDefinition`:

```text
activeStartTime
activeEndTime
```

Processing có thể đọc cache này để lọc nhẹ:

```text
ERROR/CRITICAL:
  vẫn publish như hiện tại.

WARN/INFO:
  đọc active rules trong Redis.
  nếu có rule phù hợp minSeverity + keywordPattern + active time window:
    publish alert candidate.
```

Nếu Redis lỗi hoặc cache miss:

```text
fallback về cơ chế cũ, chỉ publish ERROR/CRITICAL
```

## 8. Time Window Matching

Tạo helper chung:

```text
AlertRuleTimeWindowMatcher
```

Input:

```text
activeStartTime
activeEndTime
timestamp
```

Output:

```text
true / false
```

Logic:

```text
Nếu start/end đều null:
  true

Nếu nowTime nằm trong khoảng start <= end:
  start <= nowTime < end

Nếu window qua ngày start > end:
  nowTime >= start OR nowTime < end
```

Ví dụ:

```text
00:00 -> 06:00
03:00 => true
10:00 => false

18:00 -> 08:00
20:00 => true
02:00 => true
12:00 => false
```

## 9. Alert Evaluation

`AlertRuleMatcher` hiện đang match:

```text
severity >= minSeverity
message contains keywordPattern
```

Mở rộng thành:

```text
severity >= minSeverity
message contains keywordPattern
timestamp nằm trong activeStartTime/activeEndTime
```

Pseudo:

```java
boolean matches(AlertRuleDefinition rule, AlertSeverity severity, String message, Instant timestamp) {
    return severity.ordinal() >= rule.minSeverity().ordinal()
        && matchesKeyword(rule.keywordPattern(), message)
        && timeWindowMatcher.matches(rule.activeStartTime(), rule.activeEndTime(), timestamp);
}
```

Nên dùng `logTimestamp` để so sánh, vì đó là thời điểm log xảy ra. Nếu muốn đơn giản hơn cho MVP, có thể dùng `Instant.now()`.

## 10. Frontend UI

Trong `AlertRuleBuilder`, thêm section nhỏ:

```text
Active Time Window
```

Control đề xuất:

```text
[ ] Active all day

Start time: HH:mm
End time: HH:mm
```

Default:

```text
Active all day = true
activeStartTime = null
activeEndTime = null
```

Khi user bỏ chọn `Active all day`:

```text
Start time required
End time required
```

Không cần UI chọn timezone, không cần chọn thứ trong tuần.

## 11. Implementation Steps

### Step 1: Database

- Thêm migration add `active_start_time`, `active_end_time` vào `alerting.alert_rules`.

### Step 2: Backend model

- Thêm field vào `AlertRuleEntity`.
- Thêm field vào `AlertRuleDefinition`.
- Thêm field vào `AlertRuleRequest`, `UpdateAlertRuleRequest`, `AlertRuleResponse`.
- Update create/update rule.

### Step 3: Validation

- Cả 2 null: hợp lệ.
- Cả 2 có giá trị: hợp lệ nếu khác nhau.
- Một null một có giá trị: invalid.
- `start == end`: invalid.

### Step 4: Matching

- Thêm `AlertRuleTimeWindowMatcher`.
- Update `AlertRuleMatcher` dùng matcher này.
- Update alert evaluation để truyền `logTimestamp`.

### Step 5: Processing pre-filter

- Processing đọc Redis active rule cache theo `applicationId`.
- Với WARN/INFO, chỉ publish nếu có active rule match nhẹ theo:
  - `minSeverity`
  - `keywordPattern`
  - `activeStartTime/activeEndTime`
- Redis lỗi/cache miss thì fallback cũ.

### Step 6: Frontend

- Thêm checkbox `Active all day`.
- Thêm input time start/end.
- Update create/edit payload.
- Update rule detail/list nếu cần hiển thị khung giờ.

### Step 7: Tests

Backend:

- Rule không có time window vẫn match như cũ.
- Rule `00:00 -> 06:00` match lúc `03:00`, không match lúc `10:00`.
- Rule `18:00 -> 08:00` match lúc `20:00` và `02:00`, không match lúc `12:00`.
- Invalid nếu chỉ có start hoặc chỉ có end.
- Invalid nếu start bằng end.
- Processing fallback ERROR/CRITICAL khi Redis lỗi.

Frontend:

- Default active all day.
- Bỏ active all day thì hiện start/end time.
- Submit payload null khi active all day.
- Submit payload HH:mm khi dùng khung giờ.

## 12. Acceptance Criteria

- Existing rules không bị ảnh hưởng, vì default là cả ngày.
- User có thể cấu hình một khung giờ trong ngày cho rule.
- Rule ngoài khung giờ không tạo alert.
- Rule trong khung giờ hoạt động như logic hiện tại.
- Processing có thể dùng cùng Redis active rule cache để publish thêm WARN/INFO candidate.
- Không thêm UI/rule logic phức tạp ngoài khung giờ.

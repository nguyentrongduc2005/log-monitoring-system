# Kế hoạch triển khai Module Phát hiện Bất thường và Cảnh báo sớm bằng Log + Metric

## 1. Mục tiêu cải tiến

Mục tiêu của cải tiến là bổ sung khả năng phát hiện bất thường sớm cho hệ thống Log Monitoring hiện tại.

Hệ thống không chỉ cảnh báo khi xuất hiện log lỗi `ERROR` hoặc `CRITICAL`, mà còn có thể nhận biết các dấu hiệu bất thường từ log và metric trước khi sự cố nghiêm trọng xảy ra.

Các mục tiêu chính:

- Phát hiện sớm dấu hiệu bất thường từ log.
- Phát hiện sớm bất thường từ metric hệ thống.
- Kết hợp log và metric để tính điểm rủi ro.
- Tự động tạo cảnh báo khi điểm rủi ro vượt ngưỡng.
- Tự động gọi AI phân tích khi cảnh báo nghiêm trọng hoặc lặp lại nhiều lần.
- Không làm ảnh hưởng đến luồng xử lý log chính.

---

## 2. Ý tưởng tổng thể

Hệ thống bổ sung thêm module Detect Anomaly hoạt động song song với luồng xử lý log chính.

Processing Module vẫn thực hiện nhiệm vụ chính:

- Nhận log từ Kafka.
- Chuẩn hóa log.
- Lưu log vào ClickHouse.
- Phát hiện các log lỗi rõ ràng để tạo alert như hiện tại.

Ngoài ra, Processing Module sẽ có thêm cơ chế rule-based đơn giản để tạo các tín hiệu bất thường nhẹ, sau đó đưa sang Detect Anomaly Module.

Detect Anomaly Module sẽ xử lý hai nguồn dữ liệu:

- Log signal.
- Metric signal.

Redis được sử dụng làm buffer/state tạm thời để lưu các key theo từng rule trong một khoảng thời gian nhất định. Mỗi key đại diện cho một rule hoặc một loại tín hiệu bất thường.

---

## 3. Kiến trúc tổng thể

```text
External Services
        |
        v
Ingestion API
        |
        v
Kafka Raw Log Topic
        |
        v
Processing Module
        |
        +------------------------+
        |                        |
        v                        v
Save ClickHouse          Log Signal Filter
                                  |
                                  v
                          Detect Anomaly Module
                                  |
                +-----------------+-----------------+
                |                                   |
                v                                   v
          Log Rule Handler                  Metric Rule Handler
                |                                   |
                +-----------------+-----------------+
                                  |
                                  v
                                Redis
                                  |
                                  v
                         Score Collector Job
                                  |
                    +-------------+-------------+
                    |                           |
                    v                           v
              Create Alert              Trigger AI Analysis
                    |                           |
                    v                           v
              Alert Module              AI Evidence Collector
                    |                           |
                    v                           v
          WebSocket / Telegram          AI Anomaly Report
```

---

## 4. Vai trò của từng module

## 4.1 Processing Module

Processing Module không trực tiếp xử lý toàn bộ logic phát hiện bất thường phức tạp.

Nhiệm vụ chính:

- Chuẩn hóa log.
- Lưu log vào ClickHouse.
- Phát hiện nhanh một số log có khả năng liên quan đến bất thường.
- Tạo log signal gửi sang Detect Anomaly Module.

Ví dụ log có thể tạo signal:

- Login failed.
- HTTP 401 hoặc 403.
- HTTP 5xx.
- Error rate tăng.
- Timeout.
- Payment failed.
- Request latency cao.
- Log chứa keyword nguy hiểm.

Processing Module chỉ lọc nhẹ để giảm tải cho Detect Anomaly Module, không làm thay đổi luồng log chính.

---

## 4.2 Detect Anomaly Module

Detect Anomaly Module có hai đầu xử lý chính:

```text
Log Rule Handler
Metric Rule Handler
```

### Log Rule Handler

Xử lý các rule dạng đếm sự kiện trong một khoảng thời gian.

Ví dụ:

```text
failed_login_count >= 50 trong 60 giây
http_401_count >= 100 trong 5 phút
error_count >= 30 trong 1 phút
```

Redis key ví dụ:

```text
anomaly:log:failed_login:auth-service:ip:192.168.1.10
TTL = 60s
value = count
```

Khi log signal đến, hệ thống tăng counter tương ứng trong Redis.

Nếu key chưa tồn tại, tạo key mới và set TTL bằng window của rule.

---

### Metric Rule Handler

Metric không xử lý theo kiểu đếm, vì metric là số liệu liên tục.

Ví dụ CPU 60%, 70%, 80%, 90% là các giá trị trạng thái, không phải event cần đếm.

Do đó metric sẽ được lưu theo dạng state hoặc snapshot ngắn hạn.

Redis key ví dụ:

```text
anomaly:metric:cpu:order-service
```

Giá trị lưu:

```json
{
  "current": 91,
  "max": 94,
  "avg": 87,
  "durationAboveThreshold": 180,
  "lastSeen": "2026-06-27T20:30:00"
}
```

Các metric cần theo dõi:

- CPU usage.
- Memory usage.
- Disk usage.
- Network receive/send.
- Request latency.
- Error rate.
- Throughput.

---

## 5. Redis State Design

Redis được sử dụng làm bộ nhớ tạm thời để lưu trạng thái realtime.

Redis không dùng để lưu log thô.

Redis chỉ lưu:

- Counter của log rule.
- State của metric rule.
- Điểm tạm thời của từng rule.
- Cooldown key.
- Dedup key.
- Recent alert key.

Ví dụ key:

```text
anomaly:log:{ruleId}:{applicationId}:{dimension}
anomaly:metric:{ruleId}:{applicationId}
anomaly:score:{applicationId}
anomaly:cooldown:{applicationId}:{alertType}
anomaly:ai_lock:{alertId}
```

Ví dụ:

```text
anomaly:log:FAILED_LOGIN_60S:auth-service:ip:10.0.0.5
anomaly:metric:CPU_HIGH_5M:order-service
anomaly:score:auth-service
```

---

## 6. Cơ chế tính điểm

Mỗi rule sẽ được quy đổi thành một điểm ảnh hưởng.

Điểm không nên tính tuyến tính hoàn toàn vì mức độ ảnh hưởng của metric thường không tăng đều.

Ví dụ CPU:

```text
CPU < 75%       => 0 điểm
CPU 75% - 85%   => 30 điểm
CPU 85% - 90%   => 60 điểm
CPU > 90%       => 90 điểm
```

Ví dụ failed login:

```text
< 10 lần / 60s      => 0 điểm
10 - 30 lần / 60s   => 40 điểm
30 - 50 lần / 60s   => 70 điểm
> 50 lần / 60s      => 90 điểm
```

Sau khi mỗi rule có điểm riêng, Score Collector Job sẽ gom các điểm lại để tính điểm rủi ro tổng.

Công thức tạm thời:

```text
risk_score =
    a * log_score
  + b * metric_score
  + c * correlation_score
```

Trong đó:

```text
log_score: điểm từ các rule log
metric_score: điểm từ các rule metric
correlation_score: điểm cộng thêm nếu log và metric cùng bất thường
a, b, c: hệ số cấu hình thủ công
```

Ví dụ:

```text
risk_score =
    0.4 * log_score
  + 0.4 * metric_score
  + 0.2 * correlation_score
```

**Lưu ý quan trọng (Critical Success Factor)**:
Phần đặt tham số, cấu hình chuẩn hóa quy đổi điểm số ở các key Redis và việc tinh chỉnh các hệ số phương trình (a, b, c) là vô cùng quan trọng. Để hệ thống detect chính xác nhất và hoạt động thành công đẹp mắt, chúng ta phải liên tục thử nghiệm (tuning) để tìm ra bộ hệ số và mức điểm tối ưu nhất cho từng trường hợp.

---

## 7. Correlation Score

Correlation Score dùng để tăng độ chính xác khi nhiều tín hiệu bất thường xảy ra cùng lúc.

Ví dụ:

```text
Failed login tăng + Network receive tăng
=> nghi ngờ brute-force attack
=> cộng thêm correlation score
```

```text
Error rate tăng + CPU tăng cao
=> nghi ngờ service overload
=> cộng thêm correlation score
```

```text
Memory tăng liên tục + restart log
=> nghi ngờ memory leak
=> cộng thêm correlation score
```

Ví dụ rule correlation:

```text
FAILED_LOGIN_SPIKE + NETWORK_SPIKE => +30 điểm
ERROR_RATE_HIGH + CPU_HIGH => +30 điểm
MEMORY_HIGH + RESTART_LOG => +40 điểm
```

---

## 8. Ngưỡng xử lý

Sau khi tính `risk_score`, hệ thống quyết định hành động theo ngưỡng.

```text
risk_score < 40
=> Bình thường, không cảnh báo.

risk_score từ 40 đến 69
=> Early Warning, ghi nhận cảnh báo nhẹ.

risk_score từ 70 đến 89
=> Tạo Alert mức HIGH.

risk_score >= 90
=> Tạo Alert mức CRITICAL và đề xuất gọi AI.
```

Ngoài ra, nếu một alert lặp lại nhiều lần trong một khoảng thời gian, hệ thống cũng có thể gọi AI để phân tích sâu hơn.

Ví dụ:

```text
Cùng một loại alert xuất hiện 3 lần trong 10 phút
=> Trigger AI Analysis
```

---

## 9. Luồng tạo Alert

Khi Score Collector Job phát hiện điểm vượt ngưỡng, hệ thống tạo alert event gửi sang Alert Module.

```text
Score Collector Job
        |
        v
Alert Event
        |
        v
Alert Module
        |
        v
Redis Dedup / Cooldown
        |
        v
Create Alert
        |
        v
WebSocket + Telegram
```

Alert Module vẫn giữ vai trò trung tâm trong việc:

- Tạo alert.
- Chống trùng cảnh báo.
- Cooldown.
- Gửi WebSocket.
- Gửi Telegram.

---

## 10. Luồng gọi AI

AI không được gọi trực tiếp trong luồng xử lý log realtime.

AI chỉ được gọi sau khi alert đã được tạo.

Điều kiện gọi AI:

- Alert có severity `CRITICAL`.
- Risk score vượt ngưỡng rất cao.
- Alert lặp lại nhiều lần.
- Người dùng bấm nút `Analyze with AI`.

Luồng xử lý:

```text
Alert Created
        |
        v
Check AI Trigger Condition
        |
        v
AI Analysis Job
        |
        v
Evidence Collector
        |
        v
LLM
        |
        v
Insert AI Anomaly Report
        |
        v
Notify User Report Ready
```

---

## 11. Evidence Collector cho AI

Khi gọi AI, hệ thống sẽ gom bằng chứng từ nhiều nguồn.

Evidence gồm:

- Trigger alert.
- Risk score.
- Các rule đã match.
- Redis snapshot tại thời điểm tạo alert.
- Related logs trong ClickHouse.
- Related alerts.
- Metric snapshot.
- Metric trend.
- Top affected IP/user/endpoint/service.

AI không đọc toàn bộ log.

AI chỉ phân tích tập bằng chứng đã được hệ thống gom sẵn.

---

## 12. Snapshot signal khi tạo alert

Hệ thống không cần lưu toàn bộ signal realtime vào DB.

Redis giữ signal tạm thời.

Chỉ khi alert được tạo, hệ thống mới snapshot các signal quan trọng vào DB để phục vụ UI và AI.

Ví dụ snapshot:

```json
{
  "alertId": "ALT-001",
  "applicationId": "auth-service",
  "riskScore": 86,
  "severity": "HIGH",
  "signals": [
    {
      "rule": "FAILED_LOGIN_60S",
      "value": 52,
      "score": 90,
      "window": "60s"
    },
    {
      "rule": "CPU_HIGH_5M",
      "value": 88,
      "score": 60,
      "window": "5m"
    }
  ],
  "reason": "Failed login tăng nhanh kèm CPU tăng cao"
}
```

Cách này giúp:

- Không lưu rác quá nhiều.
- Redis vẫn xử lý nhanh.
- DB chỉ lưu bằng chứng quan trọng.
- AI có dữ liệu để phân tích.
- UI giải thích được vì sao alert được tạo.

---

## 13. Thiết kế bảng dữ liệu đề xuất

### Bảng `anomaly_rules`

Lưu cấu hình rule.

```text
id
name
type: LOG | METRIC | CORRELATION
metric_name
log_pattern
window_seconds
threshold
weight
enabled
severity
created_at
updated_at
```

---

### Bảng `anomaly_score_levels`

Lưu cách quy đổi giá trị thành điểm.

```text
id
rule_id
min_value
max_value
score
description
```

Ví dụ:

```text
CPU 75 - 85 => 30 điểm
CPU 85 - 90 => 60 điểm
CPU > 90 => 90 điểm
```

---

### Bảng `alert_anomaly_snapshots`

Lưu snapshot tại thời điểm tạo alert.

```text
id
alert_id
application_id
risk_score
severity
reason
snapshot_json
created_at
```

---

### Bảng `ai_anomaly_reports`

Lưu kết quả AI phân tích.

```text
id
alert_id
application_id
incident_type
probability
root_cause
evidence_summary
impact
recommended_actions
confidence_score
created_at
```

---

## 14. UI đề xuất

### Trang Early Warning Dashboard

Hiển thị danh sách cảnh báo sớm.

Các cột:

```text
Time
Application
Type
Severity
Risk Score
Reason
Status
Action
```

Action:

```text
View Evidence
Analyze with AI
Create Incident
Acknowledge
```

---

### Trang Alert Evidence Detail

Hiển thị chi tiết vì sao alert được tạo.

Nội dung:

- Risk score.
- Rule đã match.
- Log signals.
- Metric signals.
- Correlation signals.
- Timeline.
- Related logs.
- Related alerts.

---

### Trang AI Anomaly Report

Hiển thị kết quả AI phân tích.

Nội dung:

- Incident Type.
- Attack Probability.
- Root Cause.
- Evidence Summary.
- System Impact.
- Recommended Actions.
- Confidence Score.

---

## 15. Luồng nghiệp vụ hoàn chỉnh

```text
1. External service gửi log vào hệ thống.
2. Ingestion API đẩy log vào Kafka.
3. Processing Module consume log.
4. Processing Module chuẩn hóa và lưu log vào ClickHouse.
5. Processing Module tạo log signal nếu log match rule nhẹ.
6. Metric Worker lấy metric từ Prometheus.
7. Log Rule Handler và Metric Rule Handler cập nhật Redis state.
8. Score Collector Job định kỳ gom các Redis key.
9. Hệ thống quy đổi từng rule thành điểm.
10. Hệ thống tính risk_score tổng.
11. Nếu risk_score vượt ngưỡng, tạo Alert Event.
12. Alert Module nhận event, kiểm tra dedup/cooldown.
13. Alert Module tạo alert và gửi WebSocket/Telegram.
14. Nếu alert nghiêm trọng hoặc lặp lại nhiều lần, trigger AI Analysis Job.
15. Evidence Collector gom log, metric, alert, Redis snapshot.
16. AI phân tích và tạo AI Anomaly Report.
17. UI thông báo cho người dùng rằng báo cáo AI đã sẵn sàng.
```

---

## 16. Ưu điểm của giải pháp

Giải pháp này có các ưu điểm:

- Không ảnh hưởng luồng log chính.
- Không gọi AI trên toàn bộ log realtime.
- Redis xử lý nhanh các state ngắn hạn.
- Có thể phát hiện bất thường trước khi lỗi nghiêm trọng xảy ra.
- Kết hợp được log và metric.
- Có thể giải thích vì sao alert được tạo.
- Có thể mở rộng rule mới dễ dàng.
- Có thể nâng cấp sang model ML trong tương lai.

---

## 17. Hạn chế hiện tại

Phiên bản đầu vẫn dựa trên rule-based và scoring thủ công.

Các hạn chế:

- Cần tuning tham số thủ công.
- Có thể có false positive nếu threshold chưa chuẩn.
- Chưa học được baseline theo từng ứng dụng.
- Chưa phát hiện được seasonality theo giờ/ngày.
- Chưa có mô hình machine learning thực sự.

---

## 18. Hướng phát triển tiếp theo

Trong tương lai, hệ thống có thể nâng cấp:

- Tự học baseline theo từng application.
- Tính anomaly score bằng mô hình machine learning.
- Tự điều chỉnh threshold theo lịch sử.
- Phân tích seasonality theo giờ/ngày/tuần.
- Dự đoán nguy cơ sự cố trước khi xảy ra.
- Tối ưu trọng số scoring bằng dữ liệu thực tế.
- Kết hợp thêm distributed tracing.

---

## 19. Kết luận

Giải pháp đề xuất bổ sung module Detect Anomaly sử dụng Redis làm state buffer ngắn hạn, kết hợp rule-based scoring trên cả log và metric để phát hiện sớm bất thường.

Hệ thống không gọi AI trực tiếp trong luồng realtime, mà chỉ gọi AI sau khi alert đã được tạo và có đủ bằng chứng. Điều này giúp hệ thống vừa đảm bảo hiệu năng, vừa tăng khả năng phân tích và hỗ trợ người dùng xử lý sự cố.

Điểm mới của hướng cải tiến là:

```text
Early Warning
+ Log Metric Correlation
+ Redis-based Realtime Scoring
+ AI Evidence Analysis
```

Từ đó, hệ thống chuyển từ mô hình cảnh báo lỗi bị động sang mô hình giám sát chủ động, có khả năng phát hiện sớm rủi ro và hỗ trợ kỹ sư vận hành ra quyết định nhanh hơn.

```

```

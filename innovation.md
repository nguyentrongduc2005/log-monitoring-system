# Định hướng cải tiến hệ thống AI Log Monitoring & Early Warning

## 1. Hiện trạng hệ thống

Hiện tại hệ thống đã hoàn thành hầu hết các chức năng của một Log Monitoring Platform.

### Luồng xử lý hiện tại

```text
External Service
        |
        v
  Ingestion API
        |
        v
 Kafka Raw Topic
        |
        +-------------------------+
        |                         |
        v                         v
Realtime Worker           Processing Worker
        |                         |
        |                         | Normalize
        |                         | Parse
        |                         | Enrich
        |                         | Save ClickHouse
        |                         |
        |                         +-------> ClickHouse
        |                         |
        |                         | ERROR / CRITICAL
        |                         v
        |                  Kafka Alert Topic
        |                         |
        |                         v
        |                  Alert Module
        |                         |
        |                  Match Rule
        |                  Redis Counter
        |                  Cooldown
        |                         |
        |                         v
        |              Alert + Notify
        |
        v
Live Log View
```

Hiện Alert Module hỗ trợ:

- Rule theo Keyword
- Rule theo Log Level
- Threshold
- Cooldown
- Redis Counter
- WebSocket Notification
- Telegram Notification

## 2. AI hiện tại

AI hiện được sử dụng trong module Incident.

Use case:

```text
Alert
    |
User chọn Investigate
    |
    v
Incident
    |
Evidence Collector
    |
LLM
    |
Incident Report
```

Evidence hiện tại gồm:

- Trigger Alert
- Related Alerts
- Related Logs
- Top Error Fingerprints
- Sample Logs

AI có nhiệm vụ:

- Phân tích nguyên nhân
- Tổng hợp bằng chứng
- Đánh giá ảnh hưởng
- Đề xuất hướng xử lý

Đây là vị trí phù hợp của AI và không nên thay đổi.

AI không nên đặt trong Processing hoặc Alert Module.

---

# 3. Hạn chế của hệ thống hiện tại

Hiện tại hệ thống hoạt động theo cơ chế Reactive.

```text
Lỗi xảy ra
      ↓
Alert
      ↓
Engineer xử lý
```

Nghĩa là hệ thống chỉ cảnh báo khi lỗi đã xuất hiện.

Trong khi mentor mong muốn hệ thống có khả năng:

```text
Nhận biết sớm dấu hiệu bất thường
```

Ví dụ:

- CPU tăng bất thường
- Network tăng bất thường
- Login thất bại tăng nhanh
- Login ngoài giờ
- Dấu hiệu brute-force
- Dấu hiệu quá tải

Ngay cả khi hệ thống chưa sinh nhiều ERROR.

Đây chính là Early Warning.

---

# 4. Hướng cải tiến tổng thể

Không thay đổi kiến trúc hiện tại.

Chỉ bổ sung thêm hai capability mới:

## Capability 1

Thu thập Metrics

Nguồn dữ liệu:

- node_exporter
- Prometheus

Metrics sử dụng:

- CPU
- Memory
- Disk
- Network

## Capability 2

Early Warning

Hệ thống không chỉ phát hiện Error.

Mà còn phát hiện:

- Dấu hiệu tấn công
- Dấu hiệu quá tải
- Dấu hiệu bất thường

trước khi sự cố thực sự xảy ra.

---

# 5. Kiến trúc sau cải tiến

```text
                           External Service
                                   |
                              Push Log
                                   |
                                   v
                            Ingestion API
                                   |
                                   v
                            Kafka Raw Topic
                                   |
         +-------------------------+---------------------------+
         |                         |                           |
         v                         v                           v
Realtime Worker          Processing Worker          Log Anomaly Worker
         |                         |                           |
         |                         |                           |
         |                    Normalize                 Detect Anomaly
         |                    Save ClickHouse                 |
         |                         |                          |
         |                         |                          |
         |                  Alert Candidate                  |
         |                         |                          |
         +-------------------------+--------------------------+
                                   |
                                   v
                           Kafka Alert Topic
                                   |
                                   v
                              Alert Module
                                   |
                          Match Rule / Redis
                                   |
                            Alert Created
                                   |
                   +---------------+----------------+
                   |                                |
                   v                                v
              Notification                   Incident Module
                                                      |
                                              Evidence Collector
                                                      |
                 +------------------------+-----------+----------------------+
                 |                        |                                  |
                 v                        v                                  v
           ClickHouse               Alert Database                    Prometheus
                 |                        |                                  |
                 +------------------------+----------------------------------+
                                                      |
                                                      v
                                                Prompt Builder
                                                      |
                                                      v
                                                     LLM
                                                      |
                                                      v
                                          Incident Investigation Report
```

---

# 6. Early Warning

Đây là capability mới quan trọng nhất của dự án.

Hiện tại:

```text
Error
    ↓
Alert
```

Sau cải tiến:

```text
Dấu hiệu bất thường
        ↓
Early Warning
        ↓
Alert
```

Mục tiêu:

Giúp Engineer biết rằng hệ thống đang có dấu hiệu bất thường trước khi sự cố thực sự xảy ra.

---

# 7. Log Anomaly Worker

Đây là worker mới.

Worker này consume toàn bộ Raw Log.

Không quan tâm log có phải ERROR hay không.

Nhiệm vụ:

- Detect Failed Login tăng nhanh
- Detect Login ngoài giờ
- Detect Error Rate tăng
- Detect nhiều HTTP 401/403
- Detect bất thường theo Rule

Nếu phát hiện bất thường:

Sinh Alert Candidate

Đẩy vào:

```text
Kafka Alert Topic
```

Alert Module vẫn là nơi quản lý Rule.

Không thay đổi kiến trúc hiện tại.

---

# 8. Metric Collection

Hệ thống bổ sung thêm Metrics.

Luồng:

```text
node_exporter
      |
      v
Prometheus
      |
      v
Metric Polling Worker
```

Worker này định kỳ query:

- CPU
- Memory
- Disk
- Network

Sau đó đóng gói thành Metric Event.

Ví dụ:

```text
CPU = 92%

Memory = 83%

Network Receive = 320MB/min
```

Metric Event tiếp tục được đưa vào:

```text
Kafka Alert Topic
```

Alert Module sẽ dùng Metric Rule để Match.

Nhờ đó toàn bộ Rule Engine vẫn chỉ nằm tại Alert Module.

---

# 9. Rule Engine sau cải tiến

Hiện tại:

- Keyword Rule
- Error Level Rule

Sau cải tiến:

## Log Rule

- Keyword
- Level
- Threshold

## Security Rule

- Failed Login
- Login ngoài giờ
- Theo IP
- Theo Username
- Theo Service

## Metric Rule

- CPU vượt ngưỡng
- Memory vượt ngưỡng
- Network vượt ngưỡng
- Disk vượt ngưỡng

## Correlation Rule

Trong tương lai:

- Failed Login + CPU tăng
- Failed Login + Network tăng
- CPU tăng + Error Rate tăng

---

# 10. Incident AI sau cải tiến

Incident vẫn là nơi AI hoạt động.

Không thay đổi vị trí.

Điểm thay đổi là Evidence.

Evidence hiện tại:

- Trigger Alert
- Related Logs
- Related Alerts
- Fingerprints

Evidence mới:

- Trigger Alert
- Related Logs
- Related Alerts
- Metric Snapshot
- Metric Trend
- Top Fingerprints
- Baseline

AI sẽ phân tích toàn bộ Evidence thay vì chỉ đọc Log.

---

# 11. Output mới của AI

Thay vì chỉ Summary.

AI sẽ trả:

```text
Incident Type

Attack Probability

Root Cause

Evidence Summary

System Impact

Recommended Actions

Confidence Score
```

Ví dụ:

```text
Incident:
Possible SSH Brute Force

Probability:
87%

Root Cause:
Nhiều Failed Login từ cùng Source IP.

Evidence:
- 52 Failed Login
- CPU tăng 35%
- Network Receive tăng 60%

Recommendation:
- Block IP
- Kiểm tra Account
- Enable Rate Limit
```

---

# 12. Giá trị mới của hệ thống

## Hiện tại

```text
Log

↓

Alert

↓

Engineer
```

Engineer phải tự đọc Log để tìm nguyên nhân.

---

## Sau cải tiến

```text
Log

+

Metrics

↓

Early Warning

↓

Alert

↓

AI Investigation

↓

Incident Report

↓

Engineer Decision
```

Engineer không còn phải đọc hàng trăm Log.

Hệ thống tự:

- Thu thập bằng chứng
- Phân tích nguyên nhân
- Đánh giá xác suất
- Đề xuất hướng xử lý

---

# 13. Điểm mới của đề tài

Không nên xem AI là điểm mới.

Điểm mới thực sự là:

## 1. Early Warning

Hệ thống phát hiện dấu hiệu bất thường trước khi sự cố xảy ra.

---

## 2. Log + Metrics Correlation

Kết hợp Log và Metrics để tăng độ chính xác khi phát hiện sự cố.

---

## 3. AI Incident Investigator

AI không đọc từng Log.

AI phân tích tập hợp bằng chứng (Evidence Set) để đưa ra:

- Root Cause
- Attack Probability
- Recommendation

---

## 4. AI Decision Support

AI không thay thế Engineer.

AI đóng vai trò hỗ trợ ra quyết định.

Engineer vẫn là người xác nhận cuối cùng.

---

# 14. Giá trị mang lại

Đối với Engineer:

- Phát hiện sớm rủi ro.
- Giảm thời gian điều tra sự cố.
- Không cần đọc hàng trăm dòng Log.
- Có sẵn Root Cause và Recommendation.

Đối với hệ thống:

- Giảm thời gian phản ứng.
- Phát hiện sớm dấu hiệu quá tải.
- Phát hiện sớm dấu hiệu tấn công.
- Tăng khả năng giám sát chủ động thay vì chỉ phản ứng sau khi lỗi đã xảy ra.

Từ một hệ thống Log Monitoring truyền thống, dự án được mở rộng thành một nền tảng **AI-assisted Early Warning & Incident Investigation**, trong đó hệ thống không chỉ ghi nhận và cảnh báo lỗi mà còn chủ động phát hiện dấu hiệu bất thường, thu thập bằng chứng đa nguồn (Log + Metrics) và hỗ trợ kỹ sư vận hành ra quyết định thông qua AI.

# Kế hoạch Triển khai Anomaly Detection - Phase 1

Mục tiêu của Phase 1 là thiết lập luồng dữ liệu (pipeline) từ Processing Module sang Anomaly Detection Module, bao gồm việc lọc log (pre-filter), đẩy event qua Kafka, nhận event và query metric từ Prometheus. Các logic tính điểm (scoring) và cảnh báo (alerting) sẽ được thực hiện ở các phase sau.

## 1. Pre-filter Rules tại Processing Module
Để tránh làm quá tải Anomaly Detection Module, Processing Module sẽ chỉ đẩy các log đáng ngờ (signals) đi tiếp. Danh sách các rule (có thể hardcode tạm hoặc load từ memory) bao gồm:

- **Log Level**: Bắt tất cả log có level là `ERROR`, `CRITICAL`.
- **Keyword Matching**: Trong log message chứa các từ khóa nhạy cảm: `failed`, `timeout`, `denied`, `exception`, `unauthorized`, `out of memory`.

Khi một log match bất kỳ rule nào ở trên, nó sẽ được chuyển đổi thành `AnomalySignalEvent` và đẩy vào Kafka.

## 2. Tạo Kafka Topic gửi event
- **Tên Topic**: `log.anomaly.signals` (hoặc cấu hình tùy chọn trong `application.yml`).
- **Cấu trúc Event (`AnomalySignalEvent`)**:
  - `applicationId` (UUID)
  - `timestamp` (Instant)
  - `logId` (UUID - để trace ngược lại ClickHouse nếu cần)
  - `level` (String)
  - `matchedRule` (String - ví dụ: "KEYWORD_FAILED" hoặc "LEVEL_ERROR")
  - `serviceName` (String)

## 3. Tạo Module `anomaly_detection`
Thiết lập module mới tuân thủ đúng architecture constraints của dự án:
- **Package gốc**: `com.vdt.log_monitoring.modules.anomaly`
- **Cấu trúc thư mục**:
  - `api/`: Chứa Facade và DTOs công khai (nếu có).
  - `internal/`: Chứa logic nội bộ.
  - `internal/consumer/`: Chứa Kafka listener.
  - `internal/metric/`: Chứa worker query Prometheus.

## 4. Đầu nhận và xử lý Log Event (Log Handler)
- Tạo class `AnomalyLogSignalConsumer` trong `internal/consumer`.
- Đánh dấu `@KafkaListener(topics = "${kafka.topics.anomaly-signals}")`.
- **Nhiệm vụ Phase 1**: Lắng nghe event `AnomalySignalEvent` từ Processing Module. Tạm thời chỉ deserialize và in log (hoặc lưu tạm state thô vào Redis) để chứng minh luồng hoạt động thành công. Logic đếm (count) theo time-window sẽ làm ở phase sau.

## 5. Đầu query số liệu định kỳ (Metric Worker)
- Tạo class `PrometheusMetricWorker` trong `internal/metric`.
- Sử dụng `@Scheduled(fixedRateString = "${anomaly.metric.scrape-interval:60000}")` để chạy định kỳ (ví dụ 60s/lần).
- **Nhiệm vụ Phase 1**: Dùng Spring `RestClient` gọi đến API của Prometheus (`/api/v1/query`) để lấy các thông số cơ bản (VD: CPU usage, Error rate). Tạm thời in ra log hoặc lưu raw state vào Redis.

---

### Các bước thực thi dự kiến:
1. Tạo class `AnomalySignalEvent` (trong package `shared` hoặc `anomaly/api`).
2. Sửa file cấu hình Kafka (`application.yml` hoặc Kafka config class) để thêm topic `log.anomaly.signals`.
3. Sửa `Processing Module` (nơi đang lưu ClickHouse) để thêm logic Pre-filter và `KafkaTemplate.send()`.
4. Tạo package `modules/anomaly` và cấu hình module.
5. Code `AnomalyLogSignalConsumer`.
6. Code `PrometheusMetricWorker`.
7. Chạy test toàn luồng để xác nhận event nhảy từ Processing sang Anomaly thành công và Worker gọi được Prometheus.

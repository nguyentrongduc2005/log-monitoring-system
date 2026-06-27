# Kế hoạch triển khai: Tái cấu trúc module Alerting (Global Rule Counter & Log Samples Column)

Kế hoạch này thay đổi cơ chế đếm cảnh báo từ đếm theo vân tay riêng lẻ sang **đếm toàn cục theo quy tắc (`ruleId + applicationId`)**. Khi cảnh báo kích hoạt (hoặc tái kích hoạt), hệ thống sẽ truy vấn ClickHouse để lấy tối đa 5 mẫu log thô tiêu biểu nhất, chuyển đổi thành danh sách String (`List<String>`) và lưu trữ trực tiếp vào cột `log_samples` (JSON/TEXT) trong bảng `alerts` (không cần tạo bảng mới). Trong thời gian Cooldown, hệ thống sẽ hoàn toàn im lặng, không ghi xuống database giao dịch để tối ưu hiệu năng.

---

## Danh sách tệp tin thay đổi (File Map)

### Cơ sở dữ liệu (Database Migrations)
*   [NEW] [V11__restructure_alerts_add_log_samples_column.sql](file:///wsl$/Ubuntu/home/nguye/Workspace/project/log-monitoring-system/apps/backend/src/main/resources/db/migration/postgresql/V11__restructure_alerts_add_log_samples_column.sql) — Di chuyển lược đồ dữ liệu: xoá các trường log đơn lẻ trong `alerts`, thêm cột `log_samples` để lưu trữ danh sách log mẫu dạng văn bản.

### Tầng Thực thể & Kho lưu trữ (Entities & Repositories)
*   [MODIFY] [AlertEntity.java](file:///wsl$/Ubuntu/home/nguye/Workspace/project/log-monitoring-system/apps/backend/src/main/java/com/vdt/log_monitoring/modules/alerting/internal/alert/AlertEntity.java) — Xoá các cột log cũ, thêm thuộc tính `logSamples` (ánh xạ `@ElementCollection` hoặc converter mapping sang cột TEXT/JSON).
*   [MODIFY] [AlertRepository.java](file:///wsl$/Ubuntu/home/nguye/Workspace/project/log-monitoring-system/apps/backend/src/main/java/com/vdt/log_monitoring/modules/alerting/internal/alert/AlertRepository.java) — Cập nhật truy vấn tìm cảnh báo active theo `ruleId` và `applicationId` (bỏ `fingerprint`).

### Tầng Logic & Caching (Logic & Caching Services)
*   [MODIFY] [AlertThresholdCache.java](file:///wsl$/Ubuntu/home/nguye/Workspace/project/log-monitoring-system/apps/backend/src/main/java/com/vdt/log_monitoring/modules/alerting/internal/cache/AlertThresholdCache.java) — Thay đổi khóa đếm Redis thành `ruleId + applicationId` (bỏ `fingerprint`).
*   [MODIFY] [AlertService.java](file:///wsl$/Ubuntu/home/nguye/Workspace/project/log-monitoring-system/apps/backend/src/main/java/com/vdt/log_monitoring/modules/alerting/internal/alert/AlertService.java) — Cập nhật logic tạo/tái kích hoạt Alert, tìm kiếm Alert đang active theo rule và application.
*   [MODIFY] [AlertEvaluationService.java](file:///wsl$/Ubuntu/home/nguye/Workspace/project/log-monitoring-system/apps/backend/src/main/java/com/vdt/log_monitoring/modules/alerting/internal/evaluation/AlertEvaluationService.java) — Khi `TRIGGERED`, thực hiện truy vấn ClickHouse lấy top 5 mẫu log thô, ánh xạ thành danh sách chuỗi và lưu vào cột `log_samples` của Alert. Trong thời gian Cooldown, bỏ qua không làm gì cả.

### Tầng Vận chuyển & Giao tiếp (DTOs & Notifiers)
*   [MODIFY] [AlertResponse.java](file:///wsl$/Ubuntu/home/nguye/Workspace/project/log-monitoring-system/apps/backend/src/main/java/com/vdt/log_monitoring/api/alerting/dto/AlertResponse.java) — Trả kèm mảng 5 mẫu log thô (`logSamples`) lên Frontend.
*   [MODIFY] [AlertNotificationMessage.java](file:///wsl$/Ubuntu/home/nguye/Workspace/project/log-monitoring-system/apps/backend/src/main/java/com/vdt/log_monitoring/modules/realtime/api/events/AlertNotificationMessage.java) — Cập nhật payload WebSocket mang danh sách mẫu log.
*   [MODIFY] [TelegramNotifier.java](file:///wsl$/Ubuntu/home/nguye/Workspace/project/log-monitoring-system/apps/backend/src/main/java/com/vdt/log_monitoring/modules/alerting/internal/notification/telegram/TelegramNotifier.java) — Cập nhật mẫu tin nhắn Telegram chỉ hiển thị tối đa 2 mẫu log thô tiêu biểu hàng đầu để tối ưu hiển thị.

---

## Chi tiết các Task triển khai

### Task 1: Tạo tệp di chuyển dữ liệu (Flyway Migration)
*   Tạo tệp `V11__restructure_alerts_add_log_samples_column.sql` thực hiện:
    1.  Xoá các ràng buộc Check của cột `message` và `fingerprint` trên bảng `alerts`.
    2.  Xoá các cột: `event_id`, `ingestion_id`, `message`, `fingerprint`, `log_timestamp`.
    3.  Thêm cột `log_samples` dạng `JSONB` hoặc `TEXT` vào bảng `alerts`.

### Task 2: Cập nhật Thực thể & Repository
*   **AlertEntity.java:** Xoá các thuộc tính cũ, thêm trường `List<String> logSamples` (sử dụng `@ElementCollection` với `@CollectionTable` hoặc JPA Attribute Converter sang JSON).
*   **AlertRepository.java:** Đổi tên phương thức truy vấn thành:
    `findFirstByRuleIdAndApplicationIdAndStatusNotOrderByTriggeredAtDesc`

### Task 3: Tái cấu trúc Redis Cache đếm toàn cục
*   **AlertThresholdCache.java:** Cập nhật hàm tạo khóa để trả về:
    *   Window key: `log-monitoring:alerting:window:<ruleId>:<applicationId>`
    *   Cooldown key: `log-monitoring:alerting:cooldown:<ruleId>:<applicationId>`

### Task 4: Tích hợp truy vấn ClickHouse & Lưu mẫu log khi Trigger
*   **AlertEvaluationService.java / AlertService.java:**
    *   Khi kích hoạt hoặc tái kích hoạt (Trigger/Retrigger): Gọi phương thức `findTopErrorFingerprints` của `ProcessedLogEvidenceReader` (đã có sẵn trong dự án) để lấy danh sách lỗi thô tiêu biểu từ ClickHouse trong khung thời gian từ `firstSeenAt` đến hiện tại.
    *   Trích xuất tối đa 5 thông điệp log thô (`sampleMessage`) gộp thành danh sách String và gán vào `logSamples` của `AlertEntity`.
    *   Trong thời gian Cooldown (`decision.type() == COOLDOWN`): **Bỏ qua không ghi hay cập nhật gì xuống database Postgres** để đảm bảo tải trọng ghi đĩa bằng 0.

### Task 5: Cập nhật thông báo Telegram & WebSocket
*   **TelegramNotifier.java:** Chỉnh sửa mẫu tin nhắn để hiển thị tối đa 2 mẫu log thô đầu tiên kèm số lượng cụ thể của từng loại.
*   **AlertResponse.java & AlertNotificationMessage.java:** Cập nhật DTO để đẩy đầy đủ 5 mẫu log lên Frontend hiển thị qua API và WebSocket.

---

## Kế hoạch Xác minh (Verification Plan)

### Kiểm thử Tự động
*   Chạy kiểm thử biên dịch dự án Backend:
    ```bash
    wsl bash -i -c "make build"
    ```
*   Chạy bộ test của module Alerting để đảm bảo các service hoạt động đúng đắn:
    ```bash
    wsl bash -i -c "make test"
    ```

### Kiểm thử Thủ công
*   Mở trình duyệt, cấu hình một Alert Rule với ngưỡng 3 lỗi trong 60s cho một ứng dụng.
*   Gửi 3 log lỗi khác nhau (ví dụ: 1 NullPointer, 2 DB timeout) lên hệ thống.
*   Kiểm tra tin nhắn Telegram xem có hiển thị tối đa 2 mẫu log thô đại diện hay không.
*   Kiểm tra cơ sở dữ liệu để đảm bảo không có lệnh insert/update dư thừa xảy ra trong thời gian cooldown.

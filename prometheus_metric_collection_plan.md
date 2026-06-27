# [Plan] Tích hợp Prometheus & Metric Collection

Tài liệu này vạch ra kế hoạch triển khai việc thu thập metric cho các Application thông qua **Prometheus** và quản lý **Metric Sources** trực tiếp trên hệ thống Log Monitoring.

## Tóm tắt phạm vi công việc
- Bổ sung Prometheus vào hạ tầng Docker.
- Mở rộng chức năng của Module `identity` (Quản lý ứng dụng) để lưu trữ cấu hình Metric Target.
- Backend phơi bày API Service Discovery nội bộ để Prometheus có thể lấy danh sách Target động thông qua `http_sd_configs`.
- Bổ sung giao diện Frontend cho phép người dùng cấu hình Metric Source ở ngay trong trang chi tiết Application.

## Các thay đổi đề xuất (Proposed Changes)

### 1. Hạ tầng (Infrastructure) & Cấu hình Prometheus
- **Cập nhật `compose.yml`**: Thêm container `prometheus` (image: `prom/prometheus`). Map cổng `9090`. Cấu hình volume để mount file `prometheus.yml`.
- **Tạo `prometheus.yml`**: 
  - Thêm một job (ví dụ: `job_name: 'log_monitoring_apps'`).
  - Cấu hình `http_sd_configs` trỏ về API nội bộ của Backend: `url: 'http://backend:8080/internal/prometheus/targets'`.

### 2. Database Schema (Module Identity)
- **Tạo script Flyway mới (Ví dụ: `V15__create_metric_sources.sql`)**:
  ```sql
  CREATE TABLE identity.metric_sources (
      id UUID PRIMARY KEY,
      application_id UUID NOT NULL UNIQUE REFERENCES identity.applications(id),
      target_host VARCHAR(255) NOT NULL,
      target_port INT NOT NULL,
      metrics_path VARCHAR(255) NOT NULL DEFAULT '/actuator/prometheus',
      scrape_interval VARCHAR(32) NOT NULL DEFAULT '15s',
      enabled BOOLEAN NOT NULL DEFAULT true,
      created_at TIMESTAMP WITH TIME ZONE NOT NULL,
      updated_at TIMESTAMP WITH TIME ZONE NOT NULL
  );
  ```
  *(Lưu ý: Thêm UNIQUE constraint trên `application_id` để đảm bảo 1 App chỉ có tối đa 1 Target).*

### 3. Backend (Module Identity)
- **Entity & Repository**: 
  - Tạo `MetricSourceEntity`, quan hệ 1-1 với `ApplicationEntity`.
  - Tạo `MetricSourceRepository`.
- **Service & Logic**:
  - `MetricSourceService` để xử lý logic Thêm/Sửa/Xóa.
  - Xử lý API `/test-connection`: Gửi request HTTP GET thử nghiệm đến `http://{host}:{port}{path}` xem có phản hồi (200 OK) không.
- **REST APIs (Public)**:
  - `GET /api/applications/{id}/metric-sources`
  - `POST /api/applications/{id}/metric-sources`
  - `PUT /api/metric-sources/{id}`
  - `DELETE /api/metric-sources/{id}`
  - `POST /api/metric-sources/{id}/test-connection`
- **Internal API (Service Discovery cho Prometheus)**:
  - `GET /internal/prometheus/targets`
  - Trả về danh sách target JSON theo chuẩn của Prometheus:
    ```json
    [
      {
        "targets": ["<target_host>:<target_port>"],
        "labels": {
          "__metrics_path__": "<metrics_path>",
          "application_id": "<app_id>",
          "application_name": "<app_name>"
        }
      }
    ]
    ```

### 4. Frontend (React UI)
- **Cập nhật giao diện Application Details**:
  - Thêm tab "Metric Source" (hoặc Metric Config) bên cạnh Log/Alert rules hiện có.
- **Tính năng trong Tab Metric Source**:
  - Nếu app chưa có Metric Source: Hiện nút "Configure Metric Source".
  - Hiển thị Form nhập liệu: `Target Host`, `Target Port`, `Metrics Path` (mặc định `/actuator/prometheus`), `Scrape Interval`, `Enabled`.
  - Nút "Test Connection" gọi API để xem target có UP không.
  - Hiển thị cấu hình đã lưu và trạng thái kích hoạt (Enabled/Disabled).

## Kế hoạch kiểm thử (Verification Plan)
1. **Infra**: Chạy `make infra-up`, đảm bảo Prometheus lên ở cổng 9090.
2. **Backend**: Gọi `GET /internal/prometheus/targets` trả về đúng format JSON ở trên.
3. **Frontend**: Tạo Metric Source bằng giao diện, bấm Test Connection báo thành công.
4. **End-to-End**: 
   - Vào Prometheus UI (`localhost:9090/targets`).
   - Kiểm tra xem target đã được load qua Service Discovery và trạng thái hiển thị là UP.
   - Thấy các Label `application_id` và `application_name` xuất hiện trong cấu hình target và trên các record metric thực tế lấy về.

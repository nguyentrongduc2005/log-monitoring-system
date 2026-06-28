# Kế Hoạch Tích Hợp Anomaly - Alerting - Incident & Mở Rộng Cơ Chế Điều Tra AI

Tài liệu này vạch ra các bước triển khai để đáp ứng luồng xử lý tín hiệu bất thường (anomaly) từ khâu cảnh báo (alerting) sang khâu điều tra tự động và thủ công (incident) bằng AI, cũng như những thay đổi trên giao diện người dùng.

## 1. Giao Tiếp Alerting - Incident (Trực Tiếp)
- **Thiết kế**: Theo yêu cầu, luồng từ Alerting gọi sang Incident sẽ sử dụng **giao tiếp trực tiếp (direct method call)** qua `IncidentFacade` thay vì dùng Integration Event (như Kafka). Lý do: Logic gọi (trigger auto-report) xảy ra ít và cần tính đồng bộ cao cho cảnh báo.
- **Thay đổi Code**:
  - `Alerting` module sẽ gọi `incidentFacade.generateAnomalyReport(alertId, anomalyEvidence)` ngay khi tạo xong một cảnh báo có type là "anomaly".

## 2. Thiết Kế Lưu Trữ Mới (Bảng Report)
- **Thiết kế**: Thêm bảng `incident_anomaly_report` (hoặc `anomaly_report`) vào schema của module `incident`.
- **Dữ liệu lưu trữ**:
  - `alert_id`: Khóa ngoại liên kết với cảnh báo tương ứng.
  - `evidence_payload`: Các bằng chứng bất thường được module anomaly gửi qua (tín hiệu, metric, rule nào bị vi phạm).
  - `ai_analysis_result`: Phân tích và gợi ý của AI.
  - `created_at`, `status`.

## 3. Luồng Auto-Report & Gọi AI (Tự Động)
- **Logic Backend**:
  - Khi `IncidentFacade.generateAnomalyReport` được gọi, hệ thống thu thập log/metric **chỉ gói gọn trong phạm vi bằng chứng (evidence)** được cung cấp. Không query toàn bộ log.
  - Gửi prompt cho AI với nội dung: "Hiện tại hệ thống có những bất thường này [bằng chứng]. Dựa vào đây, có khả năng hệ thống bị lỗi gì? Cần điều tra thêm thông tin gì?".
  - Nhận kết quả từ AI, update vào bảng `report`.
- **Thông Báo Kép (Second Notification)**:
  - Cảnh báo lần 1: Đã được Alerting gửi khi có bất thường.
  - Cảnh báo lần 2: Sau khi AI phân tích Report xong, module `incident` (hoặc trả kết quả về `alerting`) sẽ gửi một thông báo thứ 2 chứa nội dung báo cáo AI.

## 4. UI: Trang Quản Lý Report
- **Thay đổi Frontend**:
  - Thêm một trang mới (Route: `/reports` hoặc `/alerts/reports`) chỉ để hiển thị danh sách các Report tự động từ AI.
  - Trang này sẽ tách biệt hoàn toàn với trang Alerts thông thường, giúp người dùng dễ dàng xem các phân tích nhanh của AI cho từng bất thường.

## 5. Luồng Phân Tích Sự Cố (Manual Root Cause Incident)
- **Thiết kế lại việc thu thập bằng chứng**:
  - Logic cũ: Trả về 100 raw error logs -> **Bỏ**.
  - Logic mới: Truy vấn log lỗi, gom nhóm (Group By Fingerprint) và đếm số lượng. Ví dụ: `[Error Timeout: 100 logs (10:00 - 10:15)]`, `[Payment Failure: 50 logs (10:05 - 10:15)]`. AI sẽ nhận dữ liệu đã được tổng hợp này.
- **Xử lý riêng cho Alert Type = Anomaly**:
  - Khi bấm "Tạo Incident" từ một cảnh báo bất thường:
    - Đọc bảng `report` để lấy thông tin tín hiệu (evidence) gốc.
    - Query **toàn bộ error logs** trong khung giờ.
    - Query **thêm log bất thường** (dựa trên tín hiệu report).
    - Gom nhóm toàn bộ (fingerprint group) và đẩy cho AI phân tích **Root Cause** (nguyên nhân gốc rễ) dựa trên bức tranh toàn cảnh.

## 6. UI: Cập Nhật Giao Diện Chi Tiết Incident
- **Thay đổi Frontend**:
  - Màn hình chi tiết Incident: Thay thế khối hiển thị raw log khô khan bằng một danh sách các **Bằng chứng (Evidence)** rõ ràng (Tên lỗi, Số lượng, Khung giờ).
  - Hiển thị rõ: Sự cố này được phân tích dựa trên những nhóm log nào, có kết hợp với dữ liệu anomaly không.

---

## 📅 Các Bước Triển Khai Thực Tế

### Giai đoạn 1: Schema & Data Flow (Backend)
1. Tạo Entity, Repository, Flyway migration cho bảng `incident_anomaly_report`.
2. Mở rộng `IncidentFacade` với hàm `generateAnomalyReport`.
3. Sửa `alerting` module để gọi `IncidentFacade` và phát thông báo lần 2.

### Giai đoạn 2: Log Collection & AI Logic (Backend)
1. Cập nhật `IncidentEvidenceCollector` để gom nhóm log theo fingerprint (thay vì lấy raw 100 logs).
2. Xây dựng bộ thu thập riêng cho Anomaly (đọc từ report, kết hợp log lỗi & log bất thường).
3. Cập nhật Prompt cho 2 luồng AI: Tự động (gợi ý điều tra) và Thủ công (Root cause).

### Giai đoạn 3: Tích hợp Giao Diện (Frontend)
1. Thêm trang `/reports`.
2. Sửa UI màn hình tạo/chi tiết Incident để render cấu trúc nhóm log rõ ràng.

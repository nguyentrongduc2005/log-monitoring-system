# Project Diagrams

Các sơ đồ trong thư mục này được viết bằng Mermaid và tách từ `detailed-diagrams.md`.

| File | Nội dung |
| --- | --- |
| [identity-access.md](identity-access.md) | Luồng đăng nhập và cấp token |
| [log-ingestion-processing.md](log-ingestion-processing.md) | Luồng tiếp nhận, xử lý, ghi log và publish downstream events |
| [realtime-stream.md](realtime-stream.md) | Luồng WebSocket realtime logs/alerts/anomalies |
| [anomaly-detection.md](anomaly-detection.md) | Luồng phát hiện bất thường từ metrics và logs |
| [alerting-deduplication.md](alerting-deduplication.md) | Luồng đánh giá alert rules, idempotency, threshold và cooldown |
| [incident-ai-assistant.md](incident-ai-assistant.md) | Luồng tạo incident và gọi AI assistant |
| [data-retention.md](data-retention.md) | Luồng retention policy và xóa log hết hạn |

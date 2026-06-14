# Danh sách sơ đồ nên vẽ cho báo cáo

> Mục tiêu: chỉ vẽ các sơ đồ giúp báo cáo rõ ý và đúng yêu cầu "tài liệu kỹ
> thuật ngắn gọn". Không cần vẽ quá nhiều sơ đồ chi tiết.

## Sơ đồ nên có

| STT | Sơ đồ | Đặt ở mục | Mục đích |
| --- | --- | --- | --- |
| 1 | Use case tổng quan | 2.5 Use case tổng quan | Cho thấy Admin, Engineer và External Application tương tác với hệ thống như thế nào. |
| 2 | Kiến trúc tổng thể | 3.1 Kiến trúc tổng thể | Thể hiện Frontend, Backend, Kafka, Worker, ClickHouse, Redis, PostgreSQL và Telegram. |
| 3 | Luồng xử lý log end-to-end | 3.2 Luồng xử lý log end-to-end | Mô tả log đi từ external application đến Kafka, worker, ClickHouse, realtime UI và alert. |
| 4 | Sequence tiếp nhận log | 3.2 Luồng xử lý log end-to-end | Cho thấy API xác thực API key, validate request, publish Kafka và trả `202 Accepted`. |
| 5 | Message Queue flow | 3.3 Thiết kế Message Queue | Thể hiện các topic `logs.raw`, `logs.live`, `alerts.critical`, `logs.dlq` và producer/consumer. |
| 6 | Luồng cảnh báo và deduplication | 3.4 Thiết kế realtime và cảnh báo | Mô tả ERROR/CRITICAL log đi qua alerting, Redis dedup và Telegram/WebSocket. |
| 7 | ERD PostgreSQL | 4.2 Thiết kế PostgreSQL | Thể hiện quan hệ giữa users, applications, api keys và user_application_access. |
| 8 | Thiết kế lưu trữ log ClickHouse | 4.3 Thiết kế ClickHouse | Cho thấy bảng `logs`, partition theo thời gian và liên kết logic với `application_id`. |
| 9 | Deployment local bằng Docker Compose | 6.1 Docker Compose | Thể hiện các container/service và port chính khi chạy demo. |
| 10 | Kịch bản demo 500 logs / 2 seconds | 6.3 Kịch bản demo | Mô tả các bước demo từ tạo API key, chạy script, xem realtime UI đến kiểm tra alert. |

## Sơ đồ vẽ thêm nếu còn thời gian

| STT | Sơ đồ | Đặt ở mục | Khi nào nên vẽ |
| --- | --- | --- | --- |
| 1 | Module backend | 3.1 Kiến trúc tổng thể | Nếu muốn giải thích Modular Monolith: identity, logs, realtime, alerting. |
| 2 | WebSocket realtime flow | 5.5 WebSocket API | Nếu realtime là phần demo quan trọng. |
| 3 | Log Search flow | 5.4 Log Search API | Nếu có triển khai tìm kiếm log bằng ClickHouse. |
| 4 | Data retention flow | 4.6 Data retention strategy | Nếu có làm retention policy hoặc muốn trình bày điểm cộng. |
| 5 | AI analysis flow | 7.3 Hướng phát triển hoặc mục AI | Nếu AI chỉ là mở rộng, nên để ở hướng phát triển thay vì chương chính. |

## Thứ tự ưu tiên vẽ

Nếu không có nhiều thời gian, ưu tiên theo thứ tự:

1. Kiến trúc tổng thể.
2. Luồng xử lý log end-to-end.
3. ERD PostgreSQL.
4. Message Queue flow.
5. Luồng cảnh báo và deduplication.
6. Deployment Docker Compose.
7. Use case tổng quan.
8. Kịch bản demo 500 logs / 2 seconds.

## Gợi ý định dạng

- Use case: dùng UML use case diagram.
- Kiến trúc tổng thể: dùng component/container diagram.
- Luồng xử lý: dùng sequence diagram hoặc flowchart.
- Kafka topic: dùng flowchart có producer/topic/consumer.
- ERD: dùng database ERD.
- Deployment: dùng deployment/container diagram.

## Ghi chú khi đưa vào báo cáo

- Mỗi sơ đồ chỉ cần caption 1-2 câu.
- Không nên đưa sơ đồ quá chi tiết làm báo cáo dài.
- Các sơ đồ phải khớp với phần đã triển khai hoặc ghi rõ là thiết kế/hướng phát triển.
- Nếu một sơ đồ đã giải thích đủ ý, không cần lặp lại bằng đoạn văn dài.

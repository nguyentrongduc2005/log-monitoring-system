# Form báo cáo kỹ thuật ngắn gọn

> Mục tiêu của form này là bám đúng yêu cầu "tài liệu kỹ thuật ngắn gọn" của đề
> tài. Mỗi mục chỉ nên viết đủ ý, ưu tiên sơ đồ, bảng và luồng xử lý thay vì
> diễn giải dài.

## BÌA

Nên ghi:
- Tên đề tài: Hệ thống Thu thập log và Giám sát Lỗi Ứng dụng.
- Tên sinh viên, mã số sinh viên, lớp, giảng viên hướng dẫn.
- Trường/khoa, năm thực hiện.

Nên kiểm tra:
- Tên đề tài có đúng với đề bài không?
- Thông tin cá nhân và giảng viên có chính xác không?

## MỤC LỤC

Nên ghi:
- Tự động tạo theo heading của báo cáo nếu dùng Word/Google Docs.
- Chỉ giữ các chương/mục thật sự có nội dung.

Nên kiểm tra:
- Số trang có khớp sau khi xuất PDF không?

## DANH MỤC TỪ VIẾT TẮT

Nên ghi bảng:

| Từ viết tắt | Ý nghĩa |
| --- | --- |
| API | Application Programming Interface |
| MQ | Message Queue |
| DB | Database |
| TTL | Time To Live |
| AI | Artificial Intelligence |

Nên kiểm tra:
- Có thuật ngữ nào xuất hiện nhiều lần nhưng chưa giải thích không?
- Kafka, Redis, ClickHouse, WebSocket có cần giải thích ngắn không?

---

# CHƯƠNG 1. GIỚI THIỆU

## 1.1 Bối cảnh và bài toán

Nên viết:
- Ứng dụng hiện đại sinh ra nhiều log khi người dùng thao tác.
- Nếu ghi từng log trực tiếp vào DB SQL sẽ dễ nghẽn khi tải tăng cao.
- Kỹ sư vận hành cần xem log realtime và nhận cảnh báo khi lỗi nghiêm trọng.

Nên tự hỏi:
- Vì sao hệ thống log monitoring là cần thiết?
- Vấn đề gì xảy ra nếu không có buffer/message queue?
- Người dùng cuối của hệ thống là ai?

Nên có:
- Không bắt buộc hình.
- Có thể thêm 1 hình minh họa vấn đề: nhiều application gửi log về một hệ
  thống trung tâm.

## 1.2 Mục tiêu hệ thống

Nên viết:
- Xây dựng hệ thống tiếp nhận log tốc độ cao.
- Đưa log thô vào Message Queue để cân bằng tải.
- Worker xử lý, chuẩn hóa và lưu log.
- Hiển thị log realtime cho kỹ sư vận hành.
- Cảnh báo lỗi `ERROR`/`CRITICAL` và chống trùng cảnh báo bằng Redis.
- Hỗ trợ phân quyền xem log theo ứng dụng.

Nên tự hỏi:
- Hệ thống cần giải quyết những mục tiêu bắt buộc nào?
- Mục tiêu nào là điểm cộng hoặc mở rộng?

Nên có:
- Bảng "Mục tiêu - Cách hệ thống đáp ứng".

## 1.3 Phạm vi thực hiện

Nên viết:
- Phạm vi bắt buộc: identity/access, log ingestion, log processing, realtime
  viewer, alerting, alert deduplication, Docker deployment.
- Phạm vi mở rộng nếu có: log search, retention policy, AI analysis,
  application health analytics.
- Nói rõ phần nào đã làm, phần nào ở mức thiết kế hoặc hướng phát triển.

Nên tự hỏi:
- Tính năng nào chắc chắn demo được?
- Tính năng nào chỉ là điểm cộng?
- Có tính năng nào không triển khai để tránh phình scope không?

Nên có:
- Bảng "Chức năng - Trạng thái - Ghi chú".

## 1.4 Công nghệ sử dụng

Nên viết bảng:

| Thành phần | Công nghệ | Vai trò |
| --- | --- | --- |
| Backend | Spring Boot | API, xử lý nghiệp vụ |
| Frontend | React | Giao diện quản trị |
| Database nghiệp vụ | PostgreSQL | User, application, API key, alert config |
| Log storage | ClickHouse | Lưu và truy vấn log |
| Message Queue | Kafka | Buffer log thô và event realtime |
| Cache/Lock | Redis | Alert deduplication |
| Triển khai | Docker Compose | Đóng gói và chạy hệ thống |

Nên tự hỏi:
- Vì sao chọn công nghệ này thay vì công nghệ khác?
- Công nghệ đó giải quyết phần nào của bài toán?

Nên có:
- Bảng công nghệ như trên.

---

# CHƯƠNG 2. PHÂN TÍCH YÊU CẦU

## 2.1 Tác nhân hệ thống

Nên viết:
- Admin: quản lý application, API key, phân quyền, cấu hình cảnh báo.
- Engineer/Kỹ sư vận hành: xem log realtime, nhận cảnh báo, điều tra lỗi.
- External Application: gửi log vào hệ thống qua API.

Nên tự hỏi:
- Ai sử dụng giao diện?
- Ai gửi log?
- Ai có quyền cấu hình hệ thống?

Nên có:
- Use case diagram tổng quan.
- Bảng "Tác nhân - Mục đích sử dụng".

## 2.2 Yêu cầu chức năng bắt buộc

Nên viết ngắn gọn theo bullet:
- Quản lý application gửi log.
- Tạo và thu hồi API key cho application.
- Tiếp nhận log đơn lẻ hoặc batch.
- Đẩy log thô vào Kafka.
- Worker consume log, chuẩn hóa và lưu xuống ClickHouse.
- Hiển thị log realtime.
- Phát hiện `ERROR`/`CRITICAL` để tạo cảnh báo.
- Chống trùng cảnh báo bằng Redis.
- Gửi thông báo Telegram nếu có triển khai.
- Phân quyền kỹ sư chỉ xem được log application được cấp quyền.

Nên tự hỏi:
- Mỗi chức năng đến từ yêu cầu nào trong đề tài?
- Input/output chính của chức năng là gì?
- Chức năng nào nằm trên hot path tiếp nhận log?

Nên có:
- Bảng "Mã yêu cầu - Tên yêu cầu - Mô tả - Độ ưu tiên".

## 2.3 Yêu cầu chức năng mở rộng

Nên viết:
- Log Search/Log Investigation: lọc log theo application, thời gian, level,
  trace ID, nội dung message.
- Retention Policy: xóa hoặc nén log cũ, đặc biệt log `INFO` quá 7 ngày.
- AI Analysis: phân tích, phân loại hoặc gợi ý nguyên nhân lỗi.
- Application Health Analytics: thống kê tỷ lệ lỗi giữa các application theo
  thời gian.

Nên tự hỏi:
- Tính năng nào có triển khai thật?
- Tính năng nào chỉ trình bày ở hướng phát triển?
- Có cần đánh dấu rõ "điểm cộng" không?

Nên có:
- Bảng "Tính năng mở rộng - Lý do - Trạng thái".

## 2.4 Yêu cầu phi chức năng

Nên viết:
- Hiệu năng: tiếp nhận được 500 log trong 2 giây ở kịch bản demo.
- Khả năng mở rộng: dùng Kafka để tách tốc độ nhận log và tốc độ xử lý.
- Bảo mật: API key, JWT, phân quyền theo application.
- Tin cậy: retry, DLQ nếu có, không mất log khi worker lỗi.
- Khả năng quan sát: metric ingestion, Kafka lag, lỗi ghi ClickHouse.

Nên tự hỏi:
- Hệ thống cần chịu tải bao nhiêu?
- Nếu ClickHouse/Kafka/Redis lỗi thì hệ thống phản ứng thế nào?
- Có yêu cầu bảo mật nào liên quan log nhạy cảm không?

Nên có:
- Bảng "Yêu cầu phi chức năng - Cách đáp ứng - Cách kiểm chứng".

## 2.5 Use case tổng quan

Nên viết:
- Liệt kê 5-7 use case chính, không cần quá nhiều.
- Ví dụ: đăng nhập, tạo application, tạo API key, gửi log batch, xem realtime
  log, nhận cảnh báo, tìm kiếm log.

Nên tự hỏi:
- Use case nào xuất hiện trong demo?
- Use case nào chứng minh được yêu cầu chính của đề tài?

Nên có:
- Use case diagram.
- Bảng mô tả ngắn từng use case.

---

# CHƯƠNG 3. THIẾT KẾ KIẾN TRÚC

## 3.1 Kiến trúc tổng thể

Nên viết:
- Hệ thống triển khai theo Modular Monolith cho backend.
- Backend gồm các module chính: identity, logs, realtime, alerting.
- Các hạ tầng chính: PostgreSQL, ClickHouse, Kafka, Redis.
- Frontend gọi API/WebSocket để quản trị và xem log.

Nên tự hỏi:
- Các thành phần chính của hệ thống là gì?
- Thành phần nào chịu trách nhiệm tiếp nhận, xử lý, lưu trữ, hiển thị?
- Vì sao không ghi log trực tiếp vào PostgreSQL?

Nên có:
- Sơ đồ kiến trúc tổng thể.
- Nên vẽ: External Applications -> Backend -> Kafka -> Worker -> ClickHouse ->
  Frontend/WebSocket/Alerting.

## 3.2 Luồng xử lý log end-to-end

Nên viết:
- External application gửi log kèm API key.
- Backend xác thực API key và validate request.
- Backend publish raw log vào Kafka `logs.raw`.
- Worker consume, normalize, redact, fingerprint.
- Worker batch insert vào ClickHouse.
- Worker publish log realtime vào Kafka `logs.live`.
- Realtime module gửi log qua WebSocket.
- Nếu level là `ERROR`/`CRITICAL`, hệ thống kích hoạt cảnh báo.

Nên tự hỏi:
- Request trả thành công ở bước nào?
- Nếu worker xử lý lỗi thì log đi đâu?
- Event ID được sinh và giữ xuyên suốt pipeline thế nào?

Nên có:
- Sequence diagram cho flow tiếp nhận log.
- Flowchart xử lý `INFO/WARN` và `ERROR/CRITICAL`.

## 3.3 Thiết kế Message Queue

Nên viết:
- Kafka dùng để buffer log thô và tách HTTP ingestion khỏi worker.
- Topic chính: `logs.raw`, `logs.live`, `alerts.critical`, `logs.dlq`.
- Partition theo `application_id` để giữ thứ tự trong từng application.

Nên tự hỏi:
- Topic nào dùng cho bước nào?
- Producer/consumer của từng topic là ai?
- Khi worker fail thì retry/DLQ xử lý ra sao?

Nên có bảng:

| Topic | Producer | Consumer | Mục đích |
| --- | --- | --- | --- |
| logs.raw | Ingestion API | Processing Worker | Lưu log thô chờ xử lý |
| logs.live | Processing Worker | Realtime Module | Đẩy log realtime |
| alerts.critical | Processing Worker | Alerting Module | Cảnh báo lỗi nghiêm trọng |
| logs.dlq | Processing Worker | Admin/Operations | Lưu event xử lý lỗi |

## 3.4 Thiết kế realtime và cảnh báo

Nên viết:
- Realtime module consume `logs.live` rồi broadcast qua WebSocket.
- Alerting module nhận event `ERROR`/`CRITICAL`.
- Redis dùng làm khóa chống trùng cảnh báo trong một khoảng thời gian.
- Telegram nhận thông báo sau khi qua deduplication.

Nên tự hỏi:
- Người dùng nhận log realtime bằng cơ chế nào?
- Khi cùng một lỗi xuất hiện 100 lần/phút thì hệ thống gửi mấy cảnh báo?
- Key Redis dedup gồm những thông tin nào?

Nên có:
- Sequence diagram cảnh báo lỗi.
- Bảng format Redis key dedup.

## 3.5 Các quyết định thiết kế chính

Nên viết bảng:

| Quyết định | Lý do |
| --- | --- |
| Dùng Kafka trước khi ghi DB | Tránh DB bị quá tải khi log đến dồn dập |
| Dùng ClickHouse lưu log | Phù hợp dữ liệu append-only, truy vấn theo thời gian |
| Dùng Redis dedup alert | Thao tác lock nhanh, có TTL |
| Dùng Modular Monolith | Dễ triển khai đồ án, vẫn giữ module boundary rõ |

Nên tự hỏi:
- Quyết định nào quan trọng nhất với đề tài?
- Có trade-off nào cần nói ngắn gọn không?

Nên có:
- Bảng Architecture Decision ngắn.

---

# CHƯƠNG 4. THIẾT KẾ CƠ SỞ DỮ LIỆU VÀ LƯU TRỮ

## 4.1 Tổng quan lựa chọn lưu trữ

Nên viết:
- PostgreSQL lưu dữ liệu nghiệp vụ: user, application, API key, phân quyền,
  alert rule.
- ClickHouse lưu log đã chuẩn hóa để search và analytics.
- Redis lưu khóa chống trùng cảnh báo.
- Kafka lưu event/log thô tạm thời trong pipeline, không thay thế database.

Nên tự hỏi:
- Dữ liệu nào cần transaction?
- Dữ liệu nào ghi rất nhiều và truy vấn theo thời gian?
- Dữ liệu nào chỉ cần tồn tại tạm thời?

Nên có:
- Bảng "Loại dữ liệu - Nơi lưu - Lý do".

## 4.2 Thiết kế PostgreSQL

Nên viết:
- Các bảng chính: `users`, `applications`, `application_api_keys`,
  `user_application_access`.
- Nếu làm alert: thêm `alert_rules`, `alert_occurrences`,
  `notification_channels`.
- Nêu khóa chính, khóa ngoại, unique constraint quan trọng.

Nên tự hỏi:
- Bảng nào thuộc module identity?
- Bảng nào phục vụ phân quyền theo application?
- API key có lưu raw key không?

Nên có:
- ERD PostgreSQL.
- Bảng mô tả schema rút gọn: tên bảng, mục đích, cột quan trọng.

## 4.3 Thiết kế ClickHouse

Nên viết:
- Bảng `logs` lưu log đã chuẩn hóa.
- Cột chính: `event_id`, `ingestion_id`, `application_id`,
  `application_name`, `level`, `message`, `trace_id`, `event_timestamp`,
  `received_at`, `stored_at`, `environment`, `host_name`, `attributes`.
- Gợi ý engine: `MergeTree`.
- Partition theo tháng/ngày dựa trên `event_timestamp`.
- Sort key phục vụ query theo `application_id`, `level`, `event_timestamp`.
- Query log bắt buộc có time range và limit.

Nên tự hỏi:
- Search log thường lọc theo trường nào?
- Partition theo thời gian nào hợp với retention?
- Có cần dedup theo `event_id` khi Kafka retry không?

Nên có:
- Bảng schema ClickHouse `logs`.
- Sơ đồ quan hệ logic: PostgreSQL `applications.id` -> ClickHouse
  `logs.application_id`.
- Bảng index/partition strategy.

## 4.4 Thiết kế Redis

Nên viết:
- Redis dùng cho alert deduplication.
- Key có thể dựa trên `application_id`, `level`, `fingerprint`.
- TTL quyết định khoảng thời gian chống gửi trùng.

Nên tự hỏi:
- Một lỗi được xem là trùng dựa trên tiêu chí nào?
- TTL bao lâu là hợp lý cho demo?
- Nếu Redis mất key thì ảnh hưởng thế nào?

Nên có:
- Bảng ví dụ Redis key.

## 4.5 Thiết kế Kafka topics

Nên viết:
- Lặp lại topic chính nhưng tập trung góc nhìn lưu trữ/event pipeline.
- Nêu retention ngắn cho `logs.live`, retention dài hơn cho `logs.raw` nếu cần
  replay.

Nên tự hỏi:
- Topic nào cần replay?
- Topic nào chỉ phục vụ realtime tạm thời?
- Topic nào dùng cho lỗi terminal?

Nên có:
- Bảng topic, partition key, retention, producer, consumer.

## 4.6 Data retention strategy

Nên viết:
- INFO log quá 7 ngày có thể xóa/nén theo yêu cầu điểm cộng.
- ClickHouse TTL hoặc partition operation phù hợp hơn xóa từng row.
- Retention nên chạy nền, không nằm trên hot path ingestion.

Nên tự hỏi:
- Loại log nào giữ lâu hơn?
- Chính sách retention áp dụng theo level hay theo application?
- Retention có ảnh hưởng truy vấn và demo không?

Nên có:
- Bảng "Level - Thời gian giữ - Hành động".

---

# CHƯƠNG 5. THIẾT KẾ API

## 5.1 Authentication API

Nên viết:
- API đăng nhập, refresh token, lấy thông tin user hiện tại.
- JWT dùng để bảo vệ API quản trị.

Nên tự hỏi:
- Endpoint nào public?
- Endpoint nào cần JWT?

Nên có:
- Bảng endpoint authentication.

## 5.2 Application và API Key API

Nên viết:
- API tạo/cập nhật application.
- API tạo API key, liệt kê key, revoke/rotate key.
- Raw API key chỉ hiển thị một lần khi tạo.

Nên tự hỏi:
- Admin làm gì để cho một application gửi log?
- API key được xác thực ở bước nào?

Nên có:
- Bảng endpoint.
- Sequence ngắn: Admin tạo application -> tạo API key -> external app gửi log.

## 5.3 Log Ingestion API

Nên viết:
- Endpoint nhận log single/batch.
- Header chứa API key.
- Request gồm `applicationName`, `level`, `message`, `timestamp`, `traceId`.
- Response trả `202 Accepted` sau khi publish Kafka thành công.

Nên tự hỏi:
- Request tối đa bao nhiêu event?
- Trường nào bắt buộc?
- Khi API key sai thì trả lỗi gì?

Nên có:
- Bảng request/response.
- Ví dụ JSON ngắn.

## 5.4 Log Search API

Nên viết nếu có triển khai:
- Search theo application, time range, level, trace ID, message.
- Bắt buộc có `from`, `to`, `limit`.
- Không cho client gửi raw SQL hoặc ClickHouse expression.

Nên tự hỏi:
- Người vận hành cần tìm log theo tiêu chí nào?
- Làm sao tránh query scan toàn bộ bảng?

Nên có:
- Bảng query parameters.
- Ví dụ response 1 log.

## 5.5 WebSocket API

Nên viết:
- Client subscribe kênh realtime log.
- Có thể gửi filter theo application/level.
- Server push log mới khi worker publish `logs.live`.

Nên tự hỏi:
- Client subscribe endpoint nào?
- Filter realtime áp dụng ở client hay server?
- Khi mất kết nối thì UI xử lý thế nào?

Nên có:
- Sơ đồ WebSocket flow.
- Bảng message format.

## 5.6 Alert API

Nên viết nếu có:
- API xem danh sách alert occurrence.
- API cấu hình rule/ngưỡng cảnh báo nếu thuộc scope.
- Telegram notification là output của alerting.

Nên tự hỏi:
- Alert được tạo từ điều kiện nào?
- Dedup có làm mất dữ liệu occurrence không?

Nên có:
- Bảng endpoint alert.
- Sequence diagram alert.

## 5.7 Error response

Nên viết:
- Format lỗi thống nhất: `code`, `message`, `details`, `timestamp`.
- Một số lỗi chính: invalid API key, application inactive, invalid log payload,
  rate limited, unauthorized.

Nên tự hỏi:
- Client cần thông tin gì để sửa request?
- Có để lộ secret hoặc thông tin nhạy cảm trong error không?

Nên có:
- Bảng error code.
- Ví dụ JSON lỗi.

---

# CHƯƠNG 6. TRIỂN KHAI, KIỂM THỬ VÀ KẾT QUẢ

## 6.1 Docker Compose

Nên viết:
- Các service trong docker compose: backend, frontend, PostgreSQL, Kafka,
  ClickHouse, Redis.
- Cách chạy hệ thống bằng một lệnh.

Nên tự hỏi:
- Người khác clone repo có chạy được không?
- Cần biến môi trường nào?

Nên có:
- Sơ đồ deployment local.
- Bảng service/port.

## 6.2 Cấu hình chính

Nên viết:
- Cấu hình Kafka topic.
- Cấu hình ClickHouse connection.
- Cấu hình Redis.
- Cấu hình JWT/API key.

Nên tự hỏi:
- Config nào bắt buộc?
- Secret có được đưa vào báo cáo không? Không nên.

Nên có:
- Bảng biến môi trường chính, che giá trị secret.

## 6.3 Kịch bản demo 500 logs / 2 seconds

Nên viết:
- Mô tả tool/script giả lập gửi 500 log trong 2 giây.
- Các bước demo: tạo application, tạo API key, chạy script, xem realtime UI,
  kiểm tra log lưu trong ClickHouse, kiểm tra alert.

Nên tự hỏi:
- Demo chứng minh yêu cầu nào?
- Chỉ số nào cần ghi lại?

Nên có:
- Sequence demo.
- Screenshot tool/script chạy thành công.

## 6.4 Kết quả kiểm thử hiệu năng

Nên viết:
- Số log gửi, thời gian gửi, số log accepted, số log lỗi.
- Latency trung bình/p95 nếu đo được.
- Kafka lag, thời gian worker xử lý, thời gian log xuất hiện trên UI.

Nên tự hỏi:
- Hệ thống có đạt 500 logs / 2 seconds không?
- Bottleneck nằm ở đâu?

Nên có:
- Bảng kết quả benchmark.
- Biểu đồ log throughput theo thời gian nếu có.

## 6.5 Kết quả giao diện chính

Nên viết:
- Màn hình dashboard.
- Màn hình realtime log viewer.
- Màn hình application/API key.
- Màn hình alert/notification nếu có.
- Màn hình log search nếu có.

Nên tự hỏi:
- Screenshot nào chứng minh chức năng chính?
- Có cần đánh dấu flow từ log đến alert không?

Nên có:
- Screenshot từng màn hình chính.
- Mỗi hình chỉ cần caption ngắn, không mô tả dài.

---

# CHƯƠNG 7. ĐÁNH GIÁ VÀ HƯỚNG PHÁT TRIỂN

## 7.1 Kết quả đạt được

Nên viết:
- Liệt kê các yêu cầu đã hoàn thành.
- Nêu hệ thống đã chạy được bằng Docker.
- Nêu demo 500 logs / 2 seconds nếu đạt.

Nên tự hỏi:
- Đề tài yêu cầu gì và mình đã đáp ứng thế nào?
- Có bằng chứng/screenshot/test nào đi kèm không?

Nên có:
- Bảng "Yêu cầu - Kết quả - Minh chứng".

## 7.2 Hạn chế

Nên viết trung thực:
- Chưa benchmark với tải lớn hơn.
- Chưa tối ưu sâu ClickHouse partition/index nếu chưa làm.
- AI analysis còn ở mức cơ bản nếu chưa triển khai sâu.
- Alert rule còn đơn giản nếu chưa có rule engine.

Nên tự hỏi:
- Phần nào chưa đạt mức production?
- Hạn chế nào là do thời gian/scope?

Nên có:
- Không bắt buộc hình.
- Có thể dùng bullet ngắn.

## 7.3 Hướng phát triển

Nên viết:
- Hoàn thiện log search nâng cao.
- Thêm retention policy tự động.
- Thêm AI root cause analysis.
- Thêm dashboard health analytics.
- Thêm observability cho Kafka lag, ClickHouse latency, alert delivery.
- Thêm phân quyền chi tiết hơn theo role/application.

Nên tự hỏi:
- Nếu phát triển tiếp 1-2 tháng thì nên làm gì trước?
- Tính năng nào tăng giá trị vận hành nhất?

Nên có:
- Bảng "Hướng phát triển - Lợi ích - Độ ưu tiên".

---

# KẾT LUẬN

Nên viết:
- Tóm tắt bài toán đã giải quyết.
- Nhấn mạnh Message Queue giúp hệ thống chịu tải tốt hơn so với ghi trực tiếp
  DB.
- Nhấn mạnh realtime viewer và alert dedup giúp kỹ sư vận hành phát hiện lỗi
  nhanh hơn.
- Nêu ngắn gọn kết quả demo và hướng mở rộng.

Nên tự hỏi:
- Người đọc nhớ được điểm mạnh nào của hệ thống sau khi đọc kết luận?
- Có lặp lại quá nhiều chi tiết kỹ thuật không?

# TÀI LIỆU THAM KHẢO

Nên ghi:
- Tài liệu Spring Boot.
- Tài liệu Kafka.
- Tài liệu ClickHouse.
- Tài liệu Redis.
- Tài liệu Docker.
- Tài liệu WebSocket/STOMP nếu có dùng.

Nên kiểm tra:
- Link còn truy cập được không?
- Format tài liệu tham khảo có thống nhất không?

# PHỤ LỤC

## A. OpenAPI Specification

Nên để:
- Link hoặc trích đoạn OpenAPI chính.
- Không cần copy toàn bộ nếu quá dài.

## B. Docker Compose

Nên để:
- File docker compose hoặc link repo.
- Các lệnh chạy demo.

## C. Source Code Structure

Nên để:
- Cây thư mục backend/frontend rút gọn.
- Chỉ giải thích module chính.

## D. Kịch bản Demo

Nên để:
- Các bước demo theo thứ tự.
- Tài khoản demo nếu có.
- API key demo không nên để secret thật.

## E. Link GitHub

Nên để:
- Link repository.
- Branch/tag demo nếu có.

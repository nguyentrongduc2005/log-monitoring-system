# Use Case tổng quan của hệ thống

Sơ đồ này dùng cho mục `2.5 Use case tổng quan` trong báo cáo.

**Caption đề xuất:** Hình 2.x. Use Case tổng quan của hệ thống.

```plantuml
@startuml
left to right direction
skinparam packageStyle rectangle
skinparam actorStyle awesome
skinparam shadowing false
skinparam usecase {
  BackgroundColor #F8FAFC
  BorderColor #334155
  ArrowColor #334155
}
skinparam rectangle {
  BackgroundColor #FFFFFF
  BorderColor #CBD5E1
}

actor "Admin" as Admin
actor "Engineer\n(Kỹ sư vận hành)" as Engineer
actor "External Application\n(Ứng dụng gửi log)" as App
actor "Telegram Bot" as TelegramBot

Admin --|> Engineer

rectangle "Log Monitoring System" {
  package "Quản trị hệ thống" {
    usecase "Quản lý người dùng\nvà phân quyền" as UC_UserAccess
    usecase "Quản lý ứng dụng\ngửi log" as UC_Application
    usecase "Quản lý API Key" as UC_ApiKey
    usecase "Cấu hình cảnh báo" as UC_AlertConfig
  }

  package "Tiếp nhận log" {
    usecase "Tiếp nhận log\ntừ ứng dụng" as UC_IngestLog
  }

  package "Giám sát và tra cứu" {
    usecase "Xem log\nthời gian thực" as UC_LiveLog
    usecase "Tìm kiếm\nvà lọc log" as UC_SearchLog
    usecase "Xem cảnh báo\nsự cố" as UC_ViewAlert
    usecase "Gửi thông báo\nTelegram" as UC_TelegramNotify
  }

  package "Phân tích và thống kê" {
    usecase "Xem thống kê\nsức khỏe ứng dụng" as UC_AppHealth
    usecase "Phân tích log\nbằng AI" as UC_AiAnalysis
  }
}

Admin --> UC_UserAccess
Admin --> UC_Application
Admin --> UC_ApiKey
Admin --> UC_AlertConfig

Engineer --> UC_LiveLog
Engineer --> UC_SearchLog
Engineer --> UC_ViewAlert
Engineer --> UC_AppHealth
Engineer --> UC_AiAnalysis

App --> UC_IngestLog
UC_TelegramNotify --> TelegramBot

note right of Admin
  Admin kế thừa các chức năng
  giám sát của Engineer và có
  thêm quyền quản trị hệ thống.
end note

note bottom of UC_IngestLog
  Ứng dụng bên ngoài gửi log vào hệ thống
  thông qua API Key đã được cấp.
end note

note right of UC_TelegramNotify
  Telegram Bot là tác nhân ngoài hệ thống,
  dùng để nhận và chuyển tiếp cảnh báo sự cố.
end note

@enduml
```

## Danh sách Use Case chính

| STT | Use Case | Tác nhân chính | Mô tả ngắn |
| --- | --- | --- | --- |
| 1 | Quản lý ứng dụng gửi log | Admin | Quản lý các application được phép gửi log vào hệ thống. |
| 2 | Quản lý API Key | Admin | Tạo, quản lý hoặc thu hồi API Key dùng để xác thực application gửi log. |
| 3 | Tiếp nhận log từ ứng dụng | External Application | Ứng dụng bên ngoài gửi log vào hệ thống để lưu trữ, xử lý và giám sát. |
| 4 | Xem log thời gian thực | Engineer, Admin | Theo dõi log mới phát sinh theo thời gian thực trên giao diện. |
| 5 | Tìm kiếm và lọc log | Engineer, Admin | Tra cứu log theo application, thời gian, mức độ lỗi hoặc nội dung. |
| 6 | Xem cảnh báo sự cố | Engineer, Admin | Theo dõi các cảnh báo được tạo ra khi hệ thống phát hiện log lỗi nghiêm trọng. |
| 7 | Quản lý người dùng và phân quyền | Admin | Quản lý tài khoản người dùng và phân quyền truy cập theo application. |
| 8 | Cấu hình cảnh báo | Admin | Thiết lập điều kiện hoặc quy tắc cảnh báo cho hệ thống giám sát log. |
| 9 | Xem thống kê sức khỏe ứng dụng | Engineer, Admin | Theo dõi tình trạng hoạt động, tỷ lệ lỗi và xu hướng log của từng application. |
| 10 | Phân tích log bằng AI | Engineer, Admin | Phân tích log để hỗ trợ nhận diện nguyên nhân lỗi hoặc gợi ý hướng xử lý. |

## Ghi chú

- Sơ đồ chỉ mô tả use case ở mức tổng quan, phù hợp với báo cáo ngắn khoảng 30
  trang.
- `Telegram Bot` là tác nhân phụ trợ bên ngoài hệ thống, phục vụ việc gửi thông
  báo cảnh báo và không nhất thiết phải đưa vào bảng use case chính.
- Các chi tiết như Kafka, ClickHouse, Redis, worker xử lý log và WebSocket nên
  được trình bày ở sơ đồ kiến trúc hoặc sequence diagram thay vì đưa vào use
  case diagram.
- Nếu phần AI chưa triển khai hoàn chỉnh, có thể ghi chú trong báo cáo rằng
  đây là use case mở rộng hoặc định hướng phát triển.

# Use case tổng quan

Sơ đồ này dùng cho mục `2.5 Use case tổng quan` trong báo cáo. Sơ đồ được viết
bằng Mermaid để dễ nhúng trực tiếp trong Markdown.

```mermaid
flowchart LR
    app[/"External Application<br/>Ứng dụng gửi log"/]
    engineer[/"Engineer<br/>Kỹ sư vận hành"/]
    admin[/"Admin"/]
    telegram[/"Telegram"/]

    admin -. "kế thừa quyền" .-> engineer

    subgraph system["Log Monitoring System"]
        direction TB

        subgraph access["Identity & Access"]
            login(("Đăng nhập"))
            manageUsers(("Quản lý user"))
            manageApps(("Quản lý application"))
            manageApiKeys(("Quản lý API key"))
            grantAccess(("Phân quyền xem log<br/>theo application"))
        end

        subgraph ingestion["Log Ingestion"]
            sendLog(("Gửi log vào hệ thống"))
            verifyApiKey(("Xác thực API key"))
            publishRaw(("Đẩy log thô vào Kafka"))
        end

        subgraph monitoring["Monitoring & Investigation"]
            liveLogs(("Xem live log stream"))
            filterLogs(("Lọc log theo<br/>application / level"))
            searchLogs(("Tìm kiếm log"))
            logDetail(("Xem chi tiết log"))
            healthAnalytics(("Xem health analytics"))
            aiAnalysis(("Xem AI analysis"))
        end

        subgraph alerting["Alerting"]
            viewAlerts(("Theo dõi cảnh báo lỗi"))
            triggerAlert(("Kích hoạt cảnh báo<br/>ERROR / CRITICAL"))
            dedupAlert(("Chống trùng cảnh báo<br/>bằng Redis"))
            realtimeAlert(("Nhận cảnh báo realtime"))
            telegramNotify(("Gửi thông báo Telegram"))
        end

        subgraph retention["Retention"]
            configureRetention(("Cấu hình retention policy"))
        end
    end

    app --> sendLog

    engineer --> login
    engineer --> liveLogs
    engineer --> filterLogs
    engineer --> searchLogs
    engineer --> logDetail
    engineer --> viewAlerts
    engineer --> realtimeAlert
    engineer --> healthAnalytics
    engineer --> aiAnalysis

    admin --> manageUsers
    admin --> manageApps
    admin --> manageApiKeys
    admin --> grantAccess
    admin --> configureRetention

    telegramNotify --> telegram

    sendLog -. "<<include>>" .-> verifyApiKey
    sendLog -. "<<include>>" .-> publishRaw
    sendLog -. "<<extend>><br/>khi log là ERROR / CRITICAL" .-> triggerAlert

    liveLogs -. "<<extend>>" .-> filterLogs
    searchLogs -. "<<extend>>" .-> logDetail

    triggerAlert -. "<<include>>" .-> dedupAlert
    dedupAlert -. "<<extend>><br/>khi chưa có cảnh báo trùng" .-> realtimeAlert
    dedupAlert -. "<<extend>><br/>khi bật Telegram" .-> telegramNotify
```

## Ghi chú

- `Admin` kế thừa quyền của `Engineer`, nghĩa là Admin có thể dùng các chức
  năng giám sát và điều tra log như Engineer, đồng thời có thêm quyền quản trị.
- `<<include>>` dùng cho bước luôn xảy ra trong use case chính.
- `<<extend>>` dùng cho hành vi chỉ xảy ra khi có điều kiện.
- Sơ đồ này chỉ mô tả use case ở mức tổng quan. Chi tiết Kafka, worker,
  ClickHouse và WebSocket nên trình bày ở sơ đồ kiến trúc hoặc sequence diagram.

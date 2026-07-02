# Sơ đồ Luồng Chi tiết Các Phân hệ Hệ thống (Detailed Flow Diagrams)

Tài liệu này cung cấp các sơ đồ luồng chi tiết cho từng phân hệ trong hệ thống Log Monitoring System. Mỗi phân hệ bao gồm 2 phiên bản sơ đồ: **Mermaid** (tích hợp trực tiếp hiển thị trên Markdown) và **PlantUML** (dành cho các công cụ thiết kế chuyên nghiệp).

---

## 1. Phân hệ Identity & Access (Quản lý Định danh & Quyền truy cập)

* **Loại sơ đồ**: Sơ đồ tuần tự (Sequence Diagram) mô tả luồng đăng nhập.
* **Mô tả**: Mô tả chi tiết quá trình xác thực tài khoản người dùng, đối chiếu thông tin với cơ sở dữ liệu PostgreSQL và tạo Token JWT phiên làm việc để lưu vào Redis.

### Bản vẽ Mermaid:
```mermaid
sequenceDiagram
    autonumber
    actor User as Kỹ sư / Admin
    participant Dashboard as Web Dashboard
    participant API as Identity Service (Auth Controller)
    participant DB as PostgreSQL (D3)
    participant Redis as Redis Cache (D4)

    User->>Dashboard: Nhập Email & Mật khẩu
    Dashboard->>API: HTTP POST /api/v1/auth/login (email, password)
    activate API
    API->>DB: Truy vấn User bằng email
    activate DB
    DB-->>API: Trả về bản ghi User (password_hash, role, status)
    deactivate DB
    
    alt User không tồn tại HOẶC trạng thái khác ACTIVE
        API-->>Dashboard: Trả về lỗi HTTP 401 Unauthorized
    else User hợp lệ
        Note over API: So sánh mật khẩu bằng BCrypt
        alt Mật khẩu sai
            API-->>Dashboard: Trả về lỗi HTTP 401 Unauthorized
        else Mật khẩu đúng
            Note over API: Tạo JWT token (chứa userId, role, email)
            API->>Redis: Lưu metadata token & đánh dấu hoạt động (Set TTL)
            API-->>Dashboard: HTTP 200 OK (Trả về Access Token & Profile)
            Dashboard->>Dashboard: Lưu token vào LocalStorage/Cookie
            Dashboard-->>User: Chuyển hướng về trang chủ & hiển thị giao diện
        end
    end
    deactivate API
```

### Bản vẽ PlantUML:
```plantuml
@startuml
!theme plain
skinparam monochrome true
skinparam shadowing false
skinparam defaultFontName "Courier New"

actor "Kỹ sư / Admin" as User
participant "Web Dashboard" as Dashboard
participant "Identity Service (Auth)" as API
database "PostgreSQL (D3)" as DB
database "Redis Cache (D4)" as Redis

autonumber

User -> Dashboard : Nhập Email & Mật khẩu
Dashboard -> API : HTTP POST /api/v1/auth/login (email, password)
activate API
API -> DB : Truy vấn User bằng email
activate DB
DB --> API : Trả về bản ghi User (password_hash, role, status)
deactivate DB

alt User không tồn tại hoặc status != 'ACTIVE'
    API --> Dashboard : HTTP 401 Unauthorized (Lỗi đăng nhập)
else User hợp lệ
    Note over API : So sánh mật khẩu (BCrypt verify)
    alt Mật khẩu không trùng khớp
        API --> Dashboard : HTTP 401 Unauthorized (Lỗi mật khẩu)
    else Mật khẩu chính xác
        Note over API : Tạo JWT token (chứa userId, role, email, exp)
        API -> Redis : Lưu Session/Token metadata (thiết lập TTL)
        API --> Dashboard : HTTP 200 OK (Access Token + User Profile)
        Dashboard -> Dashboard : Lưu Token vào bộ nhớ trình duyệt
        Dashboard --> User : Chuyển hướng sang giao diện chính tương ứng với quyền
    end
end
deactivate API

@enduml
```

### Bảng Phân quyền Vai trò (Permission Matrix)

Hệ thống phân tách quyền lợi cụ thể giữa vai trò **Admin** và **Engineer** như sau:

| Danh mục | Chức năng nghiệp vụ | Quyền Admin | Quyền Engineer | Ghi chú |
| :--- | :--- | :---: | :---: | :--- |
| **Quản trị người dùng** | Tạo mới, khóa hoặc xóa tài khoản người dùng | **Có** | **Không** | Quyền quản trị hệ thống tối cao |
| | Thay đổi vai trò người dùng (`role`) | **Có** | **Không** | Chỉ Admin mới được nâng/hạ quyền |
| **Quản lý ứng dụng** | Đăng ký ứng dụng mới cần theo dõi log | **Có** | **Không** | Tạo dự án giám sát mới |
| | Cấu hình quyền truy cập ứng dụng (`user_application_access`) | **Có** | **Không** | Gán ứng dụng cho kỹ sư cụ thể |
| | Tạo mới, thu hồi API Key của ứng dụng | **Có** | **Có** | Engineer chỉ thực hiện trên ứng dụng được cấp quyền `MANAGE` |
| | Cấu hình nguồn cào chỉ số (`metric_sources`) | **Có** | **Có** | Engineer chỉ thực hiện trên ứng dụng được cấp quyền `MANAGE` |
| **Giám sát & Logs** | Xem log realtime & truy vấn logs từ ClickHouse | **Có** | **Có** | Engineer chỉ xem được ứng dụng được gán quyền `VIEW` hoặc `MANAGE` |
| | Xem danh sách cảnh báo phát sinh (`alerts`) | **Có** | **Có** | Engineer chỉ xem cảnh báo của ứng dụng được phân quyền |
| **Cấu hình Cảnh báo** | Thiết lập luật cảnh báo (`alert_rules`) | **Có** | **Có** | Engineer chỉ cấu hình trên ứng dụng có quyền `MANAGE` |
| | Cấu hình chat room nhận tin nhắn (Telegram) | **Có** | **Có** | Toàn quyền thiết lập cổng kết nối |
| **Điều tra Sự cố** | Mở sự cố mới (`incidents`) | **Có** | **Có** | Ghi nhận sự cố để điều tra |
| | Đóng/Giải quyết sự cố, cập nhật timeline | **Có** | **Có** | Cập nhật tiến độ xử lý |
| | Yêu cầu trợ lý AI phân tích sự cố (`AI analysis`) | **Có** | **Có** | Gọi API phân tích bundle bằng chứng |
| **Chính sách lưu trữ** | Thiết lập chính sách lưu giữ logs (`retention_policies`) | **Có** | **Không** | Tránh rủi ro thất thoát dữ liệu logs |
| | Kích hoạt chạy tiến trình dọn dẹp log thủ công | **Có** | **Không** | Chỉ Admin mới kích hoạt dọn dẹp đĩa cứng ClickHouse |

---

## 2. Logs Ingestion & Processing (Tiếp nhận & Chuẩn hóa Log)

* **Loại sơ đồ**: Sơ đồ tuần tự (Sequence Diagram).
* **Mô tả**: Mô tả luồng đi của log từ khi ứng dụng bên ngoài đẩy về API, qua hàng đợi Kafka, được Worker chuẩn hóa và lưu trữ theo cơ chế Batch vào ClickHouse.

### Bản vẽ Mermaid:
```mermaid
sequenceDiagram
    autonumber
    actor App as Ứng dụng ngoài
    participant Ingest as Ingestion API (P1)
    participant Redis as Redis Cache (D4)
    participant Kafka as Hàng đợi Kafka (D1)
    participant Worker as Processing Worker (P2)
    participant CH as ClickHouse (D2)
    participant Anomaly as Anomaly Module (P4)

    App->>Ingest: HTTP POST /api/v1/logs (Payload + API Key)
    Note over Ingest: Kiểm tra xác thực & Rate limit
    Ingest->>Redis: Get Key Info & Rate Limit Counter
    Redis-->>Ingest: Valid Key & Rate Limit OK
    Ingest->>Kafka: Đẩy log thô vào topic `logs.raw`
    Ingest-->>App: HTTP 202 Accepted (Bắt đầu xử lý bất đồng bộ)
    
    Note over Worker: Chu kỳ Consume log từ Kafka
    Worker->>Kafka: Đọc log thô từ topic `logs.raw`
    Note over Worker: Xử lý, chuẩn hóa timestamp,<br/>phân loại level & băm fingerprint
    Worker->>Worker: Đưa log vào bộ đệm Batch Queue
    
    alt Kích thước Batch đạt giới hạn hoặc hết Timeout (1 giây)
        Worker->>CH: Ghi Batch Log chuẩn hóa vào bảng `processed_logs`
    end
    
    alt Log có mức độ nghiêm trọng WARN/ERROR/CRITICAL
        Worker->>Anomaly: Đẩy log tín hiệu nghi ngờ sang Anomaly Module
    end
```

### Bản vẽ PlantUML:
```plantuml
@startuml
!theme plain
skinparam monochrome true
skinparam shadowing false
skinparam defaultFontName "Courier New"

actor "Ứng dụng ngoài" as App
participant "Ingestion API (P1)" as Ingest
database "Redis Cache (D4)" as Redis
queue "Hàng đợi Kafka (D1)" as Kafka
participant "Processing Worker (P2)" as Worker
database "ClickHouse (D2)" as CH
participant "Anomaly Module (P4)" as Anomaly

autonumber

App -> Ingest : HTTP POST /api/v1/logs (Payload + API Key)
activate Ingest
Ingest -> Redis : Get Key Info & Check Rate Limit
activate Redis
Redis --> Ingest : Key hợp lệ & Rate limit OK
deactivate Redis

Ingest -> Kafka : Đẩy log thô vào topic "logs.raw"
Ingest --> App : HTTP 202 Accepted
deactivate Ingest

Note over Worker : Chu kỳ Consume log bất đồng bộ
activate Worker
Worker -> Kafka : Đọc log thô từ topic "logs.raw"
Note over Worker : Phân tích, chuẩn hóa timestamp,\nphân loại level và băm fingerprint
Worker -> Worker : Đưa log vào bộ đệm Batch (Batch Queue)

alt Kích thước Batch đạt giới hạn hoặc hết Timeout (1s)
    Worker -> CH : Ghi Batch log vào bảng "processed_logs"
end

alt Log có tín hiệu cảnh báo/bất thường (WARN/ERROR)
    Worker -> Anomaly : Gửi tín hiệu log nghi ngờ sang Anomaly Module
end
deactivate Worker

@enduml
```

---

## 3. Real-time Stream (Stream dữ liệu thời gian thực)

* **Loại sơ đồ**: Sơ đồ tuần tự (Sequence Diagram).
* **Mô tả**: Thể hiện quá trình thiết lập kết nối WebSocket giữa Web Dashboard của Kỹ sư vận hành và Real-time Gateway để nhận logs/alerts tức thời.

### Bản vẽ Mermaid:
```mermaid
sequenceDiagram
    autonumber
    actor Op as Kỹ sư vận hành (Dashboard)
    participant Gateway as Real-time Gateway (P6)
    participant Redis as Redis Cache (D4)
    participant Kafka as Hàng đợi Kafka (D1)

    Op->>Gateway: Thiết lập kết nối WebSocket (ws://... + Access Token)
    activate Gateway
    Gateway->>Redis: Kiểm tra tính hợp lệ của token
    Redis-->>Gateway: Token hợp lệ
    Gateway-->>Op: Chấp nhận kết nối (WebSocket Connected)
    
    par Stream Logs
        Note over Gateway: Lắng nghe topic `logs.live`
        Kafka->>Gateway: Consume live log event
        Gateway->>Op: Đẩy live log xuống Dashboard (qua WebSocket)
    and Stream Alerts
        Note over Gateway: Lắng nghe topic `alerts.critical` / `anomaly.detected`
        Kafka->>Gateway: Consume alert/anomaly event
        Gateway->>Op: Gửi cảnh báo đỏ (Real-time Alert Frame)
    end
    deactivate Gateway
```

### Bản vẽ PlantUML:
```plantuml
@startuml
!theme plain
skinparam monochrome true
skinparam shadowing false
skinparam defaultFontName "Courier New"

actor "Kỹ sư vận hành (Dashboard)" as Op
participant "Real-time Gateway (P6)" as Gateway
database "Redis Cache (D4)" as Redis
queue "Hàng đợi Kafka (D1)" as Kafka

autonumber

Op -> Gateway : Thiết lập kết nối WebSocket (ws://... + Access Token)
activate Gateway
Gateway -> Redis : Kiểm tra tính hợp lệ của token
activate Redis
Redis --> Gateway : Token hợp lệ
deactivate Redis
Gateway --> Op : Chấp nhận kết nối (WebSocket Connected)

par Stream Logs
    Kafka -> Gateway : Consume event từ topic "logs.live"
    Gateway -> Op : Đẩy live log xuống Dashboard (qua WebSocket)
else Stream Alerts
    Kafka -> Gateway : Consume event từ topic "alerts.critical" / "anomaly.detected"
    Gateway -> Op : Gửi cảnh báo đỏ (Real-time Alert Frame)
end
deactivate Gateway

@enduml
```

---

## 4. Anomaly Detection (Phát hiện Bất thường)

* **Loại sơ đồ**: Sơ đồ hoạt động/luồng xử lý (Flowchart / Activity Diagram).
* **Mô tả**: Mô tả luồng tự động quét và đánh giá các tín hiệu hệ thống (Metrics từ Prometheus và Logs nghi ngờ từ Worker) để kết luận và tạo báo cáo bất thường.

### Bản vẽ Mermaid:
```mermaid
graph TD
    Start([1. Trigger Quét Anomaly]) --> FetchConfig[2. Đọc cấu hình Metric Sources từ Postgres]
    FetchConfig --> ScrapeProm[3. Thu thập Metrics từ Prometheus]
    FetchConfig --> ReadSignals[4. Đọc Logs nghi ngờ từ Kafka logs.anomaly.signals]
    
    ScrapeProm --> CompareRules{5. So sánh với các Luật Bất thường}
    ReadSignals --> CompareRules
    
    CompareRules -- Không bất thường --> End([6. Kết thúc chu kỳ quét])
    CompareRules -- Phát hiện bất thường --> CreateReport[7. Tạo Anomaly Report trong PostgreSQL]
    
    CreateReport --> WriteKafka[8. Đẩy sự kiện bất thường vào Kafka anomaly.detected]
    WriteKafka --> TriggerAlert{9. Yêu cầu AI phân tích hoặc Gửi Alert?}
    
    TriggerAlert -- Có --> CallAlert[10. Chuyển thông tin sang Phân hệ Cảnh báo]
    TriggerAlert -- Không --> End
    CallAlert --> End
    
    classDef step fill:#ffffff,stroke:#000000,stroke-width:2px,color:#000000;
    classDef decision fill:#ffffff,stroke:#000000,stroke-width:2px,color:#000000;
    class Start,FetchConfig,ScrapeProm,ReadSignals,CreateReport,WriteKafka,CallAlert,End step;
    class CompareRules,TriggerAlert decision;
```

### Bản vẽ PlantUML:
```plantuml
@startuml
!theme plain
skinparam monochrome true
skinparam shadowing false
skinparam defaultFontName "Courier New"

start
:1. Trigger Quét Bất thường (Scheduler định kỳ hoặc Kafka Event);
:2. Đọc cấu hình Metric Sources từ PostgreSQL;
fork
    :3. Thu thập Metrics thực tế từ Prometheus;
fork again
    :4. Đọc các tín hiệu Log nghi ngờ từ Kafka logs.anomaly.signals;
end fork

:5. So sánh dữ liệu với các Luật Bất thường (Rule Evaluation);

if (Phát hiện dấu hiệu bất thường?) then (Có)
    :7. Khởi tạo Anomaly Report lưu vào PostgreSQL (anomaly.anomaly_reports);
    :8. Đẩy sự kiện bất thường vào Kafka topic "anomaly.detected";
    if (Yêu cầu gửi cảnh báo ngay?) then (Có)
        :10. Chuyển tiếp sự kiện sang Phân hệ Cảnh báo;
    else (Không)
    endif
else (Không)
    :6. Cập nhật bộ đếm kiểm tra bình thường trong Redis;
endif

stop
@enduml
```

---

## 5. Alerting & Deduplication (Đánh giá Cảnh báo & Khử trùng lặp)

* **Loại sơ đồ**: Sơ đồ hoạt động (Activity Diagram).
* **Mô tả**: Logic kiểm tra luật cảnh báo và khử trùng lặp (Deduplication) dựa trên bộ đếm Redis nhằm ngăn ngừa ngập lụt cảnh báo (Alert Fatigue).

### Bản vẽ Mermaid:
```mermaid
graph TD
    Start([1. Nhận sự kiện Alert Candidate]) --> FetchRule[2. Truy vấn Luật Cảnh báo từ PostgreSQL/Cache]
    FetchRule --> CheckActiveTime{3. Đang trong khung giờ hoạt động của luật?}
    
    CheckActiveTime -- Không --> Skip([4. Bỏ qua cảnh báo])
    CheckActiveTime -- Có --> BuildKey[5. Tạo Deduplication Key: alert:dedup:rule_id:fingerprint]
    
    BuildKey --> CheckRedis{6. Key đã tồn tại trong Redis?}
    
    CheckRedis -- Có (Đang trong cooldown) --> IncCounter[7. Tăng bộ đếm occurrence_count trong Redis]
    IncCounter --> UpdateAlert[8. Cập nhật last_seen_at và bộ đếm vào Postgres]
    UpdateAlert --> Skip
    
    CheckRedis -- Chưa có (Cảnh báo mới/Cooldown hết) --> QueryCH[9. Truy vấn lấy Log mẫu đại diện từ ClickHouse]
    QueryCH --> SaveAlert[10. Lưu Cảnh báo mới vào Postgres với status = OPEN]
    SaveAlert --> SetRedis[11. Lưu Deduplication Key vào Redis kèm thời gian hết hạn TTL]
    SetRedis --> SendNoti[12. Gửi thông báo đến Telegram & Stream Real-time]
    SendNoti --> End([13. Kết thúc])
    
    classDef step fill:#ffffff,stroke:#000000,stroke-width:2px,color:#000000;
    classDef decision fill:#ffffff,stroke:#000000,stroke-width:2px,color:#000000;
    class Start,FetchRule,BuildKey,IncCounter,UpdateAlert,QueryCH,SaveAlert,SetRedis,SendNoti,End,Skip step;
    class CheckActiveTime,CheckRedis decision;
```

### Bản vẽ PlantUML:
```plantuml
@startuml
!theme plain
skinparam monochrome true
skinparam shadowing false
skinparam defaultFontName "Courier New"

start
:1. Nhận sự kiện Alert Candidate (từ Kafka/Anomaly);
:2. Truy vấn Luật Cảnh báo tương ứng từ PostgreSQL (alerting.alert_rules);

if (Khung giờ hiện tại nằm ngoài active time window?) then (Đúng)
    :4. Bỏ qua sự kiện cảnh báo;
    stop
else (Sai)
    :5. Tạo khóa khử trùng: "alert:dedup:rule_id:fingerprint";
    
    if (Khóa "alert:dedup..." đã tồn tại trong Redis?) then (Có - Đang trong Cooldown)
        :7. Tăng bộ đếm occurrence_count trong Redis;
        :8. Cập nhật mốc "last_seen_at" và bộ đếm của Alert hiện tại trong PostgreSQL;
        :4. Bỏ qua việc gửi thông báo mới (Tránh ngập lụt);
        stop
    else (Không - Cảnh báo mới hoặc Cooldown đã hết hạn)
        :9. Truy vấn log mẫu đại diện (Samples) từ ClickHouse;
        :10. Tạo bản ghi Alert mới trong PostgreSQL (alerting.alerts) với trạng thái OPEN;
        :11. Thiết lập khóa "alert:dedup..." trong Redis với thời gian sống (TTL = cooldown_seconds);
        :12. Phân phối cảnh báo đến Telegram API và đẩy realtime lên Web Dashboard;
    endif
endif

stop
@enduml
```

---

## 6. Incident Management & AI Assistant (Quản lý Sự cố & Trợ lý AI)

* **Loại sơ đồ**: Sơ đồ tuần tự (Sequence Diagram).
* **Mô tả**: Quy trình điều tra sự cố bằng công nghệ AI, tự động thu thập thông tin logs, metrics và cảnh báo làm bằng chứng để AI phân tích nguyên nhân gốc rễ.

### Bản vẽ Mermaid:
```mermaid
sequenceDiagram
    autonumber
    actor Op as Kỹ sư vận hành (Dashboard)
    participant Service as Incident Service (P5)
    participant PG as PostgreSQL (D3)
    participant CH as ClickHouse (D2)
    participant AI as Nhà cung cấp AI (AI Provider)

    Op->>Service: Yêu cầu mở Sự cố (Tạo Incident từ Alert/Anomaly)
    activate Service
    Service->>PG: Ghi nhận sự cố mới vào bảng `incident.incidents`
    Service->>PG: Đọc các Alerts & Anomalies liên quan trong khung giờ
    PG-->>Service: Trả về danh sách cảnh báo & bất thường làm chứng cứ
    Service->>CH: Truy vấn log chi tiết của ứng dụng trong khung giờ (Time-window)
    CH-->>Service: Trả về log chi tiết làm bằng chứng
    
    Note over Service: Đóng gói bằng chứng thành Evidence Bundle (JSON)
    Service->>AI: Gửi Evidence Bundle + Yêu cầu phân tích sự cố (Prompt)
    activate AI
    Note over AI: AI phân tích sự tương quan giữa logs,<br/>metrics, cảnh báo và lỗi hệ thống
    AI-->>Service: Trả về phân tích nguyên nhân gốc rễ & đề xuất khắc phục
    deactivate AI
    
    Service->>PG: Lưu phân tích AI vào bảng `incident.incident_ai_analyses`
    Service->>PG: Ghi nhận sự kiện hoạt động vào dòng thời gian `incident.incident_timeline_events`
    Service-->>Op: Trả về kết quả phân tích AI & Giải pháp xử lý sự cố trực quan
    deactivate Service
```

### Bản vẽ PlantUML:
```plantuml
@startuml
!theme plain
skinparam monochrome true
skinparam shadowing false
skinparam defaultFontName "Courier New"

actor "Kỹ sư vận hành (Dashboard)" as Op
participant "Incident Service (P5)" as Service
database "PostgreSQL (D3)" as PG
database "ClickHouse (D2)" as CH
participant "Nhà cung cấp AI" as AI

autonumber

Op -> Service : Yêu cầu mở Sự cố (Tạo Incident từ Cảnh báo/Bất thường)
activate Service
Service -> PG : Lưu thông tin sự cố vào bảng "incident.incidents"
Service -> PG : Đọc các Alerts & Anomalies liên quan trong khung giờ xảy ra sự cố
activate PG
PG --> Service : Trả về thông tin alerts & anomalies làm chứng cứ
deactivate PG

Service -> CH : Truy vấn log ứng dụng chi tiết trong khung giờ (Time-window)
activate CH
CH --> Service : Trả về dữ liệu logs làm bằng chứng
deactivate CH

Note over Service : Đóng gói bằng chứng thành Evidence Bundle (JSON)
Service -> AI : Gửi Evidence Bundle + Yêu cầu phân tích sự cố (Prompt API)
activate AI
Note over AI : Phân tích tương quan sự cố giữa logs,\nmetrics và cảnh báo lỗi hệ thống
AI --> Service : Trả về nguyên nhân gốc rễ (Root Cause) & đề xuất giải quyết
deactivate AI

Service -> PG : Ghi nhận kết quả phân tích AI vào bảng "incident.incident_ai_analyses"
Service -> PG : Ghi nhận log hoạt động vào dòng thời gian timeline_events
Service --> Op : Trả về kết quả phân tích AI & các hành động khắc phục sự cố
deactivate Service

@enduml
```

---

## 7. Data Retention (Quản lý thời hạn và dọn dẹp log)

* **Loại sơ đồ**: Sơ đồ hoạt động (Activity Diagram).
* **Mô tả**: Cơ chế chạy tự động hàng ngày quét cấu hình chính sách lưu giữ để xóa sạch các log quá hạn phân vùng ClickHouse nhằm giải phóng đĩa cứng.

### Bản vẽ Mermaid:
```mermaid
graph TD
    Start([1. Trình lập lịch kích hoạt Job dọn dẹp hàng ngày]) --> FetchPolicies[2. Đọc các chính sách lưu giữ log đã bật từ Postgres]
    FetchPolicies --> LoopStart{3. Lặp qua từng chính sách}
    
    LoopStart -- Hết chính sách --> End([4. Kết thúc Job])
    LoopStart -- Còn chính sách --> LogRun[5. Ghi nhận phiên chạy STATUS = RUNNING vào Postgres]
    
    LogRun --> CalcTime[6. Tính toán mốc thời gian hết hạn: Hiện tại - retention_days]
    CalcTime --> FindPartitions[7. Tìm các phân vùng ClickHouse có log_timestamp cũ hơn mốc hết hạn]
    
    FindPartitions --> CheckParts{8. Có phân vùng ClickHouse cũ hết hạn hoàn toàn?}
    
    CheckParts -- Có --> DropPartition[9. Chạy lệnh: ALTER TABLE processed_logs DROP PARTITION]
    DropPartition --> UpdateRun[10. Cập nhật phiên chạy STATUS = SUCCESS và ghi nhận số dòng bị xóa]
    
    CheckParts -- Không --> DeleteRows[11. Chạy lệnh xóa bất đồng bộ: ALTER TABLE processed_logs DELETE WHERE ...]
    DeleteRows --> UpdateRun
    
    UpdateRun --> LoopStart
    
    classDef step fill:#ffffff,stroke:#000000,stroke-width:2px,color:#000000;
    classDef decision fill:#ffffff,stroke:#000000,stroke-width:2px,color:#000000;
    class Start,FetchPolicies,LogRun,CalcTime,FindPartitions,DropPartition,DeleteRows,UpdateRun,End step;
    class LoopStart,CheckParts decision;
```

### Bản vẽ PlantUML:
```plantuml
@startuml
!theme plain
skinparam monochrome true
skinparam shadowing false
skinparam defaultFontName "Courier New"

start
:1. Trigger Job dọn dẹp tự động (Scheduler kích hoạt hàng ngày);
:2. Đọc các chính sách lưu trữ log đang hoạt động từ "retention.retention_policies";

while (Còn chính sách lưu trữ chưa xử lý?) is (Còn)
    :5. Tạo bản ghi lượt chạy mới trong PostgreSQL (retention.retention_runs) với trạng thái RUNNING;
    :6. Tính toán mốc thời gian hết hạn = Current Time - policy.retention_days;
    :7. Truy vấn cấu trúc bảng ClickHouse để kiểm tra các Phân vùng (Partition) cũ;
    
    if (Có Phân vùng chứa toàn bộ log nằm trước mốc thời gian hết hạn?) then (Có)
        :9. Thực thi lệnh xóa nhanh phân vùng đĩa:\n"ALTER TABLE processed_logs DROP PARTITION 'partition_name'";
        :Ghi nhận affected_rows = kích thước phân vùng xóa;
    else (Không - Log hết hạn lẻ tẻ trong phân vùng hiện tại)
        :11. Thực thi câu lệnh xóa bất đồng bộ ClickHouse:\n"ALTER TABLE processed_logs DELETE WHERE log_timestamp < expiry_timestamp";
        :Ghi nhận affected_rows = ước tính số dòng bị xóa;
    endif
    
    :10. Cập nhật bản ghi "retention_runs" sang trạng thái SUCCESS,\nghi nhận affected_rows và thời điểm kết thúc;
endwhile (Hết)

:4. Ghi nhận hoàn thành chu trình dọn dẹp hệ thống;
stop
@enduml
```

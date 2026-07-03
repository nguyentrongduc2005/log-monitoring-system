<!-- Improved compatibility of back to top link: See: https://github.com/othneildrew/Best-README-Template/pull/73 -->

<a id="readme-top"></a>

<!-- PROJECT SHIELDS -->

[![MIT License][license-shield]][license-url]
[![Java][java-shield]][Java-url]
[![Spring Boot][spring-shield]][Spring-Boot-url]
[![React][react-shield]][React-url]

<!-- PROJECT LOGO -->
<br />
<div align="center">
  <a href="https://github.com/nguyentrongduc2005/log-monitoring-system">
  <img src="docs/assets/logo-transparent.png" alt="Logo" width="120">
</a>

  <h3 align="center">Log Monitoring System</h3>

  <p align="center">
    Nền tảng thu thập, chuẩn hóa, lưu trữ và phân tích log theo thời gian thực.
    <br />
    <a href="https://github.com/nguyentrongduc2005/log-monitoring-system"><strong>Explore the repository »</strong></a>
    <br />
    <br />
    <a href="https://github.com/nguyentrongduc2005/log-monitoring-system/issues/new?labels=bug">Report Bug</a>
    &middot;
    <a href="https://github.com/nguyentrongduc2005/log-monitoring-system/issues/new?labels=enhancement">Request Feature</a>
  </p>
</div>

<!-- TABLE OF CONTENTS -->
<details>
  <summary>Table of Contents</summary>
  <ol>
    <li>
      <a href="#about-the-project">About The Project</a>
      <ul>
        <li><a href="#architecture">Architecture</a></li>
        <li><a href="#built-with">Built With</a></li>
        <li><a href="#project-structure">Project Structure</a></li>
      </ul>
    </li>
    <li>
      <a href="#getting-started">Getting Started</a>
      <ul>
        <li><a href="#prerequisites">Prerequisites</a></li>
        <li><a href="#installation">Installation</a></li>
      </ul>
    </li>
    <li><a href="#usage">Usage</a></li>
    <li><a href="#api-documentation">API Documentation</a></li>
    <li><a href="#roadmap">Roadmap</a></li>
    <li><a href="#license">License</a></li>
  </ol>
</details>

<!-- ABOUT THE PROJECT -->

## About The Project

Log Monitoring System giúp tập trung log từ nhiều ứng dụng vào một nền tảng
duy nhất. Hệ thống tiếp nhận log qua API, sử dụng message queue để cân bằng tải,
chuẩn hóa dữ liệu bằng worker và lưu trữ log phục vụ tìm kiếm, giám sát và phân
tích.

Các chức năng chính:

- Tiếp nhận log tốc độ cao từ nhiều ứng dụng.
- Chuẩn hóa `applicationName`, `level`, `message`, `timestamp` và `traceId`.
- Tìm kiếm và lọc log theo ứng dụng, cấp độ lỗi và thời gian.
- Hiển thị live log trên dashboard mà không cần reload trang.
- Phát hiện log `ERROR` hoặc `CRITICAL` và gửi cảnh báo thời gian thực.
- Chống cảnh báo trùng lặp bằng Redis để hạn chế alert fatigue.
- Phân quyền kỹ sư theo ứng dụng và cung cấp quyền quản trị alert rule.
- Điểm cộng: analytics sức khỏe ứng dụng, retention policy và phân tích AI.

> [!NOTE]
> Dự án đang trong giai đoạn phát triển. Frontend dashboard, Dockerfile và hạ
> tầng Docker Compose đã được thiết lập. Backend được tổ chức theo Spring Boot
> Modular Monolith với các module `identity`, `ingestion`, `processing`,
> `realtime`, `alerting`, `anomaly`, `incident`, `analytics` và `retention`.
> `ingestion` nhận log và publish Kafka `logs.raw`; các module còn lại xử lý
> chuẩn hóa, realtime, cảnh báo, anomaly, incident AI, analytics và retention.

<p align="right">(<a href="#readme-top">back to top</a>)</p>

### Architecture

```mermaid
flowchart LR
    SOURCE[External Applications] -->|POST logs| INGEST[Logs Ingestion API]
    INGEST --> RAW[Kafka: logs.raw]
    RAW --> WORKER[Logs Processing Worker]
    WORKER --> CH[(ClickHouse)]
    WORKER --> LIVE[Kafka: logs.live]
    WORKER -->|ERROR / CRITICAL| ALERT[Kafka: alerts.critical]
    WORKER --> SIGNAL[Kafka: logs.anomaly.signals]
    SIGNAL --> ANOMALY[Anomaly Log Detector]

    BACKEND[Platform API] -->|/internal/prometheus/targets| PROM[Prometheus]
    PROM -->|scrape| EXPORTERS[Application / Node Exporters]
    ANOMALY -->|query metrics| PROM
    ANOMALY --> ANOMALYDB[(PostgreSQL Anomaly Reports)]
    ANOMALY --> ANOMALY_EVENT[Kafka: anomaly.detected]
    ANOMALY_EVENT --> ALERT

    ALERT --> DEDUP[Priority Alert Consumer]
    DEDUP --> REDIS[(Redis Deduplication)]
    REDIS --> ALERTDB[(PostgreSQL Alert Occurrences)]
    ALERTDB --> TELEGRAM[Telegram]
    ALERTDB --> ALERTEVENT[Realtime Alert Event]
    ALERTEVENT --> WS[WebSocket]
    LIVE --> WS
    WS --> UI[React Dashboard]
    UI -->|Historical Search| QUERY[Logs Query API]
    QUERY --> CH
    UI -->|Management API| BACKEND
    BACKEND --> PG[(PostgreSQL Identity / Alerting)]
```

Worker chỉ commit offset `logs.raw` sau khi ClickHouse và các Kafka event
downstream bắt buộc đều được acknowledge. `alerts.critical` có consumer group
và tài nguyên xử lý riêng; Kafka không tự cung cấp message priority.

Anomaly được xử lý từ hai nguồn: log signal qua Kafka `logs.anomaly.signals` và
metric health qua Prometheus. Backend cung cấp endpoint
`/internal/prometheus/targets` để Prometheus discovery target theo application,
sau đó module anomaly query Prometheus, tạo anomaly report và publish Kafka
`anomaly.detected` cho alerting.

Module `ingestion` sở hữu API nhận log và event contract `RawLogReceivedEvent`
được publish vào Kafka `logs.raw`. `processing`, `realtime`, `alerting`,
`anomaly`, `incident`, `analytics` và `retention` là các module riêng trong cùng
backend để tách rõ trách nhiệm và dễ scale/tách service về sau nếu cần.

HTTP/WebSocket controller nằm ở top-level `api`; chúng chỉ chuyển request đến
public facade của module. Business logic, persistence và Kafka/ClickHouse
adapter vẫn nằm bên trong module sở hữu.

| Component   | Responsibility                                                        |
| ----------- | --------------------------------------------------------------------- |
| PostgreSQL  | Identity, alert rule, occurrence, anomaly report, incident, retention |
| ClickHouse  | Log chuẩn hóa, search, analytics và retention                         |
| Kafka       | Buffer log thô và truyền event giữa ingestion/processing/alerting     |
| Redis       | Idempotency, alert dedup, anomaly metric snapshot và dữ liệu TTL      |
| Prometheus  | Scrape metric target và cung cấp metric cho anomaly detection         |
| Anomaly     | Phát hiện bất thường từ log signal và Prometheus metric               |
| WebSocket   | Truyền live log, alert và anomaly notification tới React dashboard    |
| Telegram    | Kênh gửi cảnh báo và anomaly/AI report notification                   |
| Incident AI | Phân tích anomaly/incident evidence và đề xuất hướng xử lý            |

<p align="right">(<a href="#readme-top">back to top</a>)</p>

### Built With

#### Backend

- [![Java][Java]][Java-url]
- [![Spring Boot][Spring-Boot]][Spring-Boot-url]
- [![PostgreSQL][PostgreSQL]][PostgreSQL-url]
- [![ClickHouse][ClickHouse]][ClickHouse-url]
- [![Apache Kafka][Kafka]][Kafka-url]
- [![Redis][Redis]][Redis-url]

#### Frontend

- [![React][React.js]][React-url]
- [![TypeScript][TypeScript]][TypeScript-url]
- [![Vite][Vite]][Vite-url]
- [![Nginx][Nginx]][Nginx-url]

#### Infrastructure

- [![Docker][Docker]][Docker-url]
- [![Prometheus][Prometheus]][Prometheus-url]

<p align="right">(<a href="#readme-top">back to top</a>)</p>

### Project Structure

```text
log-monitoring-system/
├── apps/
│   ├── backend/                 # Spring Boot API và worker
│   │   ├── src/main/java/
│   │   ├── src/main/resources/
│   │   ├── Dockerfile
│   │   └── pom.xml
│   └── frontend/                # React monitoring dashboard
│       ├── src/
│       ├── Dockerfile
│       ├── nginx.conf
│       └── package.json
├── docs/
│   ├── architecture.md          # Tài liệu kiến trúc dự án
│   ├── api.md                   # API document chuẩn
│   ├── database.md              # Database document và ERD
│   ├── api/openapi.json         # OpenAPI được export
│   └── diagram/                 # Sơ đồ Mermaid tách theo từng luồng
├── scripts/
│   └── generate-api-types.sh    # Sinh TypeScript type từ OpenAPI
├── compose.yml                  # Hạ tầng local và observability demo
├── Makefile                     # Các lệnh phát triển thường dùng
├── .env.example
└── README.md
```

<p align="right">(<a href="#readme-top">back to top</a>)</p>

<!-- GETTING STARTED -->

## Getting Started

Thực hiện các bước dưới đây để chạy dự án trên máy local.

### Prerequisites

- Git
- Java 21
- Node.js 22 trở lên
- npm
- Docker và Docker Compose
- GNU Make, tùy chọn nhưng được khuyến nghị

Kiểm tra phiên bản:

```sh
java --version
node --version
npm --version
docker compose version
```

### Installation

1. Clone repository
   ```sh
   git clone https://github.com/nguyentrongduc2005/log-monitoring-system.git
   cd log-monitoring-system
   ```
2. Tạo file environment từ sample
   ```sh
   cp .env.example .env
   cp apps/backend/.env.example apps/backend/.env
   cp apps/frontend/.env.example apps/frontend/.env.local
   ```
3. Kiểm tra các cấu hình bắt buộc trong `.env` và `apps/backend/.env`
   ```env
   POSTGRES_PASSWORD=your-secure-password
   CLICKHOUSE_PASSWORD=your-secure-password
   JWT_SECRET=your-long-random-secret
   ```
4. Khởi động toàn bộ hạ tầng local
   ```sh
   make infra-up
   ```
   Nếu không dùng Make:
   ```sh
   docker compose up -d
   ```
5. Cài đặt frontend dependencies
   ```sh
   cd apps/frontend
   npm ci
   cd ../..
   ```
6. Chạy backend
   ```sh
   make backend
   ```
7. Mở terminal khác và chạy frontend
   ```sh
   make frontend
   ```

Backend chạy tại `http://localhost:8080` và frontend chạy tại
`http://localhost:5173`.

Không commit `.env`, credential, token hoặc API key vào repository.

<p align="right">(<a href="#readme-top">back to top</a>)</p>

<!-- USAGE EXAMPLES -->

## Usage

### Development commands

| Command           | Description                                   |
| ----------------- | --------------------------------------------- |
| `make infra-up`   | Khởi động toàn bộ service trong `compose.yml` |
| `make infra-down` | Dừng các container                            |
| `make backend`    | Chạy Spring Boot backend                      |
| `make frontend`   | Chạy Vite development server                  |
| `make api`        | Sinh TypeScript API type từ OpenAPI           |
| `make build`      | Build backend và frontend                     |
| `make test`       | Chạy backend tests                            |
| `make lint`       | Chạy frontend ESLint                          |
| `make clean`      | Xóa output build                              |

### Build Docker images

```sh
docker build -t log-monitoring-backend ./apps/backend
docker build -t log-monitoring-frontend ./apps/frontend
```

`compose.yml` hiện quản lý các service hạ tầng và observability local gồm
PostgreSQL, ClickHouse, Redis, Kafka, Kafka UI, Prometheus và node-exporter demo.
Backend và frontend đã có Dockerfile, nhưng service trong Compose đang được
comment để workflow dev ưu tiên chạy bằng `make backend` và `make frontend`.

### Local service ports

| Service           | Port   |
| ----------------- | ------ |
| Backend           | `8080` |
| Frontend          | `5173` |
| PostgreSQL        | `5432` |
| ClickHouse HTTP   | `8123` |
| ClickHouse Native | `9000` |
| Kafka             | `9094` |
| Kafka UI          | `8081` |
| Redis             | `6379` |
| Prometheus        | `9090` |

<p align="right">(<a href="#readme-top">back to top</a>)</p>

<!-- API DOCUMENTATION -->

## API Documentation

Khi backend đang chạy:

```text
Swagger UI:  http://localhost:8080/swagger-ui
OpenAPI JSON: http://localhost:8080/v3/api-docs
```

Sinh TypeScript type cho frontend:

```sh
make api
```

```text
Spring Controller/DTO
        ↓
    /v3/api-docs
        ↓
docs/api/openapi.json
        ↓
apps/frontend/src/api/generated/api-types.ts
```

Không chỉnh sửa file generated bằng tay.

Tài liệu API chuẩn được mô tả tại [`docs/api.md`](docs/api.md). OpenAPI export
nằm tại [`docs/api/openapi.json`](docs/api/openapi.json). Kiến trúc, database và
sơ đồ chi tiết lần lượt nằm ở [`docs/architecture.md`](docs/architecture.md),
[`docs/database.md`](docs/database.md) và [`docs/diagram/README.md`](docs/diagram/README.md).

<p align="right">(<a href="#readme-top">back to top</a>)</p>

<!-- ROADMAP -->

## Roadmap

- [x] Khởi tạo monorepo Spring Boot và React
- [x] Thiết lập Vite, routing, query provider và API client
- [x] Thiết lập Docker Compose cho PostgreSQL, ClickHouse, Kafka và Redis
- [x] Tạo backend và frontend Dockerfile
- [x] Thiết lập OpenAPI và script generate TypeScript type
- [x] Thiết kế PostgreSQL schema và Flyway migrations
- [x] Thiết kế ClickHouse `processed_logs` table
- [x] Xây dựng single và batch ingestion API
- [x] Đẩy log thô vào Kafka topic `logs.raw`
- [x] Xây dựng processing worker, ClickHouse writer và Kafka event publishers
- [x] Hiển thị live log qua WebSocket
- [x] Xây dựng alert rule, alert occurrence và dedup bằng Redis
- [x] Tích hợp Telegram notification
- [x] Thêm authentication, API key và application-level permissions
- [x] Thêm Prometheus target discovery và metric anomaly pipeline
- [x] Thêm anomaly report, anomaly alert và AI analysis workflow nền tảng
- [x] Cập nhật architecture, API, database và Mermaid diagram docs
- [x] Đồng bộ `.env.example`, backend env sample và frontend env sample
- [x] Hoàn thiện retention policy theo yêu cầu cuối
- [x] Hoàn thiện dashboard analytics theo yêu cầu demo cuối
- [x] Hoàn thiện API tìm kiếm và phân trang log nếu cần theo contract cuối
- [x] Viết k6 scenario cho demo 500 log trong 2 giây
- [x] Thêm backend và frontend vào Docker Compose

See the [open issues](https://github.com/nguyentrongduc2005/log-monitoring-system/issues)
for a full list of proposed features and known issues.

<p align="right">(<a href="#readme-top">back to top</a>)</p>

<!-- LICENSE -->

## License

Distributed under the MIT License. See `LICENSE` for more information.

<p align="right">(<a href="#readme-top">back to top</a>)</p>

<!-- MARKDOWN LINKS & IMAGES -->
<!-- https://www.markdownguide.org/basic-syntax/#reference-style-links -->

[license-shield]: https://img.shields.io/badge/License-MIT-blue.svg?style=for-the-badge
[license-url]: LICENSE
[java-shield]: https://img.shields.io/badge/Java-21-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white
[spring-shield]: https://img.shields.io/badge/Spring_Boot-3.5-6DB33F?style=for-the-badge&logo=springboot&logoColor=white
[react-shield]: https://img.shields.io/badge/React-19-20232A?style=for-the-badge&logo=react&logoColor=61DAFB
[Java]: https://img.shields.io/badge/Java_21-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white
[Java-url]: https://openjdk.org/
[Spring-Boot]: https://img.shields.io/badge/Spring_Boot_3.5-6DB33F?style=for-the-badge&logo=springboot&logoColor=white
[Spring-Boot-url]: https://spring.io/projects/spring-boot
[React.js]: https://img.shields.io/badge/React_19-20232A?style=for-the-badge&logo=react&logoColor=61DAFB
[React-url]: https://react.dev/
[TypeScript]: https://img.shields.io/badge/TypeScript_5-3178C6?style=for-the-badge&logo=typescript&logoColor=white
[TypeScript-url]: https://www.typescriptlang.org/
[Vite]: https://img.shields.io/badge/Vite_8-646CFF?style=for-the-badge&logo=vite&logoColor=white
[Vite-url]: https://vite.dev/
[PostgreSQL]: https://img.shields.io/badge/PostgreSQL_17-4169E1?style=for-the-badge&logo=postgresql&logoColor=white
[PostgreSQL-url]: https://www.postgresql.org/
[ClickHouse]: https://img.shields.io/badge/ClickHouse_25.3-FFCC01?style=for-the-badge&logo=clickhouse&logoColor=black
[ClickHouse-url]: https://clickhouse.com/
[Kafka]: https://img.shields.io/badge/Apache_Kafka_4-231F20?style=for-the-badge&logo=apachekafka&logoColor=white
[Kafka-url]: https://kafka.apache.org/
[Redis]: https://img.shields.io/badge/Redis_8-DC382D?style=for-the-badge&logo=redis&logoColor=white
[Redis-url]: https://redis.io/
[Nginx]: https://img.shields.io/badge/Nginx_1.27-009639?style=for-the-badge&logo=nginx&logoColor=white
[Nginx-url]: https://nginx.org/
[Docker]: https://img.shields.io/badge/Docker-2496ED?style=for-the-badge&logo=docker&logoColor=white
[Docker-url]: https://www.docker.com/
[Prometheus]: https://img.shields.io/badge/Prometheus-E6522C?style=for-the-badge&logo=prometheus&logoColor=white
[Prometheus-url]: https://prometheus.io/

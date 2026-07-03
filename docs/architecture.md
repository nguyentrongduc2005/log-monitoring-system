# Architecture

## Overview

Log Monitoring System là một ứng dụng **monorepo** gồm hai phần chính:

| Phần | Công nghệ | Vai trò |
| --- | --- | --- |
| Backend | Spring Boot 3, Java 21, Maven | Cung cấp REST API, WebSocket, xử lý log, alerting, anomaly, incident và retention |
| Frontend | React, TypeScript, Vite | Dashboard vận hành, quản trị users/applications/rules/incidents và realtime UI |

Ở backend, dự án dùng kiến trúc **modular monolith**: toàn bộ backend chạy trong một Spring Boot application, nhưng code được chia thành các module nghiệp vụ độc lập dưới `com.vdt.log_monitoring.modules`. Các module không phải microservices riêng; chúng cùng deploy trong một process, dùng chung Spring context, nhưng có ranh giới package và public facade rõ ràng.

Hệ thống cũng dùng mô hình **event-driven pipeline** cho luồng log realtime: API ingestion nhận log nhanh, publish event vào Kafka, sau đó các consumer xử lý bất đồng bộ, ghi ClickHouse, tạo alert/anomaly và đẩy dữ liệu realtime qua WebSocket.

## Architectural Style

| Kiểu kiến trúc | Áp dụng trong dự án |
| --- | --- |
| Monorepo | `apps/backend`, `apps/frontend`, `docs`, `demo`, `compose.yml` nằm cùng repository |
| Modular monolith | Backend chia theo module nghiệp vụ: `identity`, `ingestion`, `processing`, `realtime`, `alerting`, `anomaly`, `incident`, `analytics`, `retention` |
| Layered backend | Controller layer gọi facade/service, internal module xử lý nghiệp vụ, repository/infrastructure truy cập storage hoặc external systems |
| Event-driven processing | Kafka topics tách ingestion khỏi processing/realtime/alerting/anomaly |
| SPA frontend | React/Vite frontend gọi REST API và kết nối STOMP WebSocket `/ws` |
| Polyglot persistence | PostgreSQL cho state nghiệp vụ, ClickHouse cho processed logs, Redis cho cache/idempotency/dedup/runtime state |

## Repository Structure

```text
.
├── apps/
│   ├── backend/       # Spring Boot backend
│   └── frontend/      # React/Vite frontend
├── docs/              # Official architecture, API, database docs
├── demo/              # Demo scripts and sample log senders
├── compose.yml        # Local infrastructure: PostgreSQL, ClickHouse, Redis, Kafka, Prometheus
├── prometheus.yml     # Prometheus config
└── Makefile           # Common local commands
```

## Backend Structure

Backend root package là `com.vdt.log_monitoring`.

```text
apps/backend/src/main/java/com/vdt/log_monitoring/
├── LogMonitoringApplication.java
├── api/       # HTTP/WebSocket adapters: controllers, request/response DTOs, exception handlers
├── modules/   # Business modules, public module contracts and internal implementation
└── shared/    # Shared DTOs, security, exceptions, event helpers
```

### Backend Layers

| Layer | Package | Trách nhiệm |
| --- | --- | --- |
| Application entry | `LogMonitoringApplication.java` | Bootstrap Spring Boot application |
| API adapter layer | `api/<domain>` | Nhận REST/WebSocket request, validate DTO, gọi module facade/service, map response/error |
| Module public API | `modules/<module>/api` | Facade interfaces, domain events, public exceptions mà module khác được dùng |
| Module internals | `modules/<module>/internal` | Service, entity, repository, worker, Kafka producer/consumer, cache, external clients |
| Shared infrastructure | `shared` | `ApiResponse`, global exception handling, JWT/security primitives, shared event helpers |
| Configuration/resources | `src/main/resources` | `application.yaml`, profile config, Flyway migrations, ClickHouse migrations |

Controller không trực tiếp sở hữu nghiệp vụ dài hạn. Ví dụ `LogIngestionController` nhận request, sau đó gọi `LogIngestionFacade`; `LogIngestionService` mới xử lý API key, sanitize, idempotency và publish raw log event.

### Backend Modules

| Module | Trách nhiệm chính | Storage/Integration chính |
| --- | --- | --- |
| `identity` | Users, roles, applications, application access, API keys, metric sources, auth token lifecycle | PostgreSQL schema `identity`, Redis API key/JWT/refresh token cache |
| `ingestion` | Nhận raw logs, kiểm tra API key, sanitize payload, idempotency, publish raw events | Kafka `logs.raw`, Redis idempotency |
| `processing` | Consume raw logs, parse/normalize/enrich/fingerprint, ghi processed logs, publish downstream events | ClickHouse `processed_logs`, Kafka `logs.live`, `alerts.critical`, `logs.anomaly.signals` |
| `realtime` | Consume realtime events và gửi STOMP messages tới clients đã authorize | Kafka, WebSocket `/ws`, identity access checks |
| `alerting` | Alert rules, alert lifecycle, chat rooms, notification delivery, dedup/threshold evaluation | PostgreSQL schema `alerting`, Redis caches, Telegram, WebSocket |
| `anomaly` | Log/metric anomaly detection, anomaly reports, anomaly detected events | PostgreSQL schema `anomaly`, Kafka, Redis, Prometheus |
| `incident` | Incident lifecycle, evidence collection, AI investigation, timeline | PostgreSQL schema `incident`, ClickHouse evidence reads, AI provider |
| `analytics` | Dashboard overview metrics and projections | ClickHouse reads, anomaly/ingestion data |
| `retention` | Retention policies and scheduled deletion of old ClickHouse logs | PostgreSQL schema `retention`, ClickHouse deletes |

## Frontend Structure

Frontend là React/Vite SPA trong `apps/frontend`.

```text
apps/frontend/src/
├── app/          # App shell, providers, router
├── api/          # API client and generated OpenAPI types
├── features/     # Feature-first pages and adapters
├── shared/       # Shared layouts, UI components, hooks, utilities
├── styles/       # Global styles
└── test/         # Frontend test setup
```

Frontend chia theo feature thay vì chia thuần theo technical layer. Các feature chính gồm `dashboard`, `live-logs`, `alerts`, `alert-rules`, `applications`, `user-access`, `incidents`, `retention`, `profile`, `auth`, `application-health`, `log-search`, và `notification-channels`.

Frontend gọi REST API qua `axios`/API adapters, dùng React Query cho server state, React Router cho navigation, và `@stomp/stompjs` để nhận realtime messages từ backend WebSocket.

## Runtime Infrastructure

Local runtime được mô tả trong `compose.yml`.

| Component | Vai trò |
| --- | --- |
| Backend | Spring Boot app, mặc định port `8080` |
| Frontend | Vite dev app hoặc Nginx image khi build Docker |
| PostgreSQL | Transactional data cho identity, alerting, anomaly, incident, retention |
| ClickHouse | Analytical storage cho processed logs |
| Redis | JWT blacklist, refresh tokens, API key verification cache, ingestion idempotency, alert/anomaly runtime state |
| Kafka | Event backbone giữa ingestion, processing, realtime, alerting và anomaly |
| Kafka UI | Local topic inspection |
| Prometheus | Metric scraping cho anomaly/health use cases |
| Telegram API | External notification channel cho alerting |
| AI provider | Gemini-compatible incident/anomaly analysis client |

## Storage Architecture

| Storage | Dữ liệu chính | Lý do dùng |
| --- | --- | --- |
| PostgreSQL | Users, applications, access grants, API keys metadata, alert rules/alerts, anomaly reports, incidents, retention policies/runs | Quan hệ dữ liệu rõ, transaction và constraint |
| ClickHouse | `processed_logs` | Ghi/đọc log khối lượng lớn, query analytics theo application/time/level |
| Redis | Cache/token/idempotency/dedup/runtime indexes | Truy cập nhanh, TTL, atomic counters |
| Kafka | Raw log events, live log events, alert candidates, anomaly signals, DLT topics | Tách API nhận log khỏi xử lý bất đồng bộ |

## Boundaries and Ownership

- `api/*` là adapter layer. Package này expose REST/WebSocket contract, không sở hữu storage schema.
- `modules/<module>/api` là public contract của module. Module khác nên dùng facade/event/exception ở đây thay vì gọi internal class.
- `modules/<module>/internal` là implementation detail của module đó: service, repository, entity, Kafka handler, cache, client.
- `shared/*` chỉ chứa hạ tầng dùng chung, không nên chứa nghiệp vụ riêng của một module.
- PostgreSQL schema được chia theo module owner: `identity`, `alerting`, `anomaly`, `incident`, `retention`.
- `processing` sở hữu ghi ClickHouse `processed_logs`; `analytics`, `incident`, và `retention` đọc/xóa qua repository riêng.
- Cross-module synchronous calls đi qua facade như `IdentityFacade`, `ApplicationAccessFacade`, `AlertingFacade`, `AnomalyFacade`, `IncidentFacade`, `RetentionFacade`.
- Cross-module asynchronous flow đi qua Kafka events.

## Main Runtime Flows

### Log Ingestion And Processing

1. External application gọi `POST /api/v1/logs` hoặc `/api/v1/logs/batch` với `X-API-Key`.
2. API adapter gọi ingestion facade.
3. Ingestion kiểm tra API key/application, sanitize raw log và kiểm tra optional `Idempotency-Key` trong Redis.
4. Ingestion publish `RawLogReceivedEvent` vào Kafka topic `logs.raw`.
5. Processing consume `logs.raw`, parse/normalize/enrich/fingerprint log.
6. Processing ghi log đã xử lý vào ClickHouse `processed_logs`.
7. Processing publish downstream events cho realtime, alerting và anomaly.

### Realtime UI

1. Frontend mở STOMP WebSocket tại `/ws`.
2. Client gửi JWT trong frame `CONNECT`.
3. `RealtimeWebSocketSecurityInterceptor` validate JWT và kiểm tra blacklist nếu Redis khả dụng.
4. Client subscribe topic theo application, ví dụ `/topic/applications/{applicationId}/logs`.
5. Realtime module consume Kafka events và gửi message tới topic tương ứng.

### Alerting And Incident

1. Alert rules được cấu hình qua admin API.
2. Processing publish critical/error candidates vào `alerts.critical`.
3. Alerting đánh giá threshold/deduplication, persist alert và gửi notification.
4. Operator có thể tạo incident từ alert.
5. Incident module gom evidence từ alerts/anomalies/logs và có thể gọi AI provider để phân tích.

### Retention

1. Admin quản lý policies qua `/api/v1/retention/policies`.
2. `RetentionScheduler` chạy theo `app.retention.interval-ms`.
3. Retention service tính cutoff theo policy.
4. ClickHouse repository xóa rows hết hạn trong `processed_logs`.
5. Kết quả chạy được ghi vào `retention.retention_runs`.

## Diagrams

Mermaid diagrams hiện có nằm trong `detailed-diagrams.md` và `README.md`. File này chỉ mô tả kiến trúc bằng text/table để giữ vai trò là tài liệu kiến trúc chính thức trong `docs/architecture.md`.

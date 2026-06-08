# Log Monitoring System Project Overview

## Purpose

Log Monitoring System centralizes logs from multiple applications for
real-time ingestion, normalization, search, monitoring, alerting, and
analytics.

Its primary goal is to give engineering teams one operational view of
application behavior and critical failures.

## Users

### Engineers

- Submit application logs.
- Search and filter logs by application, level, and time.
- Watch live logs and application health.
- Receive critical-event alerts.

### Administrators

- Manage applications, access, API keys, and alert rules.
- Control which engineers can view each application.

These roles describe target product behavior. Authentication and authorization
are not implemented in the current scaffold.

## Core Capabilities

Initial implementation scope:

- `identity`: users, roles, authentication, authorization, access, and API
  credentials
- `logs`: single and batch ingestion, normalization, storage, search, and
  error detection
- `alerting`: rules, evaluation, deduplication policy, notifications, and
  alert lifecycle
- `realtime`: live log and alert delivery

Later capabilities, created only when implementation begins:

- `incidents`: incident tracking, assignment, state transitions, timelines,
  and resolution
- `analytics`: throughput, error rate, trends, and application health
- `ai`: assisted summaries, classification, correlation, and recommendations
- `retention`: retention policies and deletion or archival orchestration

## Boundaries

The system owns log ingestion, normalization, storage, querying, live
delivery, alert rules, deduplication, and monitoring views.

External applications own log production and transport to the ingestion API.
Telegram, when integrated, owns final message delivery.

The backend is one deployable Spring Boot Modular Monolith. Business
capabilities are modules, not independently deployed services.

PostgreSQL owns transactional and configuration data. ClickHouse owns
high-volume logs and analytical data. Storage remains private to the owning
module.

## Current Implementation

The backend has Spring MVC, JPA, PostgreSQL, and Flyway configured. Early
`identity` code and a `users` migration exist, but controllers and application
services are empty. The current package and public-schema migration predate
the approved `api`/`modules` structure and module-owned schema strategy.

The remaining business modules and internal event bus are target architecture,
not implemented behavior. ClickHouse is an intended data store, while Kafka
and Redis remain optional infrastructure until a verified use case requires
them.

## Success Direction

- Sustain burst-oriented ingestion without coupling request handling to slow
  storage or notification work.
- Keep search and live monitoring usable across multiple applications.
- Avoid duplicate alerts and prevent sensitive data from leaking through logs.
- Keep module dependencies explicit enough to allow future extraction without
  paying distributed-system costs today.

## Sources

- `README.md`
- `compose.yml`
- `apps/backend/pom.xml`
- `apps/frontend/package.json`

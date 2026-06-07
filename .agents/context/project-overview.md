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

- Single and batch log ingestion
- Normalization of application, level, message, timestamp, and trace ID
- Time-based log search and filtering
- Live log and alert delivery
- Critical-event deduplication and notification
- Throughput, error-rate, and application-health analytics

## Boundaries

The system owns log ingestion, normalization, storage, querying, live
delivery, alert rules, deduplication, and monitoring views.

External applications own log production and transport to the ingestion API.
Telegram, when integrated, owns final message delivery.

## Success Direction

- Sustain burst-oriented ingestion without coupling request handling to slow
  storage or notification work.
- Keep search and live monitoring usable across multiple applications.
- Avoid duplicate alerts and prevent sensitive data from leaking through logs.

## Sources

- `README.md`
- `compose.yml`
- `apps/backend/pom.xml`
- `apps/frontend/package.json`

# Project Overview

## Goal

Centralize application logs for high-speed ingestion, normalization, search,
live monitoring, critical alerts, and application-level authorization.

Mandatory demonstration: accept 500 logs in 2 seconds through Kafka without
losing acknowledged events while the admin live viewer remains smooth.

## Scope

Initial modules:

- `identity`: users, applications, API keys, roles, and application access.
- `logs`: single/batch ingestion, Kafka processing, ClickHouse storage/search,
  and ERROR/CRITICAL detection.
- `alerting`: configurable rules, Redis deduplication, Telegram delivery, and
  alert occurrences.
- `realtime`: authorized WebSocket delivery and live filters by application and
  level.

Bonus/future: `incidents`, `analytics`, `retention`, and `ai`.

Required infrastructure: PostgreSQL, ClickHouse, Kafka, and Redis. The backend
is one Spring Boot Modular Monolith. The frontend keeps its existing
feature-based React architecture.

## Users

- Engineer: searches and watches logs only for assigned applications and
  receives critical alerts.
- Admin: manages applications, access, API keys, alert thresholds, and
  notification configuration.

## Current State

The repository currently has the Spring Boot/React scaffold, Docker images,
local PostgreSQL/ClickHouse/Kafka/Redis Compose services, OpenAPI generation,
and early identity persistence. Kafka ingestion, processing, ClickHouse
storage, realtime, alerting, and most authorization behavior remain target
architecture unless source code proves otherwise.

The current identity package and `public.users` migration predate the approved
top-level `api` plus `modules` package structure and module-owned PostgreSQL
schemas.

## Success Direction

- Keep request handling decoupled from storage and notification work.
- Do not lose events acknowledged by Kafka.
- Keep live filtering responsive during the 500-log/2-second demonstration.
- Enforce application-level authorization on search and WebSocket delivery.
- Prevent duplicate notifications and sensitive-data leakage.

## Sources

- `DETAI.md`: authoritative assignment requirements.
- Source code and executable configuration: implementation truth.

`docs/module-requirement.md` and `docs/store-requirement.md` are temporary
planning drafts and are not authoritative project sources.

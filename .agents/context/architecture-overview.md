# Log Monitoring System Architecture Overview

## Style

The repository is a monorepo with a Spring Boot backend, React frontend, and
containerized infrastructure.

The backend uses complexity-driven architecture with optional, pragmatic DDD:

- Organize code primarily by business capability or bounded context, not by
  one global technical-layer tree.
- Start each capability with the smallest structure that keeps it clear and
  testable.
- Use a compact feature package for simple CRUD, configuration, or thin
  integration capabilities.
- Apply DDD boundaries only to capabilities with meaningful invariants,
  workflows, state transitions, or integration complexity.
- Evolve structure incrementally; not every capability needs layers,
  interfaces, aggregates, value objects, or repository abstractions.

The target backend is also event-driven: synchronous HTTP ingestion hands work
to Kafka, and workers perform normalization, persistence, live delivery, and
alert routing.

## Components

| Component | Responsibility | Location |
| --- | --- | --- |
| Backend | HTTP APIs and future processing workers | `apps/backend/` |
| Frontend | Dashboard, search, live logs, and alerts | `apps/frontend/` |
| PostgreSQL | Users, roles, applications, permissions, keys, rules | Compose |
| ClickHouse | Normalized logs and analytics | Compose |
| Kafka | Raw and processed event delivery | Compose |
| Redis | Alert deduplication and temporary TTL data | Compose |

Only the backend/frontend scaffolds and Compose infrastructure currently
exist. Verify implementation before relying on a target component flow.

## Target Data Flow

1. External applications submit logs to the Spring Boot API.
2. The API validates the boundary request and publishes to `logs.raw`.
3. A worker normalizes fields and handles invalid events.
4. Valid logs are stored in ClickHouse and published for live views.
5. Critical events pass through Redis deduplication.
6. WebSocket/STOMP and notification adapters deliver live updates.
7. The frontend queries APIs and subscribes to live events.

## Ownership

- PostgreSQL owns configuration and access-control data.
- ClickHouse owns normalized log and analytics data.
- Kafka owns transient event transport, not durable query state.
- Redis owns temporary deduplication state, not authoritative rules.
- The frontend never accesses storage or messaging systems directly.

## Dependency Direction

Organize backend packages by capability, for example:

```text
com.vdt.log_monitoring
├── ingestion
├── logsearch
├── alerting
├── identity
└── shared
```

A simple capability may remain compact:

```text
applicationregistry
├── ApplicationController
├── ApplicationService
└── ApplicationRepository
```

A complex capability may separate DDD concerns:

```text
alerting
├── api
├── application
├── domain
└── infrastructure
```

These are options, not mandatory templates. Choose the least complex structure
that preserves ownership, business rules, and integration boundaries.

Keep transport and infrastructure dependent on application or domain-owned
contracts when that separation provides value. Business rules must not depend
directly on Spring controllers, database entities, or vendor SDK models.
Avoid pass-through layers and interfaces that have no architectural purpose.

Within the frontend, keep app-wide wiring in `src/app`, feature behavior in
`src/features`, reusable code in `src/shared`, and transport code in `src/api`.

## Integrations

- REST/OpenAPI for browser and producer-facing HTTP contracts
- WebSocket/STOMP for planned live delivery
- Kafka for asynchronous processing
- JDBC/JPA/Flyway for planned PostgreSQL persistence
- ClickHouse JDBC for planned log persistence
- Redis client for planned deduplication

Timeout, retry, idempotency, and failure behavior must be defined when each
integration is implemented.

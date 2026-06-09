# Backend Architecture Context

## Decisions

- One deployable Spring Boot Modular Monolith, not microservices.
- Initial modules: `identity`, `logs`, `alerting`, `realtime`.
- Future/bonus modules: `incidents`, `analytics`, `retention`, `ai`; do not
  create empty modules before their use cases begin.
- PostgreSQL owns transactional/configuration data. ClickHouse owns logs and
  analytical datasets. Kafka is mandatory for accepted raw logs. Redis is
  mandatory for alert deduplication.
- The React frontend architecture remains unchanged.

## Package Boundaries

```text
com.vdt.log_monitoring
├── api
│   ├── identity
│   ├── logs
│   ├── alerting
│   └── realtime
├── modules
│   ├── identity
│   ├── logs
│   ├── alerting
│   └── realtime
└── shared
```

- Top-level `api.<module>` contains thin HTTP/WebSocket controllers and
  transport DTOs.
- `modules.<module>.api` contains the module's small public Java
  facade/contract, not controllers.
- Each module owns its application logic, model/domain, Spring configuration,
  repositories, storage adapters, and published integration events.
- `shared` contains technical security, exception, event, and observability
  primitives only.

Forbidden:

- Importing another module's entity, repository, domain, or infrastructure.
- Reading or writing another module's PostgreSQL schema or ClickHouse dataset.
- Controllers calling module internals, repositories, or multiple modules.
- Business logic in `shared` or dependency cycles.

## Module Ownership

| Module | Responsibility and storage |
| --- | --- |
| `identity` | Users, applications, API keys, access; PostgreSQL `identity` |
| `logs` | Ingestion, processing, search; Kafka log contracts and ClickHouse `logs` |
| `alerting` | Rules, Redis dedup, occurrences, Telegram delivery; PostgreSQL `alerting` |
| `realtime` | Live filters, WebSocket sessions, authorized log/alert delivery |

Inside `logs`, `ingestion`, `processing`, and `query` are components, not
separate business modules. Processing is the only ClickHouse writer; query is
read-only; ingestion never accesses ClickHouse.

## Required Flow

```text
HTTP ingestion -> Kafka logs.raw -> processing worker -> ClickHouse
                                      ├-> Kafka logs.live -> realtime -> WebSocket
                                      └-> Kafka alerts.critical -> alerting
                                                                    ├-> Telegram
                                                                    └-> realtime alert event
```

- Return accepted only after Kafka acknowledges `logs.raw`.
- Worker writes ClickHouse in batches and commits offsets only after required
  storage/downstream acknowledgments.
- Preserve `event_id`; retries and consumers are idempotent; terminal failures
  go to `logs.dlq`.
- `alerts.critical` uses dedicated consumer resources. Redis dedup is atomic
  with TTL.
- Realtime filters by authorized application IDs and log levels without
  reloading the page.

## Design Level

- Use DDD only for real invariants or workflows; do not force it on simple
  CRUD/read-model code.
- Use direct focused application services behind module facades; no generic
  request bus for the MVP.
- Use outbox/inbox only where a durable database-to-event boundary requires it.
- Enforce boundaries with architecture tests as modules are implemented.

## Ingestion Guardrails

- Define request bytes, batch count, rate limits, timestamp range,
  idempotency, Kafka timeout, and overload responses before implementation.
- Never fall back to direct PostgreSQL or ClickHouse writes when Kafka is
  unavailable.
- Keep HTTP ingestion, Kafka processing, and ClickHouse query/write resource
  pools isolated inside the application.
- Worker concurrency is bounded by Kafka partition count.

## Data And Consistency

- PostgreSQL-backed modules own a schema named after the module.
- `logs` owns the ClickHouse log dataset; future `analytics` owns its
  projections.
- Do not use a distributed transaction across PostgreSQL and ClickHouse.
- Cross-store workflows define authority, eventual consistency, retry,
  idempotency, and reconciliation.
- Future `retention` owns policy/scheduling only; the data owner executes
  deletion or archival.

## Observability

Track accepted/rejected/rate-limited logs, ingestion latency, Kafka producer
failures and lag, ClickHouse batch/write/query latency, DLQ rate, Redis dedup,
alert delivery failures, WebSocket sessions, dropped events, and delivery
failures. Use structured logs, health indicators, metrics, and correlation IDs.

## Enforcement

Architecture tests must prevent:

- Cross-module internal/storage imports.
- Controller business logic or controller access to repositories.
- Transport adapters bypassing public module facades.
- Spring/JPA/Kafka/provider dependencies in framework-free domain types.
- Business dependencies in `shared` and module dependency cycles.

## Current Drift

The early identity code still uses `com.vdt.log_monitoring.identity`, and the
current users migration uses PostgreSQL `public`. New work should move toward
the approved top-level `api` plus `modules` structure without rewriting
unrelated existing work.

This context summarizes approved conversation decisions and `DETAI.md`.
`docs/module-requirement.md` and `docs/store-requirement.md` are temporary
planning drafts, not architecture sources. Read them only when the user
explicitly requests work on those files.

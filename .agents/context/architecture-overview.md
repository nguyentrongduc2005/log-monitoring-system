# Backend Architecture Context

## Decisions

- Architecture: one deployable Spring Boot Modular Monolith, not microservices.
- Initial modules: `identity`, `logs`, `alerting`, and `realtime`.
- Later modules: `incidents`, `analytics`, `retention`, and `ai`; do not create
  empty module skeletons before their use cases begin.
- Module API adapters live inside each module for cohesion.
- DDD is optional and driven by actual business complexity.
- Internal asynchronous communication starts with an in-process event bus.
- PostgreSQL and ClickHouse are both part of the solution from the start.
- Kafka, Redis, and an outbox require a demonstrated use case.
- The existing React frontend architecture remains unchanged.

## Backend Structure

```text
com.vdt.log_monitoring
├── modules
│   ├── identity
│   │   ├── api
│   │   ├── application
│   │   ├── model
│   │   └── infrastructure
│   ├── logs
│   │   ├── api
│   │   ├── application
│   │   ├── domain
│   │   ├── infrastructure
│   │   └── integrationevents
│   ├── alerting
│   │   ├── api
│   │   ├── application
│   │   ├── domain
│   │   ├── infrastructure
│   │   └── integrationevents
│   └── realtime
│       ├── api
│       ├── application
│       ├── model
│       └── infrastructure
└── shared
    ├── eventbus
    ├── security
    ├── exceptions
    └── common
```

Use this as a guide, not a reason to create empty layers. A simple module may
stay compact. Add `domain`, ports, facades, or integration events only when
they express real rules or boundaries.

## Module Responsibilities

| Module | Initial responsibility |
| --- | --- |
| `identity` | Users, roles, authentication, authorization, application access, and API credentials |
| `logs` | Ingestion, validation, normalization, ClickHouse storage, search, and error detection |
| `alerting` | Alert rules, evaluation, deduplication policy, notification routing, and alert lifecycle |
| `realtime` | WebSocket sessions and live delivery of logs and alerts |

Later capabilities:

- `incidents`: incident lifecycle and resolution workflow.
- `analytics`: projections, aggregations, trends, and health views.
- `retention`: policy scheduling and orchestration through data-owner contracts.
- `ai`: summaries, classification, correlation, and recommendations.

`analytics` is not required to use full DDD. Promote any module to richer DDD
only when invariants, state transitions, or workflows justify it.

## Communication

Synchronous flow:

```text
Module API Controller -> Module Application Use Case -> Module-Owned Storage
```

Cross-module communication uses:

- A narrow public application contract when an immediate result is required.
- An immutable integration event when consumers can react independently.

The default event bus is in-process. Publish events after the owning
transaction commits. Define idempotency, ordering, retry, and failure behavior
for each important consumer. Add an outbox or Kafka only when durable delivery,
replay, external consumption, or sustained buffering is required.

Forbidden:

- Importing another module's entity, repository, or infrastructure.
- Querying or writing another module's storage directly.
- Controllers coordinating multiple modules.
- Circular module dependencies.

## Data Ownership

The system deliberately uses two databases:

### PostgreSQL

Owns transactional and configuration data:

- Identity, users, roles, permissions, applications, and API credentials.
- Alert rules, alert lifecycle state, and notification configuration.
- Future incident, retention-policy, and AI configuration state.

Each PostgreSQL-backed module owns a schema named after the module. Other
modules must use its public contract or integration events.

### ClickHouse

Owns high-volume, append-oriented and analytical data:

- Raw or normalized logs retained for search.
- Log indexes and materialized views supported by ClickHouse.
- Aggregated metrics and future analytics projections.

The `logs` module owns log tables and log queries. A future `analytics` module
owns its projections or materialized analytical datasets. Other modules do not
issue ad hoc ClickHouse queries against those tables.

PostgreSQL and ClickHouse must not participate in one distributed transaction.
Workflows crossing both stores require explicit eventual consistency,
idempotency, retry, and reconciliation behavior.

Current drift: the early `identity` implementation is under
`com.vdt.log_monitoring.identity`, and its `users` table is in PostgreSQL's
public schema. It predates the approved module package and schema ownership.

## Ingestion Guardrails

Define these before exposing production log ingestion:

- Maximum single-event and batch payload size.
- Maximum batch item count.
- Per-application or per-credential rate limits.
- Idempotency or duplicate-handling semantics.
- Backpressure and overload response behavior.
- ClickHouse batch-write strategy and flush thresholds.
- Retry limits and handling of rejected or malformed logs.
- Sensitive-field redaction and timestamp validation.

Do not add Kafka merely to claim asynchronous ingestion. Add it when measured
burst handling, buffering, replay, or delivery isolation requires it.

## Retention

The future `retention` module owns policy and scheduling, not other modules'
tables. It requests deletion or archival through the data owner's public
contract. The owning module executes storage-specific operations in PostgreSQL
or ClickHouse.

## Observability

The backend must expose operational signals for:

- Accepted, rejected, duplicate, and rate-limited logs.
- Ingestion and ClickHouse write latency.
- Batch size, flush failures, retries, and backlog where applicable.
- Integration-event publication and handler failures.
- Alert evaluation and delivery failures.
- PostgreSQL and ClickHouse query latency.
- WebSocket sessions and delivery failures.

Use structured logs, health indicators, metrics, and correlation identifiers.
The monitoring system must be observable without recursively ingesting
unbounded copies of its own logs.

## Enforcement

Add automated architecture tests as module implementation grows. They must
prevent:

- Imports of another module's entity, repository, or infrastructure package.
- Access from one module to another module's persistence implementation.
- Business logic in controllers.
- Business dependencies inside `shared`.
- Circular module dependencies.

## Frontend

This document changes backend architecture only. Preserve the existing React
structure:

- `src/app`: application wiring.
- `src/features`: feature behavior.
- `src/shared`: reusable frontend code.
- `src/api`: transport clients and generated API types.

# Backend Architecture Rules

Apply to backend modules, dependencies, data flow, and integrations. These
rules do not change the existing frontend architecture.

## Style And Scope

- Build one deployable Spring Boot Modular Monolith, not microservices.
- Implement `identity`, `logs`, `alerting`, and `realtime` first.
- Do not create empty `incidents`, `analytics`, `retention`, or `ai` modules.
- Organize each capability under `com.vdt.log_monitoring.modules.<module>`.
- Keep HTTP/WebSocket adapters under top-level
  `com.vdt.log_monitoring.api.<module>`.
- Use `modules.<module>.api` only for the module's small public Java
  facade/contract.
- Keep cross-cutting technical code under `com.vdt.log_monitoring.shared`.
- Do not create global `controller`, `service`, `repository`, or `entity`
  packages.

## Module Structure

Start with the smallest useful structure:

```text
modules/<module>/
├── api/                 # public module facade/contract
├── application/
├── model/ or domain/
├── infrastructure/
└── integrationevents/  # only when events exist
```

- Use DDD only for meaningful invariants, workflows, or state transitions.
- Do not force full DDD on `analytics` or any other module.
- Do not add empty layers, marker interfaces, speculative ports, or pass-through
  services for symmetry.

## Ownership And Dependencies

Each module owns its use cases, business model, storage adapters, repositories,
and published integration events.

- Top-level API adapters depend only on the owning module's public facade.
- Public module APIs expose stable DTO/value contracts, never entities,
  repositories, provider models, or infrastructure types.
- Controllers contain no business logic and access no repository.
- Never import another module's entity, repository, or infrastructure.
- Never query or write another module's PostgreSQL schema or ClickHouse
  dataset.
- `shared` contains technical code only and does not depend on business
  modules.
- Circular module dependencies are forbidden.

Use a narrow public application contract for immediate cross-module results.
Use an integration event for independent reactions. Controllers must not
coordinate multiple modules.

Each module owns its Spring configuration/composition wiring. The application
root starts modules but does not configure their repositories, handlers, or
external adapters individually.

Implement validation, authorization, transaction handling, correlation,
logging, metrics, and idempotency through focused filters, interceptors,
decorators, or shared technical utilities. Do not place business rules in
cross-cutting code.

## Events

- Kafka is mandatory for raw-log buffering and the asynchronous log pipeline.
- Every accepted raw log must be published to Kafka before processing.
- Use an in-process event bus only for non-pipeline module reactions where
  durable queue semantics are unnecessary.
- Publish post-transaction integration events after the owning transaction
  commits.
- Keep events immutable and name them as past-tense facts.
- Define consumer idempotency, ordering, retries, and failure handling.
- Add an outbox when an important post-commit event must survive process
  failure.

## Storage

- PostgreSQL owns transactional and configuration data.
- ClickHouse owns high-volume logs and analytical datasets.
- Keep storage access private to the owning module.
- Do not use distributed transactions across PostgreSQL and ClickHouse.
- Define eventual consistency, retry, idempotency, and reconciliation for
  workflows that cross stores.
- Redis is mandatory for atomic alert deduplication but is not an
  authoritative business store.

## Ingestion

Separate the required pipeline into:

1. Ingestion API/Kafka producer.
2. Kafka `logs.raw` buffer.
3. Processing worker/Kafka consumer.

The ingestion component and worker may share the `logs` business module and
Spring Boot deployment, but they must not call each other directly. The
ingestion API never parses deeply or writes PostgreSQL/ClickHouse. The worker
runs outside the request thread, writes ClickHouse in batches, and commits its
Kafka offset only after successful storage and acknowledgment of every
required downstream Kafka event. Redelivery must preserve `event_id`; storage
and consumers must be idempotent. Terminal downstream-publication failure must
be replayable from `logs.dlq` and observable.

Define payload and batch limits, rate limiting, duplicate semantics, Kafka
backpressure, ClickHouse batching, retries, malformed-log/DLQ handling,
redaction, and timestamp validation.

Run `alerts.critical` with a dedicated consumer group and reserved execution
resources. Do not describe Kafka topic naming alone as message priority.

## Retention

A future `retention` module owns policy and scheduling only. It calls the data
owner's public contract; it never deletes another module's PostgreSQL or
ClickHouse data directly.

## Observability

Instrument ingestion counts and latency, rejection reasons, ClickHouse writes,
event failures, alert failures, database query latency, and realtime delivery.
Use health indicators, structured logs, metrics, and correlation identifiers.

## Enforcement

Add architecture tests as modules grow. Enforce forbidden cross-module imports,
storage access, controller business logic, business code in `shared`, and
dependency cycles.

Changing the Modular Monolith style, initial module scope, storage ownership,
or module communication rules requires explicit approval.

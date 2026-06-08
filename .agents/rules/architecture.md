# Backend Architecture Rules

Apply to backend modules, dependencies, data flow, and integrations. These
rules do not change the existing frontend architecture.

## Style And Scope

- Build one deployable Spring Boot Modular Monolith, not microservices.
- Implement `identity`, `logs`, `alerting`, and `realtime` first.
- Do not create empty `incidents`, `analytics`, `retention`, or `ai` modules.
- Organize each capability under `com.vdt.log_monitoring.modules.<module>`.
- Keep module API adapters under `<module>.api`.
- Keep cross-cutting technical code under `com.vdt.log_monitoring.shared`.
- Do not create global `controller`, `service`, `repository`, or `entity`
  packages.

## Module Structure

Start with the smallest useful structure:

```text
modules/<module>/
├── api/
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

- API adapters depend on their module's application boundary.
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

## Events

- Use an in-process event bus first.
- Publish integration events after the owning transaction commits.
- Keep events immutable and name them as past-tense facts.
- Define consumer idempotency, ordering, retries, and failure handling.
- Add an outbox when an important post-commit event must survive process
  failure.
- Add Kafka only for verified buffering, replay, external consumption,
  sustained burst isolation, or durable delivery needs.

## Storage

- PostgreSQL owns transactional and configuration data.
- ClickHouse owns high-volume logs and analytical datasets.
- Keep storage access private to the owning module.
- Do not use distributed transactions across PostgreSQL and ClickHouse.
- Define eventual consistency, retry, idempotency, and reconciliation for
  workflows that cross stores.
- Redis is optional infrastructure, not an authoritative business store.

## Ingestion

Before production ingestion, define payload and batch limits, rate limiting,
duplicate semantics, backpressure, ClickHouse batching, retries, malformed-log
handling, redaction, and timestamp validation.

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

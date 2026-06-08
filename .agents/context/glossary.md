# Log Monitoring System Glossary

## Architecture Terms

### Modular Monolith

One deployable Spring Boot application divided internally into business
modules with explicit ownership and dependency rules.

### Module

A business capability that owns its application logic, domain or model,
persistence, PostgreSQL schema or ClickHouse dataset, and integration events.
Approved modules are initially `identity`, `logs`, `alerting`, and `realtime`.
Later modules are created only when their use cases begin.

### Module Facade

A narrow public application contract used when another module requires an
immediate result. A facade never exposes entities, repositories, or
infrastructure types.

### Domain Event

An event meaningful inside one module's domain model. It is not exposed
directly to other modules.

### Integration Event

An immutable, past-tense fact published after a successful module transaction
for other modules to consume. Examples include
`LogErrorDetectedIntegrationEvent` and `AlertCreatedIntegrationEvent`.

### In-Process Event Bus

The default technical mechanism for publishing integration events between
modules inside the single Spring Boot runtime.

### Module-Owned Schema

A PostgreSQL schema that only its owning module may query or mutate.

### Module-Owned Dataset

A ClickHouse table, materialized view, or projection that only its owning
module may query or mutate directly.

### Backpressure

Controlled behavior when ingestion arrives faster than the system can validate,
buffer, or write logs.

## Domain Terms

### Application

A registered source that produces logs. Do not use this term for the whole Log
Monitoring System when discussing permissions or log ownership.

### Raw Log

An accepted but not yet normalized log record owned by the `logs` module.
Kafka transport is optional and does not define this domain state.

### Normalized Log

A validated log record using the platform's canonical fields, including
`applicationName`, `level`, `message`, `timestamp`, and `traceId`.

### Live Log

A normalized event delivered to connected dashboard clients without a page
reload. Live delivery is planned, not currently implemented.

### Critical Event

A log at a severity that triggers alert evaluation, currently described as
`ERROR` or `CRITICAL` in the project roadmap.

### Alert Rule

Administrator-managed configuration that determines when and where alerts are
sent.

### Deduplication

Temporary suppression of repeated alerts, planned through Redis keys with TTL.

### Retention

The policy controlling how long module-owned logs, incidents, analytics, and
other eligible data remain available.

## Acronyms

| Term | Meaning |
| --- | --- |
| API | Application Programming Interface |
| DTO | Data Transfer Object |
| TTL | Time To Live |
| DLQ | Dead Letter Queue |
| STOMP | Simple Text Oriented Messaging Protocol |
| DDD | Domain-Driven Design |

## Naming

- Use `applicationName` for the normalized source name.
- Use `traceId` for cross-service request correlation.
- Use `log level` or `level`, not `status`, for severity.
- Use `raw log` before normalization and `normalized log` afterward.
- Name integration events in the past tense with the
  `IntegrationEvent` suffix.
- Use `module` for an internal business boundary and `service` only for an
  application or domain service, not an independently deployed component.

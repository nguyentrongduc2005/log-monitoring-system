# API Design Rules

Apply to HTTP, WebSocket, module contracts, and integration events.

## Location And Contract

- Place each HTTP/WebSocket controller under
  `com.vdt.log_monitoring.api.<module>`.
- Reserve `com.vdt.log_monitoring.modules.<module>.api` for public module
  facade interfaces and contract DTOs.
- HTTP APIs use Spring MVC and the `/api/v1` namespace.
- OpenAPI source is Spring controllers and transport DTOs.
- Generate `docs/api/openapi.json` and frontend API types with `make api`.
- Never edit generated contracts manually.

## Controllers

Controllers may validate transport input, enforce authentication and
authorization, invoke one public module facade, and map its result.

Controllers must not implement business rules, access persistence, publish
domain events, import module internals, or coordinate multiple modules.

- Use transport DTOs; never expose domain or persistence entities.
- Define required, optional, nullable, and default behavior.
- Use ISO 8601 timestamps with an explicit timezone.
- Keep errors stable and never expose stack traces.
- Prefer additive changes and require approval for breaking contracts.

## Log Ingestion Contract

Use the documented MVP baseline unless load-test evidence approves a change:

- Maximum request body: `1 MiB`.
- Maximum batch: `500` events.
- Default rate: `1,000 events/second/application`, burst `2,000`.
- Accepted timestamp: from `7 days ago` through `5 minutes in the future`.
- Whole-batch validation: one invalid event rejects the complete request.
- Support `Idempotency-Key` and preserve logical event IDs across retries.
- Require Kafka acknowledgment before returning accepted.
- Default Kafka publish timeout: `3 seconds`.
- Return `429` for rate limiting and `503` for broker/overload failure, with
  retry guidance.
- Redact recognizable secrets before Kafka and again before storage/downstream
  publication.

Keep these values configurable and synchronize approved changes with the
OpenAPI and storage/module requirements.

## Module Contracts

- Expose narrow application contracts, not entities, repositories, or provider
  models.
- Use synchronous contracts only when an immediate result is required.
- Prevent circular contract dependencies.
- Facade methods call focused application services directly; do not add a
  generic request bus for the MVP.

## Integration Events

- The publishing module owns its immutable, past-tense event definition.
- Include an event ID, occurrence time, compatibility version when needed, and
  only legitimate consumer data.
- Publish after commit and define idempotency, ordering, retry, and failure
  behavior.
- Use Kafka for the required raw-log, live-log, critical-alert, and
  dead-letter pipeline.
- Consumers must be idempotent and define retry/DLQ behavior.
- Use in-process events only outside that pipeline when durable delivery is
  unnecessary.
- Use an outbox when database commit and Kafka publication require atomic
  recovery semantics.

## Realtime

`realtime` owns WebSocket sessions and delivery. Other modules provide data
through public contracts or integration events and do not manipulate sessions
directly.

Never return credentials, API-key values, password hashes, or internal
security state.

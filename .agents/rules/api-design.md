# API Design Rules

Apply to HTTP, WebSocket, module contracts, and integration events.

## Location And Contract

- Place each controller under
  `com.vdt.log_monitoring.modules.<module>.api`.
- HTTP APIs use Spring MVC and the `/api/v1` namespace.
- OpenAPI source is Spring controllers and transport DTOs.
- Generate `docs/api/openapi.json` and frontend API types with `make api`.
- Never edit generated contracts manually.

## Controllers

Controllers may validate transport input, enforce authentication and
authorization, invoke one module use case, and map its result.

Controllers must not implement business rules, access persistence, publish
domain events, or coordinate multiple modules.

- Use transport DTOs; never expose domain or persistence entities.
- Define required, optional, nullable, and default behavior.
- Use ISO 8601 timestamps with an explicit timezone.
- Keep errors stable and never expose stack traces.
- Prefer additive changes and require approval for breaking contracts.

## Log Ingestion Contract

Before implementing ingestion, explicitly define:

- Maximum request bytes and maximum events per batch.
- Per-application or per-credential rate limits.
- Duplicate and idempotency behavior.
- Accepted timestamp range and canonical log fields.
- Partial-batch versus whole-batch failure semantics.
- Overload/backpressure response and retry guidance.
- Validation and redaction behavior.

Do not invent these limits while implementing an unrelated task.

## Module Contracts

- Expose narrow application contracts, not entities, repositories, or provider
  models.
- Use synchronous contracts only when an immediate result is required.
- Prevent circular contract dependencies.

## Integration Events

- The publishing module owns its immutable, past-tense event definition.
- Include an event ID, occurrence time, compatibility version when needed, and
  only legitimate consumer data.
- Publish after commit and define idempotency, ordering, retry, and failure
  behavior.
- Use the in-process event bus by default.
- Kafka and outbox are optional reliability mechanisms, not default module
  communication.

## Realtime

`realtime` owns WebSocket sessions and delivery. Other modules provide data
through public contracts or integration events and do not manipulate sessions
directly.

Never return credentials, API-key values, password hashes, or internal
security state.

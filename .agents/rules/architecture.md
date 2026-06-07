# Architecture Rules

Apply when changing module boundaries, dependencies, data flow, integrations,
or cross-cutting behavior.

## Boundaries

- Backend source root: `apps/backend/src/main/java/com/vdt/log_monitoring/`
- Frontend app wiring: `apps/frontend/src/app/`
- Frontend features: `apps/frontend/src/features/`
- Frontend shared code: `apps/frontend/src/shared/`
- Frontend transport: `apps/frontend/src/api/`

- Organize backend code by business capability or bounded context first.
- Suggested initial capabilities include `ingestion`, `processing`,
  `logsearch`, `alerting`, `analytics`, `identity`, and `applicationregistry`;
  create only capabilities required by implemented scope.
- Start with the simplest feature-local structure that supports the current
  use case.
- A simple capability may keep controllers, services, DTOs, and repositories
  in one feature package or a few small subpackages.
- Use `domain`, `application`, `api`, and `infrastructure` separation only for
  capabilities whose complexity justifies it.
- Do not create repository-wide top-level packages based on technical class
  roles; technical concerns belong inside the capability that owns them.
- Give each capability one clear responsibility and a small public interface.
- Do not access another module's internal storage or implementation classes.
- Keep controllers thin and place behavior in the owning application module.
- Isolate PostgreSQL, ClickHouse, Kafka, Redis, WebSocket, and notification
  clients behind explicit boundaries.
- Do not create shared abstractions until at least two real consumers need
  them.
- Keep `shared` small and limited to genuinely cross-domain technical or
  kernel concepts; do not use it as a miscellaneous package.

## Architecture Selection

Use a simple feature structure when most of these are true:

- The capability is CRUD or straightforward orchestration.
- It has few business rules and no important state machine.
- It uses one persistence mechanism or external integration.
- Framework coupling does not obscure domain behavior.
- Focused tests remain easy to write.

Use pragmatic DDD when one or more of these are significant:

- Business invariants must remain valid across multiple operations.
- The capability has meaningful entities, value objects, or state transitions.
- Several use cases share domain behavior.
- Multiple infrastructure integrations need isolation.
- Event ordering, idempotency, retry, or consistency rules are central.
- Framework or persistence details make the business behavior difficult to
  understand or test.

Do not introduce DDD solely for naming consistency. Refactor a simple
capability toward DDD when observed complexity creates a concrete need.

## Dependency Direction

- In DDD-oriented capabilities, domain behavior must not depend on HTTP,
  persistence, Spring annotations, or vendor-specific models.
- In DDD-oriented capabilities, application services coordinate use cases and
  transactions while API and infrastructure code depend inward.
- Simple capabilities may use Spring and framework repositories directly when
  that remains clear, focused, and testable.
- Introduce ports or repository interfaces when they isolate a real external
  dependency, support multiple implementations, or materially improve tests.
- The frontend must access backend behavior through API or WebSocket clients,
  never infrastructure services directly.
- Avoid circular module and package dependencies.

## Pragmatic DDD

- DDD is selective, not a repository-wide requirement.
- Use entities, value objects, aggregates, domain services, and domain events
  only when they express real invariants or business language.
- Plain records, DTOs, and straightforward services are valid for simple CRUD
  or orchestration.
- Do not require one interface per class, one mapper per model, or separate
  command/query types for trivial use cases.
- Do not mirror the same model across layers without a boundary-specific
  reason.
- Keep invariants close to the domain object or service that owns them.
- Prefer incremental extraction as complexity grows over speculative layers.
- A capability may start with a compact package and split internally without
  changing its public contract.

## Data Ownership

- PostgreSQL: identity, applications, permissions, API keys, and alert rules.
- ClickHouse: normalized logs and analytics.
- Kafka: event transport.
- Redis: temporary deduplication and TTL state.

Do not duplicate an authoritative data model across stores without an approved
consistency strategy.

## Integrations

- Define timeouts and failure behavior for every network call.
- Define idempotency and retry behavior for ingestion and event consumers.
- Do not leak provider-specific models into public domain contracts.
- Update architecture documentation when ownership or data flow changes.
- Get approval before replacing the event-driven target architecture.

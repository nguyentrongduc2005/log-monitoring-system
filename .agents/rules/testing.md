# Testing Rules

Apply to behavior changes and defect fixes.

## Current Stack

- Backend: JUnit 5 through `spring-boot-starter-test`
- Current backend coverage: Spring context-load test only
- Frontend: no automated test framework configured

Do not claim test coverage that the repository does not provide. Do not add a
new test framework unless it is part of the approved scope.

## Required Coverage

When applicable, cover:

- Successful behavior
- Validation and boundary failures
- Authentication and authorization failures
- Duplicate or retry behavior for events
- External dependency failures
- Regression for the reported defect
- Module dependency and forbidden-import rules
- Top-level API adapters depending only on public module facades
- Domain packages remaining free of Spring, JPA, Kafka, and provider types
- Ingestion payload, batch, rate-limit, duplicate, and backpressure boundaries
- Kafka producer acknowledgment, consumer retry, idempotency, and DLQ behavior
- Raw offset commit only after ClickHouse and downstream Kafka acknowledgments
- Dedicated `alerts.critical` consumer latency and isolation from live delivery
- Redis deduplication under repeated and concurrent critical events
- PostgreSQL/ClickHouse consistency and retry behavior where both stores are
  involved
- Operational metrics for critical ingestion and alert failure paths

Tests must be deterministic and independent. Avoid sleeps, order dependence,
production services, and shared mutable state. Never weaken, skip, or delete a
test only to make checks pass.

## Commands

- Backend tests: `cd apps/backend && ./mvnw test`
- Backend package: `cd apps/backend && ./mvnw clean package`
- Frontend lint: `cd apps/frontend && npm run lint`
- Frontend type-check/build: `cd apps/frontend && npm run build`
- Project checks: `make test`, `make lint`, and `make build`

Start with focused checks and broaden after they pass. Report every unrun check,
pre-existing failure, and environment blocker.

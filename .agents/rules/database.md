# Data Storage Rules

Apply to PostgreSQL, ClickHouse, migrations, queries, transactions, and
retention.

## Storage Roles

Both databases are intentional parts of the backend:

- PostgreSQL 17: transactional and configuration data.
- ClickHouse 25.3: high-volume logs, log search, aggregations, and analytical
  projections.

Kafka is the mandatory raw-log buffer and event transport for the processing
pipeline. Redis is the mandatory alert-deduplication store. Neither replaces
PostgreSQL or ClickHouse as an authoritative query store.

## Ownership

- A PostgreSQL-backed module owns a schema named after the module.
- A ClickHouse-backed module owns its tables, materialized views, and queries.
- Repositories and storage mappings stay internal to the owning module.
- Never directly query, join, mutate, or map another module's storage.
- Cross-module data moves through public contracts or integration events.

The current `users` migration uses PostgreSQL's public schema and predates the
approved `identity` schema ownership.

## PostgreSQL

- Use Flyway under
  `apps/backend/src/main/resources/db/migration/postgresql/`.
- Every schema change requires a migration; applied migrations are immutable.
- Qualify tables and JPA mappings with the owning schema.
- Use constraints for critical invariants and indexes for verified queries.
- Store timestamps in UTC.
- Never store plaintext passwords, raw API keys, tokens, or secrets.

## ClickHouse

- `logs` owns raw/normalized log tables and log-search queries.
- A future `analytics` module owns its projections or analytical datasets.
- Design partitioning, ordering keys, primary indexes, and TTL from verified
  query and retention requirements.
- Write logs in batches; define batch size, flush interval, retry limits, and
  failure handling.
- Bound searches by time and result count.
- Avoid mutation-heavy relational modeling.

## Cross-Store Consistency

- Never use a distributed transaction across PostgreSQL and ClickHouse.
- State which store is authoritative for each datum.
- Define eventual consistency, idempotency, retry, and reconciliation.
- Preserve event IDs across Kafka retries and define a concrete ClickHouse
  duplicate-handling strategy; `event_id` alone does not enforce uniqueness.
- Commit a raw-log offset only after ClickHouse and required downstream Kafka
  publications are acknowledged. Redelivery after a partial success must be
  safe for storage and every consumer.
- Add an outbox when durable post-commit Kafka publication is required.

## Retention

The future `retention` module owns policy and scheduling. The data-owning
module executes deletion or archival through its own PostgreSQL or ClickHouse
adapter. Retention must not bypass module ownership.

## Verification

Verify migrations, mappings, ClickHouse DDL, constraints, indexes, ordering
keys, batching, query bounds, TTL, retries, and failure behavior with focused
tests.

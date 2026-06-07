# Database Rules

Apply to schemas, migrations, persistence models, queries, indexes,
transactions, and retention.

## Storage Roles

- PostgreSQL 17 stores users, roles, applications, permissions, API keys, and
  alert rules.
- ClickHouse 25.3 stores normalized logs and analytics.
- Redis stores temporary TTL state only.

Persistence dependencies, schemas, and migrations are not implemented yet.
Do not infer a schema from README examples.

## PostgreSQL

- Use Flyway migrations under
  `apps/backend/src/main/resources/db/migration/postgresql/` when persistence
  is introduced.
- Every schema change requires a migration.
- Applied migrations are immutable.
- Use constraints for critical invariants and indexes for verified queries.
- Store timestamps in UTC.
- Never store plaintext passwords, raw API keys, or other secrets.

The table naming, identifier strategy, soft-delete policy, and transaction
model require an approved design before first implementation.

## ClickHouse

- Design tables around verified filter, ordering, and retention needs.
- Keep log queries bounded by time and result limits.
- Define partitions, ordering keys, and TTL retention explicitly.
- Avoid mutation-heavy relational patterns.
- Plan ingestion for batches rather than row-by-row writes.

## Compatibility

- Review deployment order for schema and application changes.
- Define forward-fix or rollback behavior before destructive changes.
- Make large backfills resumable.
- Verify migrations, constraints, indexes, defaults, and retention behavior.

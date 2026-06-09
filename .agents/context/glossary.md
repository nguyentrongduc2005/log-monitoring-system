# Project Glossary

## Architecture

- **Module:** Business boundary owning its use cases, model, storage adapters,
  public facade, and events. Initial modules are `identity`, `logs`,
  `alerting`, and `realtime`.
- **Transport API:** HTTP/WebSocket adapter under top-level `api.<module>`.
- **Module facade:** Narrow Java contract under `modules.<module>.api`; exposes
  no entity, repository, or infrastructure type.
- **Integration event:** Immutable versioned fact for another module. Kafka is
  mandatory for the log pipeline.
- **In-process event:** Non-durable reaction inside the Spring Boot runtime;
  never replaces Kafka for raw logs, live logs, or critical alerts.
- **Module-owned storage:** Only the owner may directly access its PostgreSQL
  schema or ClickHouse dataset.
- **Backpressure:** Controlled rejection or slowdown when ingestion exceeds
  Kafka/worker/storage capacity.

## Domain

- **Application:** Registered external source that sends logs and defines the
  authorization boundary.
- **Raw log:** Accepted event acknowledged in Kafka `logs.raw`, not yet
  normalized.
- **Normalized log:** Validated/redacted record using `applicationName`,
  `level`, `message`, `timestamp`, and optional `traceId`.
- **Live log:** Normalized event sent to authorized WebSocket subscribers.
- **Critical event:** `ERROR` or `CRITICAL` log published to
  `alerts.critical`.
- **Alert occurrence:** Durable `alerting` record grouping repeated events by
  application and fingerprint. It is not an incident workflow.
- **Deduplication:** Redis TTL suppression that sends one notification for
  repeated matching errors while occurrence count still increases.
- **Incident:** Future assignment/acknowledge/resolve workflow referencing an
  alert occurrence.
- **Retention:** Future policy for compressing or deleting old module-owned
  data; INFO logs older than seven days are the assignment bonus target.
- **DLQ:** Kafka dead-letter topic containing redacted terminal processing
  failures for operations and replay.

## Naming

- Use `applicationName`, `traceId`, and `level`.
- Use `raw log` before normalization and `normalized log` afterward.
- Name integration events as past-tense facts.
- Use `module` for business ownership; ingestion, processing, and query are
  components inside `logs`.
- Use `level`, not `status`, for log severity.

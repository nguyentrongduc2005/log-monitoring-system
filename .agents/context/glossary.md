# Log Monitoring System Glossary

## Domain Terms

### Application

A registered source that produces logs. Do not use this term for the whole Log
Monitoring System when discussing permissions or log ownership.

### Raw Log

An accepted but not yet normalized log event. The target Kafka topic is
`logs.raw`.

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

The policy controlling how long normalized logs remain in ClickHouse.

## Acronyms

| Term | Meaning |
| --- | --- |
| API | Application Programming Interface |
| DTO | Data Transfer Object |
| TTL | Time To Live |
| DLQ | Dead Letter Queue |
| STOMP | Simple Text Oriented Messaging Protocol |

## Naming

- Use `applicationName` for the normalized source name.
- Use `traceId` for cross-service request correlation.
- Use `log level` or `level`, not `status`, for severity.
- Use `raw log` before normalization and `normalized log` afterward.

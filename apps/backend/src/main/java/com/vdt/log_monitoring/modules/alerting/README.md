# Alerting module

The module consumes critical log events, evaluates active alert rules, stores
durable alert occurrences, and dispatches notifications after the database
transaction commits.

```text
alerts.critical Kafka event
  -> detection/AlertDetectionService
  -> evaluation/AlertEvaluationService
       -> rule/AlertRuleMatcher
       -> cache/AlertThresholdCache
       -> alert/AlertService
       -> notification/NotificationDispatcher (after commit)
```

Package responsibilities:

- `api`: public facade, commands, DTOs, and module events.
- `internal/detection`: Kafka boundary and event mapping.
- `internal/evaluation`: orchestration and transaction lifecycle callbacks.
- `internal/rule`: rule persistence, immutable evaluation definitions, matching,
  and delivery-target resolution.
- `internal/cache`: Redis rule cache and atomic threshold/window/cooldown state.
- `internal/alert`: PostgreSQL alert occurrence persistence and workflow state.
- `internal/notification`: chat-room configuration and channel dispatchers.

Evaluation behavior:

- Duplicate Kafka events are ignored per rule and event ID.
- Matching events are counted atomically by rule and fingerprint.
- Below-threshold events remain in Redis only.
- Cooldown events update the active occurrence without sending again.
- Triggered events create or reopen an occurrence and dispatch every configured
  target after PostgreSQL commit.
- A rule may have WebSocket plus multiple Telegram chat-room targets.

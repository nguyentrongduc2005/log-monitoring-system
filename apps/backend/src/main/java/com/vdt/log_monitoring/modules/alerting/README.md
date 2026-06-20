````modules/alerting
  api/
    AlertingFacade.java
    events/
      AlertCreatedEvent.java
      AlertUpdatedEvent.java
  internal/
    rule/
      AlertRuleEntity.java
      AlertRuleRepository.java
      AlertRuleService.java
      AlertRuleMatcher.java
    detection/
      AlertDetectionConsumer.java
      AlertDetectionService.java
      AlertFingerprintService.java
      AlertThresholdService.java
    occurrence/
      AlertEntity.java
      AlertRepository.java
      AlertService.java
    notification/
      NotificationDispatcher.java
      TelegramNotifier.java```
      WebSocketAlertPublisher.java
      ```
````

CRUD alert rule.
Consume processed/realtime log event.
Match rule theo app + level + keyword.
Redis threshold counter.
Redis cooldown dedup.
Lưu alert vào PostgreSQL.
Publish alert realtime.
Telegram để phase sau nếu muốn.

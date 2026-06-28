# Anomaly - Alerting - Incident Redesign Plan

## 1. Muc Tieu

Tai lieu nay mo ta thiet ke moi cho luong anomaly cua du an hien tai. Muc tieu la de AI/agent co the implement dung, khong bi nham giua:

- alert thuong tu log critical/error,
- anomaly log,
- anomaly metric,
- anomaly report,
- incident investigation.

Huong moi:

```text
processing / prometheus metrics
  -> anomaly module detect bat thuong bang rule + threshold
  -> anomaly luu anomaly report nhu mot mini incident report
  -> anomaly publish event nhe sang alerting, co flag trigger AI neu can
  -> alerting tao alert + notify nhu binh thuong
  -> neu can AI, alerting kich hoat luong AI rieng de update report
  -> incident tao tu alert va keo report/evidence theo source_id khi user dieu tra
```

Nguyen tac quan trong:

- `anomaly` so huu detect logic, Redis counter/window/dedup, evidence collection va report.
- `alerting` chi tao alert/notify, khong can hieu sau log/metric anomaly.
- `incident` chi dieu tra tu alert, khong tu detect anomaly.
- Evidence chi tiet nam trong report, khong day het qua event.
- Metric anomaly nen tong hop nhieu metric trong cung window cho mot app, khong nen moi metric tao mot event rieng neu cung la resource-health anomaly.
- Anomaly report phai la noi chua thong tin day du nhat: chuyen gi co the dang xay ra, kha nang/confidence, muc do nghiem trong, can dieu tra gi, evidence nao lien quan, va ket qua AI neu co.
- AI khong goi cho moi lan detect. Chi trigger AI khi dedup lap lai nhieu lan, severity cao, hoac rule/policy danh dau can AI.

## 2. Hien Trang Source Code

### Processing

File chinh:

- `apps/backend/src/main/java/com/vdt/log_monitoring/modules/processing/internal/pipeline/LogProcessingService.java`
- `apps/backend/src/main/java/com/vdt/log_monitoring/modules/processing/internal/publisher/AnomalySignalPublisher.java`
- `apps/backend/src/main/java/com/vdt/log_monitoring/modules/processing/api/events/AnomalySignalEvent.java`

Hien tai processing:

- Luu processed log.
- Publish realtime log.
- Publish `CriticalLogDetectedEvent` cho alerting khi log level la `ERROR` hoac `CRITICAL`.
- Publish `AnomalySignalEvent` sang topic `logs.anomaly.signals` neu log co level error/critical hoac keyword match.

`AnomalySignalEvent` hien tai chi gom:

```java
UUID applicationId;
Instant timestamp;
UUID logId;
String level;
String matchedRule;
String serviceName;
String message;
String traceId;
```

Thieu:

- `fingerprint`,
- `applicationName`,
- `applicationDisplayName`,
- structured attributes nhu `userId`, `username`, `ip`, `endpoint`, `statusCode`.

### Anomaly

File chinh:

- `modules/anomaly/internal/log/AnomalyLogClassifier.java`
- `modules/anomaly/internal/log/AnomalyLogRuleHandler.java`
- `modules/anomaly/internal/rule/AnomalyLogRule.java`
- `modules/anomaly/internal/metric/PrometheusMetricWorker.java`
- `modules/anomaly/internal/metric/PrometheusMetricService.java`
- `modules/anomaly/internal/metric/AnomalyMetricRuleHandler.java`
- `modules/anomaly/internal/collector/*`

Hien tai anomaly co 2 lop logic:

1. Rule/counter Redis:
   - Log signal duoc classify.
   - Counter Redis theo rule + dimension.
   - Active index de collector biet counter nao dang active.
   - Metric snapshots luu Redis theo application + metric rule.

2. Linear scoring:
   - `AnomalyScoreCollectorJob`
   - `AnomalyScoreCollector`
   - `AnomalyLinearScorer`
   - `LogRuleAggregator`
   - `MetricRuleAggregator`

Van de:

- Linear scoring hien chi log result, chua tao alert/report.
- Huong moi khong nen de linear score la nguon tao alert chinh.
- Nen chuyen sang rule-based threshold la chinh, scoring chi giu lai nhu future/secondary signal neu can.

### Alerting

File chinh:

- `modules/alerting/internal/alert/AlertEntity.java`
- `modules/alerting/internal/alert/AlertService.java`
- `modules/alerting/internal/detection/AlertDetectionConsumer.java`
- `modules/alerting/api/AlertingFacade.java`

Hien tai alerting:

- Nhan `CriticalLogDetectedEvent` tu processing.
- Match alert rules.
- Tao alert trong `alerting.alerts`.
- Notify WebSocket/Telegram.

Van de:

- Chua co consumer cho event anomaly.
- Chua co alert field de biet alert den tu source nao.
- Dang co hook tam:

```java
if (rule.name() != null && rule.name().toLowerCase(Locale.ROOT).contains("anomaly")) {
    anomalyReportTriggers.forEach(trigger -> trigger.triggerReport(newAlert.getId(), evidence));
}
```

Hook nay nen bo. Ly do:

- alerting khong nen detect anomaly bang ten rule,
- report khong nen duoc tao sau alert bang sample message mong,
- anomaly module moi la noi tao report/evidence.

### Incident

File chinh:

- `modules/incident/internal/incident/IncidentService.java`
- `modules/incident/internal/evidence/IncidentEvidenceCollector.java`
- `modules/incident/internal/incident/IncidentAnomalyReportEntity.java`
- `api/incident/IncidentController.java`

Hien tai incident:

- Tao incident tu alert.
- Collect trigger alert + top error fingerprints.
- Neu co report theo `alertId`, keo report vao incident evidence.

Van de:

- `incident_anomaly_reports` dang tao boi migration `V16__create_incident_anomaly_reports.sql` khong co schema:

```sql
CREATE TABLE incident_anomaly_reports (...)
```

- Entity cung khong co schema:

```java
@Table(name = "incident_anomaly_reports")
```

- Theo thiet ke moi, report nen thuoc schema/module `anomaly`, khong nen thuoc incident.

## 3. Thiet Ke Dich

### Module Responsibilities

#### Anomaly Module

Anomaly module so huu:

- Rule-based anomaly detection cho log.
- Rule-based anomaly detection cho metric.
- Redis counter/window/cache/dedup cho anomaly.
- Evidence collection cho anomaly.
- Bang `anomaly.anomaly_reports`.
- Publish `AnomalyDetectedEvent` sang alerting.
- AI trigger policy va report state; AI execution co the do incident service rieng xu ly, nhung ket qua phai update lai report qua `AnomalyFacade`.

#### Alerting Module

Alerting module so huu:

- Consume `AnomalyDetectedEvent`.
- Tao alert tu event anomaly.
- Notify user.
- Ack/resolve alert.
- API/UI alert.

Alerting khong so huu:

- Log anomaly rule.
- Metric anomaly rule.
- Evidence collection.
- AI analysis.

#### Incident Module

Incident module so huu:

- Tao incident tu alert.
- Load alert.
- Neu alert co `source_type/source_id` la anomaly, load anomaly report qua facade/API cua anomaly.
- Convert report evidence thanh incident evidence.
- Chay incident AI investigation neu can.

Incident khong so huu:

- Bang anomaly report.
- Detect anomaly.

## 4. Event Contract Moi

### 4.1 Processing -> Anomaly: enrich `AnomalySignalEvent`

Nen mo rong event hien tai:

```java
public record AnomalySignalEvent(
    UUID applicationId,
    String applicationName,
    String applicationDisplayName,
    Instant timestamp,
    UUID logId,
    String level,
    String matchedRule,
    String serviceName,
    String message,
    String fingerprint,
    String traceId,
    Map<String, String> attributes
) {}
```

`attributes` co the de trong luc dau. Sau nay parser/enricher co the gan:

```json
{
  "user": "alice@example.com",
  "account": "alice@example.com",
  "ip": "10.0.0.8",
  "endpoint": "/login",
  "statusCode": "401"
}
```

Neu chua co structured attributes, `AnomalyLogClassifier` van co the fallback regex tu `message` nhu hien tai.

### 4.2 Anomaly -> Alerting: `AnomalyDetectedEvent`

Event nay phai nhe. Khong day sample logs, fingerprints, traceIds, metric time series qua event.

De xuat:

```java
package com.vdt.log_monitoring.modules.anomaly.api.events;

public record AnomalyDetectedEvent(
    UUID anomalyReportId,
    UUID applicationId,
    String applicationName,
    String applicationDisplayName,
    String sourceType,      // ANOMALY_LOG | ANOMALY_METRIC
    String ruleName,
    String severity,        // WARN | ERROR | CRITICAL, match AlertSeverity
    String title,
    String summary,
    Instant windowStart,
    Instant windowEnd,
    boolean aiTriggerRequested,
    String aiTriggerReason,
    Instant detectedAt
) {}
```

Ghi chu:

- `anomalyReportId` la link toi report.
- `sourceType` dung de alerting set `trigger_type/source_type`.
- Evidence chi tiet nam trong `anomaly.anomaly_reports.evidence_payload`.
- `aiTriggerRequested` khong co nghia la alerting phai cho AI xong moi gui alert. Alerting phai tao/gui alert truoc, sau do moi kich hoat luong AI rieng.
- `aiTriggerReason` nen ngan gon, vi du `SEVERITY_CRITICAL`, `REPEATED_DEDUP`, `RULE_REQUIRES_AI`.
- Event key Kafka nen la `applicationId.toString()`.

Topic config de them:

```yaml
app:
  kafka:
    topics:
      anomaly-detected: ${KAFKA_TOPIC_ANOMALY_DETECTED:anomaly.detected}
      anomaly-detected-dlt: ${KAFKA_TOPIC_ANOMALY_DETECTED_DLT:anomaly.detected.DLT}
      anomaly-report-updated: ${KAFKA_TOPIC_ANOMALY_REPORT_UPDATED:anomaly.report.updated}
      anomaly-ai-requested: ${KAFKA_TOPIC_ANOMALY_AI_REQUESTED:anomaly.ai.requested}
    consumer-groups:
      anomaly-detected-alerting: ${KAFKA_CONSUMER_GROUP_ANOMALY_DETECTED_ALERTING:alerting-anomaly-detected-group}
      anomaly-ai-incident: ${KAFKA_CONSUMER_GROUP_ANOMALY_AI_INCIDENT:incident-anomaly-ai-group}
```

Optional event de bao UI/notification khi report duoc AI update:

```java
public record AnomalyReportUpdatedEvent(
    UUID anomalyReportId,
    UUID applicationId,
    String sourceType,
    String updateType,      // AI_STARTED | AI_COMPLETED | AI_FAILED | REPORT_UPDATED
    String aiStatus,
    Instant updatedAt
) {}
```

## 5. Log Anomaly Detection

### 5.1 Rule Model

Log anomaly la threshold theo rule + dimension + window.

Vi du:

```text
LOGIN_FAIL_BY_USER
  keyword: login failed | failed login | bad credentials
  dimension: user/account
  threshold_count: 5
  window: 1m
  severity: WARN

LOGIN_FAIL_BY_IP
  keyword: login failed | failed password
  dimension: ip
  threshold_count: 20
  window: 1m
  severity: ERROR

ERROR_BY_FINGERPRINT
  dimension: fingerprint
  threshold_count: 30
  window: 1m
  severity: ERROR

TIMEOUT_BY_ENDPOINT
  keyword: timeout | timed out | deadline exceeded
  dimension: endpoint/service
  threshold_count: 10
  window: 2m
  severity: WARN
```

Nen thay/bo dan `scoreBands` trong `AnomalyLogRule` bang threshold ro rang:

```java
public enum AnomalyLogRule {
    LOGIN_FAIL_BY_USER(
        Duration.ofMinutes(1),
        List.of("login failed", "failed login", "bad credentials"),
        "user",
        5,
        "WARN",
        AiPolicy.ON_REPEATED_OR_CRITICAL
    );

    private final Duration window;
    private final List<String> keywords;
    private final String dimensionType;
    private final long thresholdCount;
    private final String severity;
    private final AiPolicy aiPolicy;
}
```

Co the giu enum hien tai nhung them method:

```java
public long thresholdCount();
public String severityFor(long count);
public boolean isBreached(long count);
```

### 5.2 Redis Keys

Counter key:

```text
anomaly:{applicationId}:log:{ruleName}:{dimensionType}:{dimensionValue}
```

Active index:

```text
anomaly:{applicationId}:log:{ruleName}:active
```

Dedup/cooldown key:

```text
anomaly:{applicationId}:dedup:log:{ruleName}:{dimensionType}:{dimensionValue}
```

Flow:

```text
AnomalyLogSignalConsumer
  -> AnomalyLogClassifier
  -> AnomalyLogRuleHandler.increment counter
  -> if count >= threshold and dedup key absent:
       collect log evidence
       build report diagnosis/confidence/recommendations
       decide aiTriggerRequested by policy
       save anomaly report
       publish AnomalyDetectedEvent
       set dedup key TTL = cooldown
```

AI trigger policy cho log:

```text
aiTriggerRequested = true neu:
  severity = CRITICAL
  OR observedCount >= thresholdCount * aiMultiplier
  OR same rule/dimension bi dedup lap lai >= repeatedDedupThreshold trong thoi gian gan
  OR rule.aiPolicy = ALWAYS
```

Neu chi moi vuot nguong nhe, `aiTriggerRequested = false`. Report van duoc tao day du va alert van gui som cho user.

### 5.3 Log Evidence

Evidence chi tiet luu trong `anomaly_reports.evidence_payload`, khong gui qua event.

Example:

```json
{
  "source": "LOG",
  "ruleName": "LOGIN_FAIL_BY_USER",
  "dimensionType": "user",
  "dimensionValue": "alice@example.com",
  "observedCount": 8,
  "thresholdCount": 5,
  "confidenceScore": 0.78,
  "hypothesis": "Possible brute-force or credential stuffing activity against one account.",
  "recommendedActions": [
    "Check whether the account owner initiated these attempts.",
    "Inspect source IP reputation and recent successful logins.",
    "Temporarily enforce MFA or lock the account if attempts continue."
  ],
  "windowStart": "2026-06-28T10:00:00Z",
  "windowEnd": "2026-06-28T10:01:00Z",
  "sampleMessages": [
    "login failed user=alice@example.com ip=10.0.0.8",
    "bad credentials user=alice@example.com ip=10.0.0.8"
  ],
  "fingerprints": [
    "sha256..."
  ],
  "traceIds": [
    "trace-1",
    "trace-2"
  ],
  "relatedLogIds": [
    "..."
  ]
}
```

`fingerprints` va `traceIds` dung cho incident/AI correlation, khong dung cho alerting.

## 6. Metric Anomaly Detection

### 6.1 Khong Nen Tao Mot Event Rieng Cho Tung Metric

Metric nen detect theo app + window, va tao mot report/event tong hop neu co mot hoac nhieu metric vuot nguong trong cung lan evaluate.

Vi du trong cung app:

```text
CPU = 92%
MEMORY = 91%
DISK = 70%
NETWORK_RX normal
```

Nen tao mot anomaly report:

```text
sourceType = ANOMALY_METRIC
ruleName = RESOURCE_HEALTH_THRESHOLD
title = "Resource anomaly detected for Payment Service"
summary = "CPU 92% and memory 91% exceeded configured thresholds."
confidenceScore = 0.82
hypothesis = "The service may be under resource pressure or a recent deployment may have introduced heavier workload."
```

Khong nen tao 2 alert rieng CPU va memory neu chung cung window va cung app, vi de spam notification.

### 6.2 Metric Rule Model

`AnomalyMetricRule` nen chuyen tu score band sang threshold ro rang:

```java
public enum AnomalyMetricRule {
    CPU_USAGE("%", 85.0, 90.0, "ERROR"),
    MEMORY_USAGE("%", 90.0, 95.0, "ERROR"),
    DISK_USAGE("%", 90.0, 95.0, "ERROR"),
    DISK_WRITE_RATE("bytes/sec", 50_000_000.0, 100_000_000.0, "WARN"),
    NETWORK_RX_RATE("bytes/sec", 100_000_000.0, 250_000_000.0, "WARN"),
    NETWORK_TX_RATE("bytes/sec", 100_000_000.0, 250_000_000.0, "WARN");

    private final String unit;
    private final double warningThreshold;
    private final double criticalThreshold;
}
```

Severity calculation:

```text
current >= criticalThreshold -> CRITICAL
current >= warningThreshold -> ERROR/WARN
else normal
```

### 6.3 Metric Evidence

Metric evidence luu trong report:

```json
{
  "source": "METRIC",
  "ruleName": "RESOURCE_HEALTH_THRESHOLD",
  "windowStart": "2026-06-28T10:00:00Z",
  "windowEnd": "2026-06-28T10:01:00Z",
  "confidenceScore": 0.82,
  "hypothesis": "Resource pressure detected on CPU and memory in the same window.",
  "recommendedActions": [
    "Check recent deployment or traffic spike for this application.",
    "Inspect CPU-bound requests and memory allocation patterns.",
    "Compare with application error logs in the same window."
  ],
  "breachedMetrics": [
    {
      "metricName": "CPU_USAGE",
      "currentValue": 92.5,
      "thresholdValue": 85.0,
      "criticalThresholdValue": 90.0,
      "unit": "%",
      "avg": 89.2,
      "max": 94.1,
      "samples": 4,
      "lastSeenAt": "2026-06-28T10:01:00Z"
    },
    {
      "metricName": "MEMORY_USAGE",
      "currentValue": 91.3,
      "thresholdValue": 90.0,
      "criticalThresholdValue": 95.0,
      "unit": "%",
      "avg": 88.7,
      "max": 93.0,
      "samples": 6,
      "lastSeenAt": "2026-06-28T10:01:00Z"
    }
  ],
  "normalMetrics": [
    {
      "metricName": "DISK_USAGE",
      "currentValue": 70.0,
      "unit": "%"
    }
  ]
}
```

Metric report co the enrich them related logs quanh window, nhung day la optional:

```json
{
  "relatedTopErrorFingerprints": [
    {
      "fingerprint": "...",
      "count": 12,
      "sampleMessage": "..."
    }
  ]
}
```

### 6.4 Metric Detection Flow

Hien tai `PrometheusMetricWorker` collect metric per enabled metric source va luu snapshot Redis.

Can them worker/detector moi:

```text
MetricAnomalyDetectorJob
  -> find all active application IDs with metric snapshots
  -> read snapshots for all AnomalyMetricRule
  -> evaluate breached metrics together
  -> if breachedMetrics not empty and dedup absent:
       build report diagnosis/confidence/recommendations
       decide aiTriggerRequested by policy
       create anomaly report with evidence_payload
       publish AnomalyDetectedEvent
       set dedup key
```

Dedup key cho metric:

```text
anomaly:{applicationId}:dedup:metric:RESOURCE_HEALTH
```

Neu muon tach disk/network thanh nhom rieng:

```text
RESOURCE_HEALTH: CPU/MEMORY/DISK_USAGE
IO_HEALTH: DISK_WRITE_RATE/NETWORK_RX_RATE/NETWORK_TX_RATE
```

Lam don gian truoc:

- Mot group `RESOURCE_HEALTH`.
- Mot event/report cho tat ca metric breached trong cung app/window.

AI trigger policy cho metric:

```text
aiTriggerRequested = true neu:
  co it nhat mot metric CRITICAL
  OR co tu 2 metric tro len cung vuot nguong trong mot window
  OR cung app bi dedup RESOURCE_HEALTH lap lai >= repeatedDedupThreshold
  OR breached metric trung voi spike error log trong cung window
```

Neu chi mot metric vuot nguong warning nhe va chua lap lai, chi tao report + alert som, chua can goi AI.

## 7. Database Design

### 7.1 Move Report To Anomaly Schema

Hien tai `V16__create_incident_anomaly_reports.sql` tao table khong schema. Can thay bang migration moi. Neu database da ap dung V16 trong moi truong local/dev, tao migration moi de move:

```sql
CREATE SCHEMA IF NOT EXISTS anomaly;

CREATE TABLE IF NOT EXISTS anomaly.anomaly_reports (
    id UUID NOT NULL PRIMARY KEY,
    application_id UUID NOT NULL,
    source_type VARCHAR(32) NOT NULL,
    rule_name VARCHAR(120) NOT NULL,
    severity VARCHAR(32) NOT NULL,
    status VARCHAR(32) NOT NULL,
    title VARCHAR(180) NOT NULL,
    summary TEXT,
    hypothesis TEXT,
    confidence_score DOUBLE PRECISION,
    likelihood_label VARCHAR(32),
    impact_summary TEXT,
    investigation_steps JSONB,
    recommended_actions JSONB,
    dimension_type VARCHAR(64),
    dimension_value VARCHAR(255),
    metric_group VARCHAR(120),
    observed_value DOUBLE PRECISION,
    threshold_value DOUBLE PRECISION,
    observed_count BIGINT,
    threshold_count BIGINT,
    window_start TIMESTAMPTZ NOT NULL,
    window_end TIMESTAMPTZ NOT NULL,
    evidence_payload JSONB NOT NULL,
    ai_trigger_requested BOOLEAN NOT NULL DEFAULT FALSE,
    ai_trigger_reason VARCHAR(120),
    ai_status VARCHAR(32) NOT NULL DEFAULT 'NOT_REQUESTED',
    ai_model VARCHAR(120),
    ai_prompt_version VARCHAR(64),
    ai_started_at TIMESTAMPTZ,
    ai_completed_at TIMESTAMPTZ,
    ai_summary TEXT,
    ai_confidence_score DOUBLE PRECISION,
    ai_likelihood_label VARCHAR(32),
    ai_root_cause_candidates JSONB,
    ai_recommended_actions JSONB,
    ai_investigation_steps JSONB,
    ai_result JSONB,
    ai_raw_response JSONB,
    ai_error TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_anomaly_reports_application
        FOREIGN KEY (application_id)
        REFERENCES identity.applications (id)
        ON DELETE CASCADE,
    CONSTRAINT ck_anomaly_reports_source_type
        CHECK (source_type IN ('ANOMALY_LOG', 'ANOMALY_METRIC')),
    CONSTRAINT ck_anomaly_reports_severity
        CHECK (severity IN ('WARN', 'WARNING', 'ERROR', 'CRITICAL')),
    CONSTRAINT ck_anomaly_reports_status
        CHECK (status IN ('DETECTED', 'ALERTED', 'AI_PENDING', 'AI_SUCCEEDED', 'AI_FAILED')),
    CONSTRAINT ck_anomaly_reports_ai_status
        CHECK (ai_status IN ('NOT_REQUESTED', 'PENDING', 'SUCCEEDED', 'FAILED')),
    CONSTRAINT ck_anomaly_reports_confidence_score
        CHECK (confidence_score IS NULL OR (confidence_score >= 0 AND confidence_score <= 1)),
    CONSTRAINT ck_anomaly_reports_ai_confidence_score
        CHECK (ai_confidence_score IS NULL OR (ai_confidence_score >= 0 AND ai_confidence_score <= 1)),
    CONSTRAINT ck_anomaly_reports_window
        CHECK (window_start <= window_end)
);

CREATE INDEX idx_anomaly_reports_application_created_at
    ON anomaly.anomaly_reports (application_id, created_at DESC);

CREATE INDEX idx_anomaly_reports_source_rule_window
    ON anomaly.anomaly_reports (source_type, rule_name, window_start, window_end);

CREATE INDEX idx_anomaly_reports_dimension
    ON anomaly.anomaly_reports (application_id, rule_name, dimension_type, dimension_value);
```

Report field meaning:

- `title`: tieu de ngan gon cho alert/UI.
- `summary`: tom tat su kien anomaly da detect.
- `hypothesis`: nhan dinh ban dau cua rule engine, vi du "possible brute-force".
- `confidence_score`: confidence cua rule engine, tu 0 den 1.
- `likelihood_label`: human label nhu `LOW`, `MEDIUM`, `HIGH`, `VERY_HIGH`.
- `impact_summary`: tac dong co the co.
- `investigation_steps`: cac buoc nen kiem tra, dang JSON array.
- `recommended_actions`: cac hanh dong de xuat, dang JSON array.
- `evidence_payload`: evidence source of truth, gom logs/metrics/fingerprints/traceIds/samples.
- `ai_trigger_requested`: anomaly policy quyet dinh co can goi AI khong.
- `ai_trigger_reason`: vi sao goi AI, vi du `SEVERITY_CRITICAL`, `REPEATED_DEDUP`, `MULTI_METRIC_BREACH`.
- `ai_status`: trang thai chay AI rieng cho report.
- `ai_summary`, `ai_confidence_score`, `ai_likelihood_label`: field query/hien thi nhanh.
- `ai_root_cause_candidates`, `ai_recommended_actions`, `ai_investigation_steps`: JSON structured output cho UI.
- `ai_result`: object chuan hoa ma backend tu AI response map ra.
- `ai_raw_response`: payload goc AI tra ve de debug/audit, neu can luu.

Report phai luu du thong tin de mo trang report rieng ma khong can query lai log/metric ngay lap tuc. Query lai ClickHouse/trace chi nen dung khi user tao incident hoac khi AI flow can phan tich sau hon.

Neu can migrate data cu:

```sql
INSERT INTO anomaly.anomaly_reports (
    id,
    application_id,
    source_type,
    rule_name,
    severity,
    status,
    title,
    summary,
    window_start,
    window_end,
    evidence_payload,
    ai_trigger_requested,
    ai_status,
    ai_result,
    created_at,
    updated_at
)
SELECT
    old.id,
    alert.application_id,
    'ANOMALY_LOG',
    'LEGACY_ANOMALY',
    'ERROR',
    CASE WHEN old.status = 'PENDING' THEN 'DETECTED' ELSE 'ALERTED' END,
    'Legacy anomaly report',
    'Legacy anomaly report migrated from incident_anomaly_reports',
    COALESCE(alert.first_seen_at, old.created_at),
    COALESCE(alert.last_seen_at, old.updated_at),
    old.evidence_payload,
    FALSE,
    CASE WHEN old.ai_analysis_result IS NULL THEN 'NOT_REQUESTED' ELSE 'SUCCEEDED' END,
    CASE WHEN old.ai_analysis_result IS NULL THEN NULL ELSE to_jsonb(old.ai_analysis_result) END,
    old.created_at,
    old.updated_at
FROM incident_anomaly_reports old
JOIN alerting.alerts alert ON alert.id = old.alert_id;
```

Sau khi migrate va code khong con dung table cu:

```sql
DROP TABLE IF EXISTS incident_anomaly_reports;
```

Neu du an chua can migrate data local, co the tao table moi va drop table cu trong migration rieng. Khong sua file migration cu da co the da apply.

### 7.2 Alert Table Additions

Can them field vao `alerting.alerts`:

```sql
ALTER TABLE alerting.alerts
    ADD COLUMN trigger_type VARCHAR(32) NOT NULL DEFAULT 'LOG_RULE',
    ADD COLUMN source_type VARCHAR(32),
    ADD COLUMN source_id UUID,
    ADD COLUMN summary TEXT,
    ADD COLUMN metadata_json JSONB;

ALTER TABLE alerting.alerts
    ADD CONSTRAINT ck_alerts_trigger_type
    CHECK (trigger_type IN ('LOG_RULE', 'ANOMALY_LOG', 'ANOMALY_METRIC'));

ALTER TABLE alerting.alerts
    ADD CONSTRAINT ck_alerts_source_type
    CHECK (source_type IS NULL OR source_type IN ('LOG', 'METRIC', 'ANOMALY_REPORT'));

CREATE INDEX idx_alerts_trigger_source
    ON alerting.alerts (trigger_type, source_id);
```

Meaning:

- `trigger_type`: loai alert.
- `source_type`: source object la gi.
- `source_id`: anomaly report id neu la anomaly alert.
- `summary`: text ngan cho UI/notification.
- `metadata_json`: optional context nhe, khong phai evidence source of truth.

### 7.3 Alert Entity/DTO Changes

Update:

- `AlertEntity`
- `AlertingFacade.AlertDto`
- `AlertingFacadeImpl.mapAlert`
- frontend alert types/API if needed.

Add fields:

```java
String triggerType;
String sourceType;
UUID sourceId;
String summary;
String metadataJson;
```

### 7.4 Anomaly Report Entity/Facade

Tao public facade cho anomaly report de module khac khong import internal:

```java
package com.vdt.log_monitoring.modules.anomaly.api;

public interface AnomalyFacade {
    AnomalyReportDto findReportById(UUID id);
    List<AnomalyReportDto> findReports(List<UUID> visibleApplicationIds);
    void markAlerted(UUID reportId, UUID alertId);
    void markAiPending(UUID reportId, String reason);
    void updateAiResult(UUID reportId, AnomalyAiResult result);
    void updateAiFailure(UUID reportId, String error);
}
```

DTO:

```java
record AnomalyReportDto(
    UUID id,
    UUID applicationId,
    String sourceType,
    String ruleName,
    String severity,
    String status,
    String title,
    String summary,
    String hypothesis,
    Double confidenceScore,
    String likelihoodLabel,
    String impactSummary,
    String investigationStepsJson,
    String recommendedActionsJson,
    String dimensionType,
    String dimensionValue,
    String metricGroup,
    Double observedValue,
    Double thresholdValue,
    Long observedCount,
    Long thresholdCount,
    Instant windowStart,
    Instant windowEnd,
    String evidencePayloadJson,
    boolean aiTriggerRequested,
    String aiTriggerReason,
    String aiStatus,
    String aiModel,
    String aiPromptVersion,
    Instant aiStartedAt,
    Instant aiCompletedAt,
    String aiSummary,
    Double aiConfidenceScore,
    String aiLikelihoodLabel,
    String aiRootCauseCandidatesJson,
    String aiRecommendedActionsJson,
    String aiInvestigationStepsJson,
    String aiResultJson,
    String aiRawResponseJson,
    String aiError,
    Instant createdAt,
    Instant updatedAt
) {}
```

`AnomalyAiResult` nen la DTO chuan hoa, khong phu thuoc internal incident:

```java
record AnomalyAiResult(
    String model,
    String promptVersion,
    String summary,
    Double confidenceScore,
    String likelihoodLabel,
    String rootCauseCandidatesJson,
    String recommendedActionsJson,
    String investigationStepsJson,
    String resultJson,
    String rawResponseJson
) {}
```

## 8. Alerting Implementation

### 8.1 Add Consumer

Tao:

```text
modules/alerting/internal/detection/AnomalyDetectedConsumer.java
modules/alerting/internal/detection/AnomalyAlertService.java
```

Consumer:

```java
@KafkaListener(
    topics = "${app.kafka.topics.anomaly-detected}",
    groupId = "${app.kafka.consumer-groups.anomaly-detected-alerting}"
)
public void consume(AnomalyDetectedEvent event, Acknowledgment ack) {
    anomalyAlertService.createOrUpdateAlert(event);
    ack.acknowledge();
}
```

Thu tu trong `createOrUpdateAlert` phai la:

```text
1. Tao/update alert tu anomaly event.
2. Gui notification alert truoc qua websocket/telegram theo delivery target.
3. Mark anomaly report = ALERTED va gan alertId neu schema co field alert_id.
4. Neu event.aiTriggerRequested = true:
     publish AnomalyAiRequestedEvent hoac goi facade rieng request AI.
```

Khong cho AI nam tren critical path cua alert. User phai nhan duoc canh bao som truoc.

### 8.2 Alert Creation From Anomaly

Do not use alert rules table for anomaly alert creation in phase 1. Anomaly already decided threshold. Alerting only persists and notifies.

Need new method in `AlertService`:

```java
public AlertEntity createFromAnomaly(AnomalyDetectedEvent event, Set<AlertDeliveryTarget> deliveryTargets)
```

However delivery targets are rule-based today. Options:

1. Use default channels for anomaly, e.g. WebSocket only.
2. Reuse application alert rules with special rule name.
3. Add anomaly notification config later.

Recommendation phase 1:

- WebSocket always.
- Telegram optional only if existing application rule config can be reused safely.
- Keep implementation small: create alert with delivery target `WEBSOCKET`.
- Neu `aiTriggerRequested = true`, van tao alert ngay, sau do moi request AI.
- Khi AI update report xong, gui report update event qua WebSocket den trang report va optionally Telegram summary neu user co quyen/app subscription.

Dedup in alerting:

- Find active alert by `trigger_type + source_id` for anomaly.
- If exists, call `recordOccurrence/retrigger`.
- Else create new alert.

Need repository method:

```java
Optional<AlertEntity> findFirstByTriggerTypeAndSourceIdAndStatusNotOrderByTriggeredAtDesc(
    String triggerType,
    UUID sourceId,
    AlertStatus status
);
```

For repeated anomaly on same dimension/window but new report id, dedup should already happen in anomaly via Redis. Alerting dedup by `source_id` is just idempotency.

### 8.3 AI Request Dispatch

Can tach luong AI thanh mot command/event rieng de khong cham vao logic incident creation:

```java
public record AnomalyAiRequestedEvent(
    UUID anomalyReportId,
    UUID applicationId,
    UUID alertId,
    String sourceType,
    String triggerReason,
    Instant requestedAt
) {}
```

Option recommended:

- Alerting publish `AnomalyAiRequestedEvent` after alert is saved and notification initial is sent.
- Incident module consume event bang group `incident-anomaly-ai-group`.
- Incident goi service rieng `IncidentAnomalyReportAiService`, khong dung `IncidentService.startFromAlert`.
- Service load report/evidence tu `AnomalyFacade.findReportById(reportId)`.
- Service goi AI bang prompt rieng cho anomaly report.
- Service update report qua `AnomalyFacade.updateAiResult(...)` hoac `updateAiFailure(...)`.
- Sau khi update, publish `AnomalyReportUpdatedEvent`.

Neu khong muon them topic trong phase 1, alerting co the goi `IncidentFacade.requestAnomalyReportAi(reportId, alertId, reason)` sau khi gui alert. Tuy nhien event async tot hon vi giam coupling va tranh lam cham alert request.

### 8.4 Report Update Notification

Khi nhan `AnomalyReportUpdatedEvent`, alerting/gui realtime can:

- Gui WebSocket event den report page, khong chi alert page.
- Scope theo `applicationId` de user khong co quyen app khong nhan duoc.
- Neu AI completed va delivery config cho phep, gui Telegram update ngan gon:
  - report title,
  - AI summary,
  - confidence/likelihood,
  - link/report id.

Khong gui full evidence payload qua WebSocket/Telegram. Client se fetch report detail bang API va backend se enforce application visibility.

### 8.5 Remove Old Hook

Remove:

- `AnomalyReportTrigger` interface in alerting.
- `IncidentAnomalyReportTriggerImpl`.
- calls in `AlertService` that check `rule.name().contains("anomaly")`.

## 9. Incident Implementation

### 9.1 Load Anomaly Report By Alert Source

Current `IncidentEvidenceCollector` finds report by `alertId`. New flow:

```text
alert.source_id = anomalyReportId
alert.trigger_type = ANOMALY_LOG or ANOMALY_METRIC
```

Incident should:

```java
if (alert.triggerType is ANOMALY_LOG or ANOMALY_METRIC) {
    AnomalyReportDto report = anomalyFacade.findReportById(alert.sourceId());
    add report evidence;
}
```

Evidence candidate:

```java
new IncidentEvidenceCandidate(
    EvidenceType.LOG or EvidenceType.HEALTH,
    "ANOMALY_REPORT:" + report.id(),
    report.applicationId(),
    null,
    report.severity(),
    report.title(),
    report.evidencePayloadJson(),
    report.windowStart(),
    "{\"kind\":\"ANOMALY_REPORT\",\"sourceType\":\"...\"}"
)
```

For metric anomaly, use `EvidenceType.HEALTH`.

For log anomaly, use `EvidenceType.LOG`.

### 9.2 Keep Normal Alert Flow

Normal alert still:

- Trigger alert evidence.
- Top error fingerprints from ClickHouse.
- AI incident analysis.

Do not break:

```http
POST /api/v1/incidents/from-alert/{alertId}
```

### 9.3 Separate AI Flow For Anomaly Report

Can them service rieng trong incident:

```text
modules/incident/internal/ai/IncidentAnomalyReportAiService.java
```

Hoac facade method:

```java
public interface IncidentFacade {
    void requestAnomalyReportAi(UUID reportId, UUID alertId, String triggerReason);
}
```

Service nay khac voi tao incident tu alert:

- Khong tao incident.
- Khong chay `IncidentEvidenceCollector.collectFromTriggerAlert`.
- Khong query group log/trace nhu flow incident investigation mac dinh.
- Chi load report tu `AnomalyFacade.findReportById(reportId)`.
- Lay `evidencePayloadJson`, `hypothesis`, `confidenceScore`, `recommendedActions`, `windowStart/windowEnd` tu report.
- Build AI prompt rieng: "phan tich anomaly report nay va cap nhat nhan dinh".
- Goi AI.
- Update lai report qua `AnomalyFacade.updateAiResult`.
- Publish `AnomalyReportUpdatedEvent` de alerting/UI gui update.

Ly do tach rieng:

- Flow tao incident tu alert can doc fingerprint/traceId de query log group va trace chain.
- Flow AI cho anomaly report chi can report evidence da tong hop san.
- Hai flow co muc tieu khac nhau, neu gom chung se de gay bug va query thua.

## 10. Frontend/API Changes

Current frontend imports anomaly reports from incident API:

- `features/incidents/incident-api.ts`
- `features/alerts/AnomalyReportsTab.tsx`

Need move APIs:

```http
GET /api/v1/anomaly/reports
GET /api/v1/anomaly/reports/{id}
```

Implementation:

- Add `api/anomaly/AnomalyReportController`.
- Use `AnomalyFacade`.
- Filter by visible application ids using identity/application access like incident controller.
- Update frontend API file or create `features/anomaly/anomaly-api.ts`.
- Report detail page should show:
  - rule/source/severity/window,
  - hypothesis + confidence/likelihood,
  - recommended actions/investigation steps,
  - evidence summary,
  - AI status,
  - AI summary/root cause candidates/actions after available.
- WebSocket report updates must be scoped by `applicationId`.
- Client should fetch report detail after receiving `AnomalyReportUpdatedEvent`; realtime payload should not contain full evidence/AI raw response.

Keep old incident endpoints temporarily if needed:

- `GET /api/v1/incidents/anomaly-reports`
- mark deprecated or delegate to anomaly facade.

## 11. AI Handling

AI khong phai buoc bat buoc cua detect anomaly. Report phai co nhan dinh rule-based truoc, AI chi enrich them khi policy yeu cau.

### 11.1 AI Trigger Policy

Anomaly module quyet dinh `aiTriggerRequested` khi tao report/event.

Nen trigger AI khi:

- severity la `CRITICAL`,
- count/value vuot nguong nghiem trong hon nhieu so voi threshold,
- cung rule/dimension/app bi dedup lap lai nhieu lan trong khoang thoi gian gan,
- metric anomaly co nhieu metric cung breach,
- log anomaly co dau hieu security-sensitive nhu brute-force, privilege escalation, suspicious admin action,
- rule config dat `aiPolicy = ALWAYS`.

Khong nen trigger AI khi:

- chi vuot nguong warning nhe lan dau,
- event thieu evidence,
- report dang co `ai_status = PENDING`,
- report da co AI result thanh cong va chua co evidence moi dang ke.

### 11.2 Execution Owner

Khong dat AI call trong alerting detect path.

Recommended flow:

```text
Anomaly detect -> save report -> publish AnomalyDetectedEvent(aiTriggerRequested=true)
Alerting consume -> create alert -> send initial WebSocket/Telegram
Alerting publish AnomalyAiRequestedEvent
Incident consume -> IncidentAnomalyReportAiService analyzes report evidence
Incident updates anomaly report through AnomalyFacade
Anomaly/Incident publish AnomalyReportUpdatedEvent
Alerting/WebSocket sends report update scoped by applicationId
```

Incident module duoc dung de goi AI vi hien tai logic AI/investigation gan voi incident domain, nhung phai tao service rieng cho anomaly report. Khong reuse truc tiep flow tao incident tu alert.

### 11.3 Prompt Input

AI input cho anomaly report lay tu report, khong query nhu incident creation:

```json
{
  "reportId": "...",
  "applicationId": "...",
  "sourceType": "ANOMALY_LOG",
  "ruleName": "LOGIN_FAIL_BY_USER",
  "severity": "ERROR",
  "title": "...",
  "summary": "...",
  "hypothesis": "...",
  "confidenceScore": 0.78,
  "windowStart": "...",
  "windowEnd": "...",
  "evidencePayload": {},
  "recommendedActions": []
}
```

AI output nen map ve structured JSON:

```json
{
  "summary": "Likely credential stuffing against one account.",
  "confidenceScore": 0.84,
  "likelihoodLabel": "HIGH",
  "rootCauseCandidates": [
    {
      "cause": "Credential stuffing or password spraying",
      "probability": 0.72,
      "reason": "Repeated failed login attempts for one user in a short window."
    }
  ],
  "investigationSteps": [
    "Check whether any successful login happened after the failed attempts.",
    "Review source IP reputation and geo-location.",
    "Inspect related trace IDs if available."
  ],
  "recommendedActions": [
    "Notify the account owner.",
    "Temporarily require MFA challenge.",
    "Block or rate-limit the source IP if attempts continue."
  ],
  "needsIncident": true
}
```

Store:

- normalized values in `ai_summary`, `ai_confidence_score`, `ai_likelihood_label`, `ai_root_cause_candidates`, `ai_investigation_steps`, `ai_recommended_actions`,
- full normalized object in `ai_result`,
- raw model response in `ai_raw_response` if useful.

### 11.4 Notification After AI

Khi AI xong:

- update report first,
- publish `AnomalyReportUpdatedEvent`,
- WebSocket push update to report page by `applicationId`,
- Telegram/webhook can send a second concise update if configured,
- never expose report update to users without application permission.

## 12. Implementation Order

### Phase 1 - Contracts and Schema

1. Add `AnomalyDetectedEvent`.
2. Add Kafka config for `anomaly.detected`.
3. Add migration for `anomaly` schema and `anomaly.anomaly_reports`.
4. Add migration for alert fields:
   - `trigger_type`
   - `source_type`
   - `source_id`
   - `summary`
   - `metadata_json`
5. Add anomaly report entity/repository/service/facade under module `anomaly`.
6. Add AI/report fields:
   - `hypothesis`
   - `confidence_score`
   - `recommended_actions`
   - `investigation_steps`
   - `ai_trigger_requested`
   - `ai_trigger_reason`
   - `ai_*` result fields.
7. Keep old `incident_anomaly_reports` until new flow works; remove later.

Tests:

- Migration validation via app context if available.
- Unit test report entity/service mapping.

### Phase 2 - Alerting Consumer

1. Extend `AlertEntity`.
2. Extend `AlertingFacade.AlertDto`.
3. Add mapper fields in `AlertingFacadeImpl`.
4. Add `AnomalyDetectedConsumer`.
5. Add `AlertService.createFromAnomaly`.
6. Notify via existing dispatcher.
7. If `event.aiTriggerRequested`, publish `AnomalyAiRequestedEvent` after initial notification.
8. Add handling for `AnomalyReportUpdatedEvent` so WebSocket report page can update by application scope.
9. Remove old `AnomalyReportTrigger` hook after new consumer works.

Tests:

- `AlertServiceTest` creates anomaly alert with `source_id`.
- Duplicate same event/report does not create duplicate active alert.
- `AnomalyDetectedConsumerTest` acks message and calls service.
- AI trigger path sends initial alert before requesting AI.
- Report update notification is scoped by application id.

### Phase 3 - Log Anomaly Detector

1. Extend `AnomalySignalEvent` with `fingerprint`, app names, optional attributes.
2. Update `AnomalySignalPublisher`.
3. Refactor `AnomalyLogRule` to threshold model.
4. Update `AnomalyLogClassifier` to prefer attributes then regex fallback.
5. Update `AnomalyLogRuleHandler`:
   - increment counter,
   - compare threshold,
   - check dedup key,
   - collect evidence,
   - build report hypothesis/confidence/recommended actions,
   - decide `aiTriggerRequested`,
   - save report,
   - publish event.
6. Implement log evidence collector:
   - from Redis counter state,
   - from current event,
   - optionally query ClickHouse for sample logs by app + fingerprint/dimension/window.

Tests:

- Rule below threshold only increments Redis.
- Rule crossing threshold creates report and publishes event.
- Dedup key prevents repeated event spam.
- Evidence payload contains sample messages/fingerprint/traceIds.
- Critical/repeated dedup case sets `aiTriggerRequested = true`.
- First light warning case sets `aiTriggerRequested = false`.

### Phase 4 - Metric Anomaly Detector

1. Keep `PrometheusMetricWorker` collecting snapshots per enabled metric source.
2. Add detector job after snapshots are saved:

```text
MetricAnomalyDetectorJob
```

3. For each app:
   - read all metric snapshots,
   - find breached metrics,
   - aggregate into one report with hypothesis/confidence/actions,
   - decide `aiTriggerRequested`,
   - publish one event if breached metrics not empty.
4. Add metric dedup key.
5. Evidence payload contains `breachedMetrics[]` and `normalMetrics[]`.

Tests:

- One breached CPU metric creates one report/event.
- CPU + memory breached creates one aggregated report/event, not two.
- No breached metric creates no report/event.
- Dedup suppresses repeated metric alert within cooldown.
- Multi-metric breach or critical metric sets `aiTriggerRequested = true`.

### Phase 5 - Incident Integration

1. Inject `AnomalyFacade` into incident evidence collector/service.
2. If alert trigger type is anomaly:
   - load report by `alert.sourceId`,
   - add report evidence.
3. Keep normal alert evidence path.
4. Remove dependency on old `IncidentAnomalyReportRepository`.
5. Add `IncidentAnomalyReportAiService` or equivalent facade method:
   - load report by id,
   - build AI prompt from report,
   - update report AI fields through `AnomalyFacade`,
   - publish `AnomalyReportUpdatedEvent`.
6. Ensure this service does not call `startFromAlert` and does not run normal incident evidence collection.

Tests:

- Incident from normal alert still works.
- Incident from anomaly log alert includes anomaly report evidence.
- Incident from anomaly metric alert includes health evidence.
- Anomaly AI request updates report without creating an incident.
- AI failure updates `ai_status = FAILED` and stores error.

### Phase 6 - Frontend/API

1. Add anomaly report API endpoints.
2. Move frontend anomaly report API from incident feature to anomaly/alerts feature.
3. Display source type, rule name, window, severity, evidence summary.
4. Alert detail should show trigger type/source id if present.

Tests:

- API controller auth/visibility tests.
- Frontend unit tests if existing pattern supports it.

### Phase 7 - Cleanup

1. Remove `incident_anomaly_reports` table/entity/repository after migration.
2. Remove incident facade methods:
   - `generateAnomalyReport`
   - `findAnomalyReports`
   - `findAnomalyReportById`
3. Remove `AnomalyReportTrigger`.
4. Deprecate/remove `AnomalyLinearScorer` as alert source.
5. Keep scoring classes only if future dashboard needs them; otherwise remove to reduce complexity.

## 13. Acceptance Criteria

### Functional

- Log anomaly crossing threshold creates exactly one anomaly report and one alert.
- Metric anomaly with multiple breached metrics creates one aggregated report and one alert.
- Alert notification works through existing alerting notification path.
- Incident from anomaly alert includes anomaly report evidence.
- Report contains rule-based diagnosis before AI:
  - hypothesis,
  - confidence/likelihood,
  - recommended actions,
  - investigation steps.
- AI is triggered only by policy, not for every anomaly.
- When AI is triggered, initial alert is sent before AI starts.
- AI updates anomaly report without creating an incident.
- Report update notification reaches WebSocket report page after AI completion.
- Report update notification is scoped by application visibility.
- Normal critical/error alert flow still works.
- Existing alert ack/resolve still works.

### Data

- All new reports are in `anomaly.anomaly_reports`.
- Alert anomaly rows have:
  - `trigger_type = ANOMALY_LOG` or `ANOMALY_METRIC`,
  - `source_type = ANOMALY_REPORT`,
  - `source_id = anomalyReportId`.
- Detailed evidence stays in report `evidence_payload`, not in Kafka event.
- AI output is persisted in report fields and JSON payload:
  - `ai_summary`,
  - `ai_confidence_score`,
  - `ai_root_cause_candidates`,
  - `ai_recommended_actions`,
  - `ai_investigation_steps`,
  - `ai_result`,
  - optionally `ai_raw_response`.

### Noise Control

- Log anomaly dedup key prevents repeated alert spam for same rule/dimension.
- Metric anomaly dedup key prevents repeated alert spam for same app/resource group.
- Alerting remains idempotent by `source_id`.
- Repeated dedup signals can increase AI need/confidence without spamming new alerts.

### Module Boundary

- No module imports `*.internal.*` from another module.
- Cross-module access only through:
  - public events,
  - public facades under `modules.<module>.api`.

## 14. Files Likely To Change

### Add

```text
apps/backend/src/main/java/com/vdt/log_monitoring/modules/anomaly/api/AnomalyFacade.java
apps/backend/src/main/java/com/vdt/log_monitoring/modules/anomaly/api/events/AnomalyDetectedEvent.java
apps/backend/src/main/java/com/vdt/log_monitoring/modules/anomaly/api/events/AnomalyAiRequestedEvent.java
apps/backend/src/main/java/com/vdt/log_monitoring/modules/anomaly/api/events/AnomalyReportUpdatedEvent.java
apps/backend/src/main/java/com/vdt/log_monitoring/modules/anomaly/internal/report/AnomalyReportEntity.java
apps/backend/src/main/java/com/vdt/log_monitoring/modules/anomaly/internal/report/AnomalyReportRepository.java
apps/backend/src/main/java/com/vdt/log_monitoring/modules/anomaly/internal/report/AnomalyReportService.java
apps/backend/src/main/java/com/vdt/log_monitoring/modules/anomaly/internal/AnomalyFacadeImpl.java
apps/backend/src/main/java/com/vdt/log_monitoring/modules/anomaly/internal/publisher/AnomalyDetectedPublisher.java
apps/backend/src/main/java/com/vdt/log_monitoring/modules/anomaly/internal/publisher/AnomalyReportUpdatedPublisher.java
apps/backend/src/main/java/com/vdt/log_monitoring/modules/anomaly/internal/metric/MetricAnomalyDetectorJob.java
apps/backend/src/main/java/com/vdt/log_monitoring/modules/alerting/internal/detection/AnomalyDetectedConsumer.java
apps/backend/src/main/java/com/vdt/log_monitoring/modules/alerting/internal/detection/AnomalyAlertService.java
apps/backend/src/main/java/com/vdt/log_monitoring/modules/alerting/internal/detection/AnomalyReportUpdatedConsumer.java
apps/backend/src/main/java/com/vdt/log_monitoring/modules/incident/internal/ai/IncidentAnomalyReportAiService.java
apps/backend/src/main/java/com/vdt/log_monitoring/modules/incident/internal/ai/AnomalyAiRequestedConsumer.java
apps/backend/src/main/java/com/vdt/log_monitoring/api/anomaly/AnomalyReportController.java
apps/backend/src/main/resources/db/migration/postgresql/V18__create_anomaly_reports.sql
apps/backend/src/main/resources/db/migration/postgresql/V19__add_alert_source_fields.sql
```

### Modify

```text
apps/backend/src/main/java/com/vdt/log_monitoring/modules/processing/api/events/AnomalySignalEvent.java
apps/backend/src/main/java/com/vdt/log_monitoring/modules/processing/internal/publisher/AnomalySignalPublisher.java
apps/backend/src/main/java/com/vdt/log_monitoring/modules/anomaly/internal/log/AnomalyLogClassifier.java
apps/backend/src/main/java/com/vdt/log_monitoring/modules/anomaly/internal/log/AnomalyLogRuleHandler.java
apps/backend/src/main/java/com/vdt/log_monitoring/modules/anomaly/internal/rule/AnomalyLogRule.java
apps/backend/src/main/java/com/vdt/log_monitoring/modules/anomaly/internal/rule/AnomalyMetricRule.java
apps/backend/src/main/java/com/vdt/log_monitoring/modules/alerting/internal/alert/AlertEntity.java
apps/backend/src/main/java/com/vdt/log_monitoring/modules/alerting/internal/alert/AlertService.java
apps/backend/src/main/java/com/vdt/log_monitoring/modules/alerting/internal/alert/AlertRepository.java
apps/backend/src/main/java/com/vdt/log_monitoring/modules/alerting/api/AlertingFacade.java
apps/backend/src/main/java/com/vdt/log_monitoring/modules/alerting/internal/AlertingFacadeImpl.java
apps/backend/src/main/java/com/vdt/log_monitoring/modules/incident/internal/evidence/IncidentEvidenceCollector.java
apps/backend/src/main/java/com/vdt/log_monitoring/modules/incident/api/IncidentFacade.java
apps/backend/src/main/java/com/vdt/log_monitoring/modules/realtime/internal/*.java
apps/backend/src/main/resources/application.yaml
apps/frontend/src/features/alerts/AnomalyReportsTab.tsx
apps/frontend/src/features/incidents/incident-api.ts
apps/frontend/src/features/incidents/incident-types.ts
```

### Remove Later

```text
apps/backend/src/main/java/com/vdt/log_monitoring/modules/alerting/api/AnomalyReportTrigger.java
apps/backend/src/main/java/com/vdt/log_monitoring/modules/incident/internal/IncidentAnomalyReportTriggerImpl.java
apps/backend/src/main/java/com/vdt/log_monitoring/modules/incident/internal/incident/IncidentAnomalyReportEntity.java
apps/backend/src/main/java/com/vdt/log_monitoring/modules/incident/internal/incident/IncidentAnomalyReportRepository.java
```

Do not remove old files until replacement flow and tests pass.

## 15. Notes For AI Implementer

- Do not send `fingerprints`, `traceIds`, sample logs, or metric arrays through `AnomalyDetectedEvent`; store them in report.
- Do not create one alert per breached metric in the same app/window; aggregate breached metric evidence into one metric anomaly report/event.
- Do not make alerting call AI.
- Alerting may trigger AI asynchronously only after initial alert notification is already sent.
- Do not make incident detect anomaly.
- Do not reuse incident creation AI/evidence flow for anomaly report AI; create a separate method/service.
- Report update WebSocket/Telegram must be scoped by `applicationId`.
- Do not import another module's `internal` package.
- Do not edit old Flyway migrations that may already be applied; add new migration files.
- Keep normal alert flow working while adding anomaly flow.
- Prefer focused tests after each phase before broader tests.

# Anomaly Score Collector Implementation Plan

**Source spec:** `anomaly_redis_handlers_implementation_plan.md` plus the agreed collector/scoring decisions from the June 28, 2026 design discussion.

**Goal:** Implement an anomaly collector that reads app-scoped log and metric state from Redis, converts dynamic keys into fixed rule scores with structured evidence, runs a linear scorer, and returns an explainable decision result.

**Architecture:** The existing handlers remain responsible only for classifying and writing Redis state. The new collector layer reads Redis by application id, aggregates log counters and metric snapshots into deterministic per-rule scores, attaches structured evidence for the strongest signals, then calculates a final score using configurable linear weights. Alert creation, incident creation, AI investigation calls, and database persistence are out of scope for this implementation.

**Tech stack:** Java 21, Spring Boot 3.5, Spring Scheduling, Spring Data Redis `StringRedisTemplate`, Jackson `ObjectMapper`, Lombok, JUnit 5, Mockito, AssertJ.

---

## Decisions and Scope

- The collector must not scan Redis globally. It receives candidate applications from `ApplicationAccessFacade.findAllApplications()`.
- The collector must not create alerts, incidents, notifications, or call AI. It only returns `AnomalyDecisionResult`.
- Log Redis keys may be dynamic by dimension, but collector output must be fixed by hardcoded rule ids.
- Rule scores must include structured evidence so a later alert/AI flow can explain why the score is high.
- Reason text should not be AI-generated inside the collector. Use deterministic `reasonCode` and `reasonParams`; optional display text can be rendered by later layers.
- Metric handler snapshots do not store `score` or `durationAboveThresholdSeconds`. Metric scoring happens in this collector.
- Missing Redis keys, expired counters, malformed counts, malformed metric JSON, and unknown metric rule ids must be skipped without failing the whole app collection.

## Redis Read Contract

Existing writer keys stay unchanged:

```text
anomaly:{applicationId}:log:{ruleId}:active
anomaly:{applicationId}:log:{ruleId}:{dimensionType}:{dimensionValue}
anomaly:{applicationId}:metric-rules:active
anomaly:{applicationId}:metric:{metricRuleId}
```

Log read flow:

1. For each `AnomalyLogRule`, read `SMEMBERS anomaly:{appId}:log:{ruleId}:active`.
2. Parse each member as `{dimensionType}:{dimensionValue}` using `split(":", 2)`.
3. Rebuild the counter key with `AnomalyRedisKeys.logCounter(...)`.
4. Read `GET` counter value.
5. Keep only positive numeric counts.

Metric read flow:

1. Read `SMEMBERS anomaly:{appId}:metric-rules:active`.
2. Ignore unknown rule ids.
3. For each known `AnomalyMetricRule`, read `GET anomaly:{appId}:metric:{ruleId}`.
4. Parse `ruleId`, `current`, `avg`, `max`, `samples`, `lastSeen`.
5. Keep only snapshots with parseable numeric values and valid `lastSeen`.

## Scoring Model

### Log Rule Score

For each log rule:

```text
dimensionScore = rule.score(maxDimensionCount)
volumeScore = round(rule.score(totalCount) * 0.6)
spreadScore = round(spreadScore(affectedDimensions) * 0.4)
ruleScore = max(dimensionScore, volumeScore, spreadScore)
```

Spread score:

```text
affectedDimensions < 3  => 0
affectedDimensions 3-5  => 40
affectedDimensions 6-10 => 65
affectedDimensions > 10 => 85
```

The selected max branch becomes the primary reason:

```text
LOG_TOP_DIMENSION_HIGH
LOG_TOTAL_VOLUME_HIGH
LOG_SPREAD_HIGH
```

### Metric Rule Score

For each metric snapshot:

```text
currentScore = rule.score(current)
avgScore = rule.score(avg)
maxScore = round(rule.score(max) * 0.7)
rawScore = max(currentScore, avgScore, maxScore)
confidence = min(1.0, samples / metricConfidenceSamples)
ruleScore = round(rawScore * confidence)
```

Default `metricConfidenceSamples` is `3`.

The selected max branch becomes the primary reason:

```text
METRIC_CURRENT_HIGH
METRIC_AVG_HIGH
METRIC_MAX_SPIKE_HIGH
```

### Linear Final Score

The linear scorer receives fixed rule scores and applies configured weights:

```text
finalScore = round(sum(score * weight) / sum(weights))
```

Rules with no Redis data contribute `0` but still keep their configured weight. This makes the final score stable across apps and avoids artificially inflating scores when only one rule has data.

Default weights:

```yaml
app:
  anomaly:
    scoring:
      metric-confidence-samples: 3
      top-evidence-limit: 5
      thresholds:
        warning: 40
        anomaly: 70
        critical: 85
      weights:
        "log.RESOURCE_EXHAUSTED": 0.12
        "log.SECURITY_AUTH_FAILURE": 0.18
        "log.ACCESS_DENIED": 0.10
        "log.TIMEOUT": 0.10
        "log.EXCEPTION": 0.10
        "log.CRITICAL_LEVEL": 0.12
        "log.ERROR_LEVEL": 0.10
        "log.SUSPICIOUS_KEYWORD": 0.08
        "metric.CPU_USAGE": 0.08
        "metric.MEMORY_USAGE": 0.07
        "metric.DISK_USAGE": 0.04
        "metric.DISK_WRITE_RATE": 0.00
        "metric.NETWORK_RX_RATE": 0.00
        "metric.NETWORK_TX_RATE": 0.00
```

Decision levels:

```text
0-39   NORMAL
40-69  WARNING
70-84  ANOMALY
85-100 CRITICAL
```

## Result Shape

The main return type:

```java
public record AnomalyDecisionResult(
    UUID applicationId,
    int finalScore,
    AnomalyDecisionLevel level,
    List<WeightedRuleScore> ruleScores,
    List<AnomalyEvidence> topEvidences,
    Instant evaluatedAt
) {}
```

Rule score with explainability:

```java
public record RuleScore(
    String ruleId,
    AnomalySignalSource source,
    int score,
    String reasonCode,
    Map<String, Object> reasonParams,
    List<AnomalyEvidence> evidences
) {}
```

Evidence example for login failure:

```json
{
  "source": "LOG",
  "ruleId": "SECURITY_AUTH_FAILURE",
  "dimensionType": "user",
  "dimensionValue": "alice",
  "value": 27,
  "score": 70,
  "impact": "PRIMARY",
  "attributes": {
    "totalCount": 35,
    "affectedDimensions": 4
  }
}
```

The collector should return `reasonCode` and structured `reasonParams`, not a free-form AI sentence. A later alert layer can render a user-facing sentence from these fields.

## File Map

- Modify: `apps/backend/src/main/java/com/vdt/log_monitoring/modules/anomaly/internal/config/AnomalyDetectionProperties.java` — add collector/scoring properties and safe defaults.
- Modify: `apps/backend/src/main/resources/application.yaml` — add collector interval, decision thresholds, metric confidence samples, top evidence limit, and linear weights.
- Create: `apps/backend/src/main/java/com/vdt/log_monitoring/modules/anomaly/internal/collector/AnomalySignalSource.java` — enum `LOG`, `METRIC`.
- Create: `apps/backend/src/main/java/com/vdt/log_monitoring/modules/anomaly/internal/collector/EvidenceImpact.java` — enum `PRIMARY`, `SUPPORTING`, `SPREAD`.
- Create: `apps/backend/src/main/java/com/vdt/log_monitoring/modules/anomaly/internal/collector/AnomalyDecisionLevel.java` — enum decision levels and threshold mapping.
- Create: `apps/backend/src/main/java/com/vdt/log_monitoring/modules/anomaly/internal/collector/AnomalyEvidence.java` — structured explanation/evidence record.
- Create: `apps/backend/src/main/java/com/vdt/log_monitoring/modules/anomaly/internal/collector/RuleScore.java` — per-rule score with reason code, params, and evidence.
- Create: `apps/backend/src/main/java/com/vdt/log_monitoring/modules/anomaly/internal/collector/WeightedRuleScore.java` — rule score plus configured weight and contribution.
- Create: `apps/backend/src/main/java/com/vdt/log_monitoring/modules/anomaly/internal/collector/AnomalyDecisionResult.java` — final collector return object.
- Create: `apps/backend/src/main/java/com/vdt/log_monitoring/modules/anomaly/internal/collector/LogCounterState.java` — raw Redis log counter state.
- Create: `apps/backend/src/main/java/com/vdt/log_monitoring/modules/anomaly/internal/collector/MetricSnapshotState.java` — parsed Redis metric snapshot state.
- Create: `apps/backend/src/main/java/com/vdt/log_monitoring/modules/anomaly/internal/collector/LogRuleAggregate.java` — grouped log counters for one rule.
- Create: `apps/backend/src/main/java/com/vdt/log_monitoring/modules/anomaly/internal/collector/DimensionCount.java` — dimension count used by log aggregation and evidence.
- Create: `apps/backend/src/main/java/com/vdt/log_monitoring/modules/anomaly/internal/collector/RedisAnomalyStateReader.java` — read app-scoped log counters and metric snapshots from Redis.
- Create: `apps/backend/src/main/java/com/vdt/log_monitoring/modules/anomaly/internal/collector/LogRuleAggregator.java` — aggregate log counters into `RuleScore`.
- Create: `apps/backend/src/main/java/com/vdt/log_monitoring/modules/anomaly/internal/collector/MetricRuleAggregator.java` — aggregate metric snapshots into `RuleScore`.
- Create: `apps/backend/src/main/java/com/vdt/log_monitoring/modules/anomaly/internal/collector/AnomalyLinearScorer.java` — apply linear weights, calculate final score, select top evidence.
- Create: `apps/backend/src/main/java/com/vdt/log_monitoring/modules/anomaly/internal/collector/AnomalyApplicationProvider.java` — resolve active application ids from `ApplicationAccessFacade`.
- Create: `apps/backend/src/main/java/com/vdt/log_monitoring/modules/anomaly/internal/collector/AnomalyCollectorClockConfig.java` — provide UTC `Clock` bean for deterministic collector tests.
- Create: `apps/backend/src/main/java/com/vdt/log_monitoring/modules/anomaly/internal/collector/AnomalyScoreCollector.java` — orchestrate read, aggregate, score, and return one app result.
- Create: `apps/backend/src/main/java/com/vdt/log_monitoring/modules/anomaly/internal/collector/AnomalyScoreCollectorJob.java` — scheduled entry point with `runOnce()` returning results.
- Test: `apps/backend/src/test/java/com/vdt/log_monitoring/modules/anomaly/internal/config/AnomalyDetectionPropertiesTest.java` — defaults and configured scoring values.
- Test: `apps/backend/src/test/java/com/vdt/log_monitoring/modules/anomaly/internal/collector/RedisAnomalyStateReaderTest.java` — Redis state reading and malformed value handling.
- Test: `apps/backend/src/test/java/com/vdt/log_monitoring/modules/anomaly/internal/collector/LogRuleAggregatorTest.java` — top dimension, total volume, spread scoring, and evidence.
- Test: `apps/backend/src/test/java/com/vdt/log_monitoring/modules/anomaly/internal/collector/MetricRuleAggregatorTest.java` — current/avg/max metric scoring and sample confidence.
- Test: `apps/backend/src/test/java/com/vdt/log_monitoring/modules/anomaly/internal/collector/AnomalyLinearScorerTest.java` — weighted final score, level mapping, and top evidence selection.
- Test: `apps/backend/src/test/java/com/vdt/log_monitoring/modules/anomaly/internal/collector/AnomalyApplicationProviderTest.java` — only active applications are collected.
- Test: `apps/backend/src/test/java/com/vdt/log_monitoring/modules/anomaly/internal/collector/AnomalyScoreCollectorTest.java` — end-to-end single-app collect result.
- Test: `apps/backend/src/test/java/com/vdt/log_monitoring/modules/anomaly/internal/collector/AnomalyScoreCollectorJobTest.java` — job calls collector for app ids and returns results.

## Task 1: Add Collector Configuration

**Files:**

- Modify: `apps/backend/src/main/java/com/vdt/log_monitoring/modules/anomaly/internal/config/AnomalyDetectionProperties.java`
- Modify: `apps/backend/src/main/resources/application.yaml`
- Test: `apps/backend/src/test/java/com/vdt/log_monitoring/modules/anomaly/internal/config/AnomalyDetectionPropertiesTest.java`

- [x] **Step 1: Add focused configuration tests**

Create `AnomalyDetectionPropertiesTest` covering:

- Default collector interval is `30s`.
- Default collector enabled flag is `true`.
- Default `metricConfidenceSamples` is `3`.
- Default `topEvidenceLimit` is `5`.
- Default thresholds are warning `40`, anomaly `70`, critical `85`.
- Default weights contain `log.SECURITY_AUTH_FAILURE`, `log.ERROR_LEVEL`, `metric.CPU_USAGE`, and `metric.MEMORY_USAGE`.
- `weightFor("unknown.rule")` returns `0.0`.

Expected helper methods on properties:

```java
properties.collector().interval();
properties.collector().enabled();
properties.scoring().metricConfidenceSamples();
properties.scoring().topEvidenceLimit();
properties.scoring().thresholds();
properties.scoring().weightFor("log.SECURITY_AUTH_FAILURE");
```

- [x] **Step 2: Verify the expected failure**

Run:

```text
cd apps/backend
./mvnw -Dtest=AnomalyDetectionPropertiesTest test
```

Expected: FAIL because collector/scoring properties do not exist yet.

- [x] **Step 3: Extend `AnomalyDetectionProperties`**

Add fields:

```java
Collector collector,
Scoring scoring
```

Add nested records:

```java
public record Collector(
    Boolean enabled,
    Duration interval
) {}

public record Scoring(
    int metricConfidenceSamples,
    int topEvidenceLimit,
    Thresholds thresholds,
    Map<String, Double> weights
) {
    public double weightFor(String key) {
        return weights.getOrDefault(key, 0.0);
    }
}

public record Thresholds(
    int warning,
    int anomaly,
    int critical
) {}
```

Set constructor defaults matching the Scoring Model section. For `Collector`, normalize `enabled` with `enabled = enabled == null ? Boolean.TRUE : enabled;` so missing config means the collector runs.

- [x] **Step 4: Add YAML defaults**

Modify `apps/backend/src/main/resources/application.yaml` under `app.anomaly`:

```yaml
    collector:
      enabled: ${ANOMALY_COLLECTOR_ENABLED:true}
      interval: ${ANOMALY_COLLECTOR_INTERVAL:30s}
    scoring:
      metric-confidence-samples: ${ANOMALY_SCORING_METRIC_CONFIDENCE_SAMPLES:3}
      top-evidence-limit: ${ANOMALY_SCORING_TOP_EVIDENCE_LIMIT:5}
      thresholds:
        warning: ${ANOMALY_SCORING_WARNING_THRESHOLD:40}
        anomaly: ${ANOMALY_SCORING_ANOMALY_THRESHOLD:70}
        critical: ${ANOMALY_SCORING_CRITICAL_THRESHOLD:85}
      weights:
        "log.RESOURCE_EXHAUSTED": ${ANOMALY_WEIGHT_LOG_RESOURCE_EXHAUSTED:0.12}
        "log.SECURITY_AUTH_FAILURE": ${ANOMALY_WEIGHT_LOG_SECURITY_AUTH_FAILURE:0.18}
        "log.ACCESS_DENIED": ${ANOMALY_WEIGHT_LOG_ACCESS_DENIED:0.10}
        "log.TIMEOUT": ${ANOMALY_WEIGHT_LOG_TIMEOUT:0.10}
        "log.EXCEPTION": ${ANOMALY_WEIGHT_LOG_EXCEPTION:0.10}
        "log.CRITICAL_LEVEL": ${ANOMALY_WEIGHT_LOG_CRITICAL_LEVEL:0.12}
        "log.ERROR_LEVEL": ${ANOMALY_WEIGHT_LOG_ERROR_LEVEL:0.10}
        "log.SUSPICIOUS_KEYWORD": ${ANOMALY_WEIGHT_LOG_SUSPICIOUS_KEYWORD:0.08}
        "metric.CPU_USAGE": ${ANOMALY_WEIGHT_METRIC_CPU_USAGE:0.08}
        "metric.MEMORY_USAGE": ${ANOMALY_WEIGHT_METRIC_MEMORY_USAGE:0.07}
        "metric.DISK_USAGE": ${ANOMALY_WEIGHT_METRIC_DISK_USAGE:0.04}
        "metric.DISK_WRITE_RATE": ${ANOMALY_WEIGHT_METRIC_DISK_WRITE_RATE:0.00}
        "metric.NETWORK_RX_RATE": ${ANOMALY_WEIGHT_METRIC_NETWORK_RX_RATE:0.00}
        "metric.NETWORK_TX_RATE": ${ANOMALY_WEIGHT_METRIC_NETWORK_TX_RATE:0.00}
```

- [x] **Step 5: Verify the task**

Run:

```text
cd apps/backend
./mvnw -Dtest=AnomalyDetectionPropertiesTest test
```

Expected: PASS.

## Task 2: Add Collector Result and Evidence Model

**Files:**

- Create: `apps/backend/src/main/java/com/vdt/log_monitoring/modules/anomaly/internal/collector/AnomalySignalSource.java`
- Create: `apps/backend/src/main/java/com/vdt/log_monitoring/modules/anomaly/internal/collector/EvidenceImpact.java`
- Create: `apps/backend/src/main/java/com/vdt/log_monitoring/modules/anomaly/internal/collector/AnomalyDecisionLevel.java`
- Create: `apps/backend/src/main/java/com/vdt/log_monitoring/modules/anomaly/internal/collector/AnomalyEvidence.java`
- Create: `apps/backend/src/main/java/com/vdt/log_monitoring/modules/anomaly/internal/collector/RuleScore.java`
- Create: `apps/backend/src/main/java/com/vdt/log_monitoring/modules/anomaly/internal/collector/WeightedRuleScore.java`
- Create: `apps/backend/src/main/java/com/vdt/log_monitoring/modules/anomaly/internal/collector/AnomalyDecisionResult.java`
- Test: `apps/backend/src/test/java/com/vdt/log_monitoring/modules/anomaly/internal/collector/AnomalyLinearScorerTest.java`

- [x] **Step 1: Add model test coverage through scorer fixtures**

In `AnomalyLinearScorerTest`, create fixtures that instantiate:

- `AnomalyEvidence`
- `RuleScore`
- `WeightedRuleScore`
- `AnomalyDecisionResult`
- `AnomalyDecisionLevel.fromScore(...)`

Assert level mapping:

```text
39 -> NORMAL
40 -> WARNING
70 -> ANOMALY
85 -> CRITICAL
100 -> CRITICAL
```

- [x] **Step 2: Verify the expected failure**

Run:

```text
cd apps/backend
./mvnw -Dtest=AnomalyLinearScorerTest test
```

Expected: FAIL because collector model classes do not exist yet.

- [x] **Step 3: Implement model records and enums**

Use `List.copyOf(...)` and `Map.copyOf(...)` in compact constructors so returned data is immutable.

Required signatures:

```java
public enum AnomalySignalSource { LOG, METRIC }
public enum EvidenceImpact { PRIMARY, SUPPORTING, SPREAD }
```

```java
public enum AnomalyDecisionLevel {
    NORMAL,
    WARNING,
    ANOMALY,
    CRITICAL;

    public static AnomalyDecisionLevel fromScore(int score, AnomalyDetectionProperties.Thresholds thresholds) {
        if (score >= thresholds.critical()) return CRITICAL;
        if (score >= thresholds.anomaly()) return ANOMALY;
        if (score >= thresholds.warning()) return WARNING;
        return NORMAL;
    }
}
```

```java
public record AnomalyEvidence(
    AnomalySignalSource source,
    String ruleId,
    String dimensionType,
    String dimensionValue,
    double value,
    int score,
    EvidenceImpact impact,
    Map<String, Object> attributes
) {}
```

```java
public record RuleScore(
    String ruleId,
    AnomalySignalSource source,
    int score,
    String reasonCode,
    Map<String, Object> reasonParams,
    List<AnomalyEvidence> evidences
) {}
```

```java
public record WeightedRuleScore(
    RuleScore ruleScore,
    double weight,
    double contribution
) {}
```

```java
public record AnomalyDecisionResult(
    UUID applicationId,
    int finalScore,
    AnomalyDecisionLevel level,
    List<WeightedRuleScore> ruleScores,
    List<AnomalyEvidence> topEvidences,
    Instant evaluatedAt
) {}
```

- [x] **Step 4: Verify the task**

Run:

```text
cd apps/backend
./mvnw -Dtest=AnomalyLinearScorerTest test
```

Expected: FAIL only because `AnomalyLinearScorer` is not implemented yet; model compilation errors are gone.

## Task 3: Add Redis State Reader

**Files:**

- Create: `apps/backend/src/main/java/com/vdt/log_monitoring/modules/anomaly/internal/collector/LogCounterState.java`
- Create: `apps/backend/src/main/java/com/vdt/log_monitoring/modules/anomaly/internal/collector/MetricSnapshotState.java`
- Create: `apps/backend/src/main/java/com/vdt/log_monitoring/modules/anomaly/internal/collector/RedisAnomalyStateReader.java`
- Test: `apps/backend/src/test/java/com/vdt/log_monitoring/modules/anomaly/internal/collector/RedisAnomalyStateReaderTest.java`

- [x] **Step 1: Add focused Redis reader tests**

Cover these cases:

- Reads active log members for `SECURITY_AUTH_FAILURE`, rebuilds counter keys, and returns two `LogCounterState` rows.
- Skips missing, blank, non-numeric, zero, and negative log counts.
- Splits active member only at the first colon.
- Reads active metric rule ids and parses one CPU snapshot.
- Ignores unknown metric rule ids.
- Skips malformed metric JSON.

Use mocked `StringRedisTemplate`, `ValueOperations<String, String>`, and `SetOperations<String, String>`.

- [x] **Step 2: Verify the expected failure**

Run:

```text
cd apps/backend
./mvnw -Dtest=RedisAnomalyStateReaderTest test
```

Expected: FAIL because reader classes do not exist.

- [x] **Step 3: Implement raw state records**

```java
public record LogCounterState(
    AnomalyLogRule rule,
    String dimensionType,
    String dimensionValue,
    long count
) {}
```

```java
public record MetricSnapshotState(
    AnomalyMetricRule rule,
    double current,
    double avg,
    double max,
    long samples,
    Instant lastSeen
) {}
```

- [x] **Step 4: Implement `RedisAnomalyStateReader`**

Responsibilities:

- Inject `StringRedisTemplate`, `ObjectMapper`, and `AnomalyRedisKeys`.
- `List<LogCounterState> readLogCounters(UUID applicationId)`.
- `List<MetricSnapshotState> readMetricSnapshots(UUID applicationId)`.
- Loop `AnomalyLogRule.values()` for log rules.
- Read metric rule ids from `keys.activeMetricRules(applicationId)`.
- Use `AnomalyMetricRule.valueOf(ruleId)` inside a safe parse method.
- Log malformed state at debug level only.
- Never throw because a single Redis member or JSON value is invalid.

- [x] **Step 5: Verify the task**

Run:

```text
cd apps/backend
./mvnw -Dtest=RedisAnomalyStateReaderTest test
```

Expected: PASS.

## Task 4: Add Log Rule Aggregator With Evidence

**Files:**

- Create: `apps/backend/src/main/java/com/vdt/log_monitoring/modules/anomaly/internal/collector/DimensionCount.java`
- Create: `apps/backend/src/main/java/com/vdt/log_monitoring/modules/anomaly/internal/collector/LogRuleAggregate.java`
- Create: `apps/backend/src/main/java/com/vdt/log_monitoring/modules/anomaly/internal/collector/LogRuleAggregator.java`
- Test: `apps/backend/src/test/java/com/vdt/log_monitoring/modules/anomaly/internal/collector/LogRuleAggregatorTest.java`

- [x] **Step 1: Add focused log aggregation tests**

Cover:

- For `SECURITY_AUTH_FAILURE`, user `alice` count `30` becomes primary evidence with score `90` and reason code `LOG_TOP_DIMENSION_HIGH`.
- Multiple dimensions with total count high but no single high dimension can choose `LOG_TOTAL_VOLUME_HIGH`.
- Many affected dimensions can choose `LOG_SPREAD_HIGH`.
- Returned evidence includes primary top dimension and supporting dimensions sorted by count descending.
- Empty input returns zero-score `RuleScore` for every `AnomalyLogRule`.

- [x] **Step 2: Verify the expected failure**

Run:

```text
cd apps/backend
./mvnw -Dtest=LogRuleAggregatorTest test
```

Expected: FAIL because aggregator classes do not exist.

- [x] **Step 3: Implement aggregation records**

```java
public record DimensionCount(
    String dimensionType,
    String dimensionValue,
    long count
) {}
```

```java
public record LogRuleAggregate(
    AnomalyLogRule rule,
    long totalCount,
    long maxDimensionCount,
    int affectedDimensions,
    List<DimensionCount> topDimensions
) {}
```

- [x] **Step 4: Implement `LogRuleAggregator`**

Required public method:

```java
public List<RuleScore> aggregate(List<LogCounterState> counters)
```

Behavior:

- Group by `AnomalyLogRule`.
- For each rule in `AnomalyLogRule.values()`, return exactly one `RuleScore`.
- Sort top dimensions by count descending, then `dimensionType`, then `dimensionValue`.
- Keep top 3 dimensions in rule evidence.
- Compute `dimensionScore`, `volumeScore`, and `spreadScore`.
- Pick the highest score branch. Tie priority: top dimension, total volume, spread.
- Use reason params:

```java
Map.of(
    "totalCount", totalCount,
    "maxDimensionCount", maxDimensionCount,
    "affectedDimensions", affectedDimensions,
    "topDimensionType", top.dimensionType(),
    "topDimensionValue", top.dimensionValue()
)
```

For zero-score rules, use reason code `NO_LOG_SIGNAL` and empty evidence.

- [x] **Step 5: Verify the task**

Run:

```text
cd apps/backend
./mvnw -Dtest=LogRuleAggregatorTest test
```

Expected: PASS.

## Task 5: Add Metric Rule Aggregator With Evidence

**Files:**

- Create: `apps/backend/src/main/java/com/vdt/log_monitoring/modules/anomaly/internal/collector/MetricRuleAggregator.java`
- Test: `apps/backend/src/test/java/com/vdt/log_monitoring/modules/anomaly/internal/collector/MetricRuleAggregatorTest.java`

- [x] **Step 1: Add focused metric aggregation tests**

Cover:

- CPU current `91.2`, avg `87.0`, max `94.5`, samples `5` returns score `85` with `METRIC_CURRENT_HIGH`.
- CPU current low, avg high returns reason `METRIC_AVG_HIGH`.
- CPU spike only uses `maxScore * 0.7`.
- Samples `1` with `metricConfidenceSamples=3` reduces raw score by one third.
- Missing metric snapshot returns zero-score `RuleScore` for every metric rule.

- [x] **Step 2: Verify the expected failure**

Run:

```text
cd apps/backend
./mvnw -Dtest=MetricRuleAggregatorTest test
```

Expected: FAIL because `MetricRuleAggregator` does not exist.

- [x] **Step 3: Implement `MetricRuleAggregator`**

Required constructor dependency:

```java
AnomalyDetectionProperties properties
```

Required public method:

```java
public List<RuleScore> aggregate(List<MetricSnapshotState> snapshots)
```

Behavior:

- Return exactly one `RuleScore` for every `AnomalyMetricRule`.
- Use `properties.scoring().metricConfidenceSamples()` for confidence.
- Use `current`, `avg`, and `max` scores from `AnomalyMetricRule.score(...)`.
- Apply `0.7` multiplier to max spike score.
- Choose reason code by highest raw branch. Tie priority: current, avg, max.
- Evidence includes:
  - primary evidence for the selected branch
  - supporting evidence for the other two branches when their score is greater than `0`
- Reason params include `current`, `avg`, `max`, `samples`, and `lastSeen`.
- For zero-score or absent snapshots, use reason code `NO_METRIC_SIGNAL` and empty evidence.

- [x] **Step 4: Verify the task**

Run:

```text
cd apps/backend
./mvnw -Dtest=MetricRuleAggregatorTest test
```

Expected: PASS.

## Task 6: Add Linear Scorer

**Files:**

- Create: `apps/backend/src/main/java/com/vdt/log_monitoring/modules/anomaly/internal/collector/AnomalyLinearScorer.java`
- Test: `apps/backend/src/test/java/com/vdt/log_monitoring/modules/anomaly/internal/collector/AnomalyLinearScorerTest.java`

- [x] **Step 1: Complete linear scorer tests**

Cover:

- Calculates `round(sum(score * weight) / sum(weights))`.
- Includes zero-score rules in the denominator when they have configured weight.
- Ignores rules with missing/zero weight in numerator and denominator.
- Maps final score to configured decision level.
- Sorts weighted rule scores by contribution descending.
- Selects top evidence by parent weighted contribution, then evidence score.
- Limits `topEvidences` to `properties.scoring().topEvidenceLimit()`.

- [x] **Step 2: Verify the expected failure**

Run:

```text
cd apps/backend
./mvnw -Dtest=AnomalyLinearScorerTest test
```

Expected: FAIL because `AnomalyLinearScorer` does not exist.

- [x] **Step 3: Implement `AnomalyLinearScorer`**

Required constructor dependency:

```java
AnomalyDetectionProperties properties
```

Required public method:

```java
public AnomalyDecisionResult score(UUID applicationId, List<RuleScore> ruleScores, Instant evaluatedAt)
```

Weight key format:

```java
private String weightKey(RuleScore score) {
    String prefix = score.source() == AnomalySignalSource.LOG ? "log." : "metric.";
    return prefix + score.ruleId();
}
```

Final score should be clamped to `0..100`.

- [x] **Step 4: Verify the task**

Run:

```text
cd apps/backend
./mvnw -Dtest=AnomalyLinearScorerTest test
```

Expected: PASS.

## Task 7: Add Application Provider and Collector Orchestrator

**Files:**

- Create: `apps/backend/src/main/java/com/vdt/log_monitoring/modules/anomaly/internal/collector/AnomalyApplicationProvider.java`
- Create: `apps/backend/src/main/java/com/vdt/log_monitoring/modules/anomaly/internal/collector/AnomalyCollectorClockConfig.java`
- Create: `apps/backend/src/main/java/com/vdt/log_monitoring/modules/anomaly/internal/collector/AnomalyScoreCollector.java`
- Test: `apps/backend/src/test/java/com/vdt/log_monitoring/modules/anomaly/internal/collector/AnomalyApplicationProviderTest.java`
- Test: `apps/backend/src/test/java/com/vdt/log_monitoring/modules/anomaly/internal/collector/AnomalyScoreCollectorTest.java`

- [x] **Step 1: Add focused orchestration tests**

`AnomalyApplicationProviderTest`:

- Mocks `ApplicationAccessFacade.findAllApplications()`.
- Returns only application DTOs where `status` equals `ACTIVE`.
- Returns UUID ids only.

`AnomalyScoreCollectorTest`:

- Mocks `RedisAnomalyStateReader`, `LogRuleAggregator`, `MetricRuleAggregator`, and `AnomalyLinearScorer`.
- Verifies `collect(applicationId)` reads log counters and metric snapshots once.
- Verifies it combines log and metric `RuleScore` lists.
- Verifies it returns the exact `AnomalyDecisionResult` from the scorer.

- [x] **Step 2: Verify the expected failure**

Run:

```text
cd apps/backend
./mvnw -Dtest=AnomalyApplicationProviderTest,AnomalyScoreCollectorTest test
```

Expected: FAIL because application provider and collector do not exist.

- [x] **Step 3: Implement `AnomalyApplicationProvider`**

Required dependency:

```java
ApplicationAccessFacade applicationAccessFacade
```

Required method:

```java
public List<UUID> findActiveApplicationIds()
```

Filter:

```java
"ACTIVE".equalsIgnoreCase(application.status())
```

- [x] **Step 4: Implement `AnomalyScoreCollector`**

Required dependencies:

```java
RedisAnomalyStateReader stateReader
LogRuleAggregator logRuleAggregator
MetricRuleAggregator metricRuleAggregator
AnomalyLinearScorer linearScorer
Clock clock
```

If no `Clock` bean exists, add a bean inside this package:

```java
@Configuration
class AnomalyCollectorClockConfig {
    @Bean
    Clock anomalyCollectorClock() {
        return Clock.systemUTC();
    }
}
```

Required method:

```java
public AnomalyDecisionResult collect(UUID applicationId)
```

Flow:

```text
read log counters
read metric snapshots
aggregate log rule scores
aggregate metric rule scores
combine scores
linearScorer.score(applicationId, combinedScores, Instant.now(clock))
return result
```

- [x] **Step 5: Verify the task**

Run:

```text
cd apps/backend
./mvnw -Dtest=AnomalyApplicationProviderTest,AnomalyScoreCollectorTest test
```

Expected: PASS.

## Task 8: Add Scheduled Collector Job

**Files:**

- Create: `apps/backend/src/main/java/com/vdt/log_monitoring/modules/anomaly/internal/collector/AnomalyScoreCollectorJob.java`
- Test: `apps/backend/src/test/java/com/vdt/log_monitoring/modules/anomaly/internal/collector/AnomalyScoreCollectorJobTest.java`

- [x] **Step 1: Add focused job tests**

Cover:

- If collector is disabled, `runOnce()` returns an empty list and does not call provider.
- If enabled, `runOnce()` gets active app ids and calls `AnomalyScoreCollector.collect(appId)` for each.
- If one app collection throws, job logs and continues collecting the remaining apps.
- Scheduled method delegates to `runOnce()` and does not return alert/incident side effects.

- [x] **Step 2: Verify the expected failure**

Run:

```text
cd apps/backend
./mvnw -Dtest=AnomalyScoreCollectorJobTest test
```

Expected: FAIL because job does not exist.

- [x] **Step 3: Implement `AnomalyScoreCollectorJob`**

Required dependencies:

```java
AnomalyDetectionProperties properties
AnomalyApplicationProvider applicationProvider
AnomalyScoreCollector scoreCollector
```

Required methods:

```java
@Scheduled(fixedRateString = "${app.anomaly.collector.interval:30s}")
public void collectScheduled()

public List<AnomalyDecisionResult> runOnce()
```

Behavior:

- `collectScheduled()` calls `runOnce()` and logs compact result summaries.
- `runOnce()` returns `List<AnomalyDecisionResult>`.
- The job does not publish Kafka messages, write DB rows, create alerts, create incidents, or call AI.

- [x] **Step 4: Verify the task**

Run:

```text
cd apps/backend
./mvnw -Dtest=AnomalyScoreCollectorJobTest test
```

Expected: PASS.

## Task 9: Run Focused Collector Regression

**Files:**

- Test: `apps/backend/src/test/java/com/vdt/log_monitoring/modules/anomaly/internal/config/AnomalyDetectionPropertiesTest.java`
- Test: `apps/backend/src/test/java/com/vdt/log_monitoring/modules/anomaly/internal/collector/RedisAnomalyStateReaderTest.java`
- Test: `apps/backend/src/test/java/com/vdt/log_monitoring/modules/anomaly/internal/collector/LogRuleAggregatorTest.java`
- Test: `apps/backend/src/test/java/com/vdt/log_monitoring/modules/anomaly/internal/collector/MetricRuleAggregatorTest.java`
- Test: `apps/backend/src/test/java/com/vdt/log_monitoring/modules/anomaly/internal/collector/AnomalyLinearScorerTest.java`
- Test: `apps/backend/src/test/java/com/vdt/log_monitoring/modules/anomaly/internal/collector/AnomalyApplicationProviderTest.java`
- Test: `apps/backend/src/test/java/com/vdt/log_monitoring/modules/anomaly/internal/collector/AnomalyScoreCollectorTest.java`
- Test: `apps/backend/src/test/java/com/vdt/log_monitoring/modules/anomaly/internal/collector/AnomalyScoreCollectorJobTest.java`
- Existing tests under `apps/backend/src/test/java/com/vdt/log_monitoring/modules/anomaly/internal/log/`
- Existing tests under `apps/backend/src/test/java/com/vdt/log_monitoring/modules/anomaly/internal/metric/`
- Existing tests under `apps/backend/src/test/java/com/vdt/log_monitoring/modules/anomaly/internal/redis/`
- Existing tests under `apps/backend/src/test/java/com/vdt/log_monitoring/modules/anomaly/internal/rule/`

- [x] **Step 1: Run focused anomaly tests**

Run:

```text
cd apps/backend
./mvnw -Dtest=AnomalyDetectionPropertiesTest,RedisAnomalyStateReaderTest,LogRuleAggregatorTest,MetricRuleAggregatorTest,AnomalyLinearScorerTest,AnomalyApplicationProviderTest,AnomalyScoreCollectorTest,AnomalyScoreCollectorJobTest,AnomalyRuleCatalogTest,AnomalyRedisKeysTest,AnomalyLogClassifierTest,AnomalyLogRuleHandlerTest,AnomalyMetricRuleHandlerTest,PrometheusMetricServiceTest,PrometheusMetricWorkerTest test
```

Expected: PASS.

- [x] **Step 2: Run package build**

Run:

```text
cd apps/backend
./mvnw -DskipTests package
```

Expected: BUILD SUCCESS.

## Final Verification

Run the full backend suite:

```text
cd apps/backend
./mvnw test
```

Expected: all tests pass.

If this fails in the sandbox with PostgreSQL socket access like `java.net.SocketException: Operation not permitted`, rerun the same command outside the sandbox with approval. The expected result outside the sandbox is full test success.

Run the final build:

```text
cd apps/backend
./mvnw -DskipTests package
```

Expected: BUILD SUCCESS and a repackaged Spring Boot jar under `apps/backend/target/`.

## Spec Coverage

- Reads app-scoped Redis state without global scanning: Task 3, Task 7.
- Handles dynamic log keys and returns fixed rule scores: Task 4.
- Scores login failure and other log rules with top impacted dimension evidence: Task 4.
- Scores metric snapshots using current/avg/max/samples in collector, not handler: Task 5.
- Keeps deterministic reason codes and structured params instead of AI-generated reason strings: Task 2, Task 4, Task 5.
- Applies configurable linear function and decision thresholds: Task 1, Task 6.
- Returns result only, with no alert/incident/AI side effects: Task 7, Task 8.
- Provides tests for Redis edge cases, aggregation, scoring, job flow, and regression: Task 3 through Task 9.

## Self-Review Notes

- Paths match the current backend package structure under `com.vdt.log_monitoring.modules.anomaly.internal`.
- The plan uses existing `AnomalyRedisKeys`, `AnomalyLogRule`, `AnomalyMetricRule`, `AnomalyDetectionProperties`, and `ApplicationAccessFacade`.
- The plan does not require new dependencies.
- The plan keeps existing handlers unchanged except for configuration extension.
- The scheduled job is testable through `runOnce()` and does not own downstream alert behavior.
- Independent sub-agent review was not run because the current tool policy only allows spawning sub-agents when the user explicitly requests delegation.

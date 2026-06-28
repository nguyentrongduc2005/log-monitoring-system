# Anomaly Redis Handlers Implementation Plan

**Source spec:** `detection.md`, `anomaly_detection_phase1_plan.md`, and the agreed decisions in the June 28, 2026 design discussion.

**Goal:** Implement hardcoded anomaly log and metric handlers that classify current signals, write per-application Redis state, and prepare deterministic inputs for a later score collector.

**Architecture:** Processing continues to publish lightweight `AnomalySignalEvent` messages to Kafka. The anomaly module consumes log signals and periodically pulls Prometheus metrics, then writes short-lived log counters and fresh metric snapshots into Redis under application-scoped keys. Rule definitions stay hardcoded in code for Phase 2, with small catalog/score classes so rules can later move to database-backed configuration without changing handler flow.

**Tech stack:** Java 21, Spring Boot 3.5, Spring Kafka, Spring Data Redis `StringRedisTemplate`, Jackson `ObjectMapper`, Prometheus HTTP API through Spring `RestClient`, JUnit 5, Mockito, AssertJ.

---

## Decisions and Scope

- In scope:
  - Hardcoded log rule catalog.
  - Hardcoded metric rule catalog.
  - Log handler that maps each `AnomalySignalEvent` to at most one primary log rule and increments one Redis counter.
  - Metric handler that stores CPU, memory, disk, disk write, network receive, and network transmit snapshots in Redis.
  - Redis key naming, TTL policy, app-scoped active state indexes, and focused unit tests.
  - Score conversion helpers used by handlers and ready for the later collector.
- Out of scope for this implementation:
  - Database-backed anomaly rules.
  - HTTP/status/endpoint anomaly rules, because current logs do not contain HTTP fields.
  - Alert creation, AI trigger, incident creation, or UI work.
  - Full score collector job implementation. This plan only shapes Redis keys so a later collector can read them cleanly.
- Metric snapshot TTL is used only as a freshness guard. It is not a metric time-window calculation.
- Log counters use TTL because they represent rolling event windows.
- Each Redis key must include `applicationId` so signals from multiple applications are never aggregated together.

## Redis Contract

### App-Scoped Index Keys

```text
anomaly:{applicationId}:log:{ruleId}:active
anomaly:{applicationId}:metric-rules:active
```

- `anomaly:{applicationId}:log:{ruleId}:active`: Redis set of active dimensions for one app/rule pair. Store members as `{dimensionType}:{dimensionValue}` so a later collector can iterate each hardcoded rule and rebuild counter keys without parsing rule ids from full key names.
- `anomaly:{applicationId}:metric-rules:active`: Redis set of metric rule ids currently observed for the app.
- Give index keys a TTL of `15m` whenever they are updated.
- Do not create a global Redis app index. A later score collector should obtain candidate applications from the application registry/configuration, then read these app-scoped indexes for each application.

### Log Counter Keys

```text
anomaly:{applicationId}:log:{ruleId}:{dimensionType}:{dimensionValue}
```

Examples:

```text
anomaly:00000000-0000-0000-0000-000000000103:log:SECURITY_AUTH_FAILURE:user:john
anomaly:00000000-0000-0000-0000-000000000103:log:SECURITY_AUTH_FAILURE:ip:10.0.0.8
anomaly:00000000-0000-0000-0000-000000000103:log:TIMEOUT:service:Payments
anomaly:00000000-0000-0000-0000-000000000103:log:RESOURCE_EXHAUSTED:service:Payments
```

Value:

```text
count
```

TTL:

- `window + grace`, where grace is 60 seconds.
- If a rule window is 5 minutes, Redis TTL is 6 minutes.

### Metric Snapshot Keys

```text
anomaly:{applicationId}:metric:{metricRuleId}
```

Examples:

```text
anomaly:00000000-0000-0000-0000-000000000103:metric:CPU_USAGE
anomaly:00000000-0000-0000-0000-000000000103:metric:MEMORY_USAGE
```

Value:

```json
{
  "ruleId": "CPU_USAGE",
  "current": 91.2,
  "avg": 87.0,
  "max": 94.5,
  "samples": 5,
  "lastSeen": "2026-06-28T10:20:00Z"
}
```

TTL:

- `max(scrapeInterval * 6, 60s)`.
- Example: a 10 second scrape interval produces a 60 second TTL; a 60 second scrape interval produces a 360 second TTL.
- The TTL prevents stale metrics from being collected if Prometheus or the worker stops updating.

## Hardcoded Rule Catalog

### Log Rules

Only one primary rule should be recorded per event. Classification order matters:

1. `RESOURCE_EXHAUSTED`
2. `SECURITY_AUTH_FAILURE`
3. `ACCESS_DENIED`
4. `TIMEOUT`
5. `EXCEPTION`
6. `CRITICAL_LEVEL`
7. `ERROR_LEVEL`
8. `SUSPICIOUS_KEYWORD`

Rules:

| Rule id | Match input | Dimension priority | Window | Redis TTL | Score thresholds |
| --- | --- | --- | --- | --- | --- |
| `RESOURCE_EXHAUSTED` | `out of memory`, `oom`, `heap space`, `disk full`, `no space left` | `host`, `service` | 5m | 6m | 1=>80, 2-4=>95, >=5=>100 |
| `SECURITY_AUTH_FAILURE` | `failed login`, `login failed`, `authentication failed`, `bad credentials`, `invalid password`, `invalid token`, `ssh failed`, `failed password` | `user`, `account`, `ip`, `service` | 5m | 6m | <5=>0, 5-14=>40, 15-29=>70, >=30=>90 |
| `ACCESS_DENIED` | `denied`, `forbidden`, `unauthorized`, `permission denied` | `user`, `account`, `ip`, `service` | 5m | 6m | <5=>0, 5-19=>35, 20-49=>65, >=50=>90 |
| `TIMEOUT` | `timeout`, `timed out`, `deadline exceeded`, `read timeout`, `connection timeout` | `service`, `host` | 2m | 3m | <3=>0, 3-9=>35, 10-24=>65, >=25=>90 |
| `EXCEPTION` | `exception`, `stacktrace`, `nullpointer`, `illegalstate`, `runtimeexception` | `service` | 2m | 3m | <3=>0, 3-9=>35, 10-24=>65, >=25=>90 |
| `CRITICAL_LEVEL` | level is `CRITICAL` and no earlier rule matched | `service` | 5m | 6m | 1=>70, 2-4=>85, >=5=>100 |
| `ERROR_LEVEL` | level is `ERROR` and no earlier rule matched | `service` | 1m | 2m | <5=>0, 5-14=>30, 15-29=>60, >=30=>85 |
| `SUSPICIOUS_KEYWORD` | `failed`, `failure`, `degraded`, `unavailable`, `retry exhausted`, `circuit breaker open` when no earlier rule matched | `service` | 5m | 6m | <5=>0, 5-19=>30, 20-49=>60, >=50=>80 |

Dimension extraction:

- First read structured attributes if present in `LogMetadata.attributes()` later: `user`, `username`, `account`, `accountId`, `ip`, `sourceIp`, `clientIp`, `host`.
- Current `AnomalySignalEvent` does not include metadata or raw message. Add `message` and optional `traceId` to the event so anomaly classification can be done without querying ClickHouse.
- If no specific dimension is available, use `service:{serviceName}`.
- Sanitize dimension values by trimming, lowercasing dimension type, replacing whitespace with `_`, replacing `:` with `_`, and capping value length to 120 characters.

### Metric Rules

Metric rules use snapshots, not counters:

| Rule id | Input | Score thresholds |
| --- | --- | --- |
| `CPU_USAGE` | Node exporter CPU usage percent | <75=>0, 75-84=>30, 85-89=>60, 90-94=>85, >=95=>100 |
| `MEMORY_USAGE` | Node exporter memory usage percent | <75=>0, 75-84=>30, 85-91=>65, 92-96=>85, >=97=>100 |
| `DISK_USAGE` | Node exporter disk usage percent | <80=>0, 80-89=>40, 90-94=>70, >=95=>95 |
| `DISK_WRITE_RATE` | Node exporter disk write bytes/sec | Phase 2A stores current value and score `0`; baseline scoring comes later |
| `NETWORK_RX_RATE` | Node exporter network receive bytes/sec | Phase 2A stores current value and score `0`; baseline scoring comes later |
| `NETWORK_TX_RATE` | Node exporter network transmit bytes/sec | Phase 2A stores current value and score `0`; baseline scoring comes later |

Because current Prometheus worker reads node-level metrics, map snapshots to an application using the metric source/application configuration when available. If the current worker cannot resolve a specific app yet, add a narrow application resolver abstraction and initially support a configured default application id only for local/demo use. Do not write global metric keys without an application id.

## File Map

- Modify: `apps/backend/src/main/java/com/vdt/log_monitoring/modules/processing/api/events/AnomalySignalEvent.java` — include enough signal data for classification, especially `message` and `traceId`.
- Modify: `apps/backend/src/main/java/com/vdt/log_monitoring/modules/processing/internal/publisher/AnomalySignalPublisher.java` — populate new event fields.
- Modify: `apps/backend/src/main/java/com/vdt/log_monitoring/modules/anomaly/internal/consumer/AnomalyLogSignalConsumer.java` — delegate consumed events to the log handler.
- Modify: `apps/backend/src/main/java/com/vdt/log_monitoring/modules/anomaly/internal/metric/PrometheusMetricService.java` — return typed metric query results instead of raw strings and encode Prometheus query parameters safely.
- Modify: `apps/backend/src/main/java/com/vdt/log_monitoring/modules/anomaly/internal/metric/PrometheusMetricWorker.java` — delegate metric snapshots to the metric handler.
- Modify: `apps/backend/src/main/resources/application.yaml` — add anomaly Redis/metric properties.
- Create: `apps/backend/src/main/java/com/vdt/log_monitoring/modules/anomaly/internal/rule/AnomalyLogRule.java` — hardcoded log rule enum/catalog.
- Create: `apps/backend/src/main/java/com/vdt/log_monitoring/modules/anomaly/internal/rule/AnomalyMetricRule.java` — hardcoded metric rule enum/catalog.
- Create: `apps/backend/src/main/java/com/vdt/log_monitoring/modules/anomaly/internal/rule/ScoreBand.java` — shared value-to-score helper.
- Create: `apps/backend/src/main/java/com/vdt/log_monitoring/modules/anomaly/internal/log/AnomalyLogClassifier.java` — one-primary-rule classifier.
- Create: `apps/backend/src/main/java/com/vdt/log_monitoring/modules/anomaly/internal/log/AnomalyLogMatch.java` — classified log rule, dimension, and counter metadata.
- Create: `apps/backend/src/main/java/com/vdt/log_monitoring/modules/anomaly/internal/log/AnomalyLogRuleHandler.java` — writes log counters/indexes to Redis.
- Create: `apps/backend/src/main/java/com/vdt/log_monitoring/modules/anomaly/internal/metric/AnomalyMetricSnapshot.java` — typed metric snapshot written to Redis.
- Create: `apps/backend/src/main/java/com/vdt/log_monitoring/modules/anomaly/internal/metric/AnomalyMetricRuleHandler.java` — writes metric snapshots/indexes to Redis.
- Create: `apps/backend/src/main/java/com/vdt/log_monitoring/modules/anomaly/internal/redis/AnomalyRedisKeys.java` — central key builder and key sanitization.
- Create: `apps/backend/src/main/java/com/vdt/log_monitoring/modules/anomaly/internal/config/AnomalyDetectionProperties.java` — configurable scrape interval, index TTL, metric freshness TTL, and optional demo application id.
- Test: `apps/backend/src/test/java/com/vdt/log_monitoring/modules/anomaly/internal/rule/AnomalyRuleCatalogTest.java` — score thresholds and rule windows.
- Test: `apps/backend/src/test/java/com/vdt/log_monitoring/modules/anomaly/internal/redis/AnomalyRedisKeysTest.java` — app-scoped key generation and sanitization.
- Test: `apps/backend/src/test/java/com/vdt/log_monitoring/modules/anomaly/internal/log/AnomalyLogClassifierTest.java` — classification priority and fallback dimensions.
- Test: `apps/backend/src/test/java/com/vdt/log_monitoring/modules/anomaly/internal/log/AnomalyLogRuleHandlerTest.java` — Redis increment, TTL, and index updates.
- Test: `apps/backend/src/test/java/com/vdt/log_monitoring/modules/anomaly/internal/metric/AnomalyMetricRuleHandlerTest.java` — snapshot JSON, rolling state, TTL, and index updates.
- Test: `apps/backend/src/test/java/com/vdt/log_monitoring/modules/anomaly/internal/metric/PrometheusMetricWorkerTest.java` — worker converts typed metric values into snapshots and skips missing values.
- Test: `apps/backend/src/test/java/com/vdt/log_monitoring/modules/processing/internal/publisher/ProcessingEventPublisherTest.java` — add anomaly publisher event mapping coverage.

## Task 1: Extend the Anomaly Signal Event Contract

**Files:**

- Modify: `apps/backend/src/main/java/com/vdt/log_monitoring/modules/processing/api/events/AnomalySignalEvent.java`
- Modify: `apps/backend/src/main/java/com/vdt/log_monitoring/modules/processing/internal/publisher/AnomalySignalPublisher.java`
- Test: `apps/backend/src/test/java/com/vdt/log_monitoring/modules/processing/internal/publisher/ProcessingEventPublisherTest.java`

- [ ] **Step 1: Add focused publisher test coverage**

Add a test method to `ProcessingEventPublisherTest`:

```java
@Test
void anomalyPublisherSendsSignalEventWithClassificationInputs() {
    KafkaTemplate<String, AnomalySignalEvent> kafkaTemplate = mock();
    when(kafkaTemplate.send(eq("logs.anomaly.signals"), eq(APPLICATION_ID.toString()), org.mockito.ArgumentMatchers.any()))
            .thenReturn(CompletableFuture.completedFuture(null));
    AnomalySignalPublisher publisher = new AnomalySignalPublisher(
            kafkaTemplate,
            "logs.anomaly.signals",
            Duration.ofSeconds(1));

    publisher.publish(processedLog(LogLevel.ERROR), "KEYWORD_MATCH_FAILED");

    ArgumentCaptor<AnomalySignalEvent> eventCaptor = ArgumentCaptor.forClass(AnomalySignalEvent.class);
    verify(kafkaTemplate).send(eq("logs.anomaly.signals"), eq(APPLICATION_ID.toString()), eventCaptor.capture());
    assertThat(eventCaptor.getValue().applicationId()).isEqualTo(APPLICATION_ID);
    assertThat(eventCaptor.getValue().logId()).isEqualTo(EVENT_ID);
    assertThat(eventCaptor.getValue().level()).isEqualTo("ERROR");
    assertThat(eventCaptor.getValue().message()).isEqualTo("payment failed");
    assertThat(eventCaptor.getValue().traceId()).isEqualTo("trace-1");
    assertThat(eventCaptor.getValue().matchedRule()).isEqualTo("KEYWORD_MATCH_FAILED");
    assertThat(eventCaptor.getValue().serviceName()).isEqualTo("Payments");
}
```

- [ ] **Step 2: Verify the expected failure**

Run:

```text
cd apps/backend
./mvnw -Dtest=ProcessingEventPublisherTest test
```

Expected: fail because `AnomalySignalEvent` does not expose `message()` or `traceId()` yet.

- [ ] **Step 3: Extend the event and publisher**

Change `AnomalySignalEvent` to include:

```java
String message,
String traceId
```

Populate both fields in `AnomalySignalPublisher.publish()` from `ProcessedLog.message()` and `ProcessedLog.traceId()`.

- [ ] **Step 4: Verify the task**

Run:

```text
cd apps/backend
./mvnw -Dtest=ProcessingEventPublisherTest test
```

Expected: pass.

## Task 2: Add Hardcoded Rule Catalog and Redis Key Builder

**Files:**

- Create: `apps/backend/src/main/java/com/vdt/log_monitoring/modules/anomaly/internal/rule/ScoreBand.java`
- Create: `apps/backend/src/main/java/com/vdt/log_monitoring/modules/anomaly/internal/rule/AnomalyLogRule.java`
- Create: `apps/backend/src/main/java/com/vdt/log_monitoring/modules/anomaly/internal/rule/AnomalyMetricRule.java`
- Create: `apps/backend/src/main/java/com/vdt/log_monitoring/modules/anomaly/internal/redis/AnomalyRedisKeys.java`
- Test: `apps/backend/src/test/java/com/vdt/log_monitoring/modules/anomaly/internal/rule/AnomalyRuleCatalogTest.java`
- Test: `apps/backend/src/test/java/com/vdt/log_monitoring/modules/anomaly/internal/redis/AnomalyRedisKeysTest.java`

- [ ] **Step 1: Add tests for score bands and key format**

`AnomalyRuleCatalogTest` should assert:

- `RESOURCE_EXHAUSTED.score(1) == 80`.
- `SECURITY_AUTH_FAILURE.score(15) == 70`.
- `ERROR_LEVEL.score(30) == 85`.
- `CPU_USAGE.score(90.0) == 85`.
- `DISK_WRITE_RATE.score(999999.0) == 0` for this phase.
- Log rule TTL equals `window + 60 seconds`.

`AnomalyRedisKeysTest` should assert:

- `logCounter(appId, SECURITY_AUTH_FAILURE, "ip", "10.0.0.8")` returns `anomaly:{appId}:log:SECURITY_AUTH_FAILURE:ip:10.0.0.8`.
- Values with spaces and colons are sanitized.
- `metricSnapshot(appId, CPU_USAGE)` returns `anomaly:{appId}:metric:CPU_USAGE`.
- Active index keys are app-scoped: `anomaly:{appId}:log:{ruleId}:active` and `anomaly:{appId}:metric-rules:active`.

- [ ] **Step 2: Verify the expected failure**

Run:

```text
cd apps/backend
./mvnw -Dtest=AnomalyRuleCatalogTest,AnomalyRedisKeysTest test
```

Expected: fail because the classes do not exist yet.

- [ ] **Step 3: Implement rule catalog classes**

Create `ScoreBand` as a small immutable record:

```java
public record ScoreBand(double minInclusive, double maxInclusive, int score) {
    public boolean matches(double value) {
        return value >= minInclusive && value <= maxInclusive;
    }
}
```

Create `AnomalyLogRule` enum with fields:

```java
Duration window;
Duration grace;
List<String> keywords;
List<String> dimensionPriority;
List<ScoreBand> scoreBands;
```

Expose:

```java
Duration ttl()
int score(long count)
boolean matches(String level, String normalizedMessage)
```

Create `AnomalyMetricRule` enum with fields:

```java
List<ScoreBand> scoreBands;
```

Expose:

```java
int score(double current)
```

- [ ] **Step 4: Implement central Redis key builder**

`AnomalyRedisKeys` should expose:

```java
String logRuleActiveIndex(UUID applicationId, AnomalyLogRule rule)
String logDimensionMember(String dimensionType, String dimensionValue)
String activeMetricRules(UUID applicationId)
String logCounter(UUID applicationId, AnomalyLogRule rule, String dimensionType, String dimensionValue)
String metricSnapshot(UUID applicationId, AnomalyMetricRule rule)
String sanitizePart(String value)
```

Sanitization rules:

- `null` or blank becomes `unknown`.
- Trim.
- Replace whitespace with `_`.
- Replace `:` with `_`.
- Cap to 120 characters.

- [ ] **Step 5: Verify the task**

Run:

```text
cd apps/backend
./mvnw -Dtest=AnomalyRuleCatalogTest,AnomalyRedisKeysTest test
```

Expected: pass.

## Task 3: Implement the Log Classifier

**Files:**

- Create: `apps/backend/src/main/java/com/vdt/log_monitoring/modules/anomaly/internal/log/AnomalyLogMatch.java`
- Create: `apps/backend/src/main/java/com/vdt/log_monitoring/modules/anomaly/internal/log/AnomalyLogClassifier.java`
- Test: `apps/backend/src/test/java/com/vdt/log_monitoring/modules/anomaly/internal/log/AnomalyLogClassifierTest.java`

- [ ] **Step 1: Add classifier tests**

Cover these cases:

- Message `failed login for user=john from ip=10.0.0.8` classifies as `SECURITY_AUTH_FAILURE`, dimension `user:john`.
- Message `ssh failed password from ip=10.0.0.8` classifies as `SECURITY_AUTH_FAILURE`, dimension `ip:10.0.0.8`.
- Message `out of memory while handling request` classifies as `RESOURCE_EXHAUSTED` even if level is `ERROR`.
- Message `connection timeout to downstream` classifies as `TIMEOUT`.
- Message `NullPointerException at service` classifies as `EXCEPTION`.
- Message `payment failed after provider response` classifies as `SUSPICIOUS_KEYWORD`, because it is not an auth failure.
- Level `CRITICAL` with otherwise generic message classifies as `CRITICAL_LEVEL`.
- Level `ERROR` with otherwise generic message classifies as `ERROR_LEVEL`.
- Level `INFO` with no suspicious keyword returns empty.

- [ ] **Step 2: Verify the expected failure**

Run:

```text
cd apps/backend
./mvnw -Dtest=AnomalyLogClassifierTest test
```

Expected: fail because the classifier does not exist.

- [ ] **Step 3: Implement classification**

`AnomalyLogClassifier` should:

- Normalize message to lowercase once.
- Evaluate `AnomalyLogRule` values in enum declaration order.
- Return the first matching rule only.
- Extract dimensions from message using conservative regexes:
  - user/account: `(?:user|username|account|accountId)=([A-Za-z0-9@._-]+)`
  - ip/source ip: `(?:ip|sourceIp|clientIp|from)=([0-9a-fA-F:.]+)`
  - host: `(?:host)=([A-Za-z0-9._-]+)`
- Use `serviceName` as fallback.

`AnomalyLogMatch` should contain:

```java
AnomalyLogRule rule;
String dimensionType;
String dimensionValue;
```

- [ ] **Step 4: Verify the task**

Run:

```text
cd apps/backend
./mvnw -Dtest=AnomalyLogClassifierTest test
```

Expected: pass.

## Task 4: Implement the Log Rule Handler and Consumer Delegation

**Files:**

- Create: `apps/backend/src/main/java/com/vdt/log_monitoring/modules/anomaly/internal/log/AnomalyLogRuleHandler.java`
- Modify: `apps/backend/src/main/java/com/vdt/log_monitoring/modules/anomaly/internal/consumer/AnomalyLogSignalConsumer.java`
- Create: `apps/backend/src/main/java/com/vdt/log_monitoring/modules/anomaly/internal/config/AnomalyDetectionProperties.java`
- Modify: `apps/backend/src/main/resources/application.yaml`
- Test: `apps/backend/src/test/java/com/vdt/log_monitoring/modules/anomaly/internal/log/AnomalyLogRuleHandlerTest.java`

- [ ] **Step 1: Add Redis handler tests**

Mock `StringRedisTemplate`, `ValueOperations<String, String>`, and `SetOperations<String, String>`. Verify TTLs through `StringRedisTemplate.expire(key, duration)`.

Cover:

- Matching `SECURITY_AUTH_FAILURE` increments exactly one log counter key.
- Handler sets counter TTL from the matched log rule only when Redis increment returns `1`.
- Handler adds the dimension member to `anomaly:{applicationId}:log:{ruleId}:active`.
- Handler refreshes TTL on the app-scoped active counter index.
- Non-matching `INFO` event does not touch Redis.

- [ ] **Step 2: Verify the expected failure**

Run:

```text
cd apps/backend
./mvnw -Dtest=AnomalyLogRuleHandlerTest test
```

Expected: fail because the handler does not exist.

- [ ] **Step 3: Add configuration properties**

Create `AnomalyDetectionProperties`:

```java
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ConfigurationProperties(prefix = "app.anomaly")
public record AnomalyDetectionProperties(
        Duration indexTtl,
        Metric metric
) {
    public AnomalyDetectionProperties {
        indexTtl = indexTtl == null ? Duration.ofMinutes(15) : indexTtl;
        metric = metric == null ? new Metric(Duration.ofSeconds(10), Duration.ofSeconds(60), null) : metric;
    }

    public record Metric(
            Duration scrapeInterval,
            Duration freshnessTtl,
            String defaultApplicationId
    ) {
        public Duration effectiveFreshnessTtl() {
            Duration configuredFreshness = freshnessTtl == null ? Duration.ZERO : freshnessTtl;
            Duration configuredScrapeInterval = scrapeInterval == null ? Duration.ofSeconds(10) : scrapeInterval;
            Duration minimumFromScrape = configuredScrapeInterval.multipliedBy(6);
            Duration minimumFreshness = Duration.ofSeconds(60);
            return List.of(configuredFreshness, minimumFromScrape, minimumFreshness).stream()
                    .max(Duration::compareTo)
                    .orElse(minimumFreshness);
        }

        public Optional<UUID> resolvedDefaultApplicationId() {
            if (defaultApplicationId == null || defaultApplicationId.isBlank()) {
                return Optional.empty();
            }
            return Optional.of(UUID.fromString(defaultApplicationId.trim()));
        }
    }
}
```

Add defaults to `application.yaml`:

```yaml
app:
  anomaly:
    index-ttl: ${ANOMALY_INDEX_TTL:15m}
    metric:
      scrape-interval: ${ANOMALY_METRIC_SCRAPE_INTERVAL:10s}
      freshness-ttl: ${ANOMALY_METRIC_FRESHNESS_TTL:60s}
      default-application-id: ${ANOMALY_METRIC_DEFAULT_APPLICATION_ID:}
```

- [ ] **Step 4: Implement handler**

`AnomalyLogRuleHandler.handle(AnomalySignalEvent event)` should:

- Call `AnomalyLogClassifier.classify(event)`.
- Return immediately if empty.
- Build the counter key using `AnomalyRedisKeys`.
- Increment the counter by one.
- If `increment()` returns `1`, expire the counter key with `match.rule().ttl()`.
- Do not refresh the counter TTL on later increments. This preserves a fixed first-seen window instead of turning the counter into an inactivity/sliding window.
- `SADD anomaly:{applicationId}:log:{ruleId}:active {dimensionType}:{dimensionValue}`.
- Expire the app-scoped rule active index using `app.anomaly.index-ttl`.

- [ ] **Step 5: Delegate from consumer**

`AnomalyLogSignalConsumer.consume()` should call `AnomalyLogRuleHandler.handle(event)` and log only compact debug/info context. Keep the consumer thin.

- [ ] **Step 6: Verify the task**

Run:

```text
cd apps/backend
./mvnw -Dtest=AnomalyLogRuleHandlerTest test
```

Expected: pass.

## Task 5: Implement Metric Snapshots and Metric Rule Handler

**Files:**

- Create: `apps/backend/src/main/java/com/vdt/log_monitoring/modules/anomaly/internal/metric/AnomalyMetricSnapshot.java`
- Create: `apps/backend/src/main/java/com/vdt/log_monitoring/modules/anomaly/internal/metric/AnomalyMetricRuleHandler.java`
- Test: `apps/backend/src/test/java/com/vdt/log_monitoring/modules/anomaly/internal/metric/AnomalyMetricRuleHandlerTest.java`

- [ ] **Step 1: Add metric handler tests**

Cover:

- CPU current `91.2` writes key `anomaly:{appId}:metric:CPU_USAGE` with JSON containing current rolling state.
- Existing snapshot state is folded into new `avg`, `max`, and `samples`.
- Handler sets freshness TTL using `effectiveFreshnessTtl()`.
- With scrape interval `30s` and configured freshness `60s`, handler uses `180s`.
- Handler adds rule id to `anomaly:{applicationId}:metric-rules:active`.
- Handler refreshes TTL on the app-scoped active metric-rule index.
- If no application id can be resolved, handler logs and skips Redis writes.

- [ ] **Step 2: Verify the expected failure**

Run:

```text
cd apps/backend
./mvnw -Dtest=AnomalyMetricRuleHandlerTest test
```

Expected: fail because metric snapshot and handler do not exist.

- [ ] **Step 3: Implement snapshot record**

`AnomalyMetricSnapshot` should contain:

```java
AnomalyMetricRule rule;
UUID applicationId;
double current;
double avg;
double max;
long samples;
Instant lastSeen;
```

The handler should serialize a stable JSON object with `ruleId`, `current`, `avg`, `max`, `samples`, and `lastSeen`.

`samples` should be kept because the handler needs it to update rolling `avg` on the next scrape without storing a separate sum. `score` and `durationAboveThresholdSeconds` should not be written by this handler because they are collector/decision-phase concepts: the collector can apply the latest hardcoded rule bands and threshold-duration logic when it aggregates Redis state.

- [ ] **Step 4: Implement metric handler**

`AnomalyMetricRuleHandler.save(AnomalyMetricSnapshot snapshot)` should:

- Skip null application id.
- Read the previous snapshot JSON from the same Redis key if present.
- Update `avg`, `max`, and `samples` from the previous snapshot.
- Write JSON to `anomaly:{applicationId}:metric:{metricRuleId}`.
- Expire snapshot key with `app.anomaly.metric.effectiveFreshnessTtl()`, which is `max(configured freshness TTL, scrapeInterval * 6, 60s)`.
- `SADD anomaly:{applicationId}:metric-rules:active {metricRuleId}`.
- Expire the app-scoped active metric-rule index with `app.anomaly.index-ttl`.

- [ ] **Step 5: Verify the task**

Run:

```text
cd apps/backend
./mvnw -Dtest=AnomalyMetricRuleHandlerTest test
```

Expected: pass.

## Task 6: Type Prometheus Query Results and Delegate the Metric Worker

**Files:**

- Modify: `apps/backend/src/main/java/com/vdt/log_monitoring/modules/anomaly/internal/metric/PrometheusMetricService.java`
- Modify: `apps/backend/src/main/java/com/vdt/log_monitoring/modules/anomaly/internal/metric/PrometheusMetricWorker.java`
- Test: `apps/backend/src/test/java/com/vdt/log_monitoring/modules/anomaly/internal/metric/PrometheusMetricServiceTest.java`
- Test: `apps/backend/src/test/java/com/vdt/log_monitoring/modules/anomaly/internal/metric/PrometheusMetricWorkerTest.java`

- [ ] **Step 1: Add service tests**

Use `RestClient.Builder` with a mock request flow or a lightweight fake if current project test style allows it.

Cover:

- Query URI is built using query parameters, not raw string concatenation.
- A successful Prometheus vector response returns the first numeric value.
- Empty vector returns `OptionalDouble.empty()`.
- HTTP/client failure returns `OptionalDouble.empty()` and does not throw.
- Worker creates and saves a CPU snapshot when `getCpuUsage()` returns a value.
- Worker skips CPU snapshot when `getCpuUsage()` returns `OptionalDouble.empty()`.
- Worker skips all metric writes when default application id is absent.

- [ ] **Step 2: Verify the expected failure**

Run:

```text
cd apps/backend
./mvnw -Dtest=PrometheusMetricServiceTest,PrometheusMetricWorkerTest test
```

Expected: fail until the service returns typed values.

- [ ] **Step 3: Change metric service return types**

Change public methods from `String` to `OptionalDouble`:

```java
OptionalDouble getCpuUsage()
OptionalDouble getMemoryUsage()
OptionalDouble getDiskUsage()
OptionalDouble getDiskWriteRate()
OptionalDouble getNetworkReceiveRate()
OptionalDouble getNetworkTransmitRate()
```

Use `RestClient` URI builder:

```java
restClient.get()
        .uri(uriBuilder -> uriBuilder
                .scheme(...)
                .host(...)
                .path("/api/v1/query")
                .queryParam("query", query)
                .build())
```

If splitting `prometheus.api-url` into scheme/host is too invasive, use `UriComponentsBuilder.fromUriString(prometheusUrl)` to append path and query param safely before passing the URI to `RestClient`.

- [ ] **Step 4: Parse Prometheus response**

Parse response JSON with Jackson into `JsonNode`.

Expected path:

```text
data.result[0].value[1]
```

Return `OptionalDouble.empty()` when status is not `success`, the vector is empty, or the value is not numeric.

- [ ] **Step 5: Delegate from worker**

`PrometheusMetricWorker` should:

- Use `app.anomaly.metric.scrape-interval` for `@Scheduled`.
- Resolve the target application id. For this phase, use `app.anomaly.metric.default-application-id` if configured.
- For each `OptionalDouble` returned by `PrometheusMetricService`, create an `AnomalyMetricSnapshot` with the matching `AnomalyMetricRule`.
- Call `AnomalyMetricRuleHandler.save(snapshot)`.
- Skip a metric when the service returns `OptionalDouble.empty()`.

- [ ] **Step 6: Verify the task**

Run:

```text
cd apps/backend
./mvnw -Dtest=PrometheusMetricServiceTest,PrometheusMetricWorkerTest test
```

Expected: pass.

## Task 7: Focused and Regression Verification

**Files:**

- No new production files.

- [ ] **Step 1: Run focused anomaly tests**

Run:

```text
cd apps/backend
./mvnw -Dtest=AnomalyRuleCatalogTest,AnomalyRedisKeysTest,AnomalyLogClassifierTest,AnomalyLogRuleHandlerTest,AnomalyMetricRuleHandlerTest,PrometheusMetricServiceTest,PrometheusMetricWorkerTest test
```

Expected: pass.

- [ ] **Step 2: Run processing publisher regression**

Run:

```text
cd apps/backend
./mvnw -Dtest=ProcessingEventPublisherTest test
```

Expected: pass.

- [ ] **Step 3: Run backend test suite**

Run:

```text
cd apps/backend
./mvnw test
```

Expected: pass. If environment services are required by unrelated integration tests, record the failing test names and rerun the focused suite from Step 1 to verify the anomaly implementation itself.

## Final Verification

- `AnomalySignalEvent` contains `applicationId`, `timestamp`, `logId`, `level`, `matchedRule`, `serviceName`, `message`, and `traceId`.
- A log signal maps to at most one primary anomaly log rule.
- Log Redis counters always include `applicationId`.
- Log counter TTL equals each hardcoded rule window plus 60 seconds.
- Metric Redis snapshots always include `applicationId`.
- Metric snapshot TTL is present and used only to prevent stale collector input.
- App-scoped active counter/metric indexes are maintained for collector-friendly reads.
- No HTTP anomaly rules are implemented.
- No database anomaly rule tables are introduced.
- No alert/AI behavior is triggered by these handlers.

## Spec Coverage

- Hardcoded rule base instead of DB rules: Tasks 2, 3, 5.
- Separate log and metric handler heads: Tasks 4 and 5.
- Redis stores log counters and metric snapshots only: Tasks 4 and 5.
- Application-scoped keys to avoid cross-app aggregation: Redis Contract, Tasks 2, 4, 5.
- Log TTL/window behavior: Redis Contract, Tasks 2 and 4.
- Metric snapshot freshness TTL without window semantics: Redis Contract, Tasks 5 and 7.
- Current log limitations without HTTP fields: Decisions and Scope, Hardcoded Rule Catalog.
- Generalized anomaly rules for auth failures, access denial, timeouts, exceptions, resource exhaustion, and generic error levels: Hardcoded Rule Catalog, Task 3.
- Future extensibility for collector/scoring: Redis app-scoped active counter and metric indexes.
- Future extensibility for richer dimensions and app metrics: Dimension extraction notes, Metric Rules notes.

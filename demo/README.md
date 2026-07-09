# Demo Log Scenarios

This folder contains timestamp-free demo logs and a sender script that prepends
ordered ISO timestamps before sending logs to the backend ingestion API.

## API keys

The demo script already has default local API keys for `payment-service` and
`order-service`, so you can run the scripts directly.

If the keys change, override them with environment variables:

```bash
export PAYMENT_SERVICE_API_KEY="lms_live_..."
export ORDER_SERVICE_API_KEY="lms_live_..."
```

Or put overrides in the root `.env` file:

```bash
PAYMENT_SERVICE_API_KEY=lms_live_...
ORDER_SERVICE_API_KEY=lms_live_...
```

## Normal traffic

Run normal traffic continuously for both services:

```bash
./demo/run-normal.sh
```

Expected result:

- Live logs keep flowing.
- No anomaly report should be created from normal traffic.

## Incident scenarios

Run one controlled incident at a time:

```bash
./demo/run-timeout.sh
./demo/run-security.sh
./demo/run-error-flood.sh
./demo/run-resource.sh
./demo/run-access-denied.sh
./demo/run-suspicious.sh
```

## Custom alert rule demo

Use this scenario to test a business-specific alert rule that is different from
the built-in anomaly rules:

```bash
./demo/run-invoice.sh
```

Create this alert rule before running the script:

```text
Rule name: Invoice job failure
Application: order-service
Minimum severity: WARN
Alert severity: ERROR
Keyword pattern: invoice_generation_failed
Threshold count: 2
Window: 300 seconds
Cooldown: 300 seconds
Active time window: All day
Delivery: WebSocket or Telegram
```

Expected result:

- The script sends 3 `WARN invoice_generation_failed` logs.
- The custom alert rule should trigger after 2 matching logs.
- This scenario should not create a duplicate anomaly notification under the
  default anomaly thresholds because it stays below the 20-event suspicious
  keyword threshold.

## Business rule demo without anomaly overlap

Use this scenario when you want exactly one business alert and no duplicate
anomaly notification during the demo:

```bash
./demo/run-inventory-mismatch.sh
```

Create this alert rule before running the script:

```text
Rule name: Inventory reservation mismatch
Application: order-service
Minimum severity: WARN
Alert severity: WARN
Keyword pattern: inventory_reservation_mismatch
Threshold count: 2
Window: 300 seconds
Cooldown: 300 seconds
Active time window: All day
Delivery: WebSocket or Telegram
```

Expected result:

- The script sends 3 `WARN inventory_reservation_mismatch` logs.
- The custom alert rule should trigger after 2 matching logs.
- This avoids the built-in anomaly log rules because it does not use CRITICAL,
  ERROR, timeout, exception, denied/auth, resource, failed/failure, degraded,
  unavailable, retry-exhausted, or circuit-breaker keywords.

## Metric anomaly demo

Use this script to inject a metric snapshot that exceeds the built-in anomaly
thresholds:

```bash
APP_ID=<application-uuid> ./demo/run-metric-anomaly.sh
```

Defaults:

```text
METRIC_RULE=CPU_USAGE
CURRENT=95.0
RECENT_VALUES=80,86,88,91,92,95.0
```

Expected result:

- The script writes the Redis metric keys read by the backend metric detector.
- Within about 30 seconds, an `ANOMALY_METRIC` report should appear under
  Alerts -> Anomaly Reports.
- Default `CPU_USAGE=95.0` is above the 90% critical threshold, so the report
  severity should be `CRITICAL` because 2 of the last 3 samples are above the
  critical threshold.

Useful defaults:

```bash
BASE_URL=http://localhost:8080/api/v1
BATCH_SIZE=10
REPEAT=1
FOREVER=0
LINE_STEP_MS=1000
LOOP_DELAY_MS=3000
```

The log files intentionally do not include timestamps. The script generates a
fresh timestamp for each line, increasing by `LINE_STEP_MS`, so the backend sees
the event sequence in chronological order.

```text
Rule name: Invoice job failure
Application: order-service
Minimum severity: ERROR
Alert severity: ERROR
Keyword pattern: invoice_generation_failed
Threshold count: 2
Window (s): 300
Cooldown (s): 300
Active all day: checked
Delivery: WebSocket Live Delivery



./run-metric-anomaly.sh ec6a0a1e-4060-4363-98fa-7994692b017c
```

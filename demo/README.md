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

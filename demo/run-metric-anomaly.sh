#!/usr/bin/env bash

set -euo pipefail

usage() {
  cat <<'EOF'
Usage:
  APP_ID=ec6a0a1e-4060-4363-98fa-7994692b017c ./demo/run-metric-anomaly.sh
  ./demo/run-metric-anomaly.sh ec6a0a1e-4060-4363-98fa-7994692b017c

Environment variables:
  APP_ID            Application UUID to attach the metric anomaly to.
  METRIC_RULE       CPU_USAGE, MEMORY_USAGE, DISK_USAGE, DISK_WRITE_RATE,
                    NETWORK_RX_RATE, or NETWORK_TX_RATE. Default: CPU_USAGE.
  CURRENT           Current metric value. Default: 95.0.
  AVG               Average metric value. Default: derived from RECENT_VALUES.
  MAX               Maximum metric value. Default: derived from RECENT_VALUES.
  SAMPLES           Sample count. Default: derived from RECENT_VALUES.
  RECENT_VALUES     Comma-separated recent values. Default creates a critical
                    CPU-like pattern: 80,86,88,91,92,CURRENT.
  TTL_SECONDS       Snapshot TTL. Default: 120.
  INDEX_TTL_SECONDS Active index TTL. Default: 900.

The backend metric detector runs every 30 seconds by default. After running
this script, wait up to 30 seconds and open Alerts -> Anomaly Reports.
EOF
}

if [[ "${1:-}" == "-h" || "${1:-}" == "--help" ]]; then
  usage
  exit 0
fi

APP_ID="${APP_ID:-${1:-}}"
METRIC_RULE="${METRIC_RULE:-CPU_USAGE}"
CURRENT="${CURRENT:-95.0}"
RECENT_VALUES="${RECENT_VALUES:-80,86,88,91,92,$CURRENT}"
DERIVED_STATS="$(awk -v values="$RECENT_VALUES" 'BEGIN {
  n = split(values, parts, ",");
  sum = 0;
  max = parts[1] + 0;
  for (i = 1; i <= n; i++) {
    value = parts[i] + 0;
    sum += value;
    if (value > max) {
      max = value;
    }
  }
  printf "%.4f %.4f %d", sum / n, max, n;
}')"
read -r DERIVED_AVG DERIVED_MAX DERIVED_SAMPLES <<<"$DERIVED_STATS"
AVG="${AVG:-$DERIVED_AVG}"
MAX="${MAX:-$DERIVED_MAX}"
SAMPLES="${SAMPLES:-$DERIVED_SAMPLES}"
TTL_SECONDS="${TTL_SECONDS:-120}"
INDEX_TTL_SECONDS="${INDEX_TTL_SECONDS:-900}"
METRIC_GROUP="RESOURCE_HEALTH"

if [[ -z "$APP_ID" ]]; then
  echo "APP_ID is required." >&2
  usage >&2
  exit 1
fi

case "$METRIC_RULE" in
  CPU_USAGE|MEMORY_USAGE|DISK_USAGE|DISK_WRITE_RATE|NETWORK_RX_RATE|NETWORK_TX_RATE)
    ;;
  *)
    echo "Unsupported METRIC_RULE: $METRIC_RULE" >&2
    usage >&2
    exit 1
    ;;
esac

redis() {
  docker compose exec -T redis redis-cli "$@"
}

NOW="$(date -u +"%Y-%m-%dT%H:%M:%SZ")"
SNAPSHOT_KEY="anomaly:$APP_ID:metric:$METRIC_RULE"
RULE_INDEX_KEY="anomaly:$APP_ID:metric-rules:active"
APP_INDEX_KEY="anomaly:metric-applications:active"
DEDUP_KEY="anomaly:$APP_ID:dedup:metric:$METRIC_GROUP"
RECENT_VALUES_JSON="[$RECENT_VALUES]"
PAYLOAD="{\"ruleId\":\"$METRIC_RULE\",\"current\":$CURRENT,\"avg\":$AVG,\"max\":$MAX,\"samples\":$SAMPLES,\"recentValues\":$RECENT_VALUES_JSON,\"lastSeen\":\"$NOW\"}"

redis DEL "$DEDUP_KEY" >/dev/null
redis SADD "$APP_INDEX_KEY" "$APP_ID" >/dev/null
redis EXPIRE "$APP_INDEX_KEY" "$INDEX_TTL_SECONDS" >/dev/null
redis SADD "$RULE_INDEX_KEY" "$METRIC_RULE" >/dev/null
redis EXPIRE "$RULE_INDEX_KEY" "$INDEX_TTL_SECONDS" >/dev/null
redis SETEX "$SNAPSHOT_KEY" "$TTL_SECONDS" "$PAYLOAD" >/dev/null

cat <<EOF
Metric anomaly snapshot injected.

Application: $APP_ID
Metric rule: $METRIC_RULE
Current:     $CURRENT
Recent:      $RECENT_VALUES_JSON
Snapshot:    $SNAPSHOT_KEY

Wait up to 30 seconds, then open Alerts -> Anomaly Reports.
EOF

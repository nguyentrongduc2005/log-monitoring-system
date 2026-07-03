#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
PYTHON_SCRIPT="$SCRIPT_DIR/demo_ingest.py"

BASE_URL="${BASE_URL:-http://localhost:8080/api/v1}"
SCENARIO="${SCENARIO:-normal}"
SERVICE="${SERVICE:-all}"
BATCH_SIZE="${BATCH_SIZE:-10}"
REPEAT="${REPEAT:-1}"
FOREVER="${FOREVER:-0}"
LINE_STEP_MS="${LINE_STEP_MS:-1000}"
LOOP_DELAY_MS="${LOOP_DELAY_MS:-3000}"

usage() {
  cat <<'EOF'
Usage:
  SCENARIO=normal SERVICE=all FOREVER=1 ./demo/send-demo-logs.sh
  SCENARIO=timeout SERVICE=payment-service ./demo/send-demo-logs.sh

Environment variables:
  BASE_URL                 Backend API base URL.
  SCENARIO                 normal, timeout, security, error-flood, resource,
                           access-denied, suspicious, invoice.
  SERVICE                  all, payment-service, or order-service.
  BATCH_SIZE               Logs per API batch.
  REPEAT                   Number of scenario loops when FOREVER=0.
  FOREVER                  Set 1 to loop until Ctrl+C.
  LINE_STEP_MS             Timestamp increment between log lines.
  LOOP_DELAY_MS            Delay between scenario loops.
  PAYMENT_SERVICE_API_KEY  API key for payment-service.
  ORDER_SERVICE_API_KEY    API key for order-service.
EOF
}

if [[ "${1:-}" == "-h" || "${1:-}" == "--help" ]]; then
  usage
  exit 0
fi

if ! command -v python3 >/dev/null 2>&1; then
  echo "python3 is required." >&2
  exit 1
fi

if [[ ! -f "$PYTHON_SCRIPT" ]]; then
  echo "Demo ingestion script not found: $PYTHON_SCRIPT" >&2
  exit 1
fi

args=(
  "$PYTHON_SCRIPT"
  "--base-url" "$BASE_URL"
  "--scenario" "$SCENARIO"
  "--service" "$SERVICE"
  "--batch-size" "$BATCH_SIZE"
  "--repeat" "$REPEAT"
  "--line-step-ms" "$LINE_STEP_MS"
  "--loop-delay-ms" "$LOOP_DELAY_MS"
)

if [[ "$FOREVER" == "1" ]]; then
  args+=("--forever")
fi

exec python3 "${args[@]}"

#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
PYTHON_SCRIPT="$SCRIPT_DIR/ingest_logs.py"

BASE_URL="${BASE_URL:-http://localhost:8080/api/v1}"
APP_NAME="${APP_NAME:-payment-service}"
API_KEY="${API_KEY:-${LMS_API_KEY:-}}"
MODE="${MODE:-batch}"
BATCH_SIZE="${BATCH_SIZE:-100}"
REPEAT="${REPEAT:-1}"
SLEEP_MS="${SLEEP_MS:-0}"
FOREVER="${FOREVER:-0}"

usage() {
  cat <<'EOF'
Usage:
  ./send-test-logs.sh [log-file] [extra ingest_logs.py options]

Examples:
  ./send-test-logs.sh
  API_KEY='lms_...' ./send-test-logs.sh ./payment.log
  LMS_API_KEY='lms_...' REPEAT=3 SLEEP_MS=200 ./send-test-logs.sh ./payment.log
  API_KEY='lms_...' MODE=single ./send-test-logs.sh ./payment.log
  API_KEY='lms_...' FOREVER=1 ./send-test-logs.sh ./payment.log

Environment variables:
  API_KEY or LMS_API_KEY  Raw application API key (required)
  APP_NAME                Registered application name (default: payment-service)
  BASE_URL                Backend API base URL (default: http://localhost:8080/api/v1)
  MODE                    batch or single (default: batch)
  BATCH_SIZE              Logs per batch, 1-500 (default: 100)
  REPEAT                  Number of times to resend the file (default: 1)
  SLEEP_MS                Delay between requests in milliseconds (default: 0)
  FOREVER                 Set to 1 to keep resending until Ctrl+C (default: 0)
EOF
}

if [[ "${1:-}" == "-h" || "${1:-}" == "--help" ]]; then
  usage
  exit 0
fi

LOG_FILE="${1:-${LOG_FILE:-$SCRIPT_DIR/app.log}}"
if (( $# > 0 )); then
  shift
fi

if [[ -z "$API_KEY" ]]; then
  if [[ -t 0 ]]; then
    read -r -s -p "Raw application API key: " API_KEY
    echo
  fi
  if [[ -z "$API_KEY" ]]; then
    echo "Missing API_KEY or LMS_API_KEY." >&2
    exit 1
  fi
fi

if ! command -v python3 >/dev/null 2>&1; then
  echo "python3 is required." >&2
  exit 1
fi

if [[ ! -f "$PYTHON_SCRIPT" ]]; then
  echo "Python ingestion script not found: $PYTHON_SCRIPT" >&2
  exit 1
fi

if [[ ! -f "$LOG_FILE" ]]; then
  echo "Log file does not exist: $LOG_FILE" >&2
  exit 1
fi

args=(
  "$PYTHON_SCRIPT"
  --file "$LOG_FILE"
  --application-name "$APP_NAME"
  --api-key "$API_KEY"
  --base-url "$BASE_URL"
  --mode "$MODE"
  --batch-size "$BATCH_SIZE"
  --repeat "$REPEAT"
  --sleep-ms "$SLEEP_MS"
)

if [[ "$FOREVER" == "1" ]]; then
  args+=(--forever)
fi

args+=("$@")

echo "Running ingest_logs.py for '$APP_NAME' using '$LOG_FILE' ($MODE mode)"
exec python3 "${args[@]}"

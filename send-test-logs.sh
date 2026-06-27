#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
PYTHON_SCRIPT="$SCRIPT_DIR/ingest_logs.py"

BASE_URL="${BASE_URL:-http://localhost:8080/api/v1}"
MODE="${MODE:-batch}"
REPEAT="${REPEAT:-1}"
FOREVER="${FOREVER:-1}"

usage() {
  cat <<'EOF'
Usage:
  ./send-test-logs.sh [extra ingest_logs.py options]

Examples:
  ./send-test-logs.sh
  REPEAT=3 ./send-test-logs.sh
  FOREVER=1 ./send-test-logs.sh

Environment variables:
  BASE_URL                Backend API base URL (default: http://localhost:8080/api/v1)
  MODE                    batch or single (default: batch)
  REPEAT                  Number of times to resend the file (default: 1)
  FOREVER                 Keep resending until Ctrl+C (default: 1; set 0 for one run)

Note: API Keys and Application Names are now configured inside ingest_logs.py!
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
  echo "Python ingestion script not found: $PYTHON_SCRIPT" >&2
  exit 1
fi

args=(
  "$PYTHON_SCRIPT"
  "--base-url" "$BASE_URL"
  "--mode" "$MODE"
  "--repeat" "$REPEAT"
)

if [[ "$FOREVER" == "1" ]]; then
  args+=("--forever")
fi

args+=("$@")

echo "Running parallel ingestion via ingest_logs.py..."
exec python3 "${args[@]}"

#!/usr/bin/env bash

set -euo pipefail

BASE_URL="${BASE_URL:-http://localhost:8080/api/v1}"
APP_NAME="${APP_NAME:-payment-service}"
LOG_COUNT="${LOG_COUNT:-3}"
LEVEL="${LEVEL:-ERROR}"
MESSAGE="${MESSAGE:-Payment processing failed due to gateway timeout}"

if [[ -z "${API_KEY:-}" ]]; then
  echo "Missing API_KEY." >&2
  echo "Usage: API_KEY='<raw-api-key>' APP_NAME='payment-service' ./send-test-logs.sh" >&2
  exit 1
fi

if ! command -v curl >/dev/null 2>&1; then
  echo "curl is required." >&2
  exit 1
fi

if ! command -v python3 >/dev/null 2>&1; then
  echo "python3 is required to build the JSON payload." >&2
  exit 1
fi

if [[ ! "$LOG_COUNT" =~ ^[1-9][0-9]*$ ]] || (( LOG_COUNT > 500 )); then
  echo "LOG_COUNT must be an integer between 1 and 500." >&2
  exit 1
fi

if [[ ! "$LEVEL" =~ ^(TRACE|DEBUG|INFO|WARN|ERROR|CRITICAL)$ ]]; then
  echo "LEVEL must be TRACE, DEBUG, INFO, WARN, ERROR, or CRITICAL." >&2
  exit 1
fi

export APP_NAME LOG_COUNT LEVEL MESSAGE
payload="$(python3 - <<'PY'
import json
import os

application_name = os.environ["APP_NAME"]
count = int(os.environ["LOG_COUNT"])
level = os.environ["LEVEL"]
message = os.environ["MESSAGE"]

print(json.dumps({
    "applicationName": application_name,
    "rawLogs": [f"{level} {message}" for _ in range(count)],
}))
PY
)"

response_file="$(mktemp)"
trap 'rm -f "$response_file"' EXIT

idempotency_key="alert-test-$(date +%s)-$$"
endpoint="${BASE_URL%/}/logs/batch"

echo "Sending $LOG_COUNT $LEVEL logs for '$APP_NAME' to $endpoint"

http_status="$(curl --silent --show-error \
  --output "$response_file" \
  --write-out '%{http_code}' \
  --request POST \
  --header "X-API-Key: $API_KEY" \
  --header "Idempotency-Key: $idempotency_key" \
  --header "Content-Type: application/json" \
  --data "$payload" \
  "$endpoint")"

if [[ "$http_status" != "202" ]]; then
  echo "Request failed with HTTP $http_status:" >&2
  cat "$response_file" >&2
  echo >&2
  exit 1
fi

echo "Accepted by ingestion API (HTTP 202):"
python3 -m json.tool < "$response_file" 2>/dev/null || cat "$response_file"
echo
echo "Alert processing is asynchronous; allow a few seconds for Kafka consumers."

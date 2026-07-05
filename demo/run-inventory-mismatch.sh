#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"

SCENARIO=inventory-mismatch \
SERVICE=order-service \
BATCH_SIZE="${BATCH_SIZE:-1}" \
FOREVER="${FOREVER:-0}" \
REPEAT="${REPEAT:-1}" \
"$SCRIPT_DIR/send-demo-logs.sh"

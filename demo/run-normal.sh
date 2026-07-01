#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"

SCENARIO=normal \
SERVICE=all \
BATCH_SIZE=10 \
LOOP_DELAY_MS=100 \
LINE_STEP_MS=3 \
FOREVER="${FOREVER:-1}" \
"$SCRIPT_DIR/send-demo-logs.sh"

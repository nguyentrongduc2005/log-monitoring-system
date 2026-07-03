#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"

SCENARIO=suspicious \
SERVICE=payment-service \
FOREVER="${FOREVER:-0}" \
REPEAT="${REPEAT:-1}" \
"$SCRIPT_DIR/send-demo-logs.sh"

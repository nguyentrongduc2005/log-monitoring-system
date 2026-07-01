#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

curl --fail \
  http://localhost:8080/v3/api-docs \
  --output "$ROOT_DIR/docs/api/openapi.json"

cd "$ROOT_DIR/apps/frontend"
npx openapi-typescript \
  "$ROOT_DIR/docs/api/openapi.json" \
  --output src/api/generated/api-types.ts
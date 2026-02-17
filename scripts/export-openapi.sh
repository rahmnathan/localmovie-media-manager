#!/usr/bin/env bash
set -euo pipefail

# Exports the backend OpenAPI contract to a local file.
# Usage:
#   scripts/export-openapi.sh [output-file]
# Example:
#   scripts/export-openapi.sh ../localmovies-android/local-movie-app/src/main/openapi/localmovie-openapi.json

BASE_URL="${OPENAPI_BASE_URL:-http://localhost:8080}"
OUTPUT_FILE="${1:-target/openapi/localmovie-openapi.json}"
API_DOCS_URL="${BASE_URL}/v3/api-docs"

mkdir -p "$(dirname "${OUTPUT_FILE}")"

echo "Fetching OpenAPI from ${API_DOCS_URL}"
if command -v jq >/dev/null 2>&1; then
  curl -fsSL "${API_DOCS_URL}" | jq -S . > "${OUTPUT_FILE}"
else
  curl -fsSL "${API_DOCS_URL}" > "${OUTPUT_FILE}"
fi

echo "OpenAPI contract exported to ${OUTPUT_FILE}"

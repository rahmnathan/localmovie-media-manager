#!/usr/bin/env bash
set -euo pipefail

# Generates a full Java client library from OpenAPI.
# Defaults are aligned with Android consumption from Artifactory.
#
# Usage:
#   scripts/generate-api-client.sh <spec-file> <version> [output-dir]
#
# Example:
#   scripts/generate-api-client.sh target/openapi/localmovie-openapi.json 7.123 target/api-client

SPEC_FILE="${1:?spec file required}"
ARTIFACT_VERSION="${2:?artifact version required}"
OUTPUT_DIR="${3:-target/api-client}"

GROUP_ID="com.github.rahmnathan.localmovie"
ARTIFACT_ID="localmovie-api-client"
GENERATOR_IMAGE="${OPENAPI_GENERATOR_IMAGE:-openapitools/openapi-generator-cli:v7.12.0}"

if [[ ! -f "${SPEC_FILE}" ]]; then
  echo "Spec file not found: ${SPEC_FILE}" >&2
  exit 1
fi

rm -rf "${OUTPUT_DIR}"
mkdir -p "${OUTPUT_DIR}"

echo "Generating API client from ${SPEC_FILE}"

if command -v docker >/dev/null 2>&1; then
  docker run --rm \
    -v "$(pwd):/local" \
    "${GENERATOR_IMAGE}" generate \
      -i "/local/${SPEC_FILE}" \
      -g java \
      -o "/local/${OUTPUT_DIR}" \
      --additional-properties "groupId=${GROUP_ID},artifactId=${ARTIFACT_ID},artifactVersion=${ARTIFACT_VERSION},library=okhttp-gson,dateLibrary=java8,serializationLibrary=gson,hideGenerationTimestamp=true,useJakartaEe=true"
else
  echo "docker is required to run openapi-generator-cli in CI" >&2
  exit 1
fi

echo "Building generated client jar"
mvn -f "${OUTPUT_DIR}/pom.xml" -DskipTests package

echo "Generated client:"
echo "  POM: ${OUTPUT_DIR}/pom.xml"
echo "  JAR: ${OUTPUT_DIR}/target/${ARTIFACT_ID}-${ARTIFACT_VERSION}.jar"

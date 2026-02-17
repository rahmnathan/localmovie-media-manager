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
GENERATOR_VERSION="${OPENAPI_GENERATOR_VERSION:-7.12.0}"
GENERATOR_JAR="${OPENAPI_GENERATOR_JAR:-target/openapi/openapi-generator-cli-${GENERATOR_VERSION}.jar}"

if [[ ! -f "${SPEC_FILE}" ]]; then
  echo "Spec file not found: ${SPEC_FILE}" >&2
  exit 1
fi

rm -rf "${OUTPUT_DIR}"
mkdir -p "${OUTPUT_DIR}"
mkdir -p "$(dirname "${GENERATOR_JAR}")"

SPEC_FILE_ABS="$(cd "$(dirname "${SPEC_FILE}")" && pwd)/$(basename "${SPEC_FILE}")"
OUTPUT_DIR_ABS="$(cd "$(dirname "${OUTPUT_DIR}")" && pwd)/$(basename "${OUTPUT_DIR}")"

if [[ ! -f "${GENERATOR_JAR}" ]]; then
  echo "Downloading openapi-generator-cli ${GENERATOR_VERSION}"
  curl -fsSL \
    "https://repo1.maven.org/maven2/org/openapitools/openapi-generator-cli/${GENERATOR_VERSION}/openapi-generator-cli-${GENERATOR_VERSION}.jar" \
    -o "${GENERATOR_JAR}"
fi

echo "Generating API client from ${SPEC_FILE_ABS}"
java -jar "${GENERATOR_JAR}" generate \
  -i "${SPEC_FILE_ABS}" \
  -g java \
  -o "${OUTPUT_DIR_ABS}" \
  --additional-properties "groupId=${GROUP_ID},artifactId=${ARTIFACT_ID},artifactVersion=${ARTIFACT_VERSION},library=okhttp-gson,dateLibrary=java8,serializationLibrary=gson,hideGenerationTimestamp=true,useJakartaEe=true"

echo "Building generated client jar"
mvn -f "${OUTPUT_DIR_ABS}/pom.xml" -DskipTests package

echo "Generated client:"
echo "  POM: ${OUTPUT_DIR_ABS}/pom.xml"
echo "  JAR: ${OUTPUT_DIR_ABS}/target/${ARTIFACT_ID}-${ARTIFACT_VERSION}.jar"

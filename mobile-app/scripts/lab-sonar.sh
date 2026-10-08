#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"

: "${SONAR_TOKEN:?Set SONAR_TOKEN to a token created in your local SonarQube instance}"
SONAR_HOST_URL="${SONAR_HOST_URL:-http://localhost:9000}"

./gradlew :data:labCoverage

if command -v sonar-scanner >/dev/null 2>&1; then
  sonar-scanner \
    -Dsonar.host.url="$SONAR_HOST_URL" \
    -Dsonar.token="$SONAR_TOKEN"
elif command -v docker >/dev/null 2>&1; then
    docker run --rm --network host \
    -e SONAR_HOST_URL="$SONAR_HOST_URL" \
    -e SONAR_TOKEN="$SONAR_TOKEN" \
    -v "$ROOT:/usr/src:Z" \
    sonarsource/sonar-scanner-cli
else
  echo "Neither sonar-scanner nor docker is available." >&2
  exit 2
fi

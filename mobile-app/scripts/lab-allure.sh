#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"

rm -rf data/build/allure-results data/build/reports/allure
./gradlew :data:testDebugUnitTest :data:labCoverage

if ! command -v allure >/dev/null 2>&1; then
  cat >&2 <<'MSG'
Allure CLI is not installed or not present in PATH.
Raw results are ready in data/build/allure-results.
Install Allure CLI once, then rerun this script; the tests themselves need no network access after Gradle/Robolectric dependencies are cached.
MSG
  exit 2
fi

allure generate data/build/allure-results \
  --clean \
  --output data/build/reports/allure

echo "Allure report: $ROOT/data/build/reports/allure/index.html"

#!/usr/bin/env bash
# Run EXACTLY ONE test in isolated Docker environment. One GitHub matrix entry = one test.
set -Eeuo pipefail
KIND="${1:?python-unit|android-unit|integration|e2e|device required}"
ID="${2:?unique test id required}"
SELECTOR="${3:?pytest node ID or Kotlin fully qualified test name required}"
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
cd "$ROOT"
[[ "$ID" =~ ^[a-z0-9-]+$ ]] || { echo 'Unsafe test ID' >&2; exit 2; }
export COMPOSE_PROJECT_NAME="lab2_${GITHUB_RUN_ID:-local}_${GITHUB_RUN_ATTEMPT:-1}_${ID//-/_}"
export LAB2_GRADLE_CACHE_VOLUME="${LAB2_GRADLE_CACHE_VOLUME:-studymate_lab2_gradle_cache}"
REPORTS="$ROOT/lab2/reports"
mkdir -p "$REPORTS/junit" "$REPORTS/allure-results" "$REPORTS/ci-states"
# Self-hosted runners can reuse the same checkout directory between matrix jobs.
# Each artifact must contain results from exactly this one test only.
find "$REPORTS/junit" "$REPORTS/allure-results" "$REPORTS/ci-states" -mindepth 1 -maxdepth 1 -exec rm -rf {} +
compose() { docker compose -f "$ROOT/lab2/docker-compose.test.yml" "$@"; }
cleanup() { compose down --volumes --remove-orphans >/dev/null 2>&1 || :; }
trap cleanup EXIT
trap 'exit 130' INT
trap 'exit 143' TERM
# Never use the application's normal DB; Compose provisions disposable PostgreSQL.
docker volume create "$LAB2_GRADLE_CACHE_VOLUME" >/dev/null

pytest_one() {
  local marker="$1" target="$2" suffix="$3"
  compose run --build --rm "$suffix" tests python -m pytest -m "$marker" "$target" -v \
    "--junitxml=/workspace/lab2/reports/junit/${ID}.xml" \
    --alluredir=/workspace/lab2/reports/allure-results
}

copy_android_xml() {
  local from="$1" prefix="$2" file suffix n=0
  if [[ -d "$from" ]]; then
    while IFS= read -r -d '' file; do
      suffix="${file##*/}"
      cp "$file" "$REPORTS/junit/${prefix}-${ID}-${n}-${suffix}"
      n=$((n + 1))
    done < <(find "$from" -name '*.xml' -type f -print0)
  fi
  echo "Collected $n Android JUnit report(s) for $ID"
  (( n > 0 )) || { echo "No JUnit XML produced for selected Android test $ID" >&2; return 1; }
}

case "$KIND" in
  python-unit)
    # --no-deps means a unit test never starts the HTTP API or database.
    pytest_one unit "$SELECTOR" --no-deps
    ;;
  android-unit)
    # Each GitHub job runs exactly the selected @Test method.
    rm -rf "$ROOT/mobile-app/data/build/test-results/testDebugUnitTest"
    compose run --build --rm --no-deps android-tests bash -lc \
      'chmod +x ./gradlew && ./gradlew --no-daemon :data:testDebugUnitTest --tests "$1"' _ "$SELECTOR"
    copy_android_xml "$ROOT/mobile-app/data/build/test-results/testDebugUnitTest" unit-TEST
    ;;
  integration)
    compose up --build -d --wait postgres api
    # Repeat this ONE test twice against the same isolated DB instance.
    pytest_one integration "$SELECTOR" --no-deps
    mv "$REPORTS/junit/$ID.xml" "$REPORTS/junit/${ID}-repeat1.xml"
    compose run --rm --no-deps tests python -m pytest -m integration "$SELECTOR" -v \
      "--junitxml=/workspace/lab2/reports/junit/${ID}-repeat2.xml" \
      --alluredir=/workspace/lab2/reports/allure-results
    ;;
  e2e)
    compose up --build -d --wait postgres api
    pytest_one e2e "$SELECTOR" --no-deps
    ;;
  device)
    compose up --build -d --wait postgres api
    published="$(compose port api 8000 | awk -F: '{print $NF}')"
    [[ "$published" =~ ^[0-9]+$ ]] || { echo 'No published API port for adb reverse' >&2; exit 1; }
    rm -rf "$ROOT/mobile-app/data/build/outputs/androidTest-results"
    # The runner must be Linux self-hosted with Docker host network + USB ADB.
    compose run --build --rm --no-deps android-tests bash -lc \
      'adb devices | grep -Eq "[[:space:]]device$" && adb reverse tcp:8000 "tcp:$1" && chmod +x ./gradlew && ./gradlew --no-daemon :data:connectedDebugAndroidTest "-Pandroid.testInstrumentationRunnerArguments.class=$2" -Pandroid.testInstrumentationRunnerArguments.studymate.baseUrl=http://127.0.0.1:8000/' \
      _ "$published" "${SELECTOR%.*}#${SELECTOR##*.}"
    copy_android_xml "$ROOT/mobile-app/data/build/outputs/androidTest-results" android-device
    ;;
  *) echo "Unknown test kind: $KIND" >&2; exit 2 ;;
esac

#!/usr/bin/env bash
# StudyMate Lab 2: run the entire pipeline or an individual test stage.
# Every invocation uses a fresh disposable PostgreSQL/API Compose project.
set -Eeuo pipefail

usage() {
  cat <<'USAGE'
Usage: lab2/scripts/run-lab2.sh [all|unit|unit-python|unit-android|integration|android-device|e2e|report]

  all             Unit -> integration (twice) -> optional Android integration ->
                  REAL Android app E2E (requires phone or running emulator).
                  Set RUN_ANDROID_DEVICE_TESTS=1 to include device integration.
  unit            Python and Android unit tests, plus JaCoCo coverage.
  unit-python     Python unit tests only.
  unit-android    Kotlin/Robolectric unit tests and JaCoCo only.
  integration     Server repository integration tests twice on one test DB.
  android-device  Instrumentation tests on a connected Android device; starts
                  its own PostgreSQL/API stand, without running other tests.
  e2e             Headless Android app -> Retrofit -> API -> PostgreSQL -> app.
                  Requires a connected phone or booted emulator; never runs UI.
  report          Regenerate Allure HTML from existing allure-results only.

Non-selected stages are marked skipped in a single-stage run.
All testing modes generate reports and always destroy their temporary stand.
USAGE
}

if [[ "${1:-}" == '-h' || "${1:-}" == '--help' ]]; then
  usage
  exit 0
fi
if (( $# > 1 )); then
  usage >&2
  exit 2
fi
MODE="${1:-all}"
case "$MODE" in
  all|unit|unit-python|unit-android|integration|android-device|e2e|report) ;;
  *) printf 'Unknown test stage: %s\n\n' "$MODE" >&2; usage >&2; exit 2 ;;
esac

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
cd "$ROOT"
export COMPOSE_PROJECT_NAME="studymate_lab2_${CI_JOB_ID:-$(date +%s)_$$}"
COMPOSE_FILE="$ROOT/lab2/docker-compose.test.yml"
REPORTS="$ROOT/lab2/reports"
compose() { docker compose -f "$COMPOSE_FILE" "$@"; }

# Compose may validate external volumes even when only some services are started.
# This idempotent call ensures every stage can run independently. External Docker
# volumes are never removed by `docker compose down --volumes`.
GRADLE_CACHE_VOLUME="${LAB2_GRADLE_CACHE_VOLUME:-studymate_lab2_gradle_cache}"
docker volume create "$GRADLE_CACHE_VOLUME" >/dev/null

# Regenerating a report must NOT erase the results of the previous test run.
if [[ "$MODE" == report ]]; then
  # A report-only Compose run can create a project network too; always remove it.
  trap 'compose down --volumes --remove-orphans >/dev/null 2>&1 || :' EXIT
  compose run --build --rm --no-deps report
  printf '\nAllure HTML: %s/allure-report/index.html\n' "$REPORTS"
  exit 0
fi

mkdir -p "$REPORTS/junit" "$REPORTS/allure-results" "$REPORTS/history"
# Keep Allure history, but remove results from previous test executions.
find "$REPORTS/junit" "$REPORTS/allure-results" -mindepth 1 -maxdepth 1 -exec rm -rf {} +

clean_stand() { compose down --volumes --remove-orphans >/dev/null 2>&1 || :; }
finalize() {
  local status=$?
  trap - EXIT HUP INT TERM
  clean_stand
  # The report is attempted even if the selected test stage fails.
  if ! compose run --build --rm --no-deps report; then
    echo 'WARNING: Allure HTML generation failed; JUnit/raw Allure results are preserved' >&2
  fi
  # Compose run above recreates the default project network; delete it too.
  clean_stand
  exit "$status"
}
trap finalize EXIT
trap 'exit 130' INT
trap 'exit 143' TERM

mark() { python3 "$ROOT/lab2/scripts/report_stage.py" "$1" "$2" "$3" --reports "$REPORTS"; }
copy_junit() {
  local source_dir="$1" prefix="$2" file name
  if [ -d "$source_dir" ]; then
    while IFS= read -r -d '' file; do
      name="$(basename "$file")"
      cp "$file" "$REPORTS/junit/${prefix}-${name}"
    done < <(find "$source_dir" -name '*.xml' -type f -print0)
  fi
}

# Single-stage reports explicitly distinguish the tests selected from those skipped.
mark_not_selected() {
  # Parallel GitHub jobs represent one overall pipeline: marking other stages as
  # skipped inside each job would make the combined Allure report misleading.
  if [[ "${LAB2_CI_STAGE:-0}" == 1 ]]; then return 0; fi
  local selected="$1" stage
  for stage in unit integration android-device e2e; do
    if [[ "$stage" != "$selected" ]]; then
      mark "$stage" skipped "Not selected: running only $selected"
    fi
  done
}

start_stand() {
  if ! compose up --build -d --wait postgres api; then
    echo 'ERROR: Failed to start isolated PostgreSQL + FastAPI test stand' >&2
    return 1
  fi
}

run_unit_python() {
  printf '\n=== STAGE: Python schema unit tests ===\n'
  if ! compose run --build --rm --no-deps tests python -m pytest -m unit tests/unit -v \
    --junitxml=/workspace/lab2/reports/junit/unit-server.xml \
    --alluredir=/workspace/lab2/reports/allure-results; then
    mark unit failed 'Server Python unit tests failed'
    return 1
  fi
}

run_unit_android() {
  printf '\n=== STAGE: Android JVM/Robolectric unit tests ===\n'
  local rc=0
  compose run --build --rm --no-deps android-tests bash -lc \
    'chmod +x ./gradlew && ./gradlew --no-daemon :data:labCoverage' || rc=$?
  copy_junit "$ROOT/mobile-app/data/build/test-results/testDebugUnitTest" unit
  if [ -d "$ROOT/mobile-app/data/build/allure-results" ]; then
    cp -a "$ROOT/mobile-app/data/build/allure-results/." "$REPORTS/allure-results/"
  fi
  if [[ "$rc" -ne 0 ]]; then
    mark unit failed "Android Gradle lab1 unit suite exited with code $rc"
    return 1
  fi
}

run_unit() {
  printf '\n=== STAGE 1: LAB1 Android + Python unit suites ===\n'
  run_unit_python || return 1
  run_unit_android || return 1
}

run_integration() {
  printf '\n=== STAGE 2: PostgreSQL/repository integration ===\n'
  local pass
  # Repeat on the SAME PostgreSQL instance to demonstrate test repeatability.
  for pass in 1 2; do
    echo "Integration repeat ${pass}/2 (same PostgreSQL instance)"
    if ! compose run --build --rm tests python -m pytest -m integration tests/integration -v \
      "--junitxml=/workspace/lab2/reports/junit/integration-${pass}.xml" \
      --alluredir=/workspace/lab2/reports/allure-results; then
      mark integration failed "Repository integration repeat ${pass}/2 failed"
      return 1
    fi
  done
}

run_android_device() {
  printf '\n=== STAGE: Android integration on a connected device ===\n'
  local host_port rc=0
  host_port="$(compose port api 8000 | awk -F: '{print $NF}')"
  if [[ ! "$host_port" =~ ^[0-9]+$ ]]; then
    echo 'ERROR: Cannot determine published API port for adb reverse' >&2
    mark android-device failed 'Cannot determine API host port'
    return 1
  fi
  echo "Starting Android device tests using adb reverse tcp:8000 -> tcp:${host_port}"
  # Do not import stale XML results from a previous instrumentation run.
  rm -rf "$ROOT/mobile-app/data/build/outputs/androidTest-results"
  compose run --build --rm --no-deps android-tests bash -lc \
    "adb devices | grep -Eq 'device$' && adb reverse tcp:8000 tcp:${host_port} && chmod +x ./gradlew && ./gradlew --no-daemon :data:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.z23u184.studymate.data.RealServerDataIntegrationTest -Pandroid.testInstrumentationRunnerArguments.studymate.baseUrl=http://127.0.0.1:8000/" || rc=$?
  copy_junit "$ROOT/mobile-app/data/build/outputs/androidTest-results" android-device
  if [[ "$rc" -ne 0 ]]; then
    mark android-device failed "Android instrumentation failed or ADB device unavailable (exit $rc)"
    return 1
  fi
}

run_e2e() {
  printf '\n=== STAGE 3: Headless Android APP -> Retrofit -> FastAPI -> PostgreSQL -> Android ===\n'
  local host_port rc=0
  host_port="$(compose port api 8000 | awk -F: '{print $NF}')"
  if [[ ! "$host_port" =~ ^[0-9]+$ ]]; then
    mark e2e failed 'Cannot determine API host port for Android E2E'
    return 1
  fi
  # ADB server runs on Linux host (Fedora or GitHub emulator runner). The device
  # reaches the isolated API through a device-local reverse tunnel. Android code
  # under test uses the REAL Room/Retrofit/repository/sync implementations.
  echo "Starting Android application E2E through adb reverse tcp:8000 -> tcp:${host_port}"
  rm -rf "$ROOT/mobile-app/data/build/outputs/androidTest-results"
  compose run --build --rm --no-deps android-tests bash -lc \
    "adb devices | grep -Eq 'device$' && adb reverse tcp:8000 tcp:${host_port} && chmod +x ./gradlew && ./gradlew --no-daemon :app:connectedDebugAndroidTest -Plab2E2e=true -Plab2E2eBaseUrl=http://127.0.0.1:8000/ -Pandroid.testInstrumentationRunnerArguments.class=com.z23u184.studymate.app.StudyMateApplicationE2ETest" || rc=$?
  copy_junit "$ROOT/mobile-app/app/build/outputs/androidTest-results" android-e2e
  if [[ "$rc" -ne 0 ]]; then
    mark e2e failed "Headless Android application E2E failed or ADB unavailable (exit $rc)"
    return 1
  fi
}

case "$MODE" in
  unit-python)
    mark_not_selected unit
    run_unit_python || exit 1
    ;;
  unit-android)
    mark_not_selected unit
    run_unit_android || exit 1
    ;;
  unit)
    mark_not_selected unit
    run_unit || exit 1
    ;;
  integration)
    mark_not_selected integration
    if ! start_stand; then
      mark integration failed 'Cannot start disposable PostgreSQL + FastAPI test stand'
      exit 1
    fi
    run_integration || exit 1
    ;;
  android-device)
    mark_not_selected android-device
    if ! start_stand; then
      mark android-device failed 'Cannot start disposable PostgreSQL + FastAPI test stand'
      exit 1
    fi
    run_android_device || exit 1
    ;;
  e2e)
    mark_not_selected e2e
    if ! start_stand; then
      mark e2e failed 'Cannot start disposable PostgreSQL + FastAPI test stand'
      exit 1
    fi
    run_e2e || exit 1
    ;;
  all)
    if ! run_unit; then
      mark integration skipped 'unit stage failed'
      mark android-device skipped 'unit stage failed'
      mark e2e skipped 'unit stage failed'
      exit 1
    fi
    if ! start_stand; then
      mark integration failed 'Cannot start disposable PostgreSQL + FastAPI test stand'
      mark android-device skipped 'integration stand could not start'
      mark e2e skipped 'integration stage failed'
      exit 1
    fi
    if ! run_integration; then
      mark android-device skipped 'server integration failed before device tests'
      mark e2e skipped 'integration stage failed'
      exit 1
    fi
    if [[ "${RUN_ANDROID_DEVICE_TESTS:-0}" == 1 ]]; then
      if ! run_android_device; then
        mark e2e skipped 'Android device integration failed'
        exit 1
      fi
    else
      mark android-device skipped 'Set RUN_ANDROID_DEVICE_TESTS=1 to include device tests in a full run'
    fi
    # Integration and Android E2E never share DB/files or active refresh sessions.
    printf '\n=== RESET isolated DB/storage between integration and E2E ===\n'
    clean_stand
    if ! start_stand; then
      mark e2e failed 'Cannot recreate clean E2E stand'
      exit 1
    fi
    run_e2e || exit 1
    ;;
esac

printf '\nSTAGE %s PASSED. Reports: %s\n' "$MODE" "$REPORTS"
if [[ "$MODE" == all ]]; then
  echo 'ALL STAGES PASSED.'
fi

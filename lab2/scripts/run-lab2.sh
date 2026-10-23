#!/usr/bin/env bash
# StudyMate Lab 2: run the entire pipeline or an individual test stage.
# Every invocation uses a fresh disposable PostgreSQL/API Compose project.
set -Eeuo pipefail

usage() {
  cat <<'USAGE'
Usage: lab2/scripts/run-lab2.sh [all|build|unit|unit-python|unit-android|integration|android-device|e2e|report]

  all             Unit -> integration (twice) -> optional Android integration ->
                  REAL Android app E2E (requires running Android emulator).
                  Set RUN_ANDROID_DEVICE_TESTS=1 to include device integration.
  build           Assemble the disposable Android app and its instrumentation APK.
  unit            Python and Android unit tests, plus JaCoCo coverage.
  unit-python     Python unit tests only.
  unit-android    Kotlin/Robolectric unit tests and JaCoCo only.
  integration     Server repository integration tests twice on one test DB.
  android-device  Instrumentation tests on a connected Android device; starts
                  its own PostgreSQL/API stand, without running other tests.
  e2e             Headless Android app -> Retrofit -> API -> PostgreSQL -> app.
                  Emulator only. Saves real Android E2E HTTP traffic to PCAP.
                  LAB2_E2E_PREBUILT_APKS=1 installs artifacts from build stage.
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
  all|build|unit|unit-python|unit-android|integration|android-device|e2e|report) ;;
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
  for stage in build unit integration android-device e2e; do
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

run_build() {
  printf '\n=== BUILD STAGE: Android E2E APK + instrumentation APK ===\n'
  # Exactly the same optional applicationId suffix and bridge as the E2E test.
  if ! compose run --build --rm --no-deps android-tests bash -lc \
    'chmod +x ./gradlew && ./gradlew --no-daemon :app:assembleDebug :app:assembleDebugAndroidTest -Plab2E2e=true -Plab2E2eBaseUrl=http://127.0.0.1:8000/'; then
    mark build failed 'E2E app/test APK compilation failed'
    return 1
  fi
  local dir="$ROOT/mobile-app/app/build/outputs/apk"
  test -s "$dir/debug/app-debug.apk" && test -s "$dir/androidTest/debug/app-debug-androidTest.apk" || {
    mark build failed 'Missing E2E APKs after Gradle assemble'
    return 1
  }
  echo "APK ready: $dir/debug/app-debug.apk"
  echo "Test APK ready: $dir/androidTest/debug/app-debug-androidTest.apk"
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

# Collect logs before the EXIT trap removes the disposable API and PostgreSQL.
# No server logs are needed for a successful run; diagnostics are retained on failure.
save_server_diagnostics() {
  local stage="$1" target="$REPORTS/diagnostics"
  mkdir -p "$target"
  compose logs --no-color --timestamps api > "$target/${stage}-api.log" 2>&1 || true
  compose logs --no-color --timestamps postgres > "$target/${stage}-postgres.log" 2>&1 || true
  echo "Server diagnostics saved to $target/${stage}-{api,postgres}.log" >&2
  if [[ -s "$target/${stage}-api.log" ]]; then
    echo 'Last FastAPI log lines:' >&2
    tail -n 55 "$target/${stage}-api.log" >&2
  fi
}

run_android_device() {
  printf '\n=== STAGE: Android integration on a connected device ===\n'
  local host_port rc=0 test_filter
  # Optional single-method rerun for debugging a flaky test; default is the whole suite.
  test_filter="${LAB2_ANDROID_TEST_FILTER:-com.z23u184.studymate.data.RealServerDataIntegrationTest}"
  if [[ ! "$test_filter" =~ ^[A-Za-z0-9_.]+(#[A-Za-z0-9_]+)?$ ]]; then
    echo "ERROR: Invalid LAB2_ANDROID_TEST_FILTER: $test_filter" >&2
    return 2
  fi
  echo "Android instrumentation filter: $test_filter"
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
    "adb devices | grep -Eq 'device$' && adb reverse tcp:8000 tcp:${host_port} && chmod +x ./gradlew && ./gradlew --no-daemon :data:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=${test_filter} -Pandroid.testInstrumentationRunnerArguments.studymate.baseUrl=http://127.0.0.1:8000/" || rc=$?
  copy_junit "$ROOT/mobile-app/data/build/outputs/androidTest-results" android-device
  if [[ "$rc" -ne 0 ]]; then
    save_server_diagnostics android-device
    mark android-device failed "Android instrumentation failed or ADB device unavailable (exit $rc)"
    return 1
  fi
}

run_e2e() {
  printf '\n=== E2E: Android APP -> FastAPI -> PostgreSQL -> Android (real traffic capture) ===\n'
  local host_port rc=0 serial='' android_rc=0
  local -a docker_env=()

  # Fail before touching a physical phone. Use exactly one booted emulator.
  serial="$(adb devices | awk '$1 ~ /^emulator-[0-9]+$/ && $2 == "device" { print $1; exit }')"
  if [[ ! "$serial" =~ ^emulator-[0-9]+$ ]]; then
    mark e2e failed 'No ready Android emulator detected by ADB (real phones not supported)'
    return 1
  fi
  echo "E2E target: $serial"
  docker_env=(-e "ANDROID_SERIAL=$serial")

  # The capture container shares API network namespace. Unlike replay-http.sh,
  # this capture includes requests from the actual installed Android APK.
  rm -f "$REPORTS/lab2-e2e.pcap"
  # PostgreSQL and API are already running and healthy from start_stand().
  # Do not recreate API when starting the capture sidecar: Docker may assign a
  # different published API port when its container is recreated.
  if ! compose up --build --no-recreate -d capture; then
    mark e2e failed 'Could not start tcpdump in API network namespace'
    return 1
  fi
  sleep 2 # Ensure tcpdump initialized before app login/sync requests.
  if [[ "$(compose ps --status running --services capture)" != *capture* ]]; then
    compose logs capture >&2 || true
    mark e2e failed 'tcpdump exited before E2E started'
    return 1
  fi

  # Resolve the port AFTER starting capture, since Compose may otherwise
  # recreate the API and invalidate an earlier published-port lookup.
  if ! host_port="$(compose port api 8000 | awk -F: 'NF {print $NF; exit}')" ||
      [[ ! "$host_port" =~ ^[0-9]+$ ]]; then
    save_server_diagnostics e2e
    mark e2e failed 'Cannot determine current API host port for Android E2E'
    return 1
  fi
  echo "Current API published host port: $host_port"

  # Detect an unreachable published port before starting Android instrumentation.
  if ! curl --fail --silent --show-error --connect-timeout 2 --max-time 5 \
      --retry 5 --retry-connrefused --retry-delay 1 \
      "http://127.0.0.1:${host_port}/health" >/dev/null; then
    save_server_diagnostics e2e
    mark e2e failed "API /health is unreachable on host port ${host_port}"
    return 1
  fi
  echo "API /health: OK on host port $host_port"

  echo "adb reverse tcp:8000 -> 127.0.0.1:${host_port} (Docker API published port)"
  if [[ "${LAB2_E2E_PREBUILT_APKS:-0}" == 1 ]]; then
    # CI build job already compiled both APKs. Do NOT rebuild in test stage.
    local app_apk="$ROOT/mobile-app/app/build/outputs/apk/debug/app-debug.apk"
    local test_apk="$ROOT/mobile-app/app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk"
    if [[ ! -s "$app_apk" || ! -s "$test_apk" ]]; then
      echo 'Missing build-stage APK artifacts (LAB2_E2E_PREBUILT_APKS=1)' >&2
      android_rc=1
    else
      adb -s "$serial" reverse --remove tcp:8000 >/dev/null 2>&1 || true
      adb -s "$serial" reverse tcp:8000 tcp:"$host_port" || android_rc=$?
      if [[ "$android_rc" == 0 ]]; then adb -s "$serial" reverse --list; fi
      if [[ "$android_rc" == 0 ]]; then adb -s "$serial" install -r "$app_apk" || android_rc=$?; fi
      if [[ "$android_rc" == 0 ]]; then adb -s "$serial" install -r "$test_apk" || android_rc=$?; fi
      if [[ "$android_rc" == 0 ]]; then
        local log="$REPORTS/diagnostics/android-e2e-instrumentation.log"
        mkdir -p "$(dirname "$log")"
        adb -s "$serial" shell am instrument -w -r \
          -e class com.z23u184.studymate.app.StudyMateApplicationE2ETest \
          -e studymate.baseUrl http://127.0.0.1:8000/ \
          com.z23u184.studymate.app.lab2e2e.test/androidx.test.runner.AndroidJUnitRunner \
          > "$log" 2>&1 || android_rc=$?
        cat "$log"
        python3 "$ROOT/lab2/scripts/instrumentation_report.py" "$log" --reports "$REPORTS" || android_rc=1
      fi
      adb -s "$serial" reverse --remove tcp:8000 >/dev/null 2>&1 || true
    fi
  else
    # Local developer path: Gradle builds + executes + produces Android JUnit XML.
    rm -rf "$ROOT/mobile-app/app/build/outputs/androidTest-results"
    compose run --build --rm --no-deps "${docker_env[@]}" android-tests bash -lc \
      "adb devices | grep -Eq 'device$' && adb reverse tcp:8000 tcp:${host_port} && chmod +x ./gradlew && ./gradlew --no-daemon :app:connectedDebugAndroidTest -Plab2E2e=true -Plab2E2eBaseUrl=http://127.0.0.1:8000/ -Pandroid.testInstrumentationRunnerArguments.class=com.z23u184.studymate.app.StudyMateApplicationE2ETest -Pandroid.testInstrumentationRunnerArguments.studymate.baseUrl=http://127.0.0.1:8000/" || android_rc=$?
    copy_junit "$ROOT/mobile-app/app/build/outputs/androidTest-results" android-e2e
    adb -s "$serial" reverse --remove tcp:8000 >/dev/null 2>&1 || true
  fi

  # Always stop tcpdump *before* reading the pcap, including on test failures.
  compose stop -t 5 capture || rc=1
  if [[ ! -s "$REPORTS/lab2-e2e.pcap" ]]; then
    echo 'ERROR: No real E2E traffic was captured in lab2-e2e.pcap' >&2
    rc=1
  else
    echo "Real Android E2E packet capture: $REPORTS/lab2-e2e.pcap"
  fi
  if [[ "$android_rc" != 0 || "$rc" != 0 ]]; then
    save_server_diagnostics e2e
    mark e2e failed "Android E2E or network capture failed (android=${android_rc}, capture=${rc})"
    return 1
  fi
}

case "$MODE" in
  build)
    mark_not_selected build
    run_build || exit 1
    ;;
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

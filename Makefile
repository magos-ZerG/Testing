# StudyMate Lab 2 - convenient entry points. Run from the project root.
# If Docker requires root permissions on Fedora, use `sudo make <target>`.
SHELL := /bin/bash
.DEFAULT_GOAL := help

LAB2_SCRIPT := lab2/scripts/run-lab2.sh
LAB2_GRADLE_CACHE_VOLUME ?= studymate_lab2_gradle_cache
export LAB2_GRADLE_CACHE_VOLUME

.PHONY: help all all-android unit integration android android-device e2e report report-open report-serve capture cache-info cache-clear

help:
	@printf '%s\n' \
	  'StudyMate Lab 2 targets:' \
	  '  make unit           - Python + Android unit tests, JaCoCo' \
	  '  make integration    - SQL integration tests (2 repeats)' \
	  '  make android        - Android instrumented tests on a phone' \
	  '  make e2e            - headless Android app E2E on connected phone/emulator' \
	  '  make all            - unit -> integration -> Android app E2E (device required)' \
	  '  make all-android    - full cycle including tests on a phone' \
	  '  make report         - regenerate Allure report (Docker/sudo allowed)' \
	  '  make report-open    - serve report over HTTP and open browser (no sudo)' \
	  '  make report-serve   - serve report at http://127.0.0.1:8765 (no sudo)' \
	  '  make capture        - replay HTTP and capture PCAP' \
	  '  make cache-info     - inspect persistent Gradle cache volume' \
	  '  make cache-clear    - delete Gradle cache (next run re-downloads!)' \
	  '' \
	  'Use sudo make <target> if your user cannot access the Docker daemon.'

all:
	@bash "$(LAB2_SCRIPT)" all

all-android:
	@RUN_ANDROID_DEVICE_TESTS=1 bash "$(LAB2_SCRIPT)" all

unit:
	@bash "$(LAB2_SCRIPT)" unit

integration:
	@bash "$(LAB2_SCRIPT)" integration

android android-device:
	@bash "$(LAB2_SCRIPT)" android-device

e2e:
	@bash "$(LAB2_SCRIPT)" e2e

report:
	@bash "$(LAB2_SCRIPT)" report
	@printf '\nTo view report in browser: make report-open (WITHOUT sudo)\n'

report-open:
	@bash lab2/scripts/serve-report.sh --open

report-serve:
	@bash lab2/scripts/serve-report.sh

capture:
	@bash lab2/scripts/capture-traffic.sh

cache-info:
	@docker volume inspect "$(LAB2_GRADLE_CACHE_VOLUME)" \
	  --format 'Volume: {{.Name}} | Mountpoint: {{.Mountpoint}}' || \
	  { echo 'Gradle cache does not exist yet (created on first Android/Unit run).'; exit 1; }

cache-clear:
	@echo 'Removing shared Gradle downloads/cache: $(LAB2_GRADLE_CACHE_VOLUME)'
	@docker volume rm "$(LAB2_GRADLE_CACHE_VOLUME)"

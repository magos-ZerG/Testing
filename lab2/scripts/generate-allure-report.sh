#!/usr/bin/env bash
set -euo pipefail
mkdir -p /reports/allure-results /reports/history

# Android Gradle instrumentation JUnit XML is merged with native Allure results.
for file in /reports/junit/unit-TEST*.xml /reports/junit/android-device-*.xml /reports/junit/android-e2e-*.xml; do
  if [[ -f "$file" ]]; then
    cp "$file" /reports/allure-results/
  fi
done

# The GitHub Pages publisher has already restored the authoritative history
# directly into /reports/allure-results/history. Never overwrite it.
if [[ ! -d /reports/allure-results/history && -d /reports/history/history ]]; then
  cp -a /reports/history/history /reports/allure-results/history
fi

if [[ -f /reports/allure-results/history/history-trend.json ]]; then
  echo 'Generating Allure using previous GitHub Pages history'
else
  echo 'Generating first Allure report (no previous history)'
fi

allure generate /reports/allure-results --clean -o /reports/allure-report

# Retain local report history for the next `make report`/test cycle too.
if [[ -d /reports/allure-report/history ]]; then
  rm -rf /reports/history/history
  cp -a /reports/allure-report/history /reports/history/history
fi

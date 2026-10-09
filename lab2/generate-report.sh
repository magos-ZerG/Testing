#!/usr/bin/env bash

set -e

mkdir -p /reports/allure-results /reports/history

for file in \
    /reports/junit/unit-TEST*.xml \
    /reports/junit/android-device-*.xml
do
    if [ -f "$file" ]; then
        cp "$file" /reports/allure-results/
    fi
done

if [ -d /reports/history/history ]; then
    cp -a /reports/history/history /reports/allure-results/
fi

allure generate \
    /reports/allure-results \
    --clean \
    -o /reports/allure-report

if [ -d /reports/allure-report/history ]; then
    cp -a /reports/allure-report/history /reports/history/
fi

#!/usr/bin/env bash
# Capture actual unencrypted local test HTTP traffic, including curl replay.
set -Eeuo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
cd "$ROOT"
export COMPOSE_PROJECT_NAME="studymate_capture_$(date +%s)_$$"
COMPOSE="$ROOT/lab2/docker-compose.test.yml"
compose() { docker compose -f "$COMPOSE" "$@"; }
cleanup() { compose down -v --remove-orphans >/dev/null 2>&1 || :; }
trap cleanup EXIT
# The Compose file declares a persistent external Gradle cache volume.
# Ensure it exists even when starting only the HTTP capture services.
docker volume create "${LAB2_GRADLE_CACHE_VOLUME:-studymate_lab2_gradle_cache}" >/dev/null
mkdir -p lab2/reports
compose up --build -d --wait postgres api
compose up --build -d capture
# PCAP capture starts in its own service namespace, watching API port 8000.
sleep 2
compose run --build --rm tests /bin/bash /workspace/lab2/scripts/replay-http.sh
sleep 2
compose stop capture
printf 'Traffic saved to %s\n' "$ROOT/lab2/reports/lab2-e2e.pcap"

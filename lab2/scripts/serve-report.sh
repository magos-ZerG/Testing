#!/usr/bin/env bash
# Open the generated Allure website via HTTP. Allure's data requests are not
# reliably accessible when opening index.html through the file:// protocol.
set -Eeuo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
REPORT_DIR="$ROOT/lab2/reports/allure-report"
PORT="${LAB2_REPORT_PORT:-8765}"
OPEN_BROWSER=0
if [[ "${1:-}" == "--open" ]]; then OPEN_BROWSER=1; elif [[ $# -gt 0 ]]; then
  echo 'Usage: serve-report.sh [--open]' >&2
  exit 2
fi

if [[ "$EUID" == 0 ]]; then
  echo 'Run "make report-open" / "make report-serve" as your normal desktop user (without sudo).' >&2
  exit 1
fi
if [[ ! -f "$REPORT_DIR/index.html" ]]; then
  echo 'No Allure HTML found. First run "sudo make report" or a test stage.' >&2
  exit 1
fi
if [[ ! "$PORT" =~ ^[0-9]+$ ]] || (( PORT < 1 || PORT > 65535 )); then
  echo "Invalid LAB2_REPORT_PORT: $PORT" >&2
  exit 2
fi

URL="http://127.0.0.1:${PORT}/"
echo "Allure report: $URL"
echo 'Press Ctrl+C to stop the local report server.'
python3 -m http.server "$PORT" --bind 127.0.0.1 --directory "$REPORT_DIR" &
server_pid=$!
trap 'kill "$server_pid" >/dev/null 2>&1 || true' EXIT
sleep 0.4
if ! kill -0 "$server_pid" 2>/dev/null; then
  echo "Could not start report server; maybe port $PORT is already in use." >&2
  wait "$server_pid" || true
  exit 1
fi
if [[ "$OPEN_BROWSER" == 1 ]]; then
  if command -v xdg-open >/dev/null; then
    xdg-open "$URL" >/dev/null 2>&1 || echo "Open $URL manually in your browser."
  else
    echo "xdg-open not found. Open $URL manually in your browser."
  fi
fi
wait "$server_pid"

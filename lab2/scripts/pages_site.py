#!/usr/bin/env python3
"""Preserve Allure 2 history and a browsable archive on GitHub Pages.

Usage:
  pages_site.py prepare --site .pages-site --reports lab2/reports
  (run `bash lab2/scripts/run-lab2.sh report`)
  pages_site.py finish  --site .pages-site --reports lab2/reports

This intentionally uses only the Python standard library.
"""
from __future__ import annotations

import argparse
import html
import json
import os
import shutil
import sys
import uuid
from pathlib import Path
from urllib.parse import quote

MAX_ARCHIVED_RUNS = 25


def base_url(repository: str) -> str:
    custom = os.environ.get("LAB2_PAGES_BASE_URL", "").strip()
    if custom:
        return custom.rstrip("/") + "/"
    if "/" not in repository:
        raise ValueError("GITHUB_REPOSITORY must be 'owner/repository'")
    owner, name = repository.split("/", 1)
    # GitHub Pages user/organization sites have no additional repository path.
    suffix = "" if name.lower() == f"{owner.lower()}.github.io" else f"{quote(name)}/"
    return f"https://{owner.lower()}.github.io/{suffix}"


def run_key() -> str:
    run_id = os.environ["GITHUB_RUN_ID"]
    attempt = os.environ.get("GITHUB_RUN_ATTEMPT", "1")
    if not (run_id.isdecimal() and attempt.isdecimal()):
        raise ValueError("GitHub run ID and attempt must be decimal integers")
    return f"{run_id}-{attempt}"


def save_json(path: Path, data: dict) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(data, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")


def load_trend(history: Path) -> list[dict]:
    """Read Allure 2 trend data, failing visibly on malformed saved history."""
    trend_file = history / "history-trend.json"
    if not trend_file.exists():
        return []
    raw = json.loads(trend_file.read_text(encoding="utf-8"))
    if not isinstance(raw, list):
        raise ValueError(f"Invalid Allure 2 history trend (expected an array): {trend_file}")
    return raw


def previous_history(site: Path) -> Path | None:
    """Use latest published Pages history, with archived-run recovery."""
    primary = site / "history"
    if (primary / "history-trend.json").is_file():
        return primary

    # Recover when an older workflow overwrote the site root but left archives.
    archive_root = site / "runs"
    if archive_root.is_dir():
        runs = []
        for folder in archive_root.iterdir():
            parts = folder.name.split("-", 1)
            if folder.is_dir() and len(parts) == 2 and all(x.isdecimal() for x in parts):
                if (folder / "history" / "history-trend.json").is_file():
                    runs.append((int(parts[0]), int(parts[1]), folder / "history"))
        if runs:
            runs.sort(reverse=True)
            return runs[0][2]
    return None


def prepare(site: Path, reports: Path) -> None:
    results = reports / "allure-results"
    results.mkdir(parents=True, exist_ok=True)

    # The published Pages report is the only authoritative source of history.
    # Allure 2 requires PREVIOUS_REPORT/history in CURRENT_RESULTS/history,
    # i.e. no extra intermediate reports/history/history directory.
    input_history = results / "history"
    if input_history.exists():
        shutil.rmtree(input_history)
    # Keep the legacy local history cache from accidentally superseding Pages.
    legacy_history = reports / "history" / "history"
    if legacy_history.exists():
        shutil.rmtree(legacy_history)

    previous = previous_history(site)
    if previous is not None:
        shutil.copytree(previous, input_history)
        trend = load_trend(input_history)
        print(f"Allure 2: restored {len(trend)} historical trend entries from {previous}")
    else:
        print("Allure 2: no previous Pages history; first report will start the trend")

    # An infrastructure failure before pytest starts can leave zero results.
    # Still publish a visible failed result rather than an empty Allure page.
    if not any(results.glob("*-result.json")) and not any(results.glob("*.xml")):
        failed = os.environ.get("LAB2_TEST_CONCLUSION", "failure") != "success"
        identifier = str(uuid.uuid4())
        save_json(results / f"{identifier}-result.json", {
            "uuid": identifier,
            "historyId": "studymate-lab2-infrastructure",
            "name": "CI infrastructure (tests not executed)",
            "fullName": "StudyMate.CI.infrastructure",
            "status": "broken" if failed else "skipped",
            "statusDetails": {"message": "No test results artifact was produced."},
            "stage": "finished",
            "labels": [{"name": "suite", "value": "CI infrastructure"}],
        })

    run = run_key()
    root = base_url(os.environ["GITHUB_REPOSITORY"])
    run_name = os.environ.get("GITHUB_WORKFLOW", "StudyMate Lab 2")
    run_number = os.environ.get("GITHUB_RUN_NUMBER", "0")
    # An Actions rerun retains GITHUB_RUN_ID; include the attempt to ensure
    # Allure's buildOrder remains unique and trends can distinguish reruns.
    build_order = int(os.environ["GITHUB_RUN_ID"]) * 1000 + int(os.environ.get("GITHUB_RUN_ATTEMPT", "1"))
    server = os.environ.get("GITHUB_SERVER_URL", "https://github.com").rstrip("/")
    save_json(results / "executor.json", {
        "name": "GitHub Actions",
        "type": "github",
        "buildOrder": build_order,
        "buildName": f"{run_name} #{run_number} (attempt {os.environ.get('GITHUB_RUN_ATTEMPT', '1')})",
        "buildUrl": f"{server}/{os.environ['GITHUB_REPOSITORY']}/actions/runs/{os.environ['GITHUB_RUN_ID']}",
        "reportName": f"StudyMate Lab 2 · Run {run}",
        "reportUrl": f"{root}runs/{run}/",
    })
    print(f"Allure report URL: {root}")


def archive_index(site: Path, root: str) -> None:
    runs_dir = site / "runs"
    runs = []
    for folder in runs_dir.iterdir():
        if folder.is_dir() and (folder / "index.html").exists():
            segments = folder.name.split("-", 1)
            if len(segments) == 2 and all(s.isdecimal() for s in segments):
                runs.append((int(segments[0]), int(segments[1]), folder))
    runs.sort(reverse=True)
    # Bound the published archive size, independently from Allure 2's 20-entry trends.
    for _, _, folder in runs[MAX_ARCHIVED_RUNS:]:
        shutil.rmtree(folder)
    runs = runs[:MAX_ARCHIVED_RUNS]
    items = "\n".join(
        f'<li><a href="{quote(folder.name)}/">Run {html.escape(folder.name)}</a></li>'
        for _, _, folder in runs
    )
    html_document = f'''<!doctype html>
<html lang="ru"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width, initial-scale=1">
<title>История Allure · StudyMate</title>
<style>body{{font:16px system-ui,sans-serif;max-width:760px;margin:3rem auto;padding:0 1.5rem;line-height:1.6}}a{{color:#2165ae}}li{{margin:.6rem 0}}</style>
</head><body><h1>История Allure · StudyMate</h1>
<p><a href="{html.escape(root, quote=True)}">Последний отчёт и графики трендов</a></p>
<p>Здесь хранятся последние {MAX_ARCHIVED_RUNS} опубликованных отчётов GitHub Actions.</p>
<ol>{items}</ol></body></html>'''
    (runs_dir / "index.html").write_text(html_document, encoding="utf-8")


def finish(site: Path, reports: Path) -> None:
    generated = reports / "allure-report"
    if not (generated / "index.html").is_file():
        raise SystemExit("Allure was not generated: lab2/reports/allure-report/index.html missing")

    # Refuse to replace an existing Pages history with an empty/broken report.
    # This is important because the gh-pages action publishes an entire tree.
    output_history = generated / "history"
    required = ("history.json", "history-trend.json", "duration-trend.json")
    missing = [filename for filename in required if not (output_history / filename).is_file()]
    if missing:
        raise SystemExit(f"Allure 2 report has no usable history: missing {missing}")

    old_history = previous_history(site)
    old_trend = load_trend(old_history) if old_history else []
    new_trend = load_trend(output_history)
    new_orders = {entry.get("buildOrder") for entry in new_trend if isinstance(entry, dict)}
    if not new_trend:
        raise SystemExit("Allure 2 generated an empty history-trend.json; refusing to overwrite Pages")
    if old_trend:
        # Allure 2 keeps at most the latest 20 points. Even at capacity,
        # at least one of the previous build orders must survive regeneration.
        old_orders = {entry.get("buildOrder") for entry in old_trend if isinstance(entry, dict)}
        if old_orders.isdisjoint(new_orders):
            raise SystemExit("Allure 2 LOST the previous trend! Refusing to publish without history")
        if len(new_trend) < min(20, len(old_trend) + 1):
            raise SystemExit("Allure 2 history is shorter than expected; refusing to replace published history")

    site.mkdir(parents=True, exist_ok=True)
    run = run_key()
    runs_dir = site / "runs"
    runs_dir.mkdir(exist_ok=True)
    archive = runs_dir / run
    if archive.exists():
        shutil.rmtree(archive)
    shutil.copytree(generated, archive)

    # Replace the 'latest' report in the site root without losing archives.
    for path in site.iterdir():
        if path.name in {"runs", ".nojekyll", "CNAME"}:
            continue
        if path.is_dir() and not path.is_symlink():
            shutil.rmtree(path)
        else:
            path.unlink()
    shutil.copytree(generated, site, dirs_exist_ok=True)
    (site / ".nojekyll").touch()
    archive_index(site, base_url(os.environ["GITHUB_REPOSITORY"]))
    print(f"Allure 2: kept {len(new_trend)} trend entries; {len(old_trend)} from prior report")
    print(f"History saved to {site / 'history'} and {archive / 'history'}")
    print(f"Prepared site: {site} (latest + {run} + up to {MAX_ARCHIVED_RUNS} past runs)")


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("command", choices=("prepare", "finish"))
    parser.add_argument("--site", type=Path, required=True)
    parser.add_argument("--reports", type=Path, required=True)
    args = parser.parse_args()
    if args.command == "prepare":
        prepare(args.site, args.reports)
    else:
        finish(args.site, args.reports)
    return 0


if __name__ == "__main__":
    sys.exit(main())

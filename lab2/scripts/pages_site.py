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


def prepare(site: Path, reports: Path) -> None:
    results = reports / "allure-results"
    results.mkdir(parents=True, exist_ok=True)

    # The test runner may have regenerated a local report or restored cached
    # history. ONLY the last published Pages report is the authoritative source.
    prior_results_history = results / "history"
    prior_reports_history = reports / "history"
    if prior_results_history.exists():
        shutil.rmtree(prior_results_history)
    if prior_reports_history.exists():
        shutil.rmtree(prior_reports_history)

    previous = site / "history"
    if previous.is_dir():
        shutil.copytree(previous, prior_reports_history / "history")
        print("Restored Allure history from the previous GitHub Pages report")
    else:
        print("No published history found: starting Allure trends from this run")

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
    build_order = int(os.environ["GITHUB_RUN_ID"])
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

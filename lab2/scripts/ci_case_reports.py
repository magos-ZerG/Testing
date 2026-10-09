#!/usr/bin/env python3
"""Keep per-test artifacts reliable and mark unscheduled tests as skipped in Allure."""
from __future__ import annotations

import argparse
import hashlib
import json
import os
import time
import uuid
import xml.etree.ElementTree as ET
from pathlib import Path

from ci_catalog import catalog

REPORTS = Path("lab2/reports")


def save_case_result(case: dict, stage: str, status: str, message: str) -> None:
    path = REPORTS / "allure-results"
    path.mkdir(parents=True, exist_ok=True)
    identifier = str(uuid.uuid4())
    now = int(time.time() * 1000)
    item = {
        "uuid": identifier,
        "historyId": hashlib.md5(f"lab2:{stage}:{case['selector']}".encode()).hexdigest(),
        "name": case["title"],
        "fullName": case["selector"],
        "status": status,
        "statusDetails": {"message": message},
        "stage": "finished", "start": now, "stop": now,
        "labels": [
            {"name": "suite", "value": f"StudyMate Lab 2 · {stage}"},
            {"name": "feature", "value": stage},
            {"name": "tag", "value": "GitHub Actions"},
        ],
    }
    (path / f"{identifier}-result.json").write_text(json.dumps(item, ensure_ascii=False), encoding="utf-8")


def junit_placeholder(case: dict, stage: str, status: str, reason: str) -> None:
    junit = REPORTS / "junit"
    junit.mkdir(parents=True, exist_ok=True)
    suite = ET.Element("testsuite", name=f"StudyMate.{stage}", tests="1", failures="0", errors="1" if status == "broken" else "0", skipped="1" if status == "skipped" else "0")
    testcase = ET.SubElement(suite, "testcase", classname=stage, name=case["selector"])
    ET.SubElement(testcase, "skipped" if status == "skipped" else "error", message=reason).text = reason
    ET.ElementTree(suite).write(junit / f"ci-placeholder-{case['id']}.xml", encoding="utf-8", xml_declaration=True)


def record(kind: str, identifier: str, selector: str, outcome: str) -> None:
    (REPORTS / "ci-states").mkdir(parents=True, exist_ok=True)
    case = {"id": identifier, "selector": selector, "title": selector.split("::")[-1].split(".")[-1]}
    path = REPORTS / "ci-states" / f"{identifier}.json"
    path.write_text(json.dumps({"id": identifier, "kind": kind, "selector": selector, "outcome": outcome}, ensure_ascii=False), encoding="utf-8")
    # When compilation/DB boot fails, pytest/Gradle cannot produce a report.
    has_junit = any((REPORTS / "junit").glob(f"*{identifier}*.xml"))
    if not has_junit:
        reason = f"{kind}: command {outcome}; no test XML was produced (infrastructure/build error)"
        junit_placeholder(case, kind, "broken", reason)
        save_case_result(case, kind, "broken", reason)
        print(reason)
    elif outcome != "success":
        # Python collection errors may create an empty XML but no Allure result.
        if kind in {"python-unit", "integration", "e2e"} and not any((REPORTS / "allure-results").glob("*-result.json")):
            save_case_result(case, kind, "broken", f"Test command failed before pytest could report {selector}")
        print(f"{kind}: test failed; preserving original JUnit/Allure details: {selector}")


def aggregate(include_device: bool, stage_status: dict[str, str]) -> None:
    cases = catalog()
    if not include_device:
        cases.pop("device")
    existing = REPORTS / "ci-states"
    skipped = 0
    broken = 0
    for kind, values in cases.items():
        for case in values:
            if (existing / f"{case['id']}.json").exists():
                continue
            stage = stage_status.get(kind, "skipped")
            state = "broken" if stage in {"failure", "cancelled"} else "skipped"
            reason = (f"Test could not start: GitHub job group {kind} = {stage}"
                      if state == "broken" else f"Skipped because a required preceding stage did not pass ({kind})")
            junit_placeholder(case, kind, state, reason)
            save_case_result(case, kind, state, reason)
            if state == "broken":
                broken += 1
            else:
                skipped += 1
    print(f"Restored missing test statuses for Allure: {skipped} skipped, {broken} broken")
    # Ensure diagnostics exist even if all uploads disappeared.
    if not any((REPORTS / "allure-results").glob("*-result.json")):
        case = {"id": "infrastructure", "title": "No test results uploaded", "selector": "lab2.github.artifacts"}
        save_case_result(case, "infrastructure", "broken", "GitHub Actions produced no test-result artifacts")


def main() -> None:
    parser = argparse.ArgumentParser()
    subs = parser.add_subparsers(dest="mode", required=True)
    r = subs.add_parser("record")
    r.add_argument("--kind", required=True)
    r.add_argument("--id", required=True)
    r.add_argument("--selector", required=True)
    r.add_argument("--outcome", required=True)
    p = subs.add_parser("aggregate")
    p.add_argument("--device", action="store_true")
    args = parser.parse_args()
    if args.mode == "record":
        record(args.kind, args.id, args.selector, args.outcome)
    else:
        status = {kind: os.environ.get(f"CI_STATUS_{kind.upper()}", "skipped") for kind in catalog()}
        aggregate(args.device, status)


if __name__ == "__main__":
    main()

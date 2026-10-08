#!/usr/bin/env python3
"""Emit JUnit + Allure placeholders for failed/skipped stages (CI artifact, always)."""
import argparse
import json
from datetime import datetime, timezone
from pathlib import Path
from uuid import uuid4
from xml.etree.ElementTree import Element, SubElement, ElementTree

parser = argparse.ArgumentParser()
parser.add_argument("stage", choices=["unit", "integration", "e2e", "android-device"])
parser.add_argument("status", choices=["failed", "skipped"])
parser.add_argument("reason")
parser.add_argument("--reports", default="lab2/reports")
args = parser.parse_args()
root = Path(args.reports)
(root / "junit").mkdir(parents=True, exist_ok=True)
(root / "allure-results").mkdir(parents=True, exist_ok=True)

suite = Element("testsuite", {
    "name": f"lab2.{args.stage}.stage-control",
    "tests": "1",
    "failures": "1" if args.status == "failed" else "0",
    "errors": "0", "skipped": "1" if args.status == "skipped" else "0",
})
case = SubElement(suite, "testcase", {"classname": "lab2.pipeline", "name": f"{args.stage}_{args.status}"})
SubElement(case, "failure" if args.status == "failed" else "skipped", {"message": args.reason}).text = args.reason
ElementTree(suite).write(root / "junit" / f"stage-{args.stage}-{args.status}.xml", encoding="utf-8", xml_declaration=True)

now = int(datetime.now(timezone.utc).timestamp() * 1000)
identifier = str(uuid4())
result = {
    "uuid": identifier,
    "historyId": f"lab2-{args.stage}-stage-control",
    "name": f"{args.stage}: {args.status}",
    "fullName": f"lab2.pipeline.{args.stage}",
    "status": "failed" if args.status == "failed" else "skipped",
    "statusDetails": {"message": args.reason},
    "stage": "finished",
    "start": now, "stop": now,
    "labels": [{"name": "suite", "value": "StudyMate Lab 2"}, {"name": "feature", "value": args.stage}],
}
(root / "allure-results" / f"{identifier}-result.json").write_text(json.dumps(result, ensure_ascii=False), encoding="utf-8")
print(f"{args.stage}: {args.status}: {args.reason}")

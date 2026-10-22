#!/usr/bin/env python3
"""Convert `adb shell am instrument -w -r` output into JUnit and Allure.

Unlike adb's process status, individual INSTRUMENTATION_STATUS_CODE values reflect
AndroidJUnitRunner test failures. Refuse to report success when no test finished.
"""
from __future__ import annotations

import argparse
import json
import re
import time
from pathlib import Path
from uuid import uuid4
from xml.etree.ElementTree import Element, SubElement, ElementTree

TEST_CLASS = "com.z23u184.studymate.app.StudyMateApplicationE2ETest"
TEST_NAME = "externalCommandsSyncAndRestoreThroughRealApp"


def parse(raw: str) -> tuple[bool, str]:
    statuses = [int(value) for value in re.findall(r"^INSTRUMENTATION_STATUS_CODE:\s*(-?\d+)\s*$", raw, re.M)]
    # 1 = started, 0 = passed, -2 = assertion failure, -1 = error.
    passed = statuses.count(0) == 1 and all(status not in (-1, -2) for status in statuses)
    completed = any(status in (0, -1, -2) for status in statuses)
    instrument_finished = bool(re.search(r"^INSTRUMENTATION_CODE:\s*-1\s*$", raw, re.M))
    ok = passed and completed and instrument_finished
    if ok:
        return True, ""
    errors = re.findall(r"^INSTRUMENTATION_STATUS: stack=(.*)$", raw, re.M)
    if errors:
        return False, errors[-1][:4000]
    return False, (
        "Instrumentation failed or returned no successful test result; "
        f"status codes={statuses}, completed={instrument_finished}. See raw instrumentation log."
    )


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("log", type=Path)
    parser.add_argument("--reports", type=Path, default=Path("lab2/reports"))
    args = parser.parse_args()

    raw = args.log.read_text(encoding="utf-8", errors="replace")
    passed, message = parse(raw)
    junit = args.reports / "junit"
    allure = args.reports / "allure-results"
    junit.mkdir(parents=True, exist_ok=True)
    allure.mkdir(parents=True, exist_ok=True)

    suite = Element("testsuite", name=TEST_CLASS, tests="1", failures="0" if passed else "1", errors="0")
    case = SubElement(suite, "testcase", classname=TEST_CLASS, name=TEST_NAME)
    if not passed:
        SubElement(case, "failure", message=message).text = raw[-12000:]
    SubElement(case, "system-out").text = raw[-16000:]
    ElementTree(suite).write(junit / "android-e2e-instrumentation.xml", encoding="utf-8", xml_declaration=True)

    now = int(time.time() * 1000)
    payload = {
        "uuid": str(uuid4()),
        "historyId": "studymate-app-blackbox-e2e",
        "name": TEST_NAME,
        "fullName": f"{TEST_CLASS}.{TEST_NAME}",
        "status": "passed" if passed else "failed",
        "stage": "finished",
        "start": now,
        "stop": now,
        "labels": [{"name": "suite", "value": "StudyMate Lab 2"}, {"name": "feature", "value": "Android E2E"}],
    }
    if not passed:
        payload["statusDetails"] = {"message": message, "trace": raw[-12000:]}
    (allure / f"{payload['uuid']}-result.json").write_text(json.dumps(payload), encoding="utf-8")

    print(f"Android instrumentation: {'PASS' if passed else 'FAIL'}")
    if not passed:
        print(message)
    return 0 if passed else 1


if __name__ == "__main__":
    raise SystemExit(main())

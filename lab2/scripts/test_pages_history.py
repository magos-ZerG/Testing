"""Regression checks for restoring/publishing Allure 2 history on GitHub Pages.

Run: python3 -m unittest discover -s lab2/scripts -p 'test_pages_history.py' -v
"""
from __future__ import annotations

import json
import os
import tempfile
import unittest
from pathlib import Path
from unittest.mock import patch

from pages_site import finish, prepare


class PagesHistoryTests(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.root = Path(self.temp.name)
        self.site = self.root / "site"
        self.reports = self.root / "reports"
        self.env = patch.dict(os.environ, {
            "GITHUB_REPOSITORY": "magos-ZerG/Testing",
            "GITHUB_RUN_ID": "1000",
            "GITHUB_RUN_ATTEMPT": "1",
            "GITHUB_RUN_NUMBER": "1",
            "GITHUB_WORKFLOW": "lab_02",
            "LAB2_TEST_CONCLUSION": "success",
        }, clear=False)
        self.env.start()
        self.addCleanup(self.env.stop)

    def generated_report(self, previous: list[dict], new_order: int):
        out = self.reports / "allure-report"
        (out / "history").mkdir(parents=True, exist_ok=True)
        (out / "index.html").write_text("Allure", encoding="utf-8")
        (out / "history" / "history.json").write_text("{}", encoding="utf-8")
        (out / "history" / "duration-trend.json").write_text("[]", encoding="utf-8")
        rows = [{"buildOrder": new_order, "data": {"passed": 1}}, *previous]
        (out / "history" / "history-trend.json").write_text(json.dumps(rows), encoding="utf-8")

    def test_two_publishes_keep_history_in_pages_and_archive(self):
        prepare(self.site, self.reports)
        self.assertFalse((self.reports / "allure-results" / "history").exists())
        first_order = json.loads((self.reports / "allure-results" / "executor.json").read_text())["buildOrder"]
        self.generated_report([], first_order)
        finish(self.site, self.reports)
        self.assertTrue((self.site / "runs" / "1000-1" / "history" / "history.json").is_file())

        os.environ["GITHUB_RUN_ID"] = "1001"
        prepare(self.site, self.reports)
        restored = json.loads((self.reports / "allure-results" / "history" / "history-trend.json").read_text())
        self.assertEqual([item["buildOrder"] for item in restored], [first_order])
        second_order = json.loads((self.reports / "allure-results" / "executor.json").read_text())["buildOrder"]
        self.generated_report(restored, second_order)
        finish(self.site, self.reports)
        trend = json.loads((self.site / "history" / "history-trend.json").read_text())
        self.assertEqual([item["buildOrder"] for item in trend], [second_order, first_order])
        self.assertTrue((self.site / "runs" / "1001-1" / "history" / "history-trend.json").is_file())
        self.assertTrue((self.site / "runs" / "index.html").is_file())

    def test_regression_cannot_erase_previous_trend(self):
        prepare(self.site, self.reports)
        first_order = json.loads((self.reports / "allure-results" / "executor.json").read_text())["buildOrder"]
        self.generated_report([], first_order)
        finish(self.site, self.reports)
        saved = (self.site / "history" / "history-trend.json").read_bytes()
        os.environ["GITHUB_RUN_ID"] = "1001"
        prepare(self.site, self.reports)
        self.generated_report([], 1001001)  # Simulate Allure ignoring input history.
        with self.assertRaisesRegex(SystemExit, "LOST the previous trend"):
            finish(self.site, self.reports)
        self.assertEqual((self.site / "history" / "history-trend.json").read_bytes(), saved)

    def test_recovers_previous_history_from_archive_if_root_missing(self):
        prepare(self.site, self.reports)
        self.generated_report([], 1000001)
        finish(self.site, self.reports)
        import shutil
        shutil.rmtree(self.site / "history")
        os.environ["GITHUB_RUN_ID"] = "1001"
        prepare(self.site, self.reports)
        trend_path = self.reports / "allure-results" / "history" / "history-trend.json"
        self.assertTrue(trend_path.is_file())
        self.assertEqual(json.loads(trend_path.read_text())[0]["buildOrder"], 1000001)

    def test_rerun_attempt_gets_unique_build_order(self):
        prepare(self.site, self.reports)
        first_order = json.loads((self.reports / "allure-results" / "executor.json").read_text())["buildOrder"]
        os.environ["GITHUB_RUN_ATTEMPT"] = "2"
        prepare(self.site, self.reports)
        second_order = json.loads((self.reports / "allure-results" / "executor.json").read_text())["buildOrder"]
        self.assertNotEqual(first_order, second_order)


if __name__ == "__main__":
    unittest.main()

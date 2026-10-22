"""Offline regression tests for adb instrumentation -> CI result conversion."""
import importlib.util
import sys
import unittest
from pathlib import Path

spec = importlib.util.spec_from_file_location(
    "instrumentation_report", Path(__file__).with_name("instrumentation_report.py")
)
module = importlib.util.module_from_spec(spec)
sys.modules[spec.name] = module
spec.loader.exec_module(module)


class InstrumentationReportTests(unittest.TestCase):
    def test_pass(self):
        output = """INSTRUMENTATION_STATUS: test=externalCommandsSyncAndRestoreThroughRealApp
INSTRUMENTATION_STATUS_CODE: 1
INSTRUMENTATION_STATUS_CODE: 0
INSTRUMENTATION_RESULT: stream=OK (1 test)
INSTRUMENTATION_CODE: -1
"""
        self.assertEqual(module.parse(output), (True, ""))

    def test_failure(self):
        output = """INSTRUMENTATION_STATUS_CODE: 1
INSTRUMENTATION_STATUS: stack=java.lang.AssertionError: wrong value
INSTRUMENTATION_STATUS_CODE: -2
INSTRUMENTATION_CODE: -1
"""
        passed, reason = module.parse(output)
        self.assertFalse(passed)
        self.assertIn("AssertionError", reason)

    def test_missing_results_is_failure(self):
        passed, _ = module.parse("INSTRUMENTATION_CODE: -1\n")
        self.assertFalse(passed)

    def test_crash_is_failure(self):
        passed, _ = module.parse("INSTRUMENTATION_STATUS_CODE: 1\nINSTRUMENTATION_CODE: 0\n")
        self.assertFalse(passed)


if __name__ == "__main__":
    unittest.main()

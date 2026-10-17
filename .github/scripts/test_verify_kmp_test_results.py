import importlib.util
from pathlib import Path
import tempfile
import unittest


spec = importlib.util.spec_from_file_location(
    "kmp_reports", Path(__file__).with_name("verify-kmp-test-results.py")
)
kmp_reports = importlib.util.module_from_spec(spec)
spec.loader.exec_module(kmp_reports)


class KmpReportGuardTest(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.directory = Path(self.temp.name)

    def report(self, xml, name="TEST-fixture.xml"):
        path = self.directory / name
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_text(xml)

    def test_requires_a_report_from_each_requested_suite(self):
        with self.assertRaisesRegex(ValueError, "no JUnit reports"):
            kmp_reports.verify(self.directory)

    def test_skipped_only_native_suite_cannot_pass(self):
        self.report('<testsuite tests="4" skipped="4"/>')
        with self.assertRaisesRegex(ValueError, "no tests executed"):
            kmp_reports.verify(self.directory)

    def test_failing_suite_cannot_be_hidden_by_passing_suite(self):
        self.report('<testsuite tests="10" failures="0"/>')
        self.report('<testsuite tests="1" failures="1"/>', "TEST-failed.xml")
        with self.assertRaisesRegex(ValueError, "failing tests"):
            kmp_reports.verify(self.directory)

    def test_runtime_errors_fail_the_gate(self):
        self.report('<testsuite tests="1" errors="1"/>')
        with self.assertRaisesRegex(ValueError, "failing tests"):
            kmp_reports.verify(self.directory)

    def test_reads_android_reports_grouped_by_device(self):
        self.report('<testsuite tests="3" failures="0"/>', "emulator-5554/TEST-android.xml")
        self.assertEqual(3, kmp_reports.verify(self.directory))

    def test_nested_device_failure_cannot_be_hidden(self):
        self.report('<testsuite tests="3" failures="0"/>')
        self.report('<testsuite tests="1" errors="1"/>', "emulator-5554/TEST-android.xml")
        with self.assertRaisesRegex(ValueError, "failing tests"):
            kmp_reports.verify(self.directory)

    def test_counts_executed_jvm_and_android_suites(self):
        self.report('<testsuite tests="5" failures="0" errors="0" skipped="1"/>')
        self.report(
            '<testsuites><testsuite tests="3" disabled="1"/></testsuites>',
            "TEST-android.xml",
        )
        self.assertEqual(6, kmp_reports.verify(self.directory))


if __name__ == "__main__":
    unittest.main()

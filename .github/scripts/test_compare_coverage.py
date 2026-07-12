import importlib.util
import sys
import unittest
from decimal import Decimal
from pathlib import Path


SCRIPT_PATH = Path(__file__).with_name("compare-coverage.py")
SPEC = importlib.util.spec_from_file_location("compare_coverage", SCRIPT_PATH)
assert SPEC is not None and SPEC.loader is not None
compare_coverage = importlib.util.module_from_spec(SPEC)
sys.modules[SPEC.name] = compare_coverage
SPEC.loader.exec_module(compare_coverage)


def metric(percent: str) -> compare_coverage.CoverageMetric:
    return compare_coverage.CoverageMetric(Decimal(percent))


def complete(module: str, line: str, branch: str) -> compare_coverage.ModuleCoverage:
    return compare_coverage.ModuleCoverage(module, metric(line), metric(branch))


class CoverageReportTest(unittest.TestCase):
    def test_module_statuses_preserve_gate_semantics(self) -> None:
        baseline = complete("core", "80", "70")

        passing, passing_failed = compare_coverage.module_row(
            "core", baseline, complete("core", "81", "70")
        )
        down, down_failed = compare_coverage.module_row(
            "core", baseline, complete("core", "79", "71")
        )
        new, new_failed = compare_coverage.module_row(
            "new-module",
            compare_coverage.ModuleCoverage(
                "new-module",
                None,
                None,
                "Baseline HTML not found: baseline/new-module/index.html",
            ),
            complete("new-module", "75", "60"),
        )
        missing, missing_failed = compare_coverage.module_row(
            "missing-module",
            baseline,
            compare_coverage.ModuleCoverage(
                "missing-module",
                None,
                None,
                "Current XML report not found: current.xml",
            ),
        )

        self.assertEqual("PASS", passing["status"])
        self.assertFalse(passing_failed)
        self.assertEqual("DOWN", down["status"])
        self.assertTrue(down_failed)
        self.assertEqual("NEW", new["status"])
        self.assertTrue(new_failed)
        self.assertEqual("MISSING", missing["status"])
        self.assertTrue(missing_failed)

    def test_markdown_uses_color_signals_and_delta_arrows(self) -> None:
        baseline = complete("module", "80", "70")
        rows = [
            compare_coverage.module_row(
                "passing", baseline, complete("passing", "81", "70")
            )[0],
            compare_coverage.module_row(
                "down", baseline, complete("down", "79", "71")
            )[0],
            compare_coverage.module_row(
                "new",
                compare_coverage.ModuleCoverage(
                    "new", None, None, "Baseline HTML not found: baseline/new/index.html"
                ),
                complete("new", "75", "60"),
            )[0],
            compare_coverage.module_row(
                "missing",
                baseline,
                compare_coverage.ModuleCoverage(
                    "missing", None, None, "Current XML report not found: current.xml"
                ),
            )[0],
        ]

        report = compare_coverage.markdown_report(rows, failed=True)

        self.assertIn(compare_coverage.MARKER, report)
        self.assertIn("## :bar_chart: Test Coverage Report", report)
        self.assertIn(":x: **Coverage gate failed.**", report)
        self.assertIn(":green_circle: **PASS**", report)
        self.assertIn(":red_circle: **DOWN**", report)
        self.assertIn(":large_blue_circle: **NEW**", report)
        self.assertIn(":yellow_circle: **MISSING**", report)
        self.assertIn(
            "**Modules:** :green_circle: 1 PASS · :red_circle: 1 DOWN · "
            ":large_blue_circle: 1 NEW · :yellow_circle: 1 MISSING",
            report,
        )
        self.assertIn(
            "**Trend:** :green_circle: ↑ increase · :red_circle: ↓ decrease · "
            ":white_circle: → unchanged · :warning: ↔ unavailable",
            report,
        )
        self.assertIn(":green_circle: ↑ +1.00pp", report)
        self.assertIn(":red_circle: ↓ -1.00pp", report)
        self.assertIn(":white_circle: → +0.00pp", report)
        self.assertIn(":warning: ↔ N/A", report)
        self.assertIn(
            "| Module | Line (base → head) | Δ | Branch (base → head) | Δ | Status |",
            report,
        )
        self.assertIn("| `passing` | 80.00% → 81.00% |", report)

    def test_markdown_highlights_a_passing_gate(self) -> None:
        baseline = complete("core", "80", "70")
        row, failed = compare_coverage.module_row(
            "core", baseline, complete("core", "80", "70")
        )

        report = compare_coverage.markdown_report([row], failed)

        self.assertFalse(failed)
        self.assertIn(":white_check_mark: **Coverage gate passed.**", report)

    def test_tiny_regression_keeps_down_arrow_after_rounding(self) -> None:
        self.assertEqual(
            ":red_circle: ↓ -0.00pp",
            compare_coverage.delta_label(Decimal("-0.001")),
        )


if __name__ == "__main__":
    unittest.main()

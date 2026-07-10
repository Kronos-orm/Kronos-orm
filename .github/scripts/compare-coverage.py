#!/usr/bin/env python3
import argparse
import json
import re
import sys
from dataclasses import dataclass
from decimal import Decimal, ROUND_HALF_UP
from html.parser import HTMLParser
from pathlib import Path
from typing import Any
from xml.etree import ElementTree


DEFAULT_BASELINE_DIR = ".coverage-baseline"
MARKER = "<!-- kronos-coverage-comparison -->"


class CoverageHtmlParser(HTMLParser):
    def __init__(self) -> None:
        super().__init__()
        self.tables: list[list[list[str]]] = []
        self._in_table = False
        self._in_row = False
        self._in_cell = False
        self._current_table: list[list[str]] = []
        self._current_row: list[str] = []
        self._current_cell: list[str] = []

    def handle_starttag(self, tag: str, attrs: list[tuple[str, str | None]]) -> None:
        if tag == "table":
            attr_map = dict(attrs)
            classes = (attr_map.get("class") or "").split()
            if "coverageStats" in classes:
                self._in_table = True
                self._current_table = []
        elif self._in_table and tag == "tr":
            self._in_row = True
            self._current_row = []
        elif self._in_row and tag in ("td", "th"):
            self._in_cell = True
            self._current_cell = []

    def handle_data(self, data: str) -> None:
        if self._in_cell:
            self._current_cell.append(data)

    def handle_endtag(self, tag: str) -> None:
        if self._in_cell and tag in ("td", "th"):
            value = " ".join("".join(self._current_cell).split())
            self._current_row.append(value)
            self._current_cell = []
            self._in_cell = False
        elif self._in_row and tag == "tr":
            if self._current_row:
                self._current_table.append(self._current_row)
            self._current_row = []
            self._in_row = False
        elif self._in_table and tag == "table":
            self.tables.append(self._current_table)
            self._current_table = []
            self._in_table = False


@dataclass(frozen=True)
class CoverageMetric:
    percent: Decimal | None
    covered: int | None = None
    missed: int | None = None

    @property
    def total(self) -> int | None:
        if self.covered is None or self.missed is None:
            return None
        return self.covered + self.missed


@dataclass(frozen=True)
class ModuleCoverage:
    module: str
    line: CoverageMetric | None
    branch: CoverageMetric | None
    error: str | None = None


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description="Compare current Kover XML reports with published coverage pages.",
    )
    parser.add_argument(
        "--module",
        action="append",
        required=True,
        metavar="NAME=XML_PATH",
        help="Module name and current Kover XML report path. May be repeated.",
    )
    parser.add_argument(
        "--baseline-dir",
        default=DEFAULT_BASELINE_DIR,
        help=(
            "Directory containing the published coverage branch files. "
            f"Defaults to {DEFAULT_BASELINE_DIR}."
        ),
    )
    parser.add_argument(
        "--output-json",
        required=True,
        help="Path for the machine-readable comparison report.",
    )
    parser.add_argument(
        "--output-md",
        required=True,
        help="Path for the PR/comment markdown comparison report.",
    )
    parser.add_argument(
        "--summary",
        default=None,
        help="Optional GitHub step summary file path.",
    )
    return parser.parse_args()


def parse_module_specs(specs: list[str]) -> list[tuple[str, Path]]:
    modules: list[tuple[str, Path]] = []
    for spec in specs:
        if "=" not in spec:
            raise ValueError(f"Invalid --module value '{spec}', expected NAME=XML_PATH")
        name, xml_path = spec.split("=", 1)
        name = name.strip()
        if not name:
            raise ValueError(f"Invalid --module value '{spec}', module name is empty")
        modules.append((name, Path(xml_path)))
    return modules


def percentage(covered: int, missed: int) -> Decimal:
    total = covered + missed
    if total == 0:
        return Decimal("100")
    return (Decimal(covered) * Decimal("100")) / Decimal(total)


def parse_current_xml(module: str, xml_path: Path) -> ModuleCoverage:
    if not xml_path.is_file():
        return ModuleCoverage(module, None, None, f"Current XML report not found: {xml_path}")

    try:
        root = ElementTree.parse(xml_path).getroot()
    except ElementTree.ParseError as exc:
        return ModuleCoverage(module, None, None, f"Cannot parse current XML report: {exc}")

    metrics: dict[str, CoverageMetric] = {}
    for counter in root.findall("counter"):
        kind = (counter.get("type") or "").upper()
        if kind not in {"LINE", "BRANCH"}:
            continue
        missed = int(counter.get("missed") or "0")
        covered = int(counter.get("covered") or "0")
        metrics[kind] = CoverageMetric(
            percent=percentage(covered, missed),
            covered=covered,
            missed=missed,
        )

    missing = [kind for kind in ("LINE", "BRANCH") if kind not in metrics]
    if missing:
        return ModuleCoverage(
            module,
            metrics.get("LINE"),
            metrics.get("BRANCH"),
            f"Current XML report has no report-level {', '.join(missing)} counter",
        )

    return ModuleCoverage(module, metrics["LINE"], metrics["BRANCH"])


def parse_percent(value: str) -> Decimal | None:
    normalized = value.strip()
    if not normalized or normalized.upper() == "N/A":
        return None
    match = re.search(r"([0-9]+(?:\.[0-9]+)?)\s*%", normalized)
    if match:
        return Decimal(match.group(1))
    if normalized.endswith("%"):
        normalized = normalized[:-1]
    return Decimal(normalized)


def parse_html_metric(value: str) -> CoverageMetric:
    abs_match = re.search(r"\((\d+)\s*/\s*(\d+)\)", value)
    if abs_match:
        covered = int(abs_match.group(1))
        total = int(abs_match.group(2))
        missed = total - covered
        return CoverageMetric(percent=percentage(covered, missed), covered=covered, missed=missed)
    return CoverageMetric(percent=parse_percent(value))


def parse_baseline_html(module: str, html: str) -> ModuleCoverage:
    parser = CoverageHtmlParser()
    parser.feed(html)

    for table in parser.tables:
        if len(table) < 2:
            continue
        headers = table[0]
        try:
            name_index = headers.index("Package")
            line_index = headers.index("Line, %")
            branch_index = headers.index("Branch, %")
        except ValueError:
            continue

        for row in table[1:]:
            if len(row) <= max(name_index, line_index, branch_index):
                continue
            if row[name_index] == "all classes":
                return ModuleCoverage(
                    module,
                    parse_html_metric(row[line_index]),
                    parse_html_metric(row[branch_index]),
                )

    return ModuleCoverage(
        module,
        None,
        None,
        "Baseline HTML does not contain an all classes row with Line and Branch coverage",
    )


def read_baseline(module: str, baseline_dir: Path) -> ModuleCoverage:
    fetch_error_path = baseline_dir / ".baseline-error"
    fetch_error = ""
    if fetch_error_path.is_file():
        fetch_error = fetch_error_path.read_text(encoding="utf-8", errors="replace").strip()

    html_path = baseline_dir / module / "index.html"
    if not html_path.is_file():
        detail = f"Baseline HTML not found: {html_path}"
        if fetch_error:
            detail = f"{detail}. Baseline checkout error: {fetch_error}"
        return ModuleCoverage(module, None, None, detail)
    try:
        html = html_path.read_text(encoding="utf-8", errors="replace")
    except OSError as exc:
        return ModuleCoverage(module, None, None, f"Cannot read baseline HTML {html_path}: {exc}")
    parsed = parse_baseline_html(module, html)
    if parsed.error:
        return ModuleCoverage(module, None, None, f"{parsed.error}: {html_path}")
    return parsed


def rounded(value: Decimal | None) -> str:
    if value is None:
        return "N/A"
    quantized = value.quantize(Decimal("0.01"), rounding=ROUND_HALF_UP)
    return f"{quantized:.2f}%"


def rounded_delta(value: Decimal | None) -> str:
    if value is None:
        return "N/A"
    quantized = value.quantize(Decimal("0.01"), rounding=ROUND_HALF_UP)
    sign = "+" if value >= 0 else ""
    return f"{sign}{quantized:.2f}pp"


def metric_json(metric: CoverageMetric | None) -> dict[str, Any] | None:
    if metric is None:
        return None
    return {
        "percent": None if metric.percent is None else float(metric.percent),
        "covered": metric.covered,
        "missed": metric.missed,
        "total": metric.total,
    }


def compare_metric(
    baseline: CoverageMetric | None,
    current: CoverageMetric | None,
) -> tuple[Decimal | None, bool, str | None]:
    if baseline is None or baseline.percent is None:
        return None, True, "missing baseline"
    if current is None or current.percent is None:
        return None, True, "missing current"
    delta = current.percent - baseline.percent
    return delta, delta < 0, None


def module_row(
    module: str,
    baseline: ModuleCoverage,
    current: ModuleCoverage,
) -> tuple[dict[str, Any], bool]:
    errors = [err for err in (baseline.error, current.error) if err]
    line_delta, line_failed, line_error = compare_metric(baseline.line, current.line)
    branch_delta, branch_failed, branch_error = compare_metric(baseline.branch, current.branch)

    if line_error:
        errors.append(f"line coverage {line_error}")
    if branch_error:
        errors.append(f"branch coverage {branch_error}")

    failed = bool(errors) or line_failed or branch_failed
    status = coverage_status(baseline, current, errors, line_failed, branch_failed)

    return {
        "module": module,
        "baseline": {
            "line": metric_json(baseline.line),
            "branch": metric_json(baseline.branch),
        },
        "current": {
            "line": metric_json(current.line),
            "branch": metric_json(current.branch),
        },
        "delta": {
            "line": None if line_delta is None else float(line_delta),
            "branch": None if branch_delta is None else float(branch_delta),
        },
        "status": status,
        "errors": errors,
    }, failed


def coverage_status(
    baseline: ModuleCoverage,
    current: ModuleCoverage,
    errors: list[str],
    line_failed: bool,
    branch_failed: bool,
) -> str:
    current_complete = coverage_is_complete(current)
    baseline_complete = coverage_is_complete(baseline)
    if not current_complete:
        return "MISSING"
    if not baseline_complete:
        baseline_error = baseline.error or ""
        if (
            baseline_error.startswith("Baseline HTML not found:")
            and "Baseline checkout error:" not in baseline_error
        ):
            return "NEW"
        return "MISSING"
    if line_failed or branch_failed:
        return "DOWN"
    if errors:
        return "FAIL"
    return "PASS"


def coverage_is_complete(coverage: ModuleCoverage) -> bool:
    return (
        coverage.error is None
        and coverage.line is not None
        and coverage.line.percent is not None
        and coverage.branch is not None
        and coverage.branch.percent is not None
    )


def status_label(status: str) -> str:
    labels = {
        "PASS": ":green_circle: **PASS**",
        "DOWN": ":red_circle: **DOWN**",
        "NEW": ":blue_circle: **NEW**",
        "MISSING": ":yellow_circle: **MISSING**",
        "FAIL": ":red_circle: **FAIL**",
    }
    return labels.get(status, status)


def delta_label(value: Decimal | None) -> str:
    if value is None:
        return ":warning: ↔ N/A"
    if value > 0:
        indicator = ":green_circle: ↑"
    elif value < 0:
        indicator = ":red_circle: ↓"
    else:
        indicator = ":white_circle: →"
    return f"{indicator} {rounded_delta(value)}"


def status_summary(rows: list[dict[str, Any]]) -> str:
    counts = {
        status: sum(row["status"] == status for row in rows)
        for status in ("PASS", "DOWN", "NEW", "MISSING")
    }
    return (
        "**Modules:** "
        f":green_circle: {counts['PASS']} PASS · "
        f":red_circle: {counts['DOWN']} DOWN · "
        f":blue_circle: {counts['NEW']} NEW · "
        f":yellow_circle: {counts['MISSING']} MISSING"
    )


def metric_transition(
    baseline: dict[str, Any] | None,
    current: dict[str, Any] | None,
) -> str:
    return f"{rounded(percent_value(baseline))} → {rounded(percent_value(current))}"


def markdown_report(rows: list[dict[str, Any]], failed: bool) -> str:
    if failed:
        gate_summary = (
            "> :x: **Coverage gate failed.** At least one module regressed or a "
            "required report could not be compared."
        )
    else:
        gate_summary = (
            "> :white_check_mark: **Coverage gate passed.** No module line or branch "
            "coverage decreased."
        )

    lines = [
        MARKER,
        "## :bar_chart: Test Coverage Report",
        "",
        gate_summary,
        "",
        status_summary(rows),
        "",
        (
            "**Trend:** :green_circle: ↑ increase · :red_circle: ↓ decrease · "
            ":white_circle: → unchanged · :warning: ↔ unavailable"
        ),
        "",
        "| Module | Line (base → head) | Δ | Branch (base → head) | Δ | Status |",
        "|--------|--------------------:|--:|----------------------:|--:|:------:|",
    ]

    for row in rows:
        baseline = row["baseline"]
        current = row["current"]
        delta = row["delta"]
        lines.append(
            "| "
            f"`{row['module']}` | "
            f"{metric_transition(baseline['line'], current['line'])} | "
            f"{delta_label(decimal_value(delta['line']))} | "
            f"{metric_transition(baseline['branch'], current['branch'])} | "
            f"{delta_label(decimal_value(delta['branch']))} | "
            f"{status_label(row['status'])} |"
        )

    all_errors = [(row["module"], error) for row in rows for error in row["errors"]]
    if all_errors:
        lines.extend(["", "### Coverage baseline/read errors", ""])
        for module, error in all_errors:
            lines.append(f"- `{module}`: {error}")

    return "\n".join(lines) + "\n"


def percent_value(metric: dict[str, Any] | None) -> Decimal | None:
    if metric is None or metric.get("percent") is None:
        return None
    return Decimal(str(metric["percent"]))


def decimal_value(value: Any) -> Decimal | None:
    if value is None:
        return None
    return Decimal(str(value))


def main() -> int:
    args = parse_args()
    try:
        modules = parse_module_specs(args.module)
    except ValueError as exc:
        print(f"[compare-coverage] ERROR: {exc}", file=sys.stderr)
        return 2

    rows: list[dict[str, Any]] = []
    failed = False
    baseline_dir = Path(args.baseline_dir)
    for module, xml_path in modules:
        baseline = read_baseline(module, baseline_dir)
        current = parse_current_xml(module, xml_path)
        row, row_failed = module_row(module, baseline, current)
        rows.append(row)
        failed = failed or row_failed

    report = {
        "failed": failed,
        "baselineDir": str(baseline_dir),
        "modules": rows,
    }
    output_json = Path(args.output_json)
    output_md = Path(args.output_md)
    output_json.parent.mkdir(parents=True, exist_ok=True)
    output_md.parent.mkdir(parents=True, exist_ok=True)
    output_json.write_text(json.dumps(report, indent=2, sort_keys=True) + "\n", encoding="utf-8")

    markdown = markdown_report(rows, failed)
    output_md.write_text(markdown, encoding="utf-8")
    if args.summary:
        with Path(args.summary).open("a", encoding="utf-8") as summary:
            summary.write(markdown)

    print(markdown, end="")
    return 1 if failed else 0


if __name__ == "__main__":
    raise SystemExit(main())

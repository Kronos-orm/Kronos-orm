#!/usr/bin/env python3
"""Reject empty, skipped-only or failing JUnit report directories in the KMP gate."""

import argparse
from pathlib import Path
import xml.etree.ElementTree as ET


def verify(directory: Path) -> int:
    reports = sorted(directory.rglob("TEST-*.xml"))
    if not reports:
        raise ValueError(f"{directory}: no JUnit reports; test execution is required")
    executed = 0
    for report in reports:
        root = ET.parse(report).getroot()
        suites = [root] if root.tag == "testsuite" else list(root.iter("testsuite"))
        for suite in suites:
            if int(suite.get("failures", "0")) or int(suite.get("errors", "0")):
                raise ValueError(f"{report}: failing tests")
            executed += (
                int(suite.get("tests", "0"))
                - int(suite.get("skipped", "0"))
                - int(suite.get("disabled", "0"))
            )
    if executed <= 0:
        raise ValueError(f"{directory}: no tests executed")
    return executed


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("directories", nargs="+", type=Path)
    args = parser.parse_args()
    for directory in args.directories:
        try:
            count = verify(directory)
        except (ValueError, ET.ParseError, OSError) as cause:
            parser.exit(1, f"KMP gate: {cause}\n")
        print(f"{directory}: {count} tests executed")


if __name__ == "__main__":
    main()

#!/usr/bin/env python3

import argparse
import io
import sys
import zipfile
from pathlib import Path
from typing import Any

from defusedxml import ElementTree as safe_element_tree


EXPECTED_PLUGIN_ID = "com.kotlinorm.kronos-idea-plugin"
EXPECTED_SINCE_BUILD = "262"
EXPECTED_UNTIL_BUILD = "262.*"


def fail(message: str) -> None:
    print(f"IDEA plugin artifact verification failed: {message}", file=sys.stderr)
    raise SystemExit(1)


def read_plugin_xml(artifact: Path) -> bytes:
    try:
        with zipfile.ZipFile(artifact) as distribution:
            plugin_jars = [
                name
                for name in distribution.namelist()
                if "/lib/kronos-idea-plugin-" in name
                and name.endswith(".jar")
                and "-searchableOptions.jar" not in name
            ]
            if len(plugin_jars) != 1:
                fail(
                    "expected exactly one main plugin JAR, "
                    f"found {len(plugin_jars)}: {plugin_jars}"
                )

            with distribution.open(plugin_jars[0]) as plugin_jar_stream:
                with zipfile.ZipFile(io.BytesIO(plugin_jar_stream.read())) as plugin_jar:
                    return plugin_jar.read("META-INF/plugin.xml")
    except (OSError, KeyError, zipfile.BadZipFile) as error:
        fail(f"cannot read {artifact}: {error}")


def element_text(root: Any, name: str) -> str:
    element = root.find(name)
    if element is None or element.text is None:
        fail(f"plugin.xml is missing <{name}>")
    return element.text.strip()


def main() -> None:
    parser = argparse.ArgumentParser(
        description="Verify the signed IDEA Marketplace artifact descriptor."
    )
    parser.add_argument("artifact", type=Path)
    parser.add_argument("--expected-version", required=True)
    arguments = parser.parse_args()

    if not arguments.artifact.is_file():
        fail(f"artifact does not exist: {arguments.artifact}")

    try:
        root = safe_element_tree.fromstring(read_plugin_xml(arguments.artifact))
    except safe_element_tree.ParseError as error:
        fail(f"invalid META-INF/plugin.xml: {error}")

    plugin_id = element_text(root, "id")
    plugin_version = element_text(root, "version")
    idea_version = root.find("idea-version")
    if idea_version is None:
        fail("plugin.xml is missing <idea-version>")

    actual = {
        "plugin id": plugin_id,
        "plugin version": plugin_version,
        "since-build": idea_version.get("since-build"),
        "until-build": idea_version.get("until-build"),
    }
    expected = {
        "plugin id": EXPECTED_PLUGIN_ID,
        "plugin version": arguments.expected_version,
        "since-build": EXPECTED_SINCE_BUILD,
        "until-build": EXPECTED_UNTIL_BUILD,
    }

    mismatches = [
        f"{name}: expected {expected[name]!r}, found {actual[name]!r}"
        for name in expected
        if actual[name] != expected[name]
    ]
    if mismatches:
        fail("; ".join(mismatches))

    print(
        f"Verified {arguments.artifact}: "
        f"{plugin_id} {plugin_version}, builds "
        f"{EXPECTED_SINCE_BUILD}..{EXPECTED_UNTIL_BUILD}"
    )


if __name__ == "__main__":
    main()

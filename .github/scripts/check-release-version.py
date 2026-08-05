#!/usr/bin/env python3
"""Fail a release if the v* tag and Gradle project version differ."""
from __future__ import annotations

import re
import sys
from pathlib import Path


def main() -> int:
    if len(sys.argv) != 2:
        print("usage: check-release-version.py <tag>", file=sys.stderr)
        return 2
    tag = sys.argv[1]
    tag_version = tag[1:] if tag.startswith("v") else tag
    build = Path("build.gradle").read_text(encoding="utf-8")
    m = re.search(r"(?m)^version\s*=\s*['\"]([^'\"]+)['\"]\s*$", build)
    if not m:
        raise SystemExit("Could not read version from build.gradle")
    project_version = m.group(1)
    if tag_version != project_version:
        raise SystemExit(f"Tag version {tag_version} does not match build.gradle version {project_version}")
    print(project_version)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())

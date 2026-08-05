#!/usr/bin/env python3
"""Extract one Keep-a-Changelog-style version section from CHANGE.md."""
from __future__ import annotations

import re
import sys
from pathlib import Path


def extract(text: str, version: str) -> str:
    pattern = re.compile(r"^## \[" + re.escape(version) + r"\](?:\s+-\s+.*)?\s*$", re.MULTILINE)
    match = pattern.search(text)
    if not match:
        raise ValueError(f"CHANGE.md has no section for [{version}]")
    start = match.end()
    next_match = re.search(r"^## \[", text[start:], re.MULTILINE)
    end = start + next_match.start() if next_match else len(text)
    body = text[start:end].strip()
    if not body:
        raise ValueError(f"CHANGE.md section [{version}] is empty")
    return body + "\n"


def main() -> int:
    if len(sys.argv) not in (2, 3):
        print("usage: extract-changelog.py <version> [output]", file=sys.stderr)
        return 2
    version = sys.argv[1].lstrip("v")
    body = extract(Path("CHANGE.md").read_text(encoding="utf-8"), version)
    if len(sys.argv) == 3:
        out = Path(sys.argv[2])
        out.parent.mkdir(parents=True, exist_ok=True)
        out.write_text(body, encoding="utf-8")
    else:
        sys.stdout.write(body)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())

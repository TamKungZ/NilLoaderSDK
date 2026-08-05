#!/usr/bin/env python3
import re
import sys
from pathlib import Path

if len(sys.argv) != 2:
    raise SystemExit("usage: extract_changelog.py <version>")

version = sys.argv[1].strip()
text = Path("CHANGE.md").read_text(encoding="utf-8")
pattern = re.compile(
    r"(?ms)^## \[" + re.escape(version) + r"\][^\n]*\n(.*?)(?=^## \[|\Z)"
)
match = pattern.search(text)
if not match:
    raise SystemExit("CHANGE.md has no section for %s" % version)

body = match.group(1).strip()
if not body:
    raise SystemExit("CHANGE.md section for %s is empty" % version)

print(body)

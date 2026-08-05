#!/usr/bin/env python3
import re
import sys
from pathlib import Path

if len(sys.argv) != 2:
    raise SystemExit("usage: verify_release_tag.py v<version>")

tag = sys.argv[1].strip()
if not tag.startswith("v"):
    raise SystemExit("release tag must start with v")

text = Path("build.gradle").read_text(encoding="utf-8")
match = re.search(r"(?m)^version\s*=\s*['\"]([^'\"]+)['\"]\s*$", text)
if not match:
    raise SystemExit("could not read version from build.gradle")

version = match.group(1)
if tag != "v" + version:
    raise SystemExit("tag %s does not match build.gradle version %s" % (tag, version))

print("release tag verified:", tag)

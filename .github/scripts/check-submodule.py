#!/usr/bin/env python3
"""Verify that MinecraftRemapping is a real, initialized, pinned Git submodule."""
from __future__ import annotations

import subprocess
from pathlib import Path

PATH = "tools/MinecraftRemapping"
PIN = "8ca7ba25dfd67eae43b3c73d02603ff6c085a6d7"


def git(*args: str) -> str:
    result = subprocess.run(
        ["git", *args], check=True, stdout=subprocess.PIPE, stderr=subprocess.PIPE, text=True
    )
    return result.stdout.strip()


def main() -> int:
    stage = git("ls-files", "--stage", "--", PATH)
    parts = stage.split()
    if len(parts) < 4 or parts[0] != "160000":
        raise SystemExit(
            "MinecraftRemapping is not committed as a real Git submodule (mode 160000). "
            "Run scripts/setup-minecraft-remapping-submodule.sh or the PowerShell equivalent, then commit the staged gitlink."
        )
    if parts[1] != PIN:
        raise SystemExit(f"MinecraftRemapping gitlink is {parts[1]}, expected pinned commit {PIN}")
    if not Path(PATH).is_dir():
        raise SystemExit("MinecraftRemapping working tree is not initialized; checkout submodules first")
    actual = git("-C", PATH, "rev-parse", "HEAD")
    if actual != PIN:
        raise SystemExit(f"MinecraftRemapping checkout is {actual}, expected {PIN}")
    print(f"MinecraftRemapping submodule OK: {PIN}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())

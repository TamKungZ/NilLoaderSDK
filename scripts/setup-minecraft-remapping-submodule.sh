#!/bin/sh
set -eu

ROOT=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
cd "$ROOT"

PATH_IN_REPO="tools/MinecraftRemapping"
PIN="8ca7ba25dfd67eae43b3c73d02603ff6c085a6d7"

if ! git rev-parse --is-inside-work-tree >/dev/null 2>&1; then
  echo "This command must be run inside the NilLoaderSDK Git repository." >&2
  exit 1
fi

# A ZIP cannot store Git's mode-160000 index entry. Recreate the exact pinned
# gitlink from .gitmodules, then initialize its working tree normally.
rm -rf "$PATH_IN_REPO"
git add .gitmodules
git update-index --add --cacheinfo "160000,$PIN,$PATH_IN_REPO"
git submodule sync -- "$PATH_IN_REPO"
git submodule update --init -- "$PATH_IN_REPO"

actual=$(git -C "$PATH_IN_REPO" rev-parse HEAD)
if [ "$actual" != "$PIN" ]; then
  echo "Unexpected MinecraftRemapping commit: $actual (expected $PIN)" >&2
  exit 1
fi

echo "MinecraftRemapping submodule staged at $PIN"

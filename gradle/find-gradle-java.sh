#!/bin/sh
# Prints a Gradle 8.8-compatible JDK home. Diagnostics go to stderr.
set -u

java_major() {
  home=$1
  exe="$home/bin/java"
  [ -x "$exe" ] || return 1
  v=$("$exe" -XshowSettings:properties -version 2>&1 | awk -F= '/^[[:space:]]*java\.specification\.version[[:space:]]*=/ {gsub(/[[:space:]]/,"",$2); print $2; exit}')
  [ -n "$v" ] || v=$("$exe" -version 2>&1 | sed -n '1s/.*version "\([^"]*\)".*/\1/p')
  case "$v" in
    1.*) v=$(printf '%s' "$v" | cut -d. -f2) ;;
    *) v=$(printf '%s' "$v" | cut -d. -f1) ;;
  esac
  case "$v" in ''|*[!0-9]*) return 1 ;; esac
  printf '%s\n' "$v"
}

is_supported_major() {
  case "$1" in 17|18|19|20|21|22) return 0 ;; *) return 1 ;; esac
}

try_exact_home() {
  home=${1:-}
  [ -n "$home" ] || return 1
  [ -x "$home/bin/java" ] || return 1
  major=$(java_major "$home" 2>/dev/null || true)
  is_supported_major "$major" || return 1
  printf '%s\n' "$home"
  return 0
}

# An explicit SDK override wins. A compatible JAVA_HOME comes next.
if [ -n "${NILSDK_GRADLE_JAVA_HOME:-}" ]; then
  if try_exact_home "$NILSDK_GRADLE_JAVA_HOME"; then exit 0; fi
  echo "NilLoaderSDK: NILSDK_GRADLE_JAVA_HOME is not a supported JDK (17-22): $NILSDK_GRADLE_JAVA_HOME" >&2
  exit 1
fi
if [ -n "${JAVA_HOME:-}" ] && try_exact_home "$JAVA_HOME"; then
  exit 0
fi

add_candidate() {
  h=$1
  [ -n "$h" ] || return 0
  [ -x "$h/bin/java" ] || return 0
  # Duplicates are harmless, but avoid common repeats without relying on regex matching.
  oldIFS=$IFS
  IFS='
'
  for existing in $CANDIDATES; do
    if [ "$existing" = "$h" ]; then
      IFS=$oldIFS
      return 0
    fi
  done
  IFS=$oldIFS
  CANDIDATES="${CANDIDATES}${CANDIDATES:+
}$h"
}

CANDIDATES=""
if command -v java >/dev/null 2>&1; then
  java_path=$(command -v java)
  if command -v readlink >/dev/null 2>&1; then
    resolved=$(readlink -f "$java_path" 2>/dev/null || true)
    [ -n "$resolved" ] && add_candidate "$(dirname "$(dirname "$resolved")")"
  fi
fi

for h in /usr/lib/jvm/* "$HOME"/.jdks/* "$HOME"/.sdkman/candidates/java/* /opt/java/* /opt/jdk/*; do
  [ -d "$h" ] && add_candidate "$h"
done

if [ "$(uname -s 2>/dev/null || true)" = "Darwin" ]; then
  for h in /Library/Java/JavaVirtualMachines/*/Contents/Home "$HOME"/Library/Java/JavaVirtualMachines/*/Contents/Home; do
    [ -d "$h" ] && add_candidate "$h"
  done
fi

# Prefer current LTS JDKs, then other Gradle 8.8-supported modern launchers.
for preferred in 21 17 22 20 19 18; do
  oldIFS=$IFS
  IFS='
'
  for h in $CANDIDATES; do
    major=$(java_major "$h" 2>/dev/null || true)
    if [ "$major" = "$preferred" ]; then
      printf '%s\n' "$h"
      exit 0
    fi
  done
  IFS=$oldIFS
done

echo "NilLoaderSDK: no compatible Gradle JVM found." >&2
echo "Install JDK 21 or 17, or set NILSDK_GRADLE_JAVA_HOME to its home directory." >&2
echo "Gradle 8.8 cannot run on Java 25 (class-file major 69)." >&2
exit 1

#!/usr/bin/env bash
set -euo pipefail

# bump-version.sh
# A small script to bump project version consistently across files.
# It reads current version from build-logic/src/main/kotlin/publishing.gradle.kts
# and updates the following locations:
#  - build-logic/src/main/kotlin/publishing.gradle.kts (project.version = "...")
#  - kronos-gradle-plugin/src/main/kotlin/com/kotlinorm/compiler/plugin/KronosGradlePlugin.kt (version = "...")
#
# Usage examples:
#  - Auto bump to next snapshot:              .github/scripts/bump-version.sh next-snapshot
#  - Set explicit release version:            .github/scripts/bump-version.sh set 1.2.3
#  - Set explicit snapshot version:           .github/scripts/bump-version.sh set 1.2.4-SNAPSHOT
#  - Bump to release (drop -SNAPSHOT):        .github/scripts/bump-version.sh release-from-current
#  - Bump to next release (patch):            .github/scripts/bump-version.sh next-release
#
# The script prints NEW_VERSION=... so callers can capture with GITHUB_OUTPUT.

ROOT_DIR="$(git rev-parse --show-toplevel)"
PUBLISHING_KTS="$ROOT_DIR/build-logic/src/main/kotlin/publishing.gradle.kts"
PLUGIN_KT="$ROOT_DIR/kronos-gradle-plugin/src/main/kotlin/com/kotlinorm/compiler/plugin/KronosGradlePlugin.kt"

err() { echo "[bump-version] ERROR: $*" >&2; exit 1; }

grepCurrent() {
  # Extract the first occurrence of project.version = "X"
  local v
  v=$(grep -E 'project\.version\s*=\s*"[^"]+"' "$PUBLISHING_KTS" | head -n1 | sed -E 's/.*project\.version\s*=\s*"([^"]+)".*/\1/')
  if [[ -z "$v" ]]; then err "Cannot read current version from $PUBLISHING_KTS"; fi
  echo "$v"
}

isSnapshot() { [[ "$1" == *-SNAPSHOT ]]; }

incPatch() {
  local v="$1"
  local core=${v%-SNAPSHOT}
  IFS='.' read -r MA MI PA <<<"$core"
  if [[ -z "$MA" || -z "$MI" || -z "$PA" ]]; then err "Version not in MAJOR.MINOR.PATCH format: $v"; fi
  PA=$((PA+1))
  echo "$MA.$MI.$PA"
}

setVersionInFiles() {
  local newv="$1"
  # publishing.gradle.kts
  sed -i.bak -E "s/(project\.version\s*=\s*")[^"]+(".*)/\1${newv}\2/" "$PUBLISHING_KTS"
  rm -f "$PUBLISHING_KTS.bak"

  # KronosGradlePlugin.kt
  sed -i.bak -E "s/(version\s*=\s*")[^"]+(".*)/\1${newv}\2/" "$PLUGIN_KT"
  rm -f "$PLUGIN_KT.bak"
}

cmd=${1:-}
case "$cmd" in
  set)
    newv=${2:-}
    [[ -z "$newv" ]] && err "Usage: $0 set <version>"
    ;;
  next-snapshot)
    cur=$(grepCurrent)
    if isSnapshot "$cur"; then
      base=$(incPatch "$cur")
    else
      base=$(incPatch "$cur")
    fi
    newv="$base-SNAPSHOT"
    ;;
  release-from-current)
    cur=$(grepCurrent)
    if isSnapshot "$cur"; then
      newv="${cur%-SNAPSHOT}"
    else
      newv="$cur"
    fi
    ;;
  next-release)
    cur=$(grepCurrent)
    base=$(incPatch "$cur")
    if isSnapshot "$base"; then base="${base%-SNAPSHOT}"; fi
    newv="$base"
    ;;
  *)
    err "Unknown command. Use one of: set, next-snapshot, release-from-current, next-release"
    ;;
esac

setVersionInFiles "$newv"
echo "NEW_VERSION=$newv"

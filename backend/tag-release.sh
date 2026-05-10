#!/usr/bin/env bash
set -euo pipefail

usage() {
    echo "Usage: $0 <tag>"
    echo "  tag  Version tag to apply to both vibin and vibin-app (e.g. 1.0.0-beta.1)"
    exit 1
}

[[ $# -ne 1 ]] && usage

TAG="$1"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
VIBIN_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
VIBIN_APP_DIR="$(cd "$VIBIN_DIR/../vibin-app" && pwd)"

die() { echo "error: $*" >&2; exit 1; }

check_repo() {
    local dir="$1" name="$2"
    [[ -d "$dir/.git" ]] || die "'$dir' is not a git repository"
    if ! git -C "$dir" diff --quiet || ! git -C "$dir" diff --cached --quiet; then
        die "$name has uncommitted changes, commit or stash before tagging"
    fi
    if git -C "$dir" rev-parse "refs/tags/$TAG" &>/dev/null; then
        die "$name already has tag '$TAG'"
    fi
}

echo "==> Checking repositories"
check_repo "$VIBIN_DIR"     "vibin"
check_repo "$VIBIN_APP_DIR" "vibin-app"

echo "==> Tagging vibin @ $TAG"
git -C "$VIBIN_DIR" tag -a "$TAG" -m "Release $TAG"

echo "==> Tagging vibin-app @ $TAG"
git -C "$VIBIN_APP_DIR" tag -a "$TAG" -m "Release $TAG"

echo "==> Pushing vibin tag"
git -C "$VIBIN_DIR" push origin "$TAG"

echo "==> Pushing vibin-app tag"
git -C "$VIBIN_APP_DIR" push origin "$TAG"

echo "==> Done — both repos tagged and pushed as '$TAG'"

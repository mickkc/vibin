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
VIBIN_DIR="$(cd "$SCRIPT_DIR" && pwd)"
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

check_versions() {
  # Check versions in server (version and supportedAppVersions in Application.kt)
  local server_version="$(grep -oPz 'val version: String\s+get\(\) = "\K.*(?=")' "$VIBIN_DIR/backend/src/main/kotlin/de/mickkc/vibin/Application.kt")"
  local server_supported_versions="$(grep -oPz 'val supportedAppVersions: List<String>\s+get\(\) = listOf\(\K.+(?=\))' "$VIBIN_DIR/backend/src/main/kotlin/de/mickkc/vibin/Application.kt")"

  if [[ "$server_version" != "$TAG" ]]; then
    die "Server version in Application.kt is not '$TAG': $server_version"
  fi

  if [[ "$server_supported_versions" != \"*"$TAG"*\" ]]; then
    die "Version '$TAG' is not specified in supported versions in Application.kt: $server_supported_versions"
  fi

  # Check versions in app (version in pubspec.yaml and supportedServerVersions in main.dart)
  local app_version="$(grep -oPz 'version: \K.+(?=\+\d+)' "$VIBIN_APP_DIR/pubspec.yaml")"
  local app_supported_versions="$(grep -oPz 'final supportedServerVersions = \[\s+\K.+' "$VIBIN_APP_DIR/lib/main.dart")"

  if [[ "$app_version" != "$TAG" ]]; then
    die "App version in pubspec.yaml is not '$TAG': $app_version"
  fi

  if [[ "$app_supported_versions" != \"*"$TAG"*\" ]]; then
    die "Version '$TAG' is not specified in supported versions in main.dart: $app_supported_versions"
  fi

  # Check Dockerfile frontend version
  local dockerfile_frontend_version="$(grep -oP 'ARG VIBIN_TAG=\K.*' "$VIBIN_DIR/Dockerfile")"

  if [[ "$dockerfile_frontend_version" != "$TAG" ]]; then
    die "Build arg in Dockerfile is not '$TAG': $dockerfile_frontend_version"
  fi
}

echo "==> Checking repositories"
check_repo "$VIBIN_DIR"     "vibin"
check_repo "$VIBIN_APP_DIR" "vibin-app"

check_versions

echo "==> Tagging vibin @ $TAG"
git -C "$VIBIN_DIR" tag -a "$TAG" -m "Release $TAG"

echo "==> Tagging vibin-app @ $TAG"
git -C "$VIBIN_APP_DIR" tag -a "$TAG" -m "Release $TAG"

echo "==> Pushing vibin tag"
git -C "$VIBIN_DIR" push origin "$TAG"

echo "==> Pushing vibin-app tag"
git -C "$VIBIN_APP_DIR" push origin "$TAG"

echo ""
echo "============================== Done! =============================="

echo "==> Pipeline: https://jks.ndu.wtf/job/Vibin'%20Docker%20release/view/tags/job/$TAG/"
echo "==> Server src: https://codeberg.org/mickkc/vibin/src/tag/$TAG"
echo "==> App src: https://codeberg.org/mickkc/vibin-app/src/tag/$TAG"

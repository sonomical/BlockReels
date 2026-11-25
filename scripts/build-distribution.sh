#!/usr/bin/env bash
# SPDX-License-Identifier: MIT
# Build a clean committed release APK and record source, APK checksum, and signer identity.
set -euo pipefail
project_dir=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)
cd "$project_dir"

expected_signer=${1:-}
if [[ ! $expected_signer =~ ^[[:xdigit:]]{64}$ || $# != 1 ]]; then
  echo 'Usage: scripts/build-distribution.sh <trusted-release-cert-sha256-without-colons>' >&2
  exit 2
fi

revision=$(git rev-parse --verify 'HEAD^{commit}')
if ! git diff --quiet HEAD -- || [[ -n $(git ls-files --others --exclude-standard) ]]; then
  echo 'Commit the reviewed source or run this command in a clean checkout.' >&2
  exit 1
fi

work=$(mktemp -d)
trap 'rm -rf -- "$work"' EXIT
mkdir "$work/source" "$work/logs"
git archive --format=tar "$revision" | tar -x -C "$work/source"

sdk=${ANDROID_SDK_ROOT:-${ANDROID_HOME:-}}
if [[ -z $sdk && -f "$project_dir/local.properties" ]]; then
  sdk=$(sed -n 's/^sdk.dir=//p' "$project_dir/local.properties")
fi
[[ -d $sdk/build-tools ]] || { echo 'Set ANDROID_SDK_ROOT to the installed Android SDK.' >&2; exit 1; }
export ANDROID_SDK_ROOT="$sdk"

cd "$work/source"
printf 'sdk.dir=%s\n' "$sdk" >local.properties
if [[ -f "$project_dir/key.properties" ]]; then
  cp "$project_dir/key.properties" key.properties
fi

VERIFY_LOG_DIR="$work/logs" scripts/verify-local.sh release
apk=app/build/outputs/apk/release/app-release.apk
[[ -s $apk && -s $work/logs/release-signature.log ]]

badging="$work/logs/release-badging.log"
if ! grep -Fq "package: name='com.blockreels.app'" "$badging" || grep -q '^application-debuggable' "$badging"; then
  echo 'Unexpected release package or debugging flag.' >&2
  exit 1
fi

cert=$(sed -n 's/^Signer #1 certificate SHA-256 digest: //p' "$work/logs/release-signature.log")
[[ ${cert,,} == ${expected_signer,,} ]] || { echo 'Release signing identity did not match.' >&2; exit 1; }
if grep -q '^Signer #2 certificate SHA-256 digest:' "$work/logs/release-signature.log"; then
  echo 'Unexpected additional APK signer.' >&2
  exit 1
fi

output_root="$project_dir/build/distribution"
mkdir -p "$output_root"
output=$(mktemp -d "$output_root/.staging.XXXXXX")
trap 'rm -rf -- "$work" "$output"' EXIT
cp "$apk" "$output/app-release.apk"
(cd "$output" && sha256sum app-release.apk >SHA256SUMS)
printf 'git_commit=%s\nsigner_sha256=%s\n' "$revision" "$cert" >"$output/source.txt"
cp "$work/logs/release-permissions.txt" "$output/permissions.txt"
destination="$output_root/$revision"
mv -Tn -- "$output" "$destination"
printf 'Verified distribution and receipt: %s\n' "$destination"

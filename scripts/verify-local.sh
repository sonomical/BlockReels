#!/usr/bin/env bash
# SPDX-License-Identifier: MIT
# Run local verification: ./scripts/verify-local.sh [all|tests|debug|release]
set -uo pipefail
project_dir=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)
cd "$project_dir" || exit 2
verify_logs=${VERIFY_LOG_DIR:-$(mktemp -d /tmp/blockreels-verify.XXXXXX)}
mkdir -p "$verify_logs"
gradle_cli="$project_dir/scripts/android-gradle.sh"
verify_status=0

run() {
  local step=$1; shift
  "$@" >"$verify_logs/$step.log" 2>&1
  local result=$?
  printf '%s: exit %s (%s/%s.log)\n' "$step" "$result" "$verify_logs" "$step"
  if (( result != 0 )); then tail -25 "$verify_logs/$step.log"; verify_status=1; fi
  return "$result"
}

inspect_apk() {
  local variant=$1 apk=$2 sdk_dir aapt_bin signer_bin
  sdk_dir=${ANDROID_SDK_ROOT:-${ANDROID_HOME:-}}
  if [[ -z $sdk_dir && -f local.properties ]]; then
    sdk_dir=$(sed -n 's/^sdk.dir=//p' local.properties)
  fi
  aapt_bin=$(find "$sdk_dir/build-tools" -maxdepth 2 -name aapt -type f 2>/dev/null | sort -V | tail -1)
  signer_bin="$(dirname "$aapt_bin")/apksigner"
  [[ -x $aapt_bin && -x $signer_bin && -f $apk ]] || { echo 'APK inspection tools or APK missing'; verify_status=1; return 1; }
  run "$variant-badging" "$aapt_bin" dump badging "$apk"
  run "$variant-manifest" "$aapt_bin" dump xmltree "$apk" AndroidManifest.xml
  run "$variant-permissions" "$aapt_bin" dump permissions "$apk" || return
  local unexpected permissions
  permissions=$(sed -n "s/^uses-permission[^:]*: name='\([^']*\)'.*/\1/p" "$verify_logs/$variant-permissions.log")
  [[ -n $permissions ]] || { echo 'No parsed APK permissions; inspect the manifest log.'; verify_status=1; return 1; }
  printf '%s\n' "$permissions" >"$verify_logs/$variant-permissions.txt"
  unexpected=$(printf '%s\n' "$permissions" |
    grep -Ev '^(android[.]permission[.](REQUEST_IGNORE_BATTERY_OPTIMIZATIONS|VIBRATE|MODIFY_AUDIO_SETTINGS|FOREGROUND_SERVICE|FOREGROUND_SERVICE_SPECIAL_USE|POST_NOTIFICATIONS)|com[.]blockreels[.]app[.]DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION)$' || true)
  if [[ -n $unexpected ]]; then
    printf 'Unexpected APK permissions:\n%s\n' "$unexpected"
    verify_status=1
    return 1
  fi
  run "$variant-signature" "$gradle_cli" --exec "$signer_bin" verify --verbose --print-certs "$apk"
  sha256sum "$apk" >"$verify_logs/$variant-apk.sha256"
}

unit_tests() {
  run android-tests "$gradle_cli" :app:testDebugUnitTest --rerun-tasks
}

debug_build() {
  local apk="$project_dir/app/build/outputs/apk/debug/app-debug.apk"
  rm -f -- "$apk"
  run debug-apk "$gradle_cli" :app:assembleDebug || return
  inspect_apk debug "$apk"
}

release_build() {
  local apk="$project_dir/app/build/outputs/apk/release/app-release.apk"
  if [[ ! -f key.properties && -z ${BLOCKREELS_KEYSTORE_FILE:-} ]]; then
    echo 'Release: skipped (no release signing configuration found).'
    return
  fi
  rm -f -- "$apk"
  run release-apk "$gradle_cli" :app:assembleRelease || return
  inspect_apk release "$apk"
  if grep -q 'CN=Android Debug' "$verify_logs/release-signature.log"; then
    echo 'Release uses a debug certificate; do not distribute.'
    verify_status=1
  fi
}

main() {
  case ${1:-all} in
    all) unit_tests; debug_build; release_build ;;
    tests) unit_tests ;;
    debug) debug_build ;;
    release) release_build ;;
    *) echo 'Usage: verify-local.sh [all|tests|debug|release]'; exit 2 ;;
  esac
  printf 'Verification logs: %s\n' "$verify_logs"
  exit "$verify_status"
}
main "${1:-all}"

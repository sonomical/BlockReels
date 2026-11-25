#!/usr/bin/env bash
# SPDX-License-Identifier: MIT
# Select a complete JDK 17/21 and invoke Gradle from the repository root.
set -euo pipefail
project_dir=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)

if [[ -n ${BLOCKREELS_JAVA_HOME:-} ]]; then
  jdk_candidates=("$BLOCKREELS_JAVA_HOME")
else
  jdk_candidates=("${JAVA_HOME:-}" "$HOME"/.gradle/jdks/* /usr/lib/jvm/*
    "$HOME/Downloads/android-studio/jbr" /opt/android-studio/jbr)
fi

selected_jdk=
for candidate in "${jdk_candidates[@]}"; do
  [[ -x "$candidate/bin/java" && -x "$candidate/bin/javac" ]] || continue
  compiler_version=$("$candidate/bin/javac" -version 2>&1) || continue
  [[ $compiler_version =~ ^javac\ (17|21)(\.|$) ]] || continue
  selected_jdk=$candidate
  break
done

if [[ -z $selected_jdk ]]; then
  echo 'No complete JDK 17/21 found. Set BLOCKREELS_JAVA_HOME to a JDK containing bin/java and bin/javac.' >&2
  exit 2
fi

export JAVA_HOME="$selected_jdk"
export PATH="$JAVA_HOME/bin:$PATH"
printf 'Using JDK: %s\n' "$JAVA_HOME" >&2

if [[ ${1:-} == --exec ]]; then
  shift
  exec "$@"
fi

cd "$project_dir"
if [[ -x "$project_dir/gradlew" ]]; then
  gradle_bin="$project_dir/gradlew"
elif command -v gradle >/dev/null 2>&1; then
  gradle_bin=$(command -v gradle)
else
  echo 'Neither ./gradlew nor gradle found on PATH.' >&2
  exit 2
fi

gradle_options=(--console=plain "-Dorg.gradle.java.home=$JAVA_HOME")
[[ ${BLOCKREELS_OFFLINE:-0} == 1 ]] && gradle_options+=(--offline)
exec "$gradle_bin" "${gradle_options[@]}" "$@"

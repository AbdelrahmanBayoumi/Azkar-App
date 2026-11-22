#!/usr/bin/env bash
set -euo pipefail

source_jdk=$(realpath -- "${1:?Usage: bash builder-safety.sh <zulu-fx-8-jdk>}")
builder=$(realpath -- "$(dirname -- "${BASH_SOURCE[0]}")/../build-patched-runtime.sh")
fixture_dir=$(mktemp -d)
trap 'rm -rf -- "$fixture_dir"' EXIT

# The shared target is a disposable copy even when testing the unsafe old builder.
cp -a -- "$source_jdk" "$fixture_dir/source"
mv -- "$fixture_dir/source/jre" "$fixture_dir/shared-jre"
ln -s -- "$fixture_dir/shared-jre" "$fixture_dir/source/jre"
shared_jar="$fixture_dir/shared-jre/lib/ext/jfxrt.jar"
before=$(sha256sum -- "$shared_jar")
if "$builder" "$fixture_dir/source" "$fixture_dir/symlink-output" > "$fixture_dir/symlink.log" 2>&1; then
    echo "Builder unexpectedly accepted a symlink in the runtime patch path" >&2
    exit 1
fi
[[ $(sha256sum -- "$shared_jar") == "$before" ]]
grep -q 'Refusing symlink in JavaFX patch path' "$fixture_dir/symlink.log"
[[ ! -e "$fixture_dir/symlink-output" ]]

# GCC still compiles real code; its wrapper makes the competing mkdir deterministic.
export AZKAR_TEST_COMPETING_PATH="$fixture_dir/competing-output"
gcc() {
    command gcc "$@" || return
    if [[ " $* " == *" -shared "* ]]; then
        mkdir -- "$AZKAR_TEST_COMPETING_PATH"
        touch -- "$AZKAR_TEST_COMPETING_PATH/owned-by-competitor"
    fi
}
export -f gcc
if "$builder" "$source_jdk" "$AZKAR_TEST_COMPETING_PATH" > "$fixture_dir/competition.log" 2>&1; then
    echo "Builder unexpectedly claimed the competing directory" >&2
    exit 1
fi
[[ -f "$AZKAR_TEST_COMPETING_PATH/owned-by-competitor" ]]
[[ ! -e "$AZKAR_TEST_COMPETING_PATH/jre" ]]
echo "Builder leaves shared JDK and competing output intact"

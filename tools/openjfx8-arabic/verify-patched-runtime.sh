#!/usr/bin/env bash

set -euo pipefail

if [[ $# -ne 2 ]]; then
    echo "Usage: $0 <patched-java-8-jdk> <output-directory>" >&2
    exit 64
fi

runtime_jdk=$(realpath "$1")
output_directory=$(realpath -m "$2")
script_directory=$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)
project_directory=$(cd "$script_directory/../.." && pwd)
font_directory="$project_directory/Azkar/src/main/resources/com/bayoumi/fonts"
css_directory="$project_directory/Azkar/src/main/resources/com/bayoumi/css"
noto_font="$font_directory/NotoNaskhArabic-Bold.ttf"
tajawal_medium="$font_directory/Tajawal/Tajawal-Medium.ttf"

if [[ ! -x "$runtime_jdk/bin/javac" ]]; then
    echo "The runtime path must be a JDK: $runtime_jdk" >&2
    exit 65
fi
if [[ -z "${DISPLAY:-}" ]]; then
    echo "DISPLAY is required. Use the desktop display locally or xvfb-run in CI." >&2
    exit 69
fi

classes_directory=$(mktemp -d)
trap 'rm -rf "$classes_directory"' EXIT
mkdir -p "$output_directory"

"$runtime_jdk/bin/javac" \
    -source 1.8 \
    -target 1.8 \
    -Xlint:all \
    -d "$classes_directory" \
    "$script_directory/test/ArabicWrappingProbe.java" \
    "$script_directory/test/TajawalCssProbe.java" \
    "$script_directory/test/UnicodeTextRegressionProbe.java"

"$runtime_jdk/bin/java" \
    -cp "$classes_directory" \
    ArabicWrappingProbe \
    "$noto_font" \
    "$output_directory/arabic-wrapping.png"

"$runtime_jdk/bin/java" \
    -cp "$classes_directory" \
    TajawalCssProbe \
    "$css_directory/base.css" \
    "$css_directory/light-theme.css" \
    "$tajawal_medium" \
    "$output_directory/tajawal-medium.png"

"$runtime_jdk/bin/java" \
    -cp "$classes_directory" \
    UnicodeTextRegressionProbe

echo "JavaFX 8 Arabic verification passed."
echo "Screenshots:"
echo "  $output_directory/arabic-wrapping.png"
echo "  $output_directory/tajawal-medium.png"

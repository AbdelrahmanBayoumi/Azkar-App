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
resources="$project_directory/Azkar/src/main/resources"
font_directory="$resources/com/bayoumi/fonts"
noto_font="$font_directory/NotoNaskhArabic-Bold.ttf"

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
runtime_options=(
    -Dprism.text=native
    -Dprism.lcdtext=true
)

"$runtime_jdk/bin/javac" \
    -source 1.8 \
    -target 1.8 \
    -Xlint:all \
    -d "$classes_directory" \
    "$script_directory/test/ArabicWrappingProbe.java" \
    "$script_directory/test/BidiPunctuationProbe.java" \
    "$script_directory/test/ComplexWrapProbe.java" \
    "$script_directory/test/GlyphCacheClippingProbe.java" \
    "$script_directory/test/RendererSelectionProbe.java" \
    "$script_directory/test/NotoRasterizationProbe.java" \
    "$script_directory/test/TajawalCssProbe.java" \
    "$script_directory/test/UnicodeTextRegressionProbe.java" \
    "$project_directory/Azkar/src/main/java/com/bayoumi/util/OSUtil.java" \
    "$project_directory/Azkar/src/main/java/com/bayoumi/util/gui/ArabicTextSupport.java"

"$runtime_jdk/bin/java" -cp "$classes_directory" RendererSelectionProbe native

"$runtime_jdk/bin/jar" cf "$classes_directory/app-resources.jar" \
    -C "$resources" com/bayoumi/css \
    -C "$resources" com/bayoumi/fonts

"$runtime_jdk/bin/java" \
    "${runtime_options[@]}" \
    -cp "$classes_directory" \
    ArabicWrappingProbe \
    "$noto_font" \
    "$output_directory/arabic-wrapping.png"

"$runtime_jdk/bin/java" \
    "${runtime_options[@]}" \
    -cp "$classes_directory" \
    ComplexWrapProbe "$noto_font"

if [[ ${AZKAR_VERIFY_ES2:-0} == 1 ]]; then
    "$runtime_jdk/bin/java" \
        "${runtime_options[@]}" \
        -Dprism.order=es2 \
        -Dprism.forceGPU=true \
        -cp "$classes_directory" \
        GlyphCacheClippingProbe "$noto_font"
fi

"$runtime_jdk/bin/java" \
    "${runtime_options[@]}" \
    -cp "$classes_directory" \
    BidiPunctuationProbe \
    "$noto_font" \
    "$output_directory/bidi-punctuation.png"

"$runtime_jdk/bin/java" \
    "${runtime_options[@]}" \
    -cp "$classes_directory" \
    NotoRasterizationProbe \
    "$noto_font" \
    "$output_directory/noto-rasterization.png"

"$runtime_jdk/bin/java" \
    "${runtime_options[@]}" \
    -cp "$classes_directory:$classes_directory/app-resources.jar" \
    TajawalCssProbe \
    "$output_directory/tajawal-medium.png"

"$runtime_jdk/bin/java" \
    "${runtime_options[@]}" \
    -cp "$classes_directory" \
    UnicodeTextRegressionProbe

echo "JavaFX 8 Arabic verification passed."
echo "Screenshots:"
echo "  $output_directory/arabic-wrapping.png"
echo "  $output_directory/bidi-punctuation.png"
echo "  $output_directory/noto-rasterization.png"
echo "  $output_directory/tajawal-medium.png"

#!/usr/bin/env bash

set -euo pipefail

if [[ $# -ne 2 ]]; then
    echo "Usage: $0 <zulu-fx-8-jdk> <output-jdk>" >&2
    exit 64
fi

if [[ ! -d "$1" ]]; then
    echo "Source JDK not found: $1" >&2
    exit 66
fi

source_jdk=$(realpath "$1")
output_jdk=$(realpath -m "$2")
script_dir=$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)
source_jfxrt="$source_jdk/jre/lib/ext/jfxrt.jar"
native_source="$script_dir/src/native/pango_geometry.c"

if [[ ! -x "$source_jdk/bin/javac" || ! -f "$source_jfxrt" ]]; then
    echo "The source path must be a Java 8 JDK with JavaFX." >&2
    exit 65
fi

java_version=$($source_jdk/bin/java -version 2>&1 | head -n 1)
if [[ "$java_version" != *'1.8.0_'* ]]; then
    echo "Expected Java 8, found: $java_version" >&2
    exit 65
fi

if [[ -e "$output_jdk" ]]; then
    echo "Output already exists: $output_jdk" >&2
    exit 73
fi
if [[ "$output_jdk/" == "$source_jdk/"* ]]; then
    echo "Output must not be inside the source JDK: $output_jdk" >&2
    exit 73
fi

if ! "$source_jdk/bin/jar" tf "$source_jfxrt" \
    | grep -Fxq 'com/sun/javafx/font/freetype/PangoGlyphLayout.class'; then
    echo "PangoGlyphLayout is missing from $source_jfxrt" >&2
    exit 65
fi

for required_command in gcc pkg-config; do
    if ! command -v "$required_command" >/dev/null 2>&1; then
        echo "Required build command not found: $required_command" >&2
        exit 69
    fi
done
if ! pkg-config --exists pangoft2; then
    echo "The pangoft2 development package is required." >&2
    exit 69
fi

source_pango_libraries=()
while IFS= read -r library; do
    source_pango_libraries+=("$library")
done < <(find "$source_jdk/jre/lib" -type f -name 'libjavafx_font_pango.so' -print)
if [[ ${#source_pango_libraries[@]} -ne 1 ]]; then
    echo "Expected one libjavafx_font_pango.so under $source_jdk/jre/lib." >&2
    exit 65
fi
source_native_dir=$(dirname "${source_pango_libraries[0]}")
relative_native_dir=${source_native_dir#"$source_jdk/"}

classes_dir=$(mktemp -d)
output_owned=false

cleanup() {
    local exit_status=$?
    rm -rf -- "$classes_dir"
    if [[ $exit_status -ne 0 && "$output_owned" == true ]]; then
        rm -rf -- "$output_jdk"
    fi
    return "$exit_status"
}

trap cleanup EXIT

patch_sources=(
    "$script_dir/src/com/sun/javafx/font/freetype/PangoGlyphLayout.java"
    "$script_dir/src/com/sun/javafx/font/freetype/PangoGlyphString.java"
    "$script_dir/src/com/sun/javafx/font/freetype/PangoGlyphGeometry.java"
)

"$source_jdk/bin/javac" \
    -source 1.8 \
    -target 1.8 \
    -Xlint:all \
    -classpath "$source_jfxrt" \
    -d "$classes_dir" \
    "${patch_sources[@]}"

read -r -a pango_cflags <<< "$(pkg-config --cflags pangoft2)"
read -r -a pango_libraries <<< "$(pkg-config --libs pangoft2)"
native_patch="$classes_dir/libjavafx_font_pango_geometry.so"
gcc \
    -std=c99 \
    -O2 \
    -fPIC \
    -shared \
    -Wall \
    -Wextra \
    -Werror \
    -Wl,-z,defs \
    -I"$source_jdk/include" \
    -I"$source_jdk/include/linux" \
    "${pango_cflags[@]}" \
    "$native_source" \
    -o "$native_patch" \
    "${pango_libraries[@]}"

output_owned=true
cp -a "$source_jdk" "$output_jdk"
patched_jfxrt="$output_jdk/jre/lib/ext/jfxrt.jar"

"$source_jdk/bin/jar" uf "$patched_jfxrt" \
    -C "$classes_dir" com/sun/javafx/font/freetype/PangoGlyphLayout.class \
    -C "$classes_dir" 'com/sun/javafx/font/freetype/PangoGlyphLayout$Utf8Buffer.class' \
    -C "$classes_dir" com/sun/javafx/font/freetype/PangoGlyphString.class \
    -C "$classes_dir" com/sun/javafx/font/freetype/PangoGlyphGeometry.class

install -m 0755 \
    "$native_patch" \
    "$output_jdk/$relative_native_dir/libjavafx_font_pango_geometry.so"

echo "Patched JavaFX runtime created at: $output_jdk"
echo "Installed Pango glyph geometry bridge in: $relative_native_dir"
"$output_jdk/bin/java" -version

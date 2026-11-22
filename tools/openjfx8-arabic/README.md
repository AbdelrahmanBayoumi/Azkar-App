# OpenJFX 8 Arabic text patch

This directory builds an isolated ZuluFX 8 runtime for Azkar on Linux. The
builder compiles against the source JDK, copies it to a new output path, then
patches the layout and rendering classes in the copy's `jfxrt.jar`. It places
a glyph-geometry bridge beside the copied JavaFX native libraries.

The patch targets ZuluFX 8 on Linux x86-64. Run the verifier on each JDK build
you intend to distribute.

## Why the patch is needed

On Linux, this JavaFX 8 build uses Pango to shape text. `PangoGlyphLayout`
caches a native UTF-8 buffer by `TextRun` object identity. JavaFX can later
split that same mutable `TextRun`, changing its start and end offsets. The old
cache entry then represents the earlier text range, so wrapping can shape the
wrong bytes. In Azkar this produced clipped or duplicated Arabic and the wrong
number of visual lines.

The patched cache stores the start and end offsets alongside the native
address. It reuses the buffer while those bounds still match. If a split
changes the bounds, it frees that stale buffer and creates one for the current
range. This preserves caching for unchanged runs and preserves the separate
fix for [JDK-8246348](https://bugs.openjdk.org/browse/JDK-8246348).

JavaFX 8 has a second, independent defect in the same path. Pango returns a
width, horizontal offset, and vertical offset for every shaped glyph, but the
JavaFX bridge retains only the width. Arabic combining marks therefore render
at the pen position instead of their shaped positions. The patch retains the
two offsets for drawing, and uses the logical advances for wrapping and text
hit testing. It assembles each RTL run's Pango items in visual order, maps
their UTF-8 cluster offsets to UTF-16, and keeps surrogate pairs intact at
forced line breaks. Prism clips the rendered pixels of positioned glyphs so
a mark outside the clip cannot hide a visible part of its base letter.

`PangoGlyphLayout.java` and `PangoGlyphString.java` originate from the OpenJFX
sources shipped with the JDK. `PangoGlyphGeometry.java` and
`src/native/pango_geometry.c` form the small JNI bridge used to preserve the
missing geometry. Their license is in [`LICENSE.openjfx`](LICENSE.openjfx).
The builder applies [the text layout patch](src/text-layout.patch) to
`TextRun.java`, `PrismTextLayout.java`, and `GlyphCache.java` from the input
JDK's OpenJFX sources.

## Oracle 8 and OpenJFX do not use the same text renderer

Azkar selects `prism.text=native` with LCD text on Linux when the patched JNI
bridge loads. Other runtimes keep the previous T2K preference with LCD text
disabled. Oracle 8u221 contains T2K. ZuluFX 8 uses its available font backend
when T2K is absent.

This explains two expected differences from the old Oracle runtime:

- Unicode-mirrored ASCII brackets face differently from Oracle/T2K because
  Pango applies the RTL mirroring metadata.
- The same embedded Noto Bold file can still have different antialiasing under
  T2K and FreeType; shaped mark positions must remain attached to their base
  letters in both renderers.

## Build an isolated runtime

Run from the repository root. The output path must not already exist.

```bash
ZULU8_JDK=/path/to/zulu-fx-8-jdk
PATCHED_ZULU8=/tmp/azkar-zulu8-patched

tools/openjfx8-arabic/build-patched-runtime.sh \
    "$ZULU8_JDK" \
    "$PATCHED_ZULU8"
```

The script requires a Java 8 JDK with `bin/javac`, `jre/lib/ext/jfxrt.jar`,
one `libjavafx_font_pango.so`, JNI headers, and its matching `openjfx-src.zip`.
The build host needs `gcc`, `pkg-config`, `unzip`, `patch`, and the
`pangoft2` development metadata and headers. The destination's parent must
exist. The builder rejects symlinks in paths it needs to patch and claims the
output directory atomically. If the copy or patch fails after that claim, the
incomplete output remains for inspection. Use a fresh destination for retries.

Run `bash tools/openjfx8-arabic/test/builder-safety.sh "$ZULU8_JDK"` to verify
that the builder rejects a shared JRE and preserves a competing output path.

## Verify Arabic rendering

Run this from a Linux desktop session with `DISPLAY` already set:

```bash
tools/openjfx8-arabic/verify-patched-runtime.sh \
    "$PATCHED_ZULU8" \
    /tmp/azkar-zulu8-verification
```

The verifier checks renderer selection and runs the JavaFX 8 probes below.
Install DejaVu Sans for the mixed Arabic and emoji wrapping check. Set
`AZKAR_VERIFY_ES2=1` on a display with OpenGL support to exercise the accelerated
glyph cache. With Xvfb and Mesa, set `LIBGL_ALWAYS_SOFTWARE=1` to use software
OpenGL for that check.

- `ArabicWrappingProbe` snapshots the affected Arabic `TextFlow` and requires
  four visual lines. Unmodified ZuluFX 8u492 produces three lines on the tested
  system, so this is a discriminating regression check.
- `ComplexWrapProbe` checks surrogate-pair boundaries, retained glyphs, logical
  advances, and the wrap point in an RTL run containing an emoji.
- `GlyphCacheClippingProbe` compares the pixels inside an accelerated clip
  with the matching pixels in an unclipped rendering of `بَ`.
- `BidiPunctuationProbe` snapshots the application's real mixed Arabic,
  numeric, ASCII-bracket, and ornate-parenthesis samples. It also asserts the
  Java Unicode mirroring metadata used to interpret those pairs. Keep its
  screenshot for manual comparison because pixel output differs between T2K
  and FreeType even when the bidi result is valid.
- `NotoRasterizationProbe` renders a fixed, diacritized sample directly from
  the embedded Noto Bold file. It requires at least one non-zero vertical glyph
  offset, records the selected family, name, style, and size, and retains a
  screenshot for renderer comparisons. The unmodified tested ZuluFX fails this
  offset assertion.
- `TajawalCssProbe` loads the stylesheets and font from JAR resources, then
  checks that `.font-text-medium` resolves to `Azkar Tajawal Medium`.
- `UnicodeTextRegressionProbe` renders both Unicode ranges used by the two
  reproducers attached to JDK-8246348, guarding against its native Pango crash.

The four PNGs written to the output directory are retained for manual visual
inspection. Temporary compiled probe classes are deleted automatically.

## Build Azkar with the patched Java 8

```bash
cd Azkar
JAVA_HOME="$PATCHED_ZULU8" \
    mvn -Dmaven.compiler.source=1.8 -Dmaven.compiler.target=1.8 test
```

See the [Linux installer build steps](../../Azkar/docs/README.md#linux-installers)
to package the patched ZuluFX 8u512 JRE.

## Separate Tajawal correction

The runtime bug and the Tajawal Medium bug are independent. The embedded
Medium font previously shared an ambiguous Tajawal family/style identity.
Azkar now embeds it with these names:

- family: `Azkar Tajawal Medium`
- style: `Regular`
- PostScript name: `AzkarTajawal-Medium`

`base.css` requests that exact family. The font remains under the SIL Open Font
License; see
[`Azkar/src/main/resources/com/bayoumi/fonts/Tajawal/OFL.txt`](../../Azkar/src/main/resources/com/bayoumi/fonts/Tajawal/OFL.txt).

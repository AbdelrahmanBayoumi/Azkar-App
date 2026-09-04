# OpenJFX 8 Arabic text patch

This directory builds an isolated ZuluFX 8 runtime for Azkar on Linux. It does
not edit the JDK passed as the source. The build script first copies that JDK to
a new output path, then replaces the affected Pango layout classes inside the
copied `jfxrt.jar` and adds a small glyph-geometry bridge beside the copied
JavaFX native libraries.

The patch is currently verified against ZuluFX 8u492
(`Zulu 8.94.0.17-CA-linux64`). Treat other ZuluFX 8 builds as unverified until
the included probes pass on them.

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
two offsets, applies them to each glyph, and keeps the line advance equal to
the sum of Pango's widths.

`PangoGlyphLayout.java` and `PangoGlyphString.java` originate from the OpenJFX
sources shipped with the JDK. `PangoGlyphGeometry.java` and
`src/native/pango_geometry.c` form the small JNI bridge used to preserve the
missing geometry. Their license is in [`LICENSE.openjfx`](LICENSE.openjfx).

## Oracle 8 and OpenJFX do not use the same text renderer

Azkar selects `prism.text=native` on Linux so JavaFX uses FreeType and Pango for
complex glyph layout. It retains the legacy T2K selection on other operating
systems. Oracle 8u221 contains T2K; the tested ZuluFX 8 build does not.

This explains two expected differences from the old Oracle runtime:

- Unicode-mirrored ASCII brackets face differently from Oracle/T2K because
  Pango applies the RTL mirroring metadata.
- The same embedded Noto Bold file can still have different antialiasing under
  T2K and FreeType; shaped mark positions must remain attached to their base
  letters in both renderers.

The cache patch changes the stale-buffer wrapping case. The geometry patch
changes glyph positions only; it does not change font outlines, weight,
antialiasing, or bidi metadata. Do not swap brackets in application data or
add synthetic font weight to imitate T2K.

## Build an isolated runtime

Run from the repository root. The output path must not already exist.

```bash
ZULU8_JDK=/path/to/zulu-fx-8-jdk
PATCHED_ZULU8=/tmp/azkar-zulu8-patched

tools/openjfx8-arabic/build-patched-runtime.sh \
    "$ZULU8_JDK" \
    "$PATCHED_ZULU8"
```

The script requires a Java 8 JDK that contains `bin/javac`,
`jre/lib/ext/jfxrt.jar`, one `libjavafx_font_pango.so`, and the matching JNI
headers. The build host must provide `gcc`, `pkg-config`, and the `pangoft2`
development metadata and headers. The script exits without replacing anything
when the output path already exists.

## Verify Arabic rendering

Run this from a Linux desktop session with `DISPLAY` already set:

```bash
tools/openjfx8-arabic/verify-patched-runtime.sh \
    "$PATCHED_ZULU8" \
    /tmp/azkar-zulu8-verification
```

The verifier compiles and runs five JavaFX 8 probes:

- `ArabicWrappingProbe` snapshots the affected Arabic `TextFlow` and requires
  four visual lines. Unmodified ZuluFX 8u492 produces three lines on the tested
  system, so this is a discriminating regression check.
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
- `TajawalCssProbe` requires the embedded Medium font and the
  `.font-text-medium` CSS class to resolve to `Azkar Tajawal Medium`.
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

For a distributable Linux runtime, package the isolated output JDK according
to the application's release process. Review the JDK vendor's redistribution
terms as well as [`LICENSE.openjfx`](LICENSE.openjfx) before distributing it.

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

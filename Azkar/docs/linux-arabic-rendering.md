# Arabic rendering on Linux

This report records verification of [the Arabic rendering fixes](https://github.com/AbdelrahmanBayoumi/Azkar-App/commit/d476181ed7880dd2fc556444505e2ff06b864c37)
on 22 November 2026. Use the [runtime patch guide](../../tools/openjfx8-arabic/README.md)
and [Linux installer instructions](README.md#linux-installers) to reproduce the builds.

## What changed

| Problem | Fix |
| --- | --- |
| Wrapping sometimes shaped the previous text range after JavaFX split a run | Invalidate the cached Pango UTF-8 buffer when the run's bounds change. Map Pango clusters to UTF-16 and keep surrogate pairs intact when forcing a line break. |
| Arabic vowel marks drew at the pen position | Pass Pango's glyph offsets through the JNI bridge for drawing. Use logical advances for measuring and hit testing. |
| An RTL run could wrap after its first letter when Pango used multiple fonts | Assemble Pango items in visual order while preserving their UTF-16 cluster mappings. |
| A mark outside an accelerated clip could cause Prism to skip a visible base glyph | Keep positioned glyphs in the draw call and let Prism clip their pixels. |
| Tajawal Medium could resolve to the wrong font | Embed it with the distinct family name `Azkar Tajawal Medium`. Verify registration by applying the CSS from JAR resources in a fresh JVM. |
| The runtime builder failed when the source JDK path contained spaces | Quote the source Java path. Check that shared JDK paths and competing output directories stay intact. |
| Linux installers selected the legacy JRE | Set both install4j media entries to use the patched ZuluFX 8u512 bundle and set the Debian package architecture to `amd64`. |

Azkar selects the native renderer on Linux when the patched JNI bridge is
available. Oracle 8u221 and unmodified ZuluFX 8u512 kept the existing T2K
preference in the renderer selection probe.

## Test results

| Runtime | Maven | JavaFX rendering on Xvfb |
| --- | --- | --- |
| Oracle Java 8u221 | 64 passed, 1 skipped | Legacy renderer selection verified |
| Patched ZuluFX 8u492 | 64 passed, 1 skipped | Full verifier passed, including the ES2 clipping probe |
| Patched ZuluFX 8u512 | 64 passed, 1 skipped | Full verifier passed, including the ES2 clipping probe |

The skipped Maven case, `computeUserDataAssetsPath_realWindowsPath_onWindows`,
is Windows-specific. Xvfb used Mesa software OpenGL for the ES2 check. Both
patched releases produced four visual lines for the Arabic wrapping sample.
The verifier also covered ASCII and ornate parentheses, mark positioning with
the embedded Noto font, Tajawal CSS, and the Unicode ranges from
[JDK-8246348](https://bugs.openjdk.org/browse/JDK-8246348).

The regression checks were also run against known failures:

- The previous patch returned wrap offset 0 for `أب🌟جد` at 90 px.
  The fixed runtime returns offset 4 in UTF-16.
- The previous patch lost visible pixels when clipping `بَ`. The fixed
  runtime preserved all 53 reference ink pixels inside the test clip.
- Tajawal resolved to the expected family from the built application JAR.
  Removing `Tajawal-Medium.ttf` from a test copy made that check fail.
- The builder succeeded with a source path containing spaces and passed
  the checks for shared paths and competing output directories.

## Installer verification

install4j 8.0.11 built Linux media 806 and 807 from
[`production.install4j`](../../production.install4j). Both generated files
passed the compiler's SHA-256 manifest check.

| Artifact | Verification |
| --- | --- |
| `Azkar_unix_1_3_0.sh` | Completed a fresh unattended installation into a temporary directory. The embedded 8u512 runtime selected the native renderer and the application launched under Xvfb. |
| `Azkar_linux_1_3_0.deb` | `dpkg-deb` reported `Architecture: amd64`. After extraction and the same Pack200 unpacking performed by the package's `postinst`, the embedded 8u512 runtime selected the native renderer and the application launched under Xvfb. |

The Debian verification covered the extracted payload. System-wide
installation, desktop integration, and upgrade from an installed version
remain unverified.

## Before public release

- Align the install4j `APP_VERSION` with the chosen release version. The
  test installers used `1.3.0` from install4j, and the application reports
  `1.3.1` in [`Constants.java`](../src/main/java/com/bayoumi/util/Constants.java).
- Build both Linux installers for that version and verify the generated
  checksums and update descriptor against the final artifacts.
- Test a system-wide Debian install and an upgrade on a supported Linux desktop.

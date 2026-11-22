# Azkar App — Developer Documentation

This directory contains technical documentation, architecture decisions, and research reports for the Azkar application.

## Contents

| Document | Description |
|---|---|
| [System Tray Integration](system-tray/README.md) | How the system tray works on Linux/Windows, platform split (Windows AWT / Linux Dorkbox), and library comparison research. |
| [Arabic rendering on Linux](linux-arabic-rendering.md) | Fixes, verification results, installer smoke tests, and release checks. |

## Quick Links

- **Source**: [TrayUtil.java](../src/main/java/com/bayoumi/util/gui/tray/TrayUtil.java)
- **Dependencies**: [pom.xml](../pom.xml)

## Application Data Paths

Azkar keeps bundled resources under the application directory and chooses the
writable user-data directory at startup:

| Launch type | User-data path |
|---|---|
| Maven development (`target`) | `<project>/jarFiles` |
| install4j launcher | `%LOCALAPPDATA%/Azkar/jarFiles` on Windows, `~/.Azkar/jarFiles` on Linux/macOS |
| Standalone JAR in a writable directory | `jarFiles` beside the JAR |
| Standalone JAR in a non-writable directory | The platform user-data path above |

An existing writable `jarFiles/db/data.db` beside the application remains in
use for compatibility with older portable installations. If that database is
not writable, Azkar does not migrate or open it read-only; it uses the platform
user-data directory. A selected canonical `data.db` path that exists but is not
a regular file causes startup to fail with a clear error.

## Installer Builds

Build the JavaFX application from `Azkar/` before running install4j from the
repository root:

```bash
cd Azkar
mvn -Dmaven.compiler.source=1.8 -Dmaven.compiler.target=1.8 clean test jfx:jar
cd ..
```

Use `--build-ids` to choose media for your target platform. Running plain
`install4jc production.install4j` builds all configured media and requires
the corresponding JRE bundles. `--build-selected` uses the two Windows media
selected in the project.

```bash
# Windows: 64-bit (60) and 32-bit (792) installers
install4jc --test --build-ids=60,792 production.install4j
```

Remove `--test` to create release artifacts. Each build host must have the
pre-created JRE bundles referenced by its selected media in
`production.install4j`.

### Linux installers

Both Linux x86-64 media use `linux-amd64-1.8.0_512_azkar.tar.gz`. Build it
from the ZuluFX 8u512 Linux x64 JDK with its matching OpenJFX sources and
verify the patched runtime before packaging. Run these commands from the
repository root on Linux, with install4j 8 and its `bin` directory on `PATH`.

```bash
ZULU8_JDK=/path/to/zulu-fx-8u512-jdk
PATCHED_ZULU8=/tmp/azkar-zulu8u512-patched
tools/openjfx8-arabic/build-patched-runtime.sh "$ZULU8_JDK" "$PATCHED_ZULU8"
tools/openjfx8-arabic/verify-patched-runtime.sh "$PATCHED_ZULU8" /tmp/azkar-rendering-check

mkdir -p "$HOME/.install4j8/jres"
createbundle --id azkar --output "$HOME/.install4j8/jres" "$PATCHED_ZULU8/jre"
install4jc --build-ids=806,807 production.install4j
```

The verifier needs an X display. See the [runtime patch guide](../../tools/openjfx8-arabic/README.md)
for build dependencies and the ES2 clipping check. If install4j cannot find
the bundle, check the filename and the pre-created JRE directory used by the
compiler. The generated Debian archive must report `amd64` in its
`Architecture` field. The packaged runtime uses the system's GTK, FreeType,
Fontconfig, and Pango libraries.

# Azkar App — Developer Documentation

This directory contains technical documentation, architecture decisions, and research reports for the Azkar application.

## Contents

| Document | Description |
|---|---|
| [System Tray Integration](system-tray/README.md) | How the system tray works on Linux/Windows, platform split (Windows AWT / Linux Dorkbox), and library comparison research. |

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

Running plain `install4jc production.install4j` attempts to build all configured media sets, which requires JRE bundles for all platforms.
The project selects all four release media sets, so `--build-selected` has the
same bundle requirement:

```bash
install4jc --test --build-selected production.install4j
```

To build media sets for your target platform, specify `--build-ids`:

```bash
# Linux: Unix installer (806) and Debian archive (807)
install4jc --test --build-ids=806,807 production.install4j

# Windows: 64-bit (60) and 32-bit (792) installers
install4jc --test --build-ids=60,792 production.install4j
```

Remove `--test` to create release artifacts. Each build host must have the
pre-created JRE bundles referenced by its selected media in
`production.install4j`.

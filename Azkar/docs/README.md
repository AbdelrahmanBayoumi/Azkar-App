# Azkar App — Developer Documentation

This directory contains technical documentation, architecture decisions, and research reports for the Azkar application.

## Contents

| Document | Description |
|---|---|
| [System Tray Integration](system-tray/README.md) | How the system tray works on Linux/Windows, library comparison research, and why we chose dorkbox SystemTray. |

## Quick Links

- **Source**: [TrayUtil.java](../src/main/java/com/bayoumi/util/gui/tray/TrayUtil.java)
- **Dependencies**: [pom.xml](../pom.xml)

## Installer Builds

Build the JavaFX application from `Azkar/` before running install4j from the
repository root:

```bash
cd Azkar
mvn -Dmaven.compiler.source=1.8 -Dmaven.compiler.target=1.8 clean test jfx:jar
cd ..
```

Without explicit IDs, `install4jc` attempts to build all configured media and
requires every platform's JRE bundle. Select the media IDs for the target
platform explicitly:

```bash
# Linux: Unix installer and Debian archive
install4jc --test --build-ids=806,807 production.install4j

# Windows: 64-bit and 32-bit installers
install4jc --test --build-ids=60,792 production.install4j
```

Remove `--test` to create release artifacts. Each build host must have the
pre-created JRE bundles referenced by its selected media in
`production.install4j`.

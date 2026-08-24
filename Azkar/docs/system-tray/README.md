# System Tray Integration

> - **Status**: Platform-split implementation: Java AWT on Windows, [dorkbox SystemTray v3.17](https://github.com/dorkbox/SystemTray) on Linux / macOS
> - **Implementation targets**: Linux (verified on Cinnamon), Windows, macOS
> - **Project-verified environment**: Linux Mint (Cinnamon)
> - **Java Version**: Oracle JDK 8
> - **Last Updated**: August 2026

---

## Table of Contents

1. [Overview](#overview)
2. [How System Trays Work on Linux](#how-system-trays-work-on-linux)
3. [The Problem](#the-problem)
4. [Library Comparison](#library-comparison)
5. [Platform-Specific Architecture](#platform-specific-architecture)
6. [Current Implementation](#current-implementation)
7. [Known Limitations](#known-limitations)
8. [References](#references)

---

## Overview

The Azkar app runs in the system tray so users can close the main window while the app continues to run in the background. The tray icon provides:
- **Left-Click (Windows)** / **Menu → Open (Linux / macOS)**: Show and bring the main window to front.
- **Menu → Exit**: Fully close the application.

On Windows, the application uses Java's built-in `java.awt.SystemTray` to preserve native single left-click restore and right-click context menu. On desktop environments where XEmbed tray managers are absent (such as the tested Linux Mint Cinnamon environment), `java.awt.SystemTray` does not function; Dorkbox SystemTray (using AppIndicator / StatusNotifier) is used instead.

---

## How System Trays Work on Linux

Modern Linux desktops use **two competing protocols** for system tray icons. Understanding this is key to understanding why Java struggles.

### Protocol 1: XEmbed (Legacy — circa 2004)

The original protocol. The application creates a tiny X11 window and "embeds" it into the panel's tray area. The app has full control over the pixels and receives raw mouse events (left-click, right-click, etc.).

- **Used by**: Old GTK2 apps, Java AWT (`java.awt.SystemTray`)
- **Status**: **Deprecated**. Most modern panels no longer run an XEmbed tray manager.

### Protocol 2: StatusNotifier / AppIndicator (Modern — circa 2010)

A DBus-based protocol. The application registers itself as a "StatusNotifierItem" over DBus. The panel reads the icon and menu from the app via DBus messages. The panel owns the rendering and click handling.

- **Used by**: Applications and desktop components that implement StatusNotifier or AppIndicator
- **Status**: A common modern approach. Availability and click behavior depend on the desktop environment and installed extensions.

### What Does Cinnamon (Linux Mint) Actually Use?

We verified this on the target system by running diagnostics:

```bash
# Check for XEmbed tray manager
$ xprop -root _NET_SYSTEM_TRAY_S0
_NET_SYSTEM_TRAY_S0: not found.         # ← No XEmbed!

# Check for StatusNotifier services
$ dbus-send --session --dest=org.freedesktop.DBus \
    --type=method_call --print-reply /org/freedesktop/DBus \
    org.freedesktop.DBus.ListNames | grep StatusNotifier
    "org.x.StatusNotifierWatcher"        # ← xapp-sn-watcher (Cinnamon)
    "org.kde.StatusNotifierWatcher"      # ← compatibility alias
```

**Result on the tested system**: Cinnamon did **not** expose an XEmbed tray manager and provided StatusNotifier services through `xapp-sn-watcher`. In that environment, `java.awt.SystemTray` produced a **black square with no click events**.

---

## The Problem

When running the Azkar app on Linux Mint (Cinnamon), the system tray icon:

1. **Appeared as a black square** — Java AWT creates an XEmbed window, but since there's no XEmbed manager, the icon renders with no transparency and no proper embedding.
2. **Did not respond to any clicks** — Mouse events are never dispatched to the Java process because the XEmbed protocol isn't active.
3. **Could stall startup** — Initializing the tray on the JavaFX Application Thread could make the UI unresponsive. The current implementation performs tray initialization on a background daemon thread.

We also tried forcing GTK2 (`System.setProperty("jdk.gtk.version", "2")`) to see if the older GTK path would work. It did not — the underlying issue is the missing XEmbed tray manager, not the GTK version.

---

## Library Comparison

We evaluated the following options for Java 8 system tray integration on the target Linux environment:

| Library | Protocol | Java 8 | Icon Works | Left-Click → Open | Right-Click → Menu | Standalone |
|---|---|---|---|---|---|---|
| `java.awt.SystemTray` | XEmbed | ✅ | ❌ Black box | ❌ No events | ❌ No events | ✅ |
| `java.awt.SystemTray` + GTK2 | XEmbed | ✅ | ❌ Black box | ❌ No events | ❌ No events | ✅ |
| **dorkbox SystemTray 3.17** | **AppIndicator** | **✅** | **✅** | ❌ Menu only | **✅** | **✅** |
| FXTrayIcon | XEmbed (via AWT) | ✅ | ❌ Same AWT problem | ❌ | ❌ | ✅ |

### Key Findings

- **`java.awt.SystemTray`** did not work on the tested Linux Mint Cinnamon environment because no XEmbed tray manager was available.
- **`FXTrayIcon`** uses AWT internally, so it did not avoid the observed XEmbed behavior.
- **`dorkbox SystemTray 3.17`** worked with Java 8 on the tested Linux environment, rendered the icon correctly, and provided a functional menu. The API used by this project does not expose a distinct left-click callback for the AppIndicator backend.

---

## Platform-Specific Architecture

To provide the best user experience on each platform, `TrayUtil` splits implementation by operating system:

1. **Windows**: Uses native `java.awt.SystemTray`. This preserves the native Windows experience: single left-click instantly restores the window, while right-click displays the context popup menu (Open / Exit).
2. **Non-Windows (Linux / macOS)**: Uses `dorkbox SystemTray 3.17` (AutoDetect). On desktop environments where XEmbed is unavailable, Dorkbox uses AppIndicator / GtkStatusIcon on Linux to render the tray icon and provide a working menu.

---

## Current Implementation

### Architecture

```text
TrayUtil.java
    ├── On Windows: java.awt.SystemTray + java.awt.TrayIcon (MouseListener for single left-click)
    └── On non-Windows: dorkbox SystemTray 3.17 (AutoDetect backend)
```

### Key Design Decisions in TrayUtil.java

1. **Static factory `TrayUtil.init(stage)`** — The tray is initialized via a static method, not a constructor. The constructor is private. This makes the initialization intent explicit at the call site.

2. **Background daemon thread** — Tray initialization is offloaded to a named daemon thread (`Tray-Init-Thread`) to prevent blocking the JavaFX Application Thread.

3. **`volatile` tray fields** — The `tray` (Dorkbox) and `awtTray` (AWT) references are declared `volatile` because they are written on the init thread and read on the JavaFX Application Thread (in the close handler).

4. **Graceful fallback** — If tray initialization fails or is unsupported, `Platform.setImplicitExit(true)` is restored so closing the window exits the app normally.

### File Changes (vs. original Windows-only code)

| File | Change |
|---|---|
| `pom.xml` | Added `com.dorkbox:SystemTray:3.17` dependency |
| `TrayUtil.java` | Platform split: AWT on Windows, Dorkbox on Linux/macOS; background thread init |
| `Launcher.java` | `new TrayUtil(stage)` → `TrayUtil.init(stage)` |

---

## Known Limitations

### 1. No distinct left-click / right-click behavior on Linux
On the tested Linux Mint Cinnamon environment, clicking the tray icon opens the menu. To open the app, users click the icon and then select "Open". Behavior may differ on other desktop environments.

Applications using other tray implementations may support a distinct activation action. The limitation documented here applies to the Dorkbox 3.17 AppIndicator API used by this project.

### 2. Windows behavior requires verification on Windows hosts
The Windows implementation uses `java.awt.SystemTray` with a mouse listener to restore the window on left-click and open the menu on right-click. While verified structurally and against the Java AWT specification, any Windows-specific testing requires running on a Windows environment.

### 3. SLF4J warning in console
Dorkbox depends on SLF4J. Without an SLF4J binding configured, you'll see:
```
SLF4J: Failed to load class "org.slf4j.impl.StaticLoggerBinder".
SLF4J: Defaulting to no-operation (NOP) logger implementation
```
This is harmless and can be suppressed by adding an SLF4J binding (e.g., `slf4j-simple`) or by configuring your logging framework.

---

## References

### Protocol Documentation
- [FreeDesktop StatusNotifierItem Specification](https://www.freedesktop.org/wiki/Specifications/StatusNotifierItem/)
- [XEmbed Protocol (legacy)](https://specifications.freedesktop.org/xembed-spec/latest/)
- [xapp-sn-watcher — Cinnamon's SNI service](https://github.com/linuxmint/xapp)

### Libraries
- [dorkbox/SystemTray](https://github.com/dorkbox/SystemTray) — Cross-platform tray (used)
- [dustinkredmond/FXTrayIcon](https://github.com/dustinkredmond/FXTrayIcon) — JavaFX tray wrapper (not suitable)

### Issues & Discussions
- [dorkbox/SystemTray #78: OnClick handler for indicator](https://github.com/dorkbox/SystemTray/issues/78) — Closed issue about indicator activation
- [Qt QSystemTrayIcon::activated](https://doc.qt.io/qt-5/qsystemtrayicon.html#activated) — How Qt handles left-click via `Trigger` reason

### System Diagnostics (Linux Mint Cinnamon)
```bash
# Verify no XEmbed tray exists
$ xprop -root _NET_SYSTEM_TRAY_S0
_NET_SYSTEM_TRAY_S0: not found.

# Verify StatusNotifier is the active protocol
$ dbus-send --session --dest=org.freedesktop.DBus \
    --type=method_call --print-reply /org/freedesktop/DBus \
    org.freedesktop.DBus.ListNames | grep StatusNotifier
  "org.x.StatusNotifierWatcher"
  "org.kde.StatusNotifierWatcher"

# Verify ayatana-appindicator libraries are installed (used by dorkbox via JNA)
$ dpkg -l 'libayatana*' | grep "^ii"
ii libayatana-appindicator3-1  0.5.93-1build3  amd64  Ayatana Application Indicators (GTK-3+)
ii libayatana-indicator3-7     0.9.4-1build1   amd64  panel indicator applet - shared library

# Verify CopyQ uses Qt + DBusMenu (not XEmbed)
$ ldd $(which copyq) | grep dbus
  libdbusmenu-qt5.so.2 => /lib/x86_64-linux-gnu/libdbusmenu-qt5.so.2
```

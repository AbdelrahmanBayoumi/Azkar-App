# System Tray Integration

> **Status**: Implemented using [dorkbox SystemTray v3.17](https://github.com/dorkbox/SystemTray)  
> **Platforms**: Linux (Cinnamon/GNOME/KDE), Windows, macOS  
> **Java Version**: Oracle JDK 8  
> **Last Updated**: March 2026

---

## Table of Contents

1. [Overview](#overview)
2. [How System Trays Work on Linux](#how-system-trays-work-on-linux)
3. [The Problem](#the-problem)
4. [Library Comparison](#library-comparison)
5. [Why We Chose Dorkbox](#why-we-chose-dorkbox)
6. [Current Implementation](#current-implementation)
7. [Known Limitations](#known-limitations)
8. [Future Improvements (Java 21)](#future-improvements-java-21)
9. [References](#references)

---

## Overview

The Azkar app runs in the system tray so users can close the main window while the app continues to run in the background. The tray icon provides:
- **Menu → Open**: Show the main window.
- **Menu → Exit**: Fully close the application.

On **Windows**, this has always worked using Java's built-in `java.awt.SystemTray`. On **Linux**, the built-in approach is fundamentally broken on modern desktop environments. This document explains why and what we did about it.

---

## How System Trays Work on Linux

Modern Linux desktops use **two competing protocols** for system tray icons. Understanding this is key to understanding why Java struggles.

### Protocol 1: XEmbed (Legacy — circa 2004)

The original protocol. The application creates a tiny X11 window and "embeds" it into the panel's tray area. The app has full control over the pixels and receives raw mouse events (left-click, right-click, etc.).

- **Used by**: Old GTK2 apps, Java AWT (`java.awt.SystemTray`)
- **Status**: **Deprecated**. Most modern panels no longer run an XEmbed tray manager.

### Protocol 2: StatusNotifier / AppIndicator (Modern — circa 2010)

A DBus-based protocol. The application registers itself as a "StatusNotifierItem" over DBus. The panel reads the icon and menu from the app via DBus messages. The panel owns the rendering and click handling.

- **Used by**: Qt apps (CopyQ, Telegram), Electron apps (Discord, Slack), GTK3+ apps
- **Status**: **Active standard**. Used by Cinnamon, GNOME, KDE Plasma, XFCE.

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

**Result**: Cinnamon does **NOT** run an XEmbed tray manager. It exclusively uses StatusNotifier via its `xapp-sn-watcher` service. This means `java.awt.SystemTray` has nowhere to embed its icon — which is why it shows up as a **black square with no click events**.

---

## The Problem

When running the Azkar app on Linux Mint (Cinnamon), the system tray icon:

1. **Appeared as a black square** — Java AWT creates an XEmbed window, but since there's no XEmbed manager, the icon renders with no transparency and no proper embedding.
2. **Did not respond to any clicks** — Mouse events are never dispatched to the Java process because the XEmbed protocol isn't active.
3. **Caused the app to freeze** — During startup, Java probes for X11 tray atoms (`_NET_SYSTEM_TRAY_S0`). When they don't exist, the probe blocks the JavaFX Application Thread, causing the OS to mark the app "Not Responding".

We also tried forcing GTK2 (`System.setProperty("jdk.gtk.version", "2")`) to see if the older GTK path would work. It did not — the underlying issue is the missing XEmbed tray manager, not the GTK version.

---

## Library Comparison

We evaluated every available option for Java 8 system tray integration on Linux:

| Library | Protocol | Java 8 | Icon Works | Left-Click → Open | Right-Click → Menu | Standalone |
|---|---|---|---|---|---|---|
| `java.awt.SystemTray` | XEmbed | ✅ | ❌ Black box | ❌ No events | ❌ No events | ✅ |
| `java.awt.SystemTray` + GTK2 | XEmbed | ✅ | ❌ Black box | ❌ No events | ❌ No events | ✅ |
| **dorkbox SystemTray 3.17** | **AppIndicator** | **✅** | **✅** | ❌ Menu only | **✅** | **✅** |
| dorkbox SystemTray 4.4 | AppIndicator | ❌ (Java 11+) | N/A | N/A | N/A | ✅ |
| FXTrayIcon | XEmbed (via AWT) | ✅ | ❌ Same AWT problem | ❌ | ❌ | ✅ |
| kropp/java-statusnotifier | StatusNotifier (DBus) | ❌ (Java 17+) | ✅ | ✅ Has `Activate` | ✅ | ✅ |
| DIY with dbus-java 3.x | StatusNotifier (DBus) | ✅ | ✅ | ✅ | ✅ | ✅ |

### Key Findings

- **`java.awt.SystemTray`** is permanently broken on Linux Mint Cinnamon because the XEmbed protocol is not available.
- **`FXTrayIcon`** wraps AWT internally, so it inherits the exact same XEmbed problem.
- **`kropp/java-statusnotifier`** is the "perfect" library — it implements StatusNotifierItem directly over DBus with full `Activate` (left-click) support. But it requires **Java 17+**.
- **DIY with `dbus-java 3.x`** would work on Java 8, but requires writing ~500–800 lines of DBus boilerplate (interface definitions, property handling, service registration, menu protocol).
- **`dorkbox SystemTray 3.17`** is the only library that works on Java 8, renders the icon correctly, and provides a functional menu — but it does not expose a left-click callback.

---

## Why We Chose Dorkbox

> [!IMPORTANT]
> **Architectural Directive for Contributors & AI Assistants:**
> The current project decision is to use **Dorkbox SystemTray 3.17** as the single, unified system tray backend across all supported platforms (Windows, Linux, and macOS).
> A unified backend reduces platform-specific code, testing overhead, and interaction risks between JavaFX Application Thread and multiple tray implementations. The previous direct left-click behavior on Windows is intentionally traded for a consistent menu-based interaction across all operating systems.

### Decision: Accept `dorkbox SystemTray 3.17` with menu-only interaction

**Pros:**
- ✅ Icon renders correctly on Linux with full transparency
- ✅ Menu (Open / Exit) works reliably
- ✅ Compatible with Java 8 (Oracle JDK 1.8.0_221)
- ✅ Standalone — uses JNA internally to call `libayatana-appindicator3.so` which is already installed on Linux Mint. No extra user packages needed.
- ✅ Cross-platform — works on Windows and macOS too
- ✅ Well-maintained library (489 stars, active development)

**Cons:**
- ❌ Left-click on the icon opens the menu instead of directly opening the app. User must click "Open" from the menu.
- ❌ This is a library design choice (not a platform limitation). dorkbox intentionally does not expose click callbacks because AppIndicator behavior varies across desktop environments.
- ❌ Adds ~3MB of transitive dependencies (JNA, jna-platform, SLF4J)

**Why not DIY?**
Writing a raw DBus StatusNotifierItem implementation for Java 8 would give us left-click support, but the maintenance cost is disproportionate to the benefit. It's 500+ lines of protocol-level code for a single UX improvement (one fewer click to open the app). This doesn't justify the risk for a stable production app.

**Why not upgrade Java?**
The project is built with Oracle JDK 8, packaged with install4j, and distributed to end users. Upgrading the JDK is a separate project that impacts the entire build pipeline. When we do upgrade to **Java 21**, we should revisit this decision (see [Future Improvements](#future-improvements-java-21)).

---

## Current Implementation

### Architecture

```
┌──────────────────┐     ┌──────────────────────┐     ┌───────────────┐
│  TrayUtil.java   │     │  dorkbox SystemTray   │     │  Cinnamon     │
│  (our code)      │────▶│  v3.17 (AppIndicator) │────▶│  Panel        │
│                  │     │  via JNA → libayatana  │     │               │
└──────────────────┘     └──────────────────────┘     └───────────────┘
```

### Key Design Decisions in TrayUtil.java

1. **Static factory `TrayUtil.init(stage)`** — The tray is initialized via a static method, not a constructor. The constructor is private. This makes the initialization intent explicit at the call site.

2. **Background daemon thread** — Tray initialization is offloaded to a named daemon thread (`Tray-Init-Thread`) to prevent blocking the JavaFX Application Thread.

3. **`volatile` tray field** — The `SystemTray` reference is declared `volatile` because it's written on the init thread and read on the JavaFX thread (in the close handler).

4. **Graceful fallback** — If `SystemTray.get()` returns null (unsupported platform), `Platform.setImplicitExit(true)` is restored so closing the window exits the app normally.

### File Changes (vs. original Windows-only code)

| File | Change |
|---|---|
| `pom.xml` | Added `com.dorkbox:SystemTray:3.17` dependency |
| `TrayUtil.java` | Complete rewrite: AWT → dorkbox, constructor → static init |
| `Launcher.java` | `new TrayUtil(stage)` → `TrayUtil.init(stage)` |

---

## Known Limitations

### 1. No distinct left-click / right-click behavior on Linux
On Linux, clicking the tray icon (left or right) always opens the menu. To open the app, users must click the icon then click "Open". This is because dorkbox uses the AppIndicator protocol which routes all interactions through the menu.

Note: Other apps like CopyQ and AnyDesk can distinguish left/right clicks because they are written in C++/Qt which talks directly to the StatusNotifier DBus interface and handles the `Activate` signal natively. This is not a Linux limitation — it's a Java library limitation.

### 2. Windows behavior change
On Windows, the original AWT implementation supported left-click to open the app directly and right-click for the menu. With dorkbox, both clicks now show the menu. This is a minor UX regression for Windows users (one extra click).

### 3. SLF4J warning in console
Dorkbox depends on SLF4J. Without an SLF4J binding configured, you'll see:
```
SLF4J: Failed to load class "org.slf4j.impl.StaticLoggerBinder".
SLF4J: Defaulting to no-operation (NOP) logger implementation
```
This is harmless and can be suppressed by adding an SLF4J binding (e.g., `slf4j-simple`) or by configuring your logging framework.

---

## Future Improvements (Java 21)

When the project upgrades to Java 21, we should revisit the tray implementation:

### Option A: Use `kropp/java-statusnotifier`
- **Repo**: https://github.com/kropp/java-statusnotifier
- **What it does**: Pure Java implementation of the FreeDesktop StatusNotifierItem specification.
- **Benefit**: Full support for `Activate` (left-click), `ContextMenu` (right-click), and `SecondaryActivate`.
- **Requires**: Java 17+, `dbus-java-core:4.x`

### Option B: Use `dbus-java 4.x` directly
- **Repo**: https://github.com/hypfvieh/dbus-java
- **What it does**: Low-level DBus bindings for Java.
- **Benefit**: Full control over StatusNotifierItem registration and signal handling.
- **Requires**: Java 11+, more boilerplate than Option A.

### Option C: Platform-aware implementation
Use dorkbox on Windows (where it works great) and `kropp/java-statusnotifier` on Linux (for left-click support). Detect the OS at runtime:
```java
if (System.getProperty("os.name").toLowerCase().contains("linux")) {
    // Use StatusNotifierItem via dbus-java
} else {
    // Use dorkbox SystemTray
}
```

### Recommendation
**Option A** (`kropp/java-statusnotifier`) is the cleanest path. It handles exactly what we need with minimal code. Combine with **Option C** (platform detection) for the best UX on both Windows and Linux.

---

## References

### Protocol Documentation
- [FreeDesktop StatusNotifierItem Specification](https://www.freedesktop.org/wiki/Specifications/StatusNotifierItem/)
- [XEmbed Protocol (legacy)](https://specifications.freedesktop.org/xembed-spec/latest/)
- [xapp-sn-watcher — Cinnamon's SNI service](https://github.com/linuxmint/xapp)

### Libraries
- [dorkbox/SystemTray](https://github.com/dorkbox/SystemTray) — Cross-platform tray (used)
- [kropp/java-statusnotifier](https://github.com/kropp/java-statusnotifier) — Java StatusNotifierItem (future)
- [hypfvieh/dbus-java](https://github.com/hypfvieh/dbus-java) — Java DBus bindings
- [dustinkredmond/FXTrayIcon](https://github.com/dustinkredmond/FXTrayIcon) — JavaFX tray wrapper (not suitable)

### Issues & Discussions
- [dorkbox/SystemTray #78: OnClick handler for indicator](https://github.com/dorkbox/SystemTray/issues/78) — Open issue requesting left-click support (no resolution)
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

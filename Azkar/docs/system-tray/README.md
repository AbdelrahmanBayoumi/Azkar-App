# System Tray Integration

Azkar uses two tray backends:

- Windows: `java.awt.SystemTray`
- Linux: `dorkbox SystemTray 3.17`

Linux Mint Cinnamon is the verified Linux target for this branch.

## Behavior

- Windows: AWT left-click opens the window directly, and the popup menu provides `Open` and `Exit`.
- Linux: Dorkbox provides an `Open`/`Exit` tray menu.

## Platform Notes

- Windows keeps the built-in AWT tray behavior.
- Linux uses Dorkbox through `TrayUtil.init(stage)`.
- Some Linux desktops need AppIndicator/Ayatana support installed or enabled for tray icons to appear correctly.

## Code Paths

- `Launcher.java` initializes the tray from `start(Stage)`.
- `TrayUtil.java` selects the backend by `os.name`.
- `pom.xml` declares `com.dorkbox:SystemTray:3.17`.

## Verified Environment

- Linux Mint Cinnamon
- Java 8

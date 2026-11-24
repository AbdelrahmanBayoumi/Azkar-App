package com.azkar.ui.notifications

import de.infix.testBalloon.framework.core.testSuite
import org.assertj.core.api.Assertions.assertThat

// TestBalloon generates the test entry point, but the IDE does not see that usage.
@Suppress("unused")
val DesktopPlatformSuite by testSuite("Desktop platform detection") {
    test("recognizes the supported operating system names") {
        assertThat(desktopPlatform("Windows 11")).isEqualTo(DesktopPlatform.WINDOWS)
        assertThat(desktopPlatform("Mac OS X")).isEqualTo(DesktopPlatform.MACOS)
        assertThat(desktopPlatform("Darwin")).isEqualTo(DesktopPlatform.MACOS)
        assertThat(desktopPlatform("Linux")).isEqualTo(DesktopPlatform.LINUX)
    }

    test("leaves unsupported operating systems unknown") {
        assertThat(desktopPlatform("FreeBSD")).isEqualTo(DesktopPlatform.UNKNOWN)
        assertThat(desktopPlatform("")).isEqualTo(DesktopPlatform.UNKNOWN)
    }

    test("a headless process never assumes a tray is available") {
        DesktopPlatform.entries.forEach { platform ->
            assertThat(trayAvailable(headless = true, platform = platform, sessionBusAddress = "session"))
                .isFalse()
        }
    }

    test("Linux requires a session bus, while Windows and macOS use the OS tray") {
        assertThat(trayAvailable(false, DesktopPlatform.LINUX, null)).isFalse()
        assertThat(trayAvailable(false, DesktopPlatform.LINUX, "  ")).isFalse()
        assertThat(trayAvailable(false, DesktopPlatform.LINUX, "unix:path=/run/user/1000/bus")).isTrue()
        assertThat(trayAvailable(false, DesktopPlatform.WINDOWS, null)).isTrue()
        assertThat(trayAvailable(false, DesktopPlatform.MACOS, null)).isTrue()
        assertThat(trayAvailable(false, DesktopPlatform.UNKNOWN, null)).isFalse()
    }
}

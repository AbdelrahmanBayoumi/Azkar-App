package com.azkar.ui.shell

import com.azkar.ui.notifications.DesktopPlatform
import de.infix.testBalloon.framework.core.testSuite
import org.assertj.core.api.Assertions.assertThat

// TestBalloon generates the test entry point, but the IDE does not see that usage.
@Suppress("unused")
val TrayIconSizeSuite by testSuite("Native tray icon sizes") {
    test("uses the platform-specific tray dimensions") {
        assertThat(trayIconSize(DesktopPlatform.LINUX)).isEqualTo(TrayIconSize(22, 22))
        assertThat(trayIconSize(DesktopPlatform.WINDOWS)).isEqualTo(TrayIconSize(16, 16))
        assertThat(trayIconSize(DesktopPlatform.MACOS)).isEqualTo(TrayIconSize(22, 22))
        assertThat(trayIconSize(DesktopPlatform.UNKNOWN)).isEqualTo(TrayIconSize(32, 32))
    }
}

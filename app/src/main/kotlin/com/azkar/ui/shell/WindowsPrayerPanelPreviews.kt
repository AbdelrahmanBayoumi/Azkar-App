package com.azkar.ui.shell

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.azkar.generated.resources.Res
import com.azkar.generated.resources.trayMenuHidePanel
import com.azkar.generated.resources.trayMenuOpen
import com.azkar.generated.resources.trayMenuPause
import com.azkar.generated.resources.trayMenuQuit
import com.azkar.ui.components.ActionIcons
import com.azkar.ui.components.AzkarIcons
import com.azkar.ui.home.PrayerKey
import com.azkar.ui.home.PrayerRowState
import com.azkar.ui.home.nameResource
import com.azkar.ui.notifications.DesktopPlatform
import com.azkar.ui.theme.AppTypography
import com.azkar.ui.theme.AzkarThemePreview
import org.jetbrains.compose.resources.stringResource
import java.util.Locale

@Preview(widthDp = 264, heightDp = 248)
@Composable
internal fun WindowsPrayerSchedulePreview() {
    AzkarThemePreview(darkTheme = true, width = 264.dp, height = 248.dp) {
        WindowsPrayerSchedule(
            previewPrayerStatus(),
            onDismiss = {},
            onOpen = {},
        )
    }
}

@Preview(widthDp = 264, heightDp = 228)
@Composable
internal fun WindowsPrayerActionsPreview() {
    AzkarThemePreview(darkTheme = true, width = 264.dp, height = 228.dp) {
        WindowsTaskbarPositionMenu(
            manual = false,
            content =
                previewPrayerMenu(DesktopPlatform.WINDOWS),
            onDismiss = {},
            onMove = {},
            onAutomatic = {},
        )
    }
}

@Composable
internal fun previewPrayerStatus(): DesktopPrayerStatus {
    val times = listOf("4:51 AM", "11:30 AM", "2:19 PM", "4:39 PM", "5:59 PM")
    val prayers =
        PrayerKey.entries.zip(times).map { (key, time) ->
            PrayerRowState(key, stringResource(key.nameResource), time, time, key == PrayerKey.DHUHR, true)
        }
    val next = prayers.first { it.isNext }
    return DesktopPrayerStatus(next.name, next.timeLabel, "2h 33m", "", prayers, false)
}

@Composable
internal fun previewPrayerMenu(platform: DesktopPlatform): PrayerTrayMenuContent =
    PrayerTrayMenuContent(
        countdown = TrayMenuEntry("", AzkarIcons.Clock, {}, isEnabled = false),
        openApp = TrayMenuEntry(stringResource(Res.string.trayMenuOpen), AzkarIcons.Home, {}),
        reminders = TrayMenuEntry(stringResource(Res.string.trayMenuPause), AzkarIcons.BellOff, {}),
        quit = TrayMenuEntry(stringResource(Res.string.trayMenuQuit), ActionIcons.dismiss, {}),
        panelVisibility =
            TrayMenuEntry(
                stringResource(Res.string.trayMenuHidePanel),
                Icons.Filled.VisibilityOff,
                {},
            ),
        iconRenderProperties = trayIconRenderProperties(platform),
        schedule = prayerTraySchedule(previewPrayerStatus(), {}),
    )

@Preview(widthDp = 174, heightDp = 40)
@Composable
internal fun WindowsTaskbarWidgetPreview() {
    AzkarThemePreview(darkTheme = true, width = 174.dp, height = 40.dp) {
        val prayer = previewPrayerStatus()
        WindowsTaskbarSummary(
            WindowsTaskbarContent(
                prayer.prayerName,
                prayer.timeLabel,
                prayer.remainingLabel,
                Locale.ENGLISH,
                AppTypography(),
            ),
            dark = true,
            highlighted = false,
            vertical = false,
        )
    }
}

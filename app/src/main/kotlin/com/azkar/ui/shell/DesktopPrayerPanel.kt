package com.azkar.ui.shell

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import com.azkar.generated.resources.Res
import com.azkar.generated.resources.settingsDesktopPrayer
import com.azkar.generated.resources.settingsDesktopPrayerExtension
import com.azkar.generated.resources.settingsDesktopPrayerExtensionHint
import com.azkar.generated.resources.settingsDesktopPrayerPosition
import com.azkar.generated.resources.settingsDesktopPrayerPositionHint
import com.azkar.generated.resources.settingsDesktopPrayerTaskbar
import com.azkar.generated.resources.taskbarPositionAutomatic
import com.azkar.ui.components.AzkarOutlinedButton
import com.azkar.ui.components.AzkarToggleSwitch
import com.azkar.ui.notifications.DesktopPlatform
import com.azkar.ui.notifications.NotificationPreferencesRepository
import com.azkar.ui.theme.AzkarSpacing
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import java.nio.file.Files
import java.nio.file.Path

internal sealed class DesktopPrayerPanel(
    val settingHint: StringResource,
) {
    /** True when this platform handles the desktop entry and its tray fallback. */
    @Composable
    fun Mount(
        status: State<DesktopPrayerStatus?>,
        appState: AzkarAppState,
        tray: ReminderTrayContent,
        hidden: Boolean,
    ): Boolean {
        val preferences = koinInject<NotificationPreferencesRepository>()
        val enabled by preferences.desktopPrayerEnabled.collectAsState()
        val mode =
            when {
                !enabled -> DesktopPrayerPanelMode.DISABLED
                hidden -> DesktopPrayerPanelMode.HIDDEN
                else -> DesktopPrayerPanelMode.VISIBLE
            }
        return Show(status, appState, tray, mode)
    }

    @Composable
    protected abstract fun Show(
        status: State<DesktopPrayerStatus?>,
        appState: AzkarAppState,
        tray: ReminderTrayContent,
        mode: DesktopPrayerPanelMode,
    ): Boolean

    companion object {
        val current: DesktopPrayerPanel? by lazy {
            when (DesktopPlatform.current) {
                DesktopPlatform.WINDOWS -> windowsPrayerPanel(System.getProperty("os.name"))
                DesktopPlatform.LINUX -> if (gnomePanelAvailable()) GnomePrayerPanel() else null
                else -> null
            }
        }
    }
}

internal enum class DesktopPrayerPanelMode {
    VISIBLE,
    HIDDEN,
    DISABLED,
}

internal fun windowsPrayerPanel(osName: String?): WindowsPrayerPanel? =
    when (osName) {
        "Windows 10", "Windows 11" -> WindowsPrayerPanel()
        else -> null
    }

private fun gnomePanelAvailable(): Boolean {
    val desktop = System.getenv("XDG_CURRENT_DESKTOP").orEmpty()
    val osRelease = runCatching { Files.readString(Path.of("/etc/os-release")) }.getOrDefault("")
    return isGnomePrayerPanelSupported(desktop, osRelease)
}

internal fun isGnomePrayerPanelSupported(
    desktop: String,
    osRelease: String,
): Boolean {
    if (desktop.split(':').none { it.equals("GNOME", ignoreCase = true) }) return false
    val fields =
        osRelease
            .lineSequence()
            .mapNotNull { line ->
                val separator = line.indexOf('=')
                if (separator < 1) null else line.take(separator) to line.substring(separator + 1).trim('"', '\'')
            }.toMap()
    return when (fields["ID"]) {
        "fedora" -> true
        "ubuntu" -> (fields["VERSION_ID"]?.substringBefore('.')?.toIntOrNull() ?: 0) >= 24
        else -> false
    }
}

@Composable
internal fun DesktopPrayerPanelSetting() {
    val panel = DesktopPrayerPanel.current ?: return
    val preferences = koinInject<NotificationPreferencesRepository>()
    val enabled by preferences.desktopPrayerEnabled.collectAsState()
    val scope = rememberCoroutineScope()
    val title =
        if (panel is WindowsPrayerPanel) Res.string.settingsDesktopPrayerTaskbar else Res.string.settingsDesktopPrayer
    SettingsSectionBlock(
        title = stringResource(title),
        subtitle = stringResource(panel.settingHint),
        headingAction = {
            AzkarToggleSwitch(
                checked = enabled,
                onCheckedChange = { checked -> scope.launch { preferences.setDesktopPrayerEnabled(checked) } },
            )
        },
    )
    if (panel is WindowsPrayerPanel && enabled) WindowsPrayerPositionSetting(preferences)
    if (panel is GnomePrayerPanel && enabled) {
        val extensionEnabled by preferences.gnomePrayerExtensionEnabled.collectAsState()
        SettingsSectionBlock(
            title = stringResource(Res.string.settingsDesktopPrayerExtension),
            subtitle = stringResource(Res.string.settingsDesktopPrayerExtensionHint),
            modifier = Modifier.padding(top = AzkarSpacing.md),
            headingAction = {
                AzkarToggleSwitch(
                    checked = extensionEnabled,
                    onCheckedChange = { checked ->
                        scope.launch { preferences.setGnomePrayerExtensionEnabled(checked) }
                    },
                )
            },
        )
    }
}

@Composable
private fun WindowsPrayerPositionSetting(preferences: NotificationPreferencesRepository) {
    val position by preferences.windowsTaskbarPosition.collectAsState()
    val scope = rememberCoroutineScope()
    SettingsSectionBlock(
        title = stringResource(Res.string.settingsDesktopPrayerPosition),
        subtitle = stringResource(Res.string.settingsDesktopPrayerPositionHint),
        modifier = Modifier.padding(top = AzkarSpacing.md),
        headingAction = {
            if (position != null) {
                AzkarOutlinedButton(
                    text = stringResource(Res.string.taskbarPositionAutomatic),
                    onClick = { scope.launch { preferences.setWindowsTaskbarPosition(null) } },
                )
            }
        },
    )
}

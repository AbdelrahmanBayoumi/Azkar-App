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
import com.azkar.generated.resources.settingsDesktopPrayerStandardPanel
import com.azkar.generated.resources.settingsDesktopPrayerTaskbar
import com.azkar.generated.resources.settingsDesktopPrayerTray
import com.azkar.generated.resources.settingsPrayerPanelMenuCompact
import com.azkar.generated.resources.settingsPrayerPanelMenuExtended
import com.azkar.generated.resources.settingsPrayerPanelMenuHint
import com.azkar.generated.resources.settingsPrayerTrayMenu
import com.azkar.generated.resources.settingsStandardPrayerPanelMenu
import com.azkar.generated.resources.settingsStandardPrayerPanelMenuHint
import com.azkar.ui.components.AzkarToggleSwitch
import com.azkar.ui.components.SelectRow
import com.azkar.ui.notifications.DesktopPlatform
import com.azkar.ui.notifications.GnomePrayerDisplay
import com.azkar.ui.notifications.NotificationPreferencesRepository
import com.azkar.ui.notifications.PrayerMenuLayout
import com.azkar.ui.notifications.PrayerMenuSurface
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
        val enabled =
            if (this is GnomePrayerPanel) {
                preferences.gnomePrayerDisplay.collectAsState().value != GnomePrayerDisplay.TRAY
            } else {
                preferences.desktopPrayerEnabled.collectAsState().value
            }
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

internal val prayerTrayLayoutAvailable: Boolean
    get() = DesktopPlatform.current == DesktopPlatform.WINDOWS || DesktopPlatform.current == DesktopPlatform.LINUX

@Composable
internal fun DesktopPrayerPanelSetting() {
    val panel = DesktopPrayerPanel.current
    val preferences = koinInject<NotificationPreferencesRepository>()
    when (panel) {
        is GnomePrayerPanel -> {
            val display by preferences.gnomePrayerDisplay.collectAsState()
            GnomePrayerDisplaySetting(panel, preferences, display)
            when (display) {
                GnomePrayerDisplay.PANEL ->
                    PrayerMenuLayoutSetting(
                        preferences,
                        PrayerMenuSurface.PANEL,
                        Res.string.settingsStandardPrayerPanelMenu,
                    )
                GnomePrayerDisplay.TRAY ->
                    PrayerMenuLayoutSetting(preferences, PrayerMenuSurface.TRAY, Res.string.settingsPrayerTrayMenu)
                GnomePrayerDisplay.EXTENSION -> Unit
            }
            return
        }
        is WindowsPrayerPanel -> {
            val enabled by preferences.desktopPrayerEnabled.collectAsState()
            val scope = rememberCoroutineScope()
            SettingsSectionBlock(
                title = stringResource(Res.string.settingsDesktopPrayerTaskbar),
                subtitle = stringResource(panel.settingHint),
                headingAction = {
                    AzkarToggleSwitch(
                        checked = enabled,
                        onCheckedChange = { checked -> scope.launch { preferences.setDesktopPrayerEnabled(checked) } },
                    )
                },
            )
        }
        null -> Unit
    }
    if (prayerTrayLayoutAvailable) {
        PrayerMenuLayoutSetting(preferences, PrayerMenuSurface.TRAY, Res.string.settingsPrayerTrayMenu)
    }
}

@Composable
private fun GnomePrayerDisplaySetting(
    panel: GnomePrayerPanel,
    preferences: NotificationPreferencesRepository,
    display: GnomePrayerDisplay,
) {
    val scope = rememberCoroutineScope()
    SelectRow(
        label = stringResource(Res.string.settingsDesktopPrayer),
        hint = stringResource(panel.settingHint),
        selected = display,
        options = GnomePrayerDisplay.entries,
        optionLabel = {
            stringResource(
                when (it) {
                    GnomePrayerDisplay.EXTENSION -> Res.string.settingsDesktopPrayerExtension
                    GnomePrayerDisplay.PANEL -> Res.string.settingsDesktopPrayerStandardPanel
                    GnomePrayerDisplay.TRAY -> Res.string.settingsDesktopPrayerTray
                },
            )
        },
        onSelect = { scope.launch { preferences.setGnomePrayerDisplay(it) } },
    )
}

@Composable
private fun PrayerMenuLayoutSetting(
    preferences: NotificationPreferencesRepository,
    surface: PrayerMenuSurface,
    label: StringResource,
) {
    val panel = surface == PrayerMenuSurface.PANEL
    val hint = if (panel) Res.string.settingsStandardPrayerPanelMenuHint else Res.string.settingsPrayerPanelMenuHint
    val layout by (if (panel) preferences.prayerPanelMenuLayout else preferences.prayerTrayMenuLayout).collectAsState()
    val scope = rememberCoroutineScope()
    SelectRow(
        label = stringResource(label),
        hint = stringResource(hint),
        selected = layout,
        options = PrayerMenuLayout.entries,
        optionLabel = {
            stringResource(
                when (it) {
                    PrayerMenuLayout.COMPACT -> Res.string.settingsPrayerPanelMenuCompact
                    PrayerMenuLayout.EXTENDED -> Res.string.settingsPrayerPanelMenuExtended
                },
            )
        },
        onSelect = { scope.launch { preferences.setPrayerMenuLayout(surface, it) } },
        modifier = Modifier.padding(top = AzkarSpacing.md),
    )
}

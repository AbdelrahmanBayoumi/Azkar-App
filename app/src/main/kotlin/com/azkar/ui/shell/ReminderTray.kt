package com.azkar.ui.shell

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.azkar.generated.resources.Res
import com.azkar.generated.resources.asr
import com.azkar.generated.resources.nextPrayerLabel
import com.azkar.generated.resources.title
import com.azkar.generated.resources.todayPrayerTimes
import com.azkar.generated.resources.trayMenuHidePanel
import com.azkar.generated.resources.trayMenuOpen
import com.azkar.generated.resources.trayMenuPause
import com.azkar.generated.resources.trayMenuQuit
import com.azkar.generated.resources.trayMenuResume
import com.azkar.generated.resources.trayMenuShowPanel
import com.azkar.generated.resources.trayNextPrayerCountdown
import com.azkar.generated.resources.trayPanelHiddenMessage
import com.azkar.ui.components.ActionIcons
import com.azkar.ui.components.AzkarIcons
import com.azkar.ui.notifications.DesktopPlatform
import com.azkar.ui.notifications.NotificationAction
import com.azkar.ui.notifications.NotificationActionRouter
import com.azkar.ui.notifications.NotificationNotice
import com.azkar.ui.notifications.NotificationPreferencesRepository
import com.azkar.ui.notifications.PrayerMenuLayout
import com.azkar.ui.notifications.presentation.NotificationPublisher
import com.azkar.ui.theme.AzkarVariantsPreview
import dev.nucleusframework.composenativetray.menu.api.TrayMenuBuilder
import dev.nucleusframework.composenativetray.tray.api.NativeTray
import dev.nucleusframework.composenativetray.utils.IconRenderProperties
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import java.awt.GraphicsEnvironment
import com.azkar.ui.notifications.trayAvailable as isTrayAvailable

private val logger = KotlinLogging.logger {}

internal data class TrayIconSize(
    val width: Int,
    val height: Int,
)

internal fun trayIconSize(platform: DesktopPlatform): TrayIconSize =
    when (platform) {
        DesktopPlatform.LINUX -> TrayIconSize(22, 22)
        DesktopPlatform.WINDOWS -> TrayIconSize(16, 16)
        DesktopPlatform.MACOS -> TrayIconSize(22, 22)
        DesktopPlatform.UNKNOWN -> TrayIconSize(32, 32)
    }

internal fun trayIconRenderProperties(platform: DesktopPlatform): IconRenderProperties {
    val iconSize = trayIconSize(platform)
    return IconRenderProperties(
        sceneWidth = TRAY_ICON_RENDER_SIZE,
        sceneHeight = TRAY_ICON_RENDER_SIZE,
        sceneDensity = Density(TRAY_ICON_RENDER_DENSITY),
        targetWidth = iconSize.width,
        targetHeight = iconSize.height,
        jvmOwnsDownscaling = false,
    )
}

internal data class TrayMenuActions(
    val openApp: () -> Unit,
    val quit: () -> Unit,
    val panelHidden: () -> Unit,
)

/**
 * The tray icon and its native context menu.
 *
 * Drawn from Compose rather than assembled on an AWT tray: the icon is the app's own brand mark, so
 * it is the same vector the window and the header use, and the menu is a DSL that recomposes with
 * the state it reads. That matters for a tray that lives longer than the window — the menu labels
 * follow the app's language, and nothing here has to be rebuilt by hand when they change. AWT could
 * show neither: it renders the Windows 95-era menu on every platform, and its labels are whatever
 * string the code passed in at construction time.
 *
 * The OS supplies the mechanics: `NSStatusItem` on macOS, `Shell_NotifyIcon` on Windows, and a
 * `StatusNotifierItem` over D-Bus on Linux. Left-click runs [primaryAction], which routes whatever
 * the last notification pointed at; right-click opens the menu. Without a primary action the menu
 * would open on every click, which is not what a click on the icon should do.
 *
 * Nucleus 2.1.7 rebuilds a supplied Linux menu on every tray update. The countdown still checks
 * the wall clock each second, but native tray updates happen only when the displayed minute or
 * menu state changes.
 *
 * The menu carries the same line as a disabled row at the top: reading is its only job, so nothing
 * is clickable that has nothing to do.
 *
 * This is a composable, so it exists exactly as long as the application composition does. That is
 * what makes close-to-tray safe: the icon is still registered while the window is hidden, and the
 * library removes it when the composition goes away, without a teardown step having to get it right.
 */
@Composable
internal fun DesktopPrayerSurfaces(
    appState: AzkarAppState,
    dispatcher: NotificationPublisher,
    actionRouter: NotificationActionRouter,
    preferences: NotificationPreferencesRepository,
    menuActions: TrayMenuActions,
) {
    val prayerStatus = rememberDesktopPrayerStatus(appState.locale)
    val latestAction = dispatcher.latestAction
    val openLatest = remember(latestAction, actionRouter) { { actionRouter.dispatch(latestAction) } }
    val tray = rememberTrayContent(prayerStatus.value, openLatest, preferences, menuActions)
    val hiddenNotice =
        key(appState.locale) {
            NotificationNotice(
                stringResource(Res.string.title),
                stringResource(Res.string.trayPanelHiddenMessage),
                NotificationAction.OpenApp,
            )
        }
    val panel = DesktopPrayerPanel.current
    val gnomeDisplay = if (panel is GnomePrayerPanel) preferences.gnomePrayerDisplay.collectAsState().value else null
    // Hide applies to the selected surface. Selecting a different surface starts it visible.
    var panelHidden by remember(gnomeDisplay) { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val visibility =
        panelVisibilityAction(panelHidden) { hidden ->
            scope.launch {
                if (panelHidden != hidden) {
                    panelHidden = hidden
                    if (hidden) {
                        menuActions.panelHidden()
                        dispatcher.publish(hiddenNotice)
                    }
                }
            }
        }
    val trayWithVisibility = tray.copy(menu = tray.menu.copy(panelVisibility = visibility))
    val panelReplacesTray =
        panel?.Mount(prayerStatus, appState, trayWithVisibility, hidden = panelHidden) ?: false
    if (!panelReplacesTray) PrayerTrayNativeRegistration(if (panelHidden) trayWithVisibility else tray)
}

@Composable
private fun panelVisibilityAction(
    hidden: Boolean,
    onVisibilityChanged: (Boolean) -> Unit,
): TrayMenuEntry =
    TrayMenuEntry(
        stringResource(if (hidden) Res.string.trayMenuShowPanel else Res.string.trayMenuHidePanel),
        if (hidden) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
        onClick = { onVisibilityChanged(!hidden) },
    )

@Composable
private fun rememberTrayContent(
    status: DesktopPrayerStatus?,
    primaryAction: () -> Unit,
    preferences: NotificationPreferencesRepository,
    menuActions: TrayMenuActions,
): ReminderTrayContent {
    val queue by preferences.queue.collectAsState()
    val trayLayout by preferences.prayerTrayMenuLayout.collectAsState()
    val displayLayout = if (prayerTrayLayoutAvailable) trayLayout else PrayerMenuLayout.COMPACT
    val readOnly = remember { {} }
    val scope = rememberCoroutineScope()
    val openLabel = stringResource(Res.string.trayMenuOpen)
    val pauseLabel =
        stringResource(if (!queue.enabled) Res.string.trayMenuResume else Res.string.trayMenuPause)
    val quitLabel = stringResource(Res.string.trayMenuQuit)
    val menuIconRenderProperties = remember { trayMenuIconRenderProperties() }
    val currentMenuActions by rememberUpdatedState(menuActions)
    val currentPrimaryAction by rememberUpdatedState(primaryAction)
    val actions =
        remember {
            TrayMenuActions(
                openApp = { currentMenuActions.openApp() },
                quit = { currentMenuActions.quit() },
                panelHidden = { currentMenuActions.panelHidden() },
            )
        }
    val activate = remember { { currentPrimaryAction() } }
    val menu =
        PrayerTrayMenuContent(
            countdown =
                TrayMenuEntry(
                    status?.countdownLabel ?: stringResource(Res.string.nextPrayerLabel),
                    AzkarIcons.Clock,
                    readOnly,
                    isEnabled = false,
                ),
            openApp = TrayMenuEntry(openLabel, AzkarIcons.Home, actions.openApp),
            reminders =
                TrayMenuEntry(
                    pauseLabel,
                    if (!queue.enabled) AzkarIcons.Bell else AzkarIcons.BellOff,
                    onClick = {
                        scope.launch { preferences.updateQueue { it.copy(enabled = !it.enabled) } }
                    },
                ),
            quit = TrayMenuEntry(quitLabel, ActionIcons.dismiss, actions.quit),
            iconRenderProperties = menuIconRenderProperties,
            remindersPaused = !queue.enabled,
            trayLayout = displayLayout,
            schedule = prayerTraySchedule(status, readOnly),
        )
    return ReminderTrayContent(
        tooltip = status?.countdownLabel ?: stringResource(Res.string.title),
        icon = AzkarIcons.Brand,
        primaryAction = activate,
        menu = menu,
    )
}

@Composable
internal fun prayerTraySchedule(
    status: DesktopPrayerStatus?,
    readOnly: () -> Unit,
): List<TrayMenuEntry> {
    val prayers = status?.today.orEmpty()
    if (prayers.isEmpty()) return emptyList()
    val heading =
        TrayMenuEntry(stringResource(Res.string.todayPrayerTimes), AzkarIcons.Calendar, readOnly, isEnabled = false)
    return listOf(heading) +
        prayers.map { prayer ->
            TrayMenuEntry(
                "${prayer.name} · ${prayer.value}",
                prayer.key.desktopPrayerIcon(),
                readOnly,
                isEnabled = false,
            )
        }
}

internal data class ReminderTrayContent(
    val tooltip: String,
    val icon: ImageVector,
    val primaryAction: () -> Unit,
    val menu: PrayerTrayMenuContent,
)

internal data class TrayMenuEntry(
    val label: String,
    val icon: ImageVector,
    val onClick: () -> Unit,
    val isEnabled: Boolean = true,
)

internal data class PrayerTrayMenuContent(
    val countdown: TrayMenuEntry,
    val openApp: TrayMenuEntry,
    val reminders: TrayMenuEntry,
    val quit: TrayMenuEntry,
    val iconRenderProperties: IconRenderProperties,
    val panelVisibility: TrayMenuEntry? = null,
    val remindersPaused: Boolean = false,
    val trayLayout: PrayerMenuLayout = PrayerMenuLayout.COMPACT,
    val schedule: List<TrayMenuEntry> = emptyList(),
) {
    val actions = listOfNotNull(openApp, panelVisibility, reminders, quit)

    // Null entries are separators in both native menu implementations.
    val actionEntries = actions.flatMap { if (it == quit) listOf(null, it) else listOf(it) }
    val entries = summary(trayLayout) + listOf(null) + actionEntries

    fun summary(layout: PrayerMenuLayout): List<TrayMenuEntry> =
        if (layout == PrayerMenuLayout.EXTENDED && schedule.isNotEmpty()) schedule else listOf(countdown)
}

private fun TrayMenuBuilder.addPrayerTrayMenu(menu: PrayerTrayMenuContent) {
    menu.entries.forEach { entry ->
        if (entry == null) Divider() else addPrayerTrayMenuEntry(entry, menu.iconRenderProperties)
    }
}

private fun TrayMenuBuilder.addPrayerTrayMenuEntry(
    entry: TrayMenuEntry,
    iconRenderProperties: IconRenderProperties,
) {
    Item(
        entry.label,
        entry.icon,
        null,
        iconRenderProperties,
        isEnabled = entry.isEnabled,
        onClick = entry.onClick,
    )
}

@Composable
internal fun TrayBrandIcon(icon: ImageVector) {
    Image(imageVector = icon, contentDescription = null, modifier = Modifier.fillMaxSize())
}

@Composable
private fun PrayerTrayNativeRegistration(content: ReminderTrayContent) {
    val tray = remember { NativeTray() }
    var hasSubmittedTray by remember(tray) { mutableStateOf(false) }
    val iconRenderProperties = trayIconRenderProperties(DesktopPlatform.current)
    LaunchedEffect(tray, content) {
        if (hasSubmittedTray) {
            logger.debug { "Updating prayer tray menu" }
        } else {
            logger.info { "Registering prayer tray icon" }
        }
        val updateFailure =
            runCatching {
                tray.updateComposable(
                    iconContent = { TrayBrandIcon(content.icon) },
                    iconRenderProperties = iconRenderProperties,
                    tooltip = content.tooltip,
                    primaryAction = content.primaryAction,
                    menuContent = { addPrayerTrayMenu(content.menu) },
                )
            }.exceptionOrNull()
        if (updateFailure != null) {
            logger.error(updateFailure) { "Unable to submit prayer tray update" }
        } else {
            hasSubmittedTray = true
        }
    }
    DisposableEffect(tray) {
        onDispose {
            logger.debug { "Disposing prayer tray icon" }
            runCatching { tray.dispose() }
                .onFailure { failure -> logger.warn(failure) { "Unable to dispose prayer tray icon" } }
        }
    }
}

@AzkarVariantsPreview
@Composable
internal fun ReminderTrayActionIconsPreview() {
    ReminderTrayActionIconsContent()
}

@Composable
private fun ReminderTrayActionIconsContent() {
    Surface(shadowElevation = 4.dp) {
        Column(modifier = Modifier.width(280.dp).padding(vertical = 4.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().height(36.dp).padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = AzkarIcons.Clock,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text =
                        stringResource(
                            Res.string.trayNextPrayerCountdown,
                            stringResource(Res.string.asr),
                            "00:05",
                        ),
                    modifier = Modifier.weight(1f),
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            HorizontalDivider(modifier = Modifier.padding(horizontal = 8.dp))
            listOf(
                AzkarIcons.Home to stringResource(Res.string.trayMenuOpen),
                AzkarIcons.Bell to stringResource(Res.string.trayMenuResume),
                AzkarIcons.BellOff to stringResource(Res.string.trayMenuPause),
                ActionIcons.dismiss to stringResource(Res.string.trayMenuQuit),
            ).forEachIndexed { index, (icon, label) ->
                if (index == 3) HorizontalDivider(modifier = Modifier.padding(horizontal = 8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth().height(36.dp).padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = label,
                        color = MaterialTheme.colorScheme.onSurface,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }
    }
}

private const val TRAY_ICON_RENDER_SIZE = 192
private const val TRAY_ICON_RENDER_DENSITY = 2f

private fun trayMenuIconRenderProperties() =
    IconRenderProperties(
        sceneWidth = TRAY_MENU_ICON_SIZE,
        sceneHeight = TRAY_MENU_ICON_SIZE,
        sceneDensity = Density(1f),
        targetWidth = TRAY_MENU_ICON_SIZE,
        targetHeight = TRAY_MENU_ICON_SIZE,
        jvmOwnsDownscaling = false,
    )

private const val TRAY_MENU_ICON_SIZE = 16

/**
 * Whether hiding the window would still leave the user a way back into the app.
 *
 * The tray library answers no such question — it registers the icon and logs when it cannot, with
 * no callback to report success — so the close handler has to decide before composing the icon,
 * and the safe answer is the conservative one: never hide into a state the user cannot leave. A
 * machine with no display has no tray by definition. On Linux the icon is a `StatusNotifierItem`,
 * which needs a session bus to exist at all, so a session without one has no tray host to draw it
 * and the window stays. An unrecognized OS is conservative too; only the three supported desktop
 * platforms are assumed to provide a native tray.
 *
 * This is deliberately not `SystemTray.isSupported()`: that is the JDK's own answer, it reports
 * false on Linux for every session, and this app no longer uses the JDK tray for anything.
 */
internal fun trayAvailable(): Boolean {
    val headless = runCatching { GraphicsEnvironment.isHeadless() }.getOrDefault(true)
    return isTrayAvailable(headless, DesktopPlatform.current, System.getenv("DBUS_SESSION_BUS_ADDRESS"))
}

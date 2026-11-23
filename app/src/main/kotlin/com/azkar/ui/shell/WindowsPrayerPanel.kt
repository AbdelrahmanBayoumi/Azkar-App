package com.azkar.ui.shell

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.awt.ComposeWindow
import androidx.compose.ui.awt.SwingWindow
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import com.azkar.generated.resources.Res
import com.azkar.generated.resources.settingsDesktopPrayerWindowsHint
import com.azkar.generated.resources.taskbarPositionTitle
import com.azkar.generated.resources.todayPrayerTimes
import com.azkar.ui.notifications.NotificationPreferencesRepository
import com.azkar.ui.notifications.presentation.OverlayTheme
import com.azkar.ui.theme.AzkarTheme
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.retryWhen
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import java.awt.Dimension
import java.awt.EventQueue
import java.awt.GraphicsEnvironment
import java.awt.Insets
import java.awt.Point
import java.awt.Rectangle
import java.awt.Toolkit
import java.awt.Window.Type
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import java.util.concurrent.Executors
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

private val logger = KotlinLogging.logger {}
private const val SCHEDULE_MARGIN = 8
private const val SCHEDULE_WIDTH = 264
private const val SCHEDULE_HEIGHT = 248
internal const val POSITION_TOOLTIP_HEIGHT = 32
internal const val POSITION_ACTION_HEIGHT = 48

internal class WindowsPrayerPanel(
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : DesktopPrayerPanel(Res.string.settingsDesktopPrayerWindowsHint) {
    @Composable
    override fun Show(
        status: State<DesktopPrayerStatus?>,
        appState: AzkarAppState,
        tray: ReminderTrayContent,
        mode: DesktopPrayerPanelMode,
    ): Boolean {
        if (mode != DesktopPrayerPanelMode.VISIBLE) return false
        val popups = remember { WindowsPrayerPopups() }
        val preferences = koinInject<NotificationPreferencesRepository>()
        val position = remember { MutableStateFlow(preferences.windowsTaskbarPosition.value) }
        LaunchedEffect(preferences) {
            preferences.windowsTaskbarPosition.collect { position.value = it }
        }
        val scope = rememberCoroutineScope()
        val savePosition: (Int?) -> Unit = { selected ->
            scope.launch { preferences.setWindowsTaskbarPosition(selected) }
        }
        val theme = OverlayTheme(rememberAzkarDarkTheme(appState.themePreference), appState.locale, appState.typography)
        val content = status.value?.taskbarContent(theme, popups.schedule != null, popups.menu != null)
        val present =
            rememberWindowsTaskbarWidget(
                content = content,
                position = position,
                actions =
                    TaskbarWidgetActions(
                        moveEnabled = { popups.moveEnabled },
                        onMoveFinished = { selected ->
                            popups.moveEnabled = false
                            if (selected != null) savePosition(selected)
                        },
                        onClick = { wasOpenOnPress, slot ->
                            popups.menu = null
                            popups.schedule =
                                if (wasOpenOnPress || popups.schedule != null) {
                                    null
                                } else {
                                    popupAtWidget(slot, theme.typography.scale, SCHEDULE_HEIGHT)
                                }
                        },
                        onContextMenu = { slot ->
                            popups.schedule = null
                            popups.menu = popupAtWidget(slot, theme.typography.scale, positionMenuHeight(tray.menu))
                        },
                    ),
                ioDispatcher = ioDispatcher,
            )
        LaunchedEffect(present) { if (!present) popups.reset() }
        if (present) {
            popups.schedule?.let { bounds ->
                status.value?.let { prayer ->
                    WindowsPrayerScheduleWindow(bounds, prayer, theme, tray.menu.openApp.onClick) {
                        popups.schedule =
                            null
                    }
                }
            }
            popups.PositionWindow(theme, tray.menu, position, savePosition)
        }
        return present
    }
}

private fun DesktopPrayerStatus.taskbarContent(
    theme: OverlayTheme,
    scheduleOpen: Boolean,
    menuOpen: Boolean,
) = WindowsTaskbarContent(prayerName, timeLabel, remainingLabel, theme.locale, theme.typography, scheduleOpen, menuOpen)

private fun positionMenuHeight(menu: PrayerTrayMenuContent): Int =
    POSITION_TOOLTIP_HEIGHT + POSITION_ACTION_HEIGHT * menu.actions.size + 4

private class WindowsPrayerPopups {
    var schedule by mutableStateOf<Rectangle?>(null)
    var menu by mutableStateOf<Rectangle?>(null)
    var moveEnabled by mutableStateOf(false)

    fun reset() {
        schedule = null
        menu = null
        moveEnabled = false
    }

    @Composable
    fun PositionWindow(
        theme: OverlayTheme,
        trayMenu: PrayerTrayMenuContent,
        position: MutableStateFlow<Int?>,
        savePosition: (Int?) -> Unit,
    ) {
        val bounds = menu ?: return
        val displayedPosition by position.collectAsState()
        val dismiss = { menu = null }
        WindowsTaskbarPopup(bounds, stringResource(Res.string.taskbarPositionTitle), theme, dismiss) {
            WindowsTaskbarPositionMenu(
                manual = displayedPosition != null,
                menu = trayMenu,
                onDismiss = dismiss,
                onMove = {
                    dismiss()
                    moveEnabled = true
                },
                onAutomatic = {
                    moveEnabled = false
                    position.value = null
                    savePosition(null)
                    dismiss()
                },
            )
        }
    }
}

private data class TaskbarWidgetActions(
    val moveEnabled: () -> Boolean,
    val onMoveFinished: (Int?) -> Unit,
    val onClick: (Boolean, WindowsTaskbarSlot) -> Unit,
    val onContextMenu: (WindowsTaskbarSlot) -> Unit,
)

@Composable
private fun rememberWindowsTaskbarWidget(
    content: WindowsTaskbarContent?,
    position: MutableStateFlow<Int?>,
    actions: TaskbarWidgetActions,
    ioDispatcher: CoroutineDispatcher,
): Boolean {
    val active = remember { MutableStateFlow(false) }
    val slot = remember { MutableStateFlow<WindowsTaskbarSlot?>(null) }
    val state by rememberUpdatedState(content)
    val currentActions by rememberUpdatedState(actions)
    LaunchedEffect(Unit) {
        try {
            flow<Unit> {
                coroutineScope {
                    launch(ioDispatcher) {
                        while (isActive) {
                            slot.value =
                                WindowsTaskbarAutomation.withApartment {
                                    WindowsTaskbar.findSlot(it, position = null, previous = slot.value)
                                }
                            delay(1.seconds)
                        }
                    }
                    val contentUpdates = MutableStateFlow(state)
                    launch { snapshotFlow { state }.collect { contentUpdates.value = it } }
                    runTaskbarWidget(
                        slot,
                        contentUpdates,
                        active,
                        position,
                        TaskbarWidgetActions(
                            moveEnabled = { currentActions.moveEnabled() },
                            onMoveFinished = { selected ->
                                if (selected != null) position.value = selected
                                EventQueue.invokeLater { currentActions.onMoveFinished(selected) }
                            },
                            onClick = { wasOpenOnPress, clickedSlot ->
                                EventQueue.invokeLater { currentActions.onClick(wasOpenOnPress, clickedSlot) }
                            },
                            onContextMenu = { clickedSlot ->
                                EventQueue.invokeLater { currentActions.onContextMenu(clickedSlot) }
                            },
                        ),
                    )
                }
            }.retryWhen { failure, _ ->
                if (failure !is Exception || failure is CancellationException) return@retryWhen false
                active.value = false
                slot.value = null
                logger.warn(failure) { "Unable to place the prayer widget in the Windows taskbar" }
                delay(2.seconds)
                true
            }.collect {}
        } finally {
            slot.value = null
            active.value = false
        }
    }
    return active.collectAsState().value
}

private suspend fun runTaskbarWidget(
    slots: StateFlow<WindowsTaskbarSlot?>,
    content: StateFlow<WindowsTaskbarContent?>,
    active: MutableStateFlow<Boolean>,
    position: StateFlow<Int?>,
    actions: TaskbarWidgetActions,
) {
    // A window must be updated, pumped and destroyed on its owning OS thread.
    Executors
        .newSingleThreadExecutor { task -> Thread(task, "azkar-taskbar-widget").apply { isDaemon = true } }
        .asCoroutineDispatcher()
        .use { dispatcher ->
            withContext(dispatcher) {
                WindowsTaskbarWidget(
                    actions.onClick,
                    actions.onContextMenu,
                    actions.moveEnabled,
                    actions.onMoveFinished,
                ).use { widget ->
                    while (isActive) {
                        widget.pump()
                        val slot = slots.value
                        // Apply the selected position against the latest layout on every frame.
                        val manualBounds = position.value?.let { slot?.layout?.boundsAt(it) }
                        val placed =
                            if (slot != null &&
                                manualBounds != null
                            ) {
                                slot.copy(bounds = manualBounds)
                            } else {
                                slot
                            }
                        widget.update(placed, content.value)
                        active.value = widget.isAttached
                        delay(25.milliseconds)
                    }
                }
            }
        }
}

@Composable
private fun WindowsPrayerScheduleWindow(
    bounds: Rectangle,
    prayer: DesktopPrayerStatus,
    theme: OverlayTheme,
    openApp: () -> Unit,
    dismiss: () -> Unit,
) {
    WindowsTaskbarPopup(bounds, stringResource(Res.string.todayPrayerTimes), theme, dismiss) {
        WindowsPrayerSchedule(prayer, onDismiss = dismiss) {
            dismiss()
            openApp()
        }
    }
}

@Composable
private fun WindowsTaskbarPopup(
    bounds: Rectangle,
    title: String,
    theme: OverlayTheme,
    dismiss: () -> Unit,
    content: @Composable () -> Unit,
) {
    val dismissCurrent by rememberUpdatedState(dismiss)
    SwingWindow(
        onPreviewKeyEvent = { event ->
            val escape = event.key == Key.Escape && event.type == KeyEventType.KeyDown
            if (escape) dismiss()
            escape
        },
        create = {
            ComposeWindow().apply {
                type = Type.UTILITY
                isUndecorated = true
                isTransparent = true
                isAlwaysOnTop = true
                isResizable = false
                val focus =
                    object : WindowAdapter() {
                        override fun windowOpened(event: WindowEvent) {
                            toFront()
                            requestFocus()
                        }

                        override fun windowLostFocus(event: WindowEvent) {
                            if (isShowing) dismissCurrent()
                        }
                    }
                addWindowListener(focus)
                addWindowFocusListener(focus)
            }
        },
        dispose = ComposeWindow::dispose,
        update = { window ->
            window.title = title
            window.bounds = bounds
        },
    ) {
        AzkarTheme(
            darkTheme = theme.darkTheme,
            uiLocale = theme.locale,
            typography = theme.typography,
            content = content,
        )
    }
}

private fun popupAtWidget(
    slot: WindowsTaskbarSlot,
    textScale: Float,
    height: Int,
): Rectangle? {
    val bar = slot.layout?.screenBounds ?: return null
    // Shell_TrayWnd belongs to the primary display. Its bounds are in physical pixels.
    val config = GraphicsEnvironment.getLocalGraphicsEnvironment().defaultScreenDevice.defaultConfiguration
    val transform = config.defaultTransform
    val anchor =
        Point(
            ((bar.x + slot.bounds.centerX) / transform.scaleX).roundToInt(),
            ((bar.y + slot.bounds.centerY) / transform.scaleY).roundToInt(),
        )
    val scale = maxOf(1f, textScale)
    val size = Dimension((SCHEDULE_WIDTH * scale).roundToInt(), (height * scale).roundToInt())
    return prayerScheduleBounds(
        config.bounds,
        Toolkit.getDefaultToolkit().getScreenInsets(config),
        anchor,
        size,
        minimumHeight = height,
    )
}

/** The widget center and screen insets are expressed in AWT screen coordinates. */
internal fun prayerScheduleBounds(
    screen: Rectangle,
    insets: Insets,
    anchor: Point,
    size: Dimension,
    minimumHeight: Int = SCHEDULE_HEIGHT,
): Rectangle? {
    val left = screen.x + insets.left + SCHEDULE_MARGIN
    val top = screen.y + insets.top + SCHEDULE_MARGIN
    val right = screen.x + screen.width - insets.right - SCHEDULE_MARGIN
    val bottom = screen.y + screen.height - insets.bottom - SCHEDULE_MARGIN
    val width = minOf(size.width, right - left)
    val height = minOf(size.height, bottom - top)
    if (width < SCHEDULE_WIDTH || height < minimumHeight) return null
    val x = (anchor.x - width / 2).coerceIn(left, right - width)
    val atLeftEdge = insets.left > 0 && anchor.x < left
    val atRightEdge = insets.right > 0 && anchor.x >= right
    val y =
        if (atLeftEdge || atRightEdge) {
            (anchor.y - height / 2).coerceIn(top, bottom - height)
        } else if (anchor.y < top) {
            top
        } else {
            bottom - height
        }
    return Rectangle(x, y, width, height)
}

package com.azkar.ui.shell

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import com.azkar.generated.resources.Res
import com.azkar.generated.resources.desktopPrayerCloseSchedule
import com.azkar.generated.resources.taskbarPositionAutomatic
import com.azkar.generated.resources.taskbarPositionManual
import com.azkar.generated.resources.taskbarPositionTip
import com.azkar.generated.resources.title
import com.azkar.generated.resources.todayPrayerTimes
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.freedesktop.dbus.annotations.DBusBoundProperty
import org.freedesktop.dbus.annotations.DBusInterfaceName
import org.freedesktop.dbus.annotations.DBusMemberName
import org.freedesktop.dbus.connections.AbstractConnection
import org.freedesktop.dbus.connections.impl.DBusConnection
import org.freedesktop.dbus.connections.impl.DBusConnectionBuilder
import org.freedesktop.dbus.exceptions.DBusException
import org.freedesktop.dbus.exceptions.DBusExecutionException
import org.freedesktop.dbus.interfaces.DBus
import org.freedesktop.dbus.interfaces.DBusInterface
import org.freedesktop.dbus.interfaces.Properties
import org.freedesktop.dbus.types.Variant
import org.jetbrains.compose.resources.stringResource
import java.io.IOException

private val logger = KotlinLogging.logger {}
private const val PANEL_BUS = "com.azkar.PrayerPanel"
private const val EXTENSION_BUS = "com.azkar.PrayerPanelExtension"
private const val PANEL_PATH = "/com/azkar/PrayerPanel"
private const val PANEL_INTERFACE = "com.azkar.PrayerPanel1"
private const val SNAPSHOT_PROPERTY = "Snapshot"

@Composable
internal fun rememberGnomeShellPrayerPanel(
    prayer: DesktopPrayerStatus?,
    tray: ReminderTrayContent,
    hidden: Boolean,
): Boolean {
    val connected = remember { MutableStateFlow(false) }
    val frames by rememberUpdatedState(ShellPanelFrame(shellPanelSnapshot(prayer, tray, hidden), tray.menu))
    LaunchedEffect(Unit) {
        try {
            publishShellPanel(snapshotFlow { frames }, connected)
        } finally {
            connected.value = false
        }
    }
    return connected.collectAsState().value
}

@Composable
private fun shellPanelSnapshot(
    prayer: DesktopPrayerStatus?,
    tray: ReminderTrayContent,
    hidden: Boolean,
): ShellPanelSnapshot =
    ShellPanelSnapshot(
        hidden = hidden,
        appName = stringResource(Res.string.title),
        prayer = prayer?.prayerName.orEmpty(),
        time = prayer?.timeLabel.orEmpty(),
        remaining = prayer?.remainingLabel.orEmpty(),
        scheduleTitle = stringResource(Res.string.todayPrayerTimes),
        schedule =
            prayer?.today.orEmpty().map {
                ShellScheduleRow(it.key.id, it.name, it.value, it.isNext && prayer?.nextIsTomorrow == false)
            },
        open = tray.menu.openApp.label,
        visibility =
            tray.menu.panelVisibility
                ?.label
                .orEmpty(),
        reminders = tray.menu.reminders.label,
        remindersPaused = tray.menu.remindersPaused,
        quit = tray.menu.quit.label,
        close = stringResource(Res.string.desktopPrayerCloseSchedule),
        automatic = stringResource(Res.string.taskbarPositionAutomatic),
        manual = stringResource(Res.string.taskbarPositionManual),
        dragHint = stringResource(Res.string.taskbarPositionTip),
    )

@Serializable
private data class ShellPanelSnapshot(
    val hidden: Boolean,
    val appName: String,
    val prayer: String,
    val time: String,
    val remaining: String,
    val scheduleTitle: String,
    val schedule: List<ShellScheduleRow>,
    val open: String,
    val visibility: String,
    val reminders: String,
    val remindersPaused: Boolean,
    val quit: String,
    val close: String,
    val automatic: String,
    val manual: String,
    val dragHint: String,
)

@Serializable
private data class ShellScheduleRow(
    val key: String,
    val name: String,
    val time: String,
    val next: Boolean,
)

private data class ShellPanelFrame(
    val snapshot: ShellPanelSnapshot,
    val menu: PrayerTrayMenuContent,
)

@DBusInterfaceName(PANEL_INTERFACE)
internal interface GnomeShellPanelService : DBusInterface {
    @DBusBoundProperty(name = SNAPSHOT_PROPERTY)
    fun getSnapshot(): String

    @DBusMemberName("RunAction")
    fun runAction(action: String)
}

private suspend fun publishShellPanel(
    frames: Flow<ShellPanelFrame>,
    connected: MutableStateFlow<Boolean>,
    ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) = withContext(ioDispatcher) {
    try {
        val initial = frames.first()
        DBusConnectionBuilder.forSessionBus().withShared(false).build().use { bus ->
            val service = ShellPanelService(initial)
            bus.exportObject(service)
            bus.requestBusName(PANEL_BUS)
            coroutineScope {
                launch { watchShellPanel(bus, service, connected) }
                launch { provisionGnomePrayerExtension() }
                frames.collect { service.update(bus, it) }
            }
        }
    } catch (failure: CancellationException) {
        throw failure
    } catch (failure: DBusException) {
        logger.debug(failure) { "The GNOME Shell prayer panel could not connect" }
    } catch (failure: DBusExecutionException) {
        logger.debug(failure) { "The GNOME Shell prayer panel lost its connection" }
    } catch (failure: IOException) {
        logger.debug(failure) { "The GNOME Shell prayer panel transport failed" }
    }
}

private suspend fun watchShellPanel(
    bus: DBusConnection,
    service: ShellPanelService,
    connected: MutableStateFlow<Boolean>,
) {
    val changes = Channel<Unit>(Channel.CONFLATED)
    bus.addSigHandler(DBus.NameOwnerChanged::class.java) {
        if (it.name == EXTENSION_BUS) changes.trySend(Unit)
    }
    val daemon = bus.getRemoteObject("org.freedesktop.DBus", "/org/freedesktop/DBus", DBus::class.java)
    changes.trySend(Unit)
    for (ignored in changes) {
        val owner = runCatching { daemon.GetNameOwner(EXTENSION_BUS) }.getOrNull()
        service.extensionOwner = owner
        connected.value = owner != null
    }
}

private class ShellPanelService(
    initial: ShellPanelFrame,
) : GnomeShellPanelService {
    @Volatile
    private var frame = initial

    @Volatile
    var extensionOwner: String? = null

    override fun getObjectPath(): String = PANEL_PATH

    override fun getSnapshot(): String = Json.encodeToString(frame.snapshot)

    override fun runAction(action: String) {
        val owner = extensionOwner ?: return
        if (AbstractConnection.getCallInfo()?.source != owner) return
        val menu = frame.menu
        val entry =
            when (action) {
                "open" -> menu.openApp
                "visibility" -> menu.panelVisibility
                "reminders" -> menu.reminders
                "quit" -> menu.quit
                else -> null
            }
        entry?.takeIf { it.isEnabled }?.onClick?.invoke()
    }

    fun update(
        bus: DBusConnection,
        next: ShellPanelFrame,
    ) {
        val changed = frame.snapshot != next.snapshot
        frame = next
        if (changed) {
            val properties = mapOf(SNAPSHOT_PROPERTY to Variant(getSnapshot()))
            bus.sendMessage(Properties.PropertiesChanged(PANEL_PATH, PANEL_INTERFACE, properties, emptyList()))
        }
    }
}

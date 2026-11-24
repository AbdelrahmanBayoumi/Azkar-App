package com.azkar.ui.shell

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import com.azkar.generated.resources.Res
import com.azkar.generated.resources.settingsDesktopPrayerGnomeHint
import com.azkar.ui.notifications.DesktopPlatform
import com.azkar.ui.notifications.GnomePrayerDisplay
import com.azkar.ui.notifications.NotificationPreferencesRepository
import com.azkar.ui.notifications.PrayerMenuLayout
import dev.nucleusframework.composenativetray.utils.ComposableIconUtils
import dev.nucleusframework.composenativetray.utils.IconRenderProperties
import dev.nucleusframework.composenativetray.utils.isMenuBarInDarkMode
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.freedesktop.dbus.DBusPath
import org.freedesktop.dbus.Struct
import org.freedesktop.dbus.Tuple
import org.freedesktop.dbus.annotations.DBusBoundProperty
import org.freedesktop.dbus.annotations.DBusInterfaceName
import org.freedesktop.dbus.annotations.DBusMemberName
import org.freedesktop.dbus.annotations.Position
import org.freedesktop.dbus.connections.impl.DBusConnection
import org.freedesktop.dbus.connections.impl.DBusConnectionBuilder
import org.freedesktop.dbus.interfaces.DBus
import org.freedesktop.dbus.interfaces.DBusInterface
import org.freedesktop.dbus.interfaces.Properties
import org.freedesktop.dbus.messages.DBusSignal
import org.freedesktop.dbus.types.UInt32
import org.freedesktop.dbus.types.Variant
import org.koin.compose.koinInject
import java.nio.ByteBuffer
import javax.imageio.ImageIO

private val logger = KotlinLogging.logger {}
private const val ITEM_INTERFACE = "org.kde.StatusNotifierItem"
private const val WATCHER_INTERFACE = "org.kde.StatusNotifierWatcher"
private const val WATCHER_PATH = "/StatusNotifierWatcher"
private const val ITEM_PATH = "/StatusNotifierItem"
private const val MENU_PATH = "/PrayerMenu"
private const val PANEL_LABEL_PROPERTY = "XAyatanaLabel"

internal class GnomePrayerPanel : DesktopPrayerPanel(Res.string.settingsDesktopPrayerGnomeHint) {
    @Composable
    override fun Show(
        status: State<DesktopPrayerStatus?>,
        appState: AzkarAppState,
        tray: ReminderTrayContent,
        mode: DesktopPrayerPanelMode,
    ): Boolean {
        val trayContent =
            if (mode == DesktopPrayerPanelMode.DISABLED) {
                tray.copy(menu = tray.menu.copy(panelVisibility = null))
            } else {
                tray
            }
        val preferences = koinInject<NotificationPreferencesRepository>()
        val display by preferences.gnomePrayerDisplay.collectAsState()
        val menuLayout by preferences.prayerPanelMenuLayout.collectAsState()
        val extensionAvailable =
            display == GnomePrayerDisplay.EXTENSION &&
                mode != DesktopPrayerPanelMode.DISABLED &&
                rememberGnomeShellPrayerPanel(
                    status.value,
                    trayContent,
                    mode == DesktopPrayerPanelMode.HIDDEN,
                    rememberAzkarDarkTheme(appState.themePreference),
                )
        if (!extensionAvailable) {
            val content =
                rememberGnomePrayerContent(
                    status.value,
                    trayContent,
                    panelVisible = mode == DesktopPrayerPanelMode.VISIBLE,
                    panelLayout = menuLayout,
                )
            LaunchedEffect(Unit) {
                publishGnomePrayerStatus(snapshotFlow { content.value })
            }
        }
        // AppIndicator provides the tray icon itself when the native extension is hidden or unavailable.
        return true
    }
}

@Composable
private fun rememberGnomePrayerContent(
    prayer: DesktopPrayerStatus?,
    tray: ReminderTrayContent,
    panelVisible: Boolean,
    panelLayout: PrayerMenuLayout,
): State<GnomePrayerContent> =
    rememberUpdatedState(
        GnomePrayerContent(
            label = prayer?.takeIf { panelVisible }?.panelLabel.orEmpty(),
            tray = tray,
            menuTint = if (isMenuBarInDarkMode()) Color.White else Color.Black,
            panelVisible = panelVisible,
            panelLayout = panelLayout,
        ),
    )

internal data class GnomePrayerContent(
    val label: String,
    val tray: ReminderTrayContent,
    val menuTint: Color,
    val panelVisible: Boolean,
    val panelLayout: PrayerMenuLayout,
) {
    val layout: PrayerMenuLayout
        get() = if (panelVisible) panelLayout else tray.menu.trayLayout
}

internal fun GnomePrayerContent.menuEntries(): List<Pair<Int, TrayMenuEntry?>> {
    val menu = tray.menu
    // AppIndicator caches the widget type by ID. Keep action and separator IDs stable.
    val schedule = menu.schedule.takeIf { layout == PrayerMenuLayout.EXTENDED }.orEmpty()
    return buildList {
        val actionEntries =
            listOf(menu.countdown, null, menu.openApp, menu.panelVisibility, menu.reminders, null, menu.quit)
        if (schedule.isNotEmpty()) {
            val separatorId = actionEntries.size + 1
            schedule.forEachIndexed { index, entry -> add(separatorId + 1 + index to entry) }
            add(separatorId to null)
        }
        actionEntries.forEachIndexed { index, entry ->
            val visible =
                when (index) {
                    0, 1 -> !panelVisible && schedule.isEmpty()
                    3 -> entry != null && (!panelVisible || layout == PrayerMenuLayout.EXTENDED)
                    else -> true
                }
            if (visible) add(index + 1 to entry)
        }
    }
}

internal suspend fun publishGnomePrayerStatus(
    states: Flow<GnomePrayerContent>,
    ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) = withContext(ioDispatcher) {
    runCatching {
        val initial = states.first()
        // Closing this private connection withdraws the indicator without touching the fallback tray.
        DBusConnectionBuilder.forSessionBus().withShared(false).build().use { bus ->
            val item = GnomePrayerItem(initial)
            val menu = GnomePrayerMenuItem(initial)
            bus.exportObject(item)
            bus.exportObject(menu)
            coroutineScope {
                val watcher = launch { bus.registerWithWatcher() }
                try {
                    states.collect { content ->
                        item.publishLabel(bus, content.label)
                        menu.publish(bus, content)
                    }
                } finally {
                    watcher.cancel()
                }
            }
        }
    }.onFailure { failure ->
        if (failure is CancellationException) throw failure
        logger.warn(failure) { "Unable to publish the GNOME prayer indicator" }
    }
    Unit
}

private fun renderGnomeTrayPixmap(icon: ImageVector): GnomePrayerPixmap {
    val size = trayIconSize(DesktopPlatform.LINUX)
    val properties =
        trayIconRenderProperties(DesktopPlatform.LINUX).copy(
            targetWidth = size.width * 2,
            targetHeight = size.height * 2,
            jvmOwnsDownscaling = true,
        )
    val png = ComposableIconUtils.renderComposableToPngBytes(properties) { TrayBrandIcon(icon) }
    val image = png.inputStream().use(ImageIO::read)
    val pixels = image.getRGB(0, 0, image.width, image.height, null, 0, image.width)
    // StatusNotifierItem expects ARGB pixels in network byte order.
    val bytes =
        ByteBuffer.allocate(pixels.size * Int.SIZE_BYTES).apply {
            asIntBuffer().put(pixels)
        }
    return GnomePrayerPixmap(image.width, image.height, bytes.array())
}

private suspend fun DBusConnection.registerWithWatcher() {
    val registrations = Channel<Unit>(Channel.CONFLATED)
    addSigHandler(DBus.NameOwnerChanged::class.java) { signal ->
        if (signal.name == WATCHER_INTERFACE) {
            registrations.trySend(Unit)
        }
    }
    registrations.trySend(Unit)
    registrations.consumeEach {
        val result =
            runCatching {
                val watcher = getRemoteObject(WATCHER_INTERFACE, WATCHER_PATH, GnomePrayerWatcher::class.java)
                watcher.registerStatusNotifierItem(ITEM_PATH)
            }
        result.onFailure { failure ->
            if (failure is CancellationException) throw failure
            logger.debug(failure) { "AppIndicator watcher is unavailable" }
        }
    }
}

@DBusInterfaceName(WATCHER_INTERFACE)
internal interface GnomePrayerWatcher : DBusInterface {
    @DBusMemberName("RegisterStatusNotifierItem")
    fun registerStatusNotifierItem(path: String)
}

@DBusInterfaceName(ITEM_INTERFACE)
internal interface GnomePrayerIndicator : DBusInterface {
    @DBusBoundProperty
    fun getId(): String

    @DBusBoundProperty
    fun getStatus(): String

    @DBusBoundProperty
    fun getTitle(): String

    @DBusBoundProperty
    fun getMenu(): DBusPath

    @DBusBoundProperty(name = PANEL_LABEL_PROPERTY)
    fun getLabel(): String

    @DBusBoundProperty
    fun getIconPixmap(): List<GnomePrayerPixmap>

    @DBusMemberName("Activate")
    fun activate(
        x: Int,
        y: Int,
    )
}

internal class GnomePrayerPixmap(
    @field:Position(0) val width: Int,
    @field:Position(1) val height: Int,
    @field:Position(2) val pixels: ByteArray,
) : Struct()

private class GnomePrayerItem(
    initial: GnomePrayerContent,
) : GnomePrayerIndicator {
    @Volatile
    private var label = initial.label
    private val iconPixmap = listOf(renderGnomeTrayPixmap(initial.tray.icon))
    private val primaryAction = initial.tray.primaryAction

    override fun getObjectPath(): String = ITEM_PATH

    override fun getId(): String = "azkar-prayer"

    override fun getStatus(): String = "Active"

    override fun getTitle(): String = "Azkar"

    override fun getMenu(): DBusPath = DBusPath(MENU_PATH)

    override fun getLabel(): String = label

    override fun getIconPixmap(): List<GnomePrayerPixmap> = iconPixmap

    override fun activate(
        x: Int,
        y: Int,
    ) = primaryAction()

    fun publishLabel(
        bus: DBusConnection,
        next: String,
    ) {
        if (label == next) return
        label = next
        val properties = mapOf(PANEL_LABEL_PROPERTY to Variant(next))
        bus.sendMessage(Properties.PropertiesChanged(ITEM_PATH, ITEM_INTERFACE, properties, emptyList()))
    }
}

// GNOME AppIndicators requires a menu to show an item. This menu provides a way back into Azkar.
@DBusInterfaceName("com.canonical.dbusmenu")
internal interface GnomePrayerMenu : DBusInterface {
    @DBusBoundProperty
    fun getVersion(): UInt32

    @DBusMemberName("GetLayout")
    fun getLayout(
        parentId: Int,
        depth: Int,
        properties: List<String>,
    ): GnomePrayerMenuLayout<UInt32, GnomePrayerMenuNode>

    @DBusMemberName("GetGroupProperties")
    fun getGroupProperties(
        ids: List<Int>,
        properties: List<String>,
    ): List<GnomePrayerMenuProperties>

    @DBusMemberName("AboutToShow")
    fun aboutToShow(id: Int): Boolean

    @DBusMemberName("Event")
    fun event(
        id: Int,
        event: String,
        data: Variant<*>,
        timestamp: UInt32,
    )

    class LayoutUpdated(
        path: String,
        revision: UInt32,
        parentId: Int,
    ) : DBusSignal(path, revision, parentId)
}

internal class GnomePrayerMenuLayout<R, N>(
    @field:Position(0) val revision: R,
    @field:Position(1) val item: N,
) : Tuple()

internal class GnomePrayerMenuNode(
    @field:Position(0) val id: Int,
    @field:Position(1) val properties: Map<String, Variant<*>>,
    @field:Position(2) val children: List<Variant<*>>,
) : Struct()

internal class GnomePrayerMenuProperties(
    @field:Position(0) val id: Int,
    @field:Position(1) val properties: Map<String, Variant<*>>,
) : Struct()

internal class GnomePrayerMenuItem(
    initial: GnomePrayerContent,
) : GnomePrayerMenu {
    private val root = GnomePrayerMenuNode(0, mapOf("children-display" to Variant("submenu")), emptyList())
    private val icons = mutableMapOf<Pair<ImageVector, Color>, ByteArray>()

    @Volatile
    private var snapshot = menuSnapshot(initial, revision = 1)

    fun publish(
        bus: DBusConnection,
        content: GnomePrayerContent,
    ) {
        if (content == snapshot.content) return
        val next = menuSnapshot(content, snapshot.revision + 1)
        snapshot = next
        bus.sendMessage(GnomePrayerMenu.LayoutUpdated(MENU_PATH, UInt32(next.revision), root.id))
    }

    private fun menuSnapshot(
        content: GnomePrayerContent,
        revision: Long,
    ): MenuSnapshot {
        val visibleEntries = content.menuEntries()
        val items = visibleEntries.map { (id, entry) -> menuNode(content, id, entry) }
        val actions =
            visibleEntries
                .mapNotNull { (id, entry) ->
                    entry?.takeIf { it.isEnabled }?.let { id to it }
                }.toMap()
        return MenuSnapshot(content, revision, items, actions)
    }

    private fun menuNode(
        content: GnomePrayerContent,
        id: Int,
        entry: TrayMenuEntry?,
    ): GnomePrayerMenuNode {
        val properties =
            if (entry == null) {
                mapOf("type" to Variant("separator"))
            } else {
                mapOf(
                    "type" to Variant("standard"),
                    "label" to Variant(entry.label),
                    "enabled" to Variant(entry.isEnabled),
                    "icon-data" to
                        Variant(iconBytes(entry.icon, content.menuTint, content.tray.menu.iconRenderProperties)),
                )
            }
        return GnomePrayerMenuNode(id, properties, emptyList())
    }

    private fun iconBytes(
        icon: ImageVector,
        tint: Color,
        properties: IconRenderProperties,
    ): ByteArray =
        icons.getOrPut(icon to tint) {
            ComposableIconUtils.renderComposableToPngBytes(properties) {
                Image(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    colorFilter = ColorFilter.tint(tint),
                )
            }
        }

    override fun getObjectPath(): String = MENU_PATH

    override fun getVersion(): UInt32 = UInt32(3)

    override fun getLayout(
        parentId: Int,
        depth: Int,
        properties: List<String>,
    ): GnomePrayerMenuLayout<UInt32, GnomePrayerMenuNode> {
        val current = snapshot
        val node =
            when {
                parentId != root.id -> current.items.first { it.id == parentId }
                depth == 0 -> root
                else -> GnomePrayerMenuNode(root.id, root.properties, current.items.map { Variant(it) })
            }
        return GnomePrayerMenuLayout(UInt32(current.revision), node)
    }

    override fun getGroupProperties(
        ids: List<Int>,
        properties: List<String>,
    ): List<GnomePrayerMenuProperties> =
        (listOf(root) + snapshot.items).filter { ids.isEmpty() || it.id in ids }.map {
            GnomePrayerMenuProperties(
                it.id,
                it.properties.filterKeys { name ->
                    properties.isEmpty() ||
                        name in properties
                },
            )
        }

    override fun aboutToShow(id: Int): Boolean = false

    override fun event(
        id: Int,
        event: String,
        data: Variant<*>,
        timestamp: UInt32,
    ) {
        if (event != "clicked") return
        snapshot.actions[id]?.onClick?.invoke()
    }

    private data class MenuSnapshot(
        val content: GnomePrayerContent,
        val revision: Long,
        val items: List<GnomePrayerMenuNode>,
        val actions: Map<Int, TrayMenuEntry>,
    )
}

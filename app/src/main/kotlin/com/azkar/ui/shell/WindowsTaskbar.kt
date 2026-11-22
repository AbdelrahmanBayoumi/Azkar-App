package com.azkar.ui.shell

import java.awt.Dimension
import java.awt.Point
import java.awt.Rectangle
import java.lang.foreign.Arena
import java.lang.foreign.FunctionDescriptor
import java.lang.foreign.Linker
import java.lang.foreign.MemorySegment
import java.lang.foreign.MemorySegment.NULL
import java.lang.foreign.SymbolLookup
import java.lang.foreign.ValueLayout.ADDRESS
import java.lang.foreign.ValueLayout.JAVA_INT
import java.lang.foreign.ValueLayout.JAVA_LONG
import java.lang.invoke.MethodHandle
import kotlin.math.abs
import kotlin.math.roundToInt

private const val DEFAULT_DPI = 96
private const val ABM_GETSTATE = 4
private const val ABS_AUTOHIDE = 1L

internal data class WindowsTaskbarSlot(
    val taskbar: MemorySegment,
    val bounds: Rectangle,
    val dpi: Int,
    val dark: Boolean,
    val layout: WindowsTaskbarLayout? = null,
)

internal data class WindowsTaskbarLayout(
    val screenBounds: Rectangle,
    val occupied: List<Rectangle>,
    val dpi: Int,
) {
    val vertical: Boolean get() = screenBounds.height > screenBounds.width
    val screenStart: Int get() = if (vertical) screenBounds.y else screenBounds.x
    val length: Int get() = if (vertical) screenBounds.height else screenBounds.width

    fun boundsAt(position: Int?): Rectangle? {
        val bar = alongBar(screenBounds)
        val gap =
            windowsTaskbarGap(
                bar,
                occupied.map(::alongBar),
                Dimension(windowsPixels(if (vertical) 54 else 174, dpi), windowsPixels(40, dpi)),
                windowsPixels(6, dpi),
                minimumWidth = windowsPixels(if (vertical) 46 else 148, dpi),
                position = position,
            ) ?: return null
        gap.translate(-bar.x, -bar.y)
        return alongBar(gap)
    }

    private fun alongBar(rect: Rectangle): Rectangle =
        if (vertical) Rectangle(rect.y, rect.x, rect.height, rect.width) else rect
}

internal object WindowsTaskbar {
    private val findWindow = Win32.user32("FindWindowW", FunctionDescriptor.of(ADDRESS, ADDRESS, ADDRESS))
    private val findWindowEx =
        Win32.user32("FindWindowExW", FunctionDescriptor.of(ADDRESS, ADDRESS, ADDRESS, ADDRESS, ADDRESS))
    private val getClientRect = Win32.user32("GetClientRect", FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS))
    private val getWindowRect = Win32.user32("GetWindowRect", FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS))
    private val clientToScreen = Win32.user32("ClientToScreen", FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS))
    private val isWindowVisible = Win32.user32("IsWindowVisible", FunctionDescriptor.of(JAVA_INT, ADDRESS))
    private val getDpi = Win32.user32("GetDpiForWindow", FunctionDescriptor.of(JAVA_INT, ADDRESS))
    private val getWindowDpiContext =
        Win32.user32("GetWindowDpiAwarenessContext", FunctionDescriptor.of(ADDRESS, ADDRESS))
    private val setThreadDpiContext =
        Win32.user32("SetThreadDpiAwarenessContext", FunctionDescriptor.of(ADDRESS, ADDRESS))
    private val appBarMessage = Win32.shell32("SHAppBarMessage", FunctionDescriptor.of(JAVA_LONG, JAVA_INT, ADDRESS))
    private val regGetValue =
        Win32.advapi32(
            "RegGetValueW",
            FunctionDescriptor.of(
                JAVA_INT,
                ADDRESS,
                ADDRESS,
                ADDRESS,
                JAVA_INT,
                ADDRESS,
                ADDRESS,
                ADDRESS,
            ),
        )
    private val taskbarClass = Arena.global().wideString("Shell_TrayWnd")
    private val trayClass = Arena.global().wideString("TrayNotifyWnd")
    private val personalizeKey =
        Arena.global().wideString(
            "Software\\Microsoft\\Windows\\CurrentVersion\\Themes\\Personalize",
        )
    private val lightThemeName = Arena.global().wideString("SystemUsesLightTheme")

    private val alignmentKey =
        Arena.global().wideString(
            "Software\\Microsoft\\Windows\\CurrentVersion\\Explorer\\Advanced",
        )
    private val alignmentName = Arena.global().wideString("TaskbarAl")

    fun findSlot(
        automation: WindowsTaskbarAutomation,
        position: Int?,
        previous: WindowsTaskbarSlot?,
    ): WindowsTaskbarSlot? =
        withScreenDpi {
            findSlotAtDpi(automation, position, previous)
        }

    private fun findSlotAtDpi(
        automation: WindowsTaskbarAutomation,
        position: Int?,
        previous: WindowsTaskbarSlot?,
    ): WindowsTaskbarSlot? =
        Arena.ofConfined().use { arena ->
            val taskbar = findWindow.invokeExact(taskbarClass, NULL) as MemorySegment
            if (taskbar == NULL || (isWindowVisible.invokeExact(taskbar) as Int) == 0 || autoHides(arena)) return null
            val bounds = taskbarScreenBounds(taskbar, arena) ?: return null
            val dpi = (getDpi.invokeExact(taskbar) as Int).takeIf { it > 0 } ?: DEFAULT_DPI
            val previousLayout = previous?.takeIf { it.taskbar == taskbar && it.dpi == dpi }?.layout
            val occupied = taskbarControlBounds(bounds, automation.controls(taskbar), previousLayout)
            if (occupied.isEmpty()) return null
            val notificationArea = notificationAreaBounds(taskbar, arena)
            val layout = WindowsTaskbarLayout(bounds, (occupied + notificationArea).distinct(), dpi)
            val gap =
                layout.boundsAt(position ?: if (layout.vertical || isLeftAligned(arena)) 100 else null) ?: return null
            WindowsTaskbarSlot(taskbar, gap, dpi, dark = isDark(arena), layout = layout)
        }

    private fun notificationAreaBounds(
        taskbar: MemorySegment,
        arena: Arena,
    ): List<Rectangle> {
        val tray = findWindowEx.invokeExact(taskbar, NULL, trayClass, NULL) as MemorySegment
        val rect = arena.allocate(16, 4)
        return if (tray != NULL && (getWindowRect.invokeExact(tray, rect) as Int) != 0) {
            listOf(rect.asRectangle())
        } else {
            emptyList()
        }
    }

    private fun taskbarScreenBounds(
        taskbar: MemorySegment,
        arena: Arena,
    ): Rectangle? {
        val rect = arena.allocate(16, 4)
        val origin = arena.allocate(8, 4)
        if ((getClientRect.invokeExact(taskbar, rect) as Int) == 0 ||
            (clientToScreen.invokeExact(taskbar, origin) as Int) == 0
        ) {
            return null
        }
        return rect.asRectangle().apply { location = Point(origin.get(JAVA_INT, 0), origin.get(JAVA_INT, 4)) }
    }

    fun <T> withScreenDpi(action: () -> T): T = withDpiContext(MemorySegment.ofAddress(-4L), action)

    fun <T> withParentDpi(
        taskbar: MemorySegment,
        action: () -> T,
    ): T = withDpiContext(getWindowDpiContext.invokeExact(taskbar) as MemorySegment, action)

    private fun <T> withDpiContext(
        context: MemorySegment,
        action: () -> T,
    ): T {
        val previous = setThreadDpiContext.invokeExact(context) as MemorySegment
        return try {
            action()
        } finally {
            if (previous != NULL) {
                setThreadDpiContext.invokeExact(previous) as MemorySegment
            }
        }
    }

    private fun autoHides(arena: Arena): Boolean {
        // APPBARDATA has an HWND and an LPARAM at the x64 pointer offsets.
        val data = arena.allocate(48, 8)
        data.set(JAVA_INT, 0, data.byteSize().toInt())
        return (appBarMessage.invokeExact(ABM_GETSTATE, data) as Long) and ABS_AUTOHIDE != 0L
    }

    private fun isLeftAligned(arena: Arena): Boolean =
        System.getProperty("os.name") == "Windows 10" || readDword(arena, alignmentKey, alignmentName) == 0

    private fun isDark(arena: Arena): Boolean = readDword(arena, personalizeKey, lightThemeName) == 0

    private fun readDword(
        arena: Arena,
        key: MemorySegment,
        name: MemorySegment,
    ): Int? {
        val value = arena.allocate(JAVA_INT)
        val bytes = arena.allocate(JAVA_INT)
        bytes.set(JAVA_INT, 0, Int.SIZE_BYTES)
        // HKEY_CURRENT_USER is a sign-extended predefined handle on x64.
        val currentUser = MemorySegment.ofAddress(-2147483647L)
        val dwordType = 0x10
        val result =
            regGetValue.invokeExact(
                currentUser,
                key,
                name,
                dwordType,
                NULL,
                value,
                bytes,
            ) as Int
        return value.get(JAVA_INT, 0).takeIf { result == 0 }
    }
}

internal fun taskbarControlBounds(
    bar: Rectangle,
    controls: List<Rectangle>,
    previous: WindowsTaskbarLayout?,
): List<Rectangle> {
    // While the context menu is open, UI Automation can return only the popup's controls.
    val insideBar = controls.filter { !it.isEmpty && bar.contains(it) }
    return insideBar.ifEmpty { previous?.takeIf { it.screenBounds == bar }?.occupied.orEmpty() }
}

internal fun windowsTaskbarGap(
    bar: Rectangle,
    occupied: List<Rectangle>,
    widget: Dimension,
    margin: Int,
    minimumWidth: Int = widget.width,
    position: Int? = null,
): Rectangle? {
    val height = widget.height
    val right = bar.x + bar.width
    if (bar.height < height || bar.width < minimumWidth + 2 * margin) return null
    val y = bar.y + (bar.height - height) / 2
    val gaps =
        buildList {
            var left = bar.x + margin
            for (control in occupied.filter(bar::intersects).sortedBy { it.x }) {
                val end = control.x - margin
                if (end - left >= minimumWidth) add(left to end)
                left = maxOf(left, control.x + control.width + margin)
            }
            if (right - margin - left >= minimumWidth) add(left to right - margin)
        }
    val target = bar.x + (bar.width * ((position ?: 0).coerceIn(0, 100) / 100.0)).roundToInt()
    return gaps
        .map { (left, end) ->
            val width = minOf(widget.width, end - left)
            val x = (target - width / 2).coerceIn(left, end - width)
            Rectangle(x, y, width, height)
        }.minByOrNull { abs(it.x + it.width / 2 - target) }
}

internal fun windowsPixels(
    dip: Int,
    dpi: Int,
): Int = (dip.toDouble() * dpi / DEFAULT_DPI).roundToInt()

internal fun MemorySegment.asRectangle(): Rectangle {
    val left = get(JAVA_INT, 0)
    val top = get(JAVA_INT, 4)
    return Rectangle(left, top, get(JAVA_INT, 8) - left, get(JAVA_INT, 12) - top)
}

internal fun Arena.wideString(value: String): MemorySegment = allocateFrom(value, Charsets.UTF_16LE)

internal object Win32 {
    val linker: Linker = Linker.nativeLinker()
    private val user by lazy { library("user32") }
    private val gdi by lazy { library("gdi32") }
    private val kernel by lazy { library("kernel32") }
    private val shell by lazy { library("shell32") }
    private val ole by lazy { library("ole32") }
    private val registry by lazy { library("advapi32") }

    fun user32(
        name: String,
        signature: FunctionDescriptor,
    ) = bind(user, name, signature)

    fun gdi32(
        name: String,
        signature: FunctionDescriptor,
    ) = bind(gdi, name, signature)

    fun kernel32(
        name: String,
        signature: FunctionDescriptor,
    ) = bind(kernel, name, signature)

    fun shell32(
        name: String,
        signature: FunctionDescriptor,
    ) = bind(shell, name, signature)

    fun ole32(
        name: String,
        signature: FunctionDescriptor,
    ) = bind(ole, name, signature)

    fun advapi32(
        name: String,
        signature: FunctionDescriptor,
    ) = bind(registry, name, signature)

    private fun library(name: String): SymbolLookup = SymbolLookup.libraryLookup("$name.dll", Arena.global())

    private fun bind(
        library: SymbolLookup,
        name: String,
        signature: FunctionDescriptor,
    ): MethodHandle = linker.downcallHandle(library.findOrThrow(name), signature)
}

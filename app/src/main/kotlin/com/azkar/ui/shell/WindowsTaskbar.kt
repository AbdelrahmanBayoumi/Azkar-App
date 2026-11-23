package com.azkar.ui.shell

import com.sun.jna.Memory
import com.sun.jna.Pointer
import com.sun.jna.platform.win32.Advapi32
import com.sun.jna.platform.win32.Shell32
import com.sun.jna.platform.win32.ShellAPI
import com.sun.jna.platform.win32.ShellAPI.APPBARDATA
import com.sun.jna.platform.win32.User32
import com.sun.jna.platform.win32.WinDef.DWORD
import com.sun.jna.platform.win32.WinDef.HWND
import com.sun.jna.platform.win32.WinDef.POINT
import com.sun.jna.platform.win32.WinDef.RECT
import com.sun.jna.platform.win32.WinReg
import com.sun.jna.ptr.IntByReference
import java.awt.Dimension
import java.awt.Point
import java.awt.Rectangle
import kotlin.math.abs
import kotlin.math.roundToInt

private const val DEFAULT_DPI = 96
private const val ABS_AUTOHIDE = 1L

internal data class WindowsTaskbarSlot(
    val taskbar: HWND,
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
    private val user32 = User32.INSTANCE
    private const val PERSONALIZE_KEY = "Software\\Microsoft\\Windows\\CurrentVersion\\Themes\\Personalize"
    private const val ALIGNMENT_KEY = "Software\\Microsoft\\Windows\\CurrentVersion\\Explorer\\Advanced"

    fun findSlot(
        automation: WindowsTaskbarAutomation,
        position: Int?,
        previous: WindowsTaskbarSlot?,
    ): WindowsTaskbarSlot? =
        withScreenDpi {
            val taskbar = user32.FindWindow("Shell_TrayWnd", null) ?: return@withScreenDpi null
            if (!user32.IsWindowVisible(taskbar) || autoHides()) return@withScreenDpi null
            val bounds = taskbarScreenBounds(taskbar) ?: return@withScreenDpi null
            val dpi = taskbarUser32.GetDpiForWindow(taskbar).takeIf { it > 0 } ?: DEFAULT_DPI
            val previousLayout = previous?.takeIf { it.taskbar == taskbar && it.dpi == dpi }?.layout
            val occupied = taskbarControlBounds(bounds, automation.controls(taskbar), previousLayout)
            if (occupied.isEmpty()) return@withScreenDpi null
            val layout = WindowsTaskbarLayout(bounds, (occupied + notificationAreaBounds(taskbar)).distinct(), dpi)
            val preferredPosition = position ?: if (layout.vertical || isLeftAligned()) 100 else null
            val gap = layout.boundsAt(preferredPosition) ?: return@withScreenDpi null
            WindowsTaskbarSlot(
                taskbar,
                gap,
                dpi,
                dark = readDword(PERSONALIZE_KEY, "SystemUsesLightTheme") == 0,
                layout = layout,
            )
        }

    private fun notificationAreaBounds(taskbar: HWND): List<Rectangle> {
        val tray = user32.FindWindowEx(taskbar, null, "TrayNotifyWnd", null)
        val rect = RECT()
        return if (tray != null && user32.GetWindowRect(tray, rect)) listOf(rect.toRectangle()) else emptyList()
    }

    private fun taskbarScreenBounds(taskbar: HWND): Rectangle? {
        val rect = RECT()
        val origin = POINT()
        if (!user32.GetClientRect(taskbar, rect) || !taskbarUser32.ClientToScreen(taskbar, origin)) return null
        return rect.toRectangle().apply { location = Point(origin.x, origin.y) }
    }

    fun <T> withScreenDpi(action: () -> T): T = withDpiContext(Pointer.createConstant(-4L), action)

    fun <T> withParentDpi(
        taskbar: HWND,
        action: () -> T,
    ): T = withDpiContext(taskbarUser32.GetWindowDpiAwarenessContext(taskbar), action)

    private fun <T> withDpiContext(
        context: Pointer?,
        action: () -> T,
    ): T {
        val previous = taskbarUser32.SetThreadDpiAwarenessContext(context)
        try {
            return action()
        } finally {
            if (previous != null) taskbarUser32.SetThreadDpiAwarenessContext(previous)
        }
    }

    private fun autoHides(): Boolean {
        val data = APPBARDATA().apply { cbSize = DWORD(size().toLong()) }
        val state = Shell32.INSTANCE.SHAppBarMessage(DWORD(ShellAPI.ABM_GETSTATE.toLong()), data)
        return state.toLong() and ABS_AUTOHIDE != 0L
    }

    private fun isLeftAligned(): Boolean =
        System.getProperty("os.name") == "Windows 10" || readDword(ALIGNMENT_KEY, "TaskbarAl") == 0

    private fun readDword(
        key: String,
        name: String,
    ): Int? =
        Memory(Int.SIZE_BYTES.toLong()).use { value ->
            val result =
                Advapi32.INSTANCE.RegGetValue(
                    WinReg.HKEY_CURRENT_USER,
                    key,
                    name,
                    Advapi32.RRF_RT_REG_DWORD,
                    null,
                    value,
                    IntByReference(Int.SIZE_BYTES),
                )
            if (result == 0) value.getInt(0) else null
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

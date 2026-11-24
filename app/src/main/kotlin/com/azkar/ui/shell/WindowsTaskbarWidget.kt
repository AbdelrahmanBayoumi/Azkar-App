package com.azkar.ui.shell

import com.azkar.ui.theme.AppTypography
import com.sun.jna.Native
import com.sun.jna.Pointer
import com.sun.jna.Structure
import com.sun.jna.platform.win32.GDI32
import com.sun.jna.platform.win32.Kernel32
import com.sun.jna.platform.win32.User32
import com.sun.jna.platform.win32.WinDef.HBITMAP
import com.sun.jna.platform.win32.WinDef.HCURSOR
import com.sun.jna.platform.win32.WinDef.HDC
import com.sun.jna.platform.win32.WinDef.HINSTANCE
import com.sun.jna.platform.win32.WinDef.HWND
import com.sun.jna.platform.win32.WinDef.LPARAM
import com.sun.jna.platform.win32.WinDef.LRESULT
import com.sun.jna.platform.win32.WinDef.POINT
import com.sun.jna.platform.win32.WinDef.WPARAM
import com.sun.jna.platform.win32.WinGDI
import com.sun.jna.platform.win32.WinGDI.BITMAPINFO
import com.sun.jna.platform.win32.WinUser
import com.sun.jna.platform.win32.WinUser.BLENDFUNCTION
import com.sun.jna.platform.win32.WinUser.MSG
import com.sun.jna.platform.win32.WinUser.SIZE
import com.sun.jna.platform.win32.WinUser.WNDCLASSEX
import com.sun.jna.platform.win32.WinUser.WindowProc
import com.sun.jna.ptr.PointerByReference
import com.sun.jna.win32.StdCallLibrary
import com.sun.jna.win32.W32APIOptions
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeSource

internal data class WindowsTaskbarContent(
    val name: String,
    val time: String,
    val remaining: String,
    val locale: Locale,
    val typography: AppTypography,
    val scheduleOpen: Boolean = false,
    val menuOpen: Boolean = false,
)

private val logger = KotlinLogging.logger {}
private const val WM_SETCURSOR = 0x0020
private const val VK_LBUTTON = 0x01
private const val VK_ESCAPE = 0x1B
private const val WM_MOUSEACTIVATE = 0x0021
private const val WM_MOUSEMOVE = 0x0200
private const val WM_LBUTTONDOWN = 0x0201
private const val WM_LBUTTONUP = 0x0202
private const val WM_RBUTTONUP = 0x0205
private const val WM_MOUSELEAVE = 0x02A3
private const val WM_NCDESTROY = 0x0082
private const val TME_LEAVE = 0x00000002
private const val MA_NOACTIVATE = 3L
private const val WS_CHILD = 0x40000000
private const val WS_EX_LAYERED = 0x00080000
private const val SW_SHOWNOACTIVATE = 4
private const val SWP_NOACTIVATE = 0x0010
private const val PM_REMOVE = 1
private const val IDC_SIZEWE = 32644L
private const val IDC_HAND = 32649L

/** Created and destroyed on the same thread that dispatches the child window's messages. */
internal class WindowsTaskbarWidget(
    private val onClick: (wasOpenOnPress: Boolean, slot: WindowsTaskbarSlot) -> Unit,
    private val onContextMenu: (WindowsTaskbarSlot) -> Unit,
    private val moveEnabled: () -> Boolean,
    private val onMoveFinished: (Int?) -> Unit,
) : AutoCloseable {
    private val user32 = User32.INSTANCE
    private val instance = Kernel32.INSTANCE.GetModuleHandle(null)
    private val className = "AzkarPrayerWidget${nextClassId.incrementAndGet()}"
    private val message = MSG()
    private val mouseTracking = WidgetMouseTracking().apply { dwFlags = TME_LEAVE }
    private val cursorPoint = POINT()
    private val moveCursor = taskbarUser32.LoadCursor(null, Pointer.createConstant(IDC_SIZEWE))

    // Windows stores the callback address until the class is unregistered.
    private val procedure =
        WindowProc { handle, code, wParam, lParam ->
            windowProc(handle, code, wParam, lParam)
        }
    private var dragOffset: Double? = null
    private var dragStart = 0
    private var pixels = ByteArray(0)
    private var window: HWND? = null
    private var hovered = false
    private var scheduleWasOpenOnPress = false
    private var frame: Frame? = null
    private var attempted: Frame? = null
    private var retryAt = TimeSource.Monotonic.markNow()

    init {
        register()
    }

    val isAttached: Boolean get() = frame != null

    suspend fun update(
        slot: WindowsTaskbarSlot?,
        content: WindowsTaskbarContent?,
    ) {
        if (slot == null || content == null) {
            detach()
            attempted = null
            return
        }
        if (window != null && frame?.slot?.taskbar != slot.taskbar) detach()
        val placed = dragSlot(slot)
        val next = Frame(placed, content, hovered || content.scheduleOpen || content.menuOpen || moveEnabled())
        if (next == frame || (next == attempted && retryAt.hasNotPassedNow())) return
        // Offscreen Compose shares the desktop UI thread. Only the finished pixels cross to Win32.
        if (frame?.renderState != next.renderState) {
            pixels = withContext(Dispatchers.Main) { renderWindowsTaskbar(content, placed, next.highlighted) }
        }
        if (show(next, pixels)) {
            frame = next
            attempted = null
        } else {
            detach()
            attempted = next
            retryAt = TimeSource.Monotonic.markNow() + 1.seconds
            logger.debug { "The Windows taskbar did not accept the prayer widget" }
        }
    }

    private fun cursorAlongBar(layout: WindowsTaskbarLayout): Int? =
        WindowsTaskbar.withScreenDpi {
            if (!user32.GetCursorPos(cursorPoint)) return@withScreenDpi null
            if (layout.vertical) cursorPoint.y else cursorPoint.x
        }

    private fun dragSlot(slot: WindowsTaskbarSlot): WindowsTaskbarSlot {
        if (moveEnabled() && user32.GetAsyncKeyState(VK_ESCAPE) < 0) {
            dragOffset = null
            onMoveFinished(null)
            return slot
        }
        val offset = dragOffset
        val layout = slot.layout
        return if (offset != null && layout != null) draggedSlot(slot, layout, offset) else slot
    }

    private fun draggedSlot(
        slot: WindowsTaskbarSlot,
        layout: WindowsTaskbarLayout,
        offset: Double,
    ): WindowsTaskbarSlot {
        val cursor = cursorAlongBar(layout) ?: return slot
        val percent =
            (((cursor - offset - layout.screenStart) / layout.length) * 100).roundToInt().coerceIn(
                0,
                100,
            )
        val moved = cursor != dragStart
        if (user32.GetAsyncKeyState(VK_LBUTTON) >= 0) {
            dragOffset = null
            onMoveFinished(percent.takeIf { moved })
        }
        val bounds = if (moved) layout.boundsAt(percent) else null
        return if (bounds != null) slot.copy(bounds = bounds) else slot
    }

    fun pump() {
        while (user32.PeekMessage(message, null, 0, 0, PM_REMOVE)) {
            user32.DispatchMessage(message)
        }
    }

    private fun show(
        next: Frame,
        pixels: ByteArray,
    ): Boolean =
        WindowsTaskbar.withParentDpi(next.slot.taskbar) {
            val slot = next.slot
            val bounds = slot.bounds
            val title = "${next.content.name}, ${next.content.time}, ${next.content.remaining}"
            // Match Explorer’s DPI context when creating the child window.
            val hwnd =
                window ?: user32
                    .CreateWindowEx(
                        WS_EX_LAYERED,
                        className,
                        title,
                        WS_CHILD,
                        bounds.x,
                        bounds.y,
                        bounds.width,
                        bounds.height,
                        slot.taskbar,
                        null,
                        instance,
                        null,
                    )?.also { window = it } ?: return@withParentDpi false
            taskbarUser32.SetWindowText(hwnd, title)
            val positioned =
                user32.SetWindowPos(
                    hwnd,
                    null,
                    bounds.x,
                    bounds.y,
                    bounds.width,
                    bounds.height,
                    SWP_NOACTIVATE,
                )
            if (!positioned || !paintWidget(hwnd, bounds.width, bounds.height, pixels)) return@withParentDpi false
            user32.ShowWindow(hwnd, SW_SHOWNOACTIVATE)
            true
        }

    private fun register() {
        val wndClass =
            WNDCLASSEX().apply {
                lpfnWndProc = procedure
                hInstance = instance
                hCursor = taskbarUser32.LoadCursor(null, Pointer.createConstant(IDC_HAND))
                lpszClassName = className
            }
        check(user32.RegisterClassEx(wndClass).toInt() != 0) { "Unable to register the taskbar widget window" }
    }

    private fun handleSetCursor(
        handle: HWND,
        wParam: WPARAM,
        lParam: LPARAM,
    ): LRESULT {
        if (!moveEnabled()) return user32.DefWindowProc(handle, WM_SETCURSOR, wParam, lParam)
        taskbarUser32.SetCursor(moveCursor)
        return LRESULT(1)
    }

    private fun trackHover(handle: HWND) {
        if (handle != window || hovered) return
        mouseTracking.hwndTrack = handle
        hovered = taskbarUser32.TrackMouseEvent(mouseTracking)
    }

    private fun handleLeftPress() {
        if (!moveEnabled()) {
            // The schedule can lose focus and close before the button is released.
            scheduleWasOpenOnPress = frame?.content?.scheduleOpen == true
        } else {
            val current = frame?.slot?.takeIf { it.layout != null } ?: return
            val layout = checkNotNull(current.layout)
            val cursor = cursorAlongBar(layout) ?: return
            val center = if (layout.vertical) current.bounds.centerY else current.bounds.centerX
            dragStart = cursor
            dragOffset = cursor - layout.screenStart - center
        }
    }

    private fun handleRightRelease() {
        if (moveEnabled()) {
            dragOffset = null
            onMoveFinished(null)
        } else {
            frame?.slot?.let(onContextMenu)
        }
    }

    private fun windowProc(
        handle: HWND,
        code: Int,
        wParam: WPARAM,
        lParam: LPARAM,
    ): LRESULT {
        // Exceptions must never cross the upcall boundary.
        return runCatching {
            when (code) {
                WM_SETCURSOR -> handleSetCursor(handle, wParam, lParam)
                WM_MOUSEACTIVATE -> LRESULT(MA_NOACTIVATE)
                WM_MOUSEMOVE -> {
                    trackHover(handle)
                    LRESULT(0)
                }
                WM_MOUSELEAVE -> {
                    if (handle == window) hovered = false
                    LRESULT(0)
                }
                WM_LBUTTONDOWN -> {
                    handleLeftPress()
                    LRESULT(0)
                }
                WM_LBUTTONUP -> {
                    if (!moveEnabled()) frame?.slot?.let { onClick(scheduleWasOpenOnPress, it) }
                    scheduleWasOpenOnPress = false
                    LRESULT(0)
                }
                WM_RBUTTONUP -> {
                    handleRightRelease()
                    LRESULT(0)
                }
                WM_NCDESTROY -> {
                    if (handle == window) {
                        window = null
                        hovered = false
                        scheduleWasOpenOnPress = false
                        frame = null
                        attempted = null
                    }
                    user32.DefWindowProc(handle, code, wParam, lParam)
                }
                else -> user32.DefWindowProc(handle, code, wParam, lParam)
            }
        }.getOrElse {
            logger.warn(it) { "Unable to handle a Windows prayer widget event" }
            LRESULT(0)
        }
    }

    private fun detach() {
        val previous = window
        dragOffset = null
        window = null
        hovered = false
        scheduleWasOpenOnPress = false
        frame = null
        if (previous != null) user32.DestroyWindow(previous)
    }

    override fun close() {
        detach()
        check(user32.UnregisterClass(className, instance)) {
            "Unable to unregister the taskbar widget window"
        }
        java.lang.ref.Reference
            .reachabilityFence(procedure)
    }

    private data class Frame(
        val slot: WindowsTaskbarSlot,
        val content: WindowsTaskbarContent,
        val highlighted: Boolean,
    ) {
        val renderState = RenderState(content, highlighted, slot.dark, slot.dpi, slot.bounds.size)
    }

    private data class RenderState(
        val content: WindowsTaskbarContent,
        val highlighted: Boolean,
        val dark: Boolean,
        val dpi: Int,
        val size: java.awt.Dimension,
    )

    companion object {
        private val nextClassId = AtomicInteger()
    }
}

private fun paintWidget(
    hwnd: HWND,
    width: Int,
    height: Int,
    pixels: ByteArray,
): Boolean {
    val gdi = GDI32.INSTANCE
    val dc = gdi.CreateCompatibleDC(null) ?: return false
    val painted: Boolean
    try {
        val info =
            BITMAPINFO().apply {
                bmiHeader.biWidth = width
                bmiHeader.biHeight = -height
                bmiHeader.biPlanes = 1
                bmiHeader.biBitCount = 32
                bmiHeader.biCompression = WinGDI.BI_RGB
            }
        val bits = PointerByReference()
        val bitmap = gdi.CreateDIBSection(dc, info, WinGDI.DIB_RGB_COLORS, bits, null, 0)
        if (bitmap == null) {
            painted = false
        } else {
            try {
                bits.value.write(0, pixels, 0, pixels.size)
                painted = updateWidgetLayer(hwnd, dc, bitmap, SIZE(width, height))
            } finally {
                gdi.DeleteObject(bitmap)
            }
        }
    } finally {
        gdi.DeleteDC(dc)
    }
    return painted
}

private fun updateWidgetLayer(
    hwnd: HWND,
    dc: HDC,
    bitmap: HBITMAP,
    size: SIZE,
): Boolean {
    val gdi = GDI32.INSTANCE
    val previous = gdi.SelectObject(dc, bitmap) ?: return false
    try {
        val blend =
            BLENDFUNCTION().apply {
                SourceConstantAlpha = 0xff.toByte()
                AlphaFormat = WinUser.AC_SRC_ALPHA.toByte()
            }
        return User32.INSTANCE.UpdateLayeredWindow(hwnd, null, null, size, dc, POINT(), 0, blend, WinUser.ULW_ALPHA)
    } finally {
        gdi.SelectObject(dc, previous)
    }
}

// These APIs are absent from jna-platform's User32 interface.
// JNA uses the exact Win32 method names to resolve the exported symbols.
@Suppress("FunctionNaming", "ktlint:standard:function-naming")
internal interface TaskbarUser32 : StdCallLibrary {
    fun SetWindowText(
        window: HWND,
        text: String,
    ): Boolean

    fun LoadCursor(
        instance: HINSTANCE?,
        resource: Pointer,
    ): HCURSOR?

    fun SetCursor(cursor: HCURSOR?): HCURSOR?

    fun TrackMouseEvent(event: WidgetMouseTracking): Boolean

    fun ClientToScreen(
        window: HWND,
        point: POINT,
    ): Boolean

    fun GetDpiForWindow(window: HWND): Int

    fun GetWindowDpiAwarenessContext(window: HWND): Pointer?

    fun SetThreadDpiAwarenessContext(context: Pointer?): Pointer?
}

@Structure.FieldOrder("cbSize", "dwFlags", "hwndTrack", "dwHoverTime")
internal class WidgetMouseTracking : Structure() {
    @JvmField var cbSize: Int = 0

    @JvmField var dwFlags: Int = 0

    @JvmField var hwndTrack: HWND? = null

    @JvmField var dwHoverTime: Int = 0

    init {
        cbSize = size()
    }
}

internal val taskbarUser32 by lazy {
    Native.load("user32", TaskbarUser32::class.java, W32APIOptions.UNICODE_OPTIONS)
}

package com.azkar.ui.shell

import com.azkar.ui.theme.AppTypography
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.lang.foreign.Arena
import java.lang.foreign.FunctionDescriptor
import java.lang.foreign.MemorySegment
import java.lang.foreign.MemorySegment.NULL
import java.lang.foreign.ValueLayout.ADDRESS
import java.lang.foreign.ValueLayout.JAVA_BYTE
import java.lang.foreign.ValueLayout.JAVA_INT
import java.lang.foreign.ValueLayout.JAVA_LONG
import java.lang.foreign.ValueLayout.JAVA_SHORT
import java.lang.invoke.MethodHandles
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

/** Created and destroyed on the same thread that dispatches the child window's messages. */
internal class WindowsTaskbarWidget(
    private val onClick: (wasOpenOnPress: Boolean, slot: WindowsTaskbarSlot) -> Unit,
    private val onContextMenu: (WindowsTaskbarSlot) -> Unit,
    private val moveEnabled: () -> Boolean,
    private val onMoveFinished: (Int?) -> Unit,
) : AutoCloseable {
    private val arena = Arena.ofConfined()
    private val instance = getModuleHandle.invokeExact(NULL) as MemorySegment
    private val className = arena.wideString("AzkarPrayerWidget${nextClassId.incrementAndGet()}")
    private val message = arena.allocate(48, 8) // MSG on x64
    private val mouseTracking =
        arena.allocate(24, 8).apply {
            // TRACKMOUSEEVENT on x64
            set(JAVA_INT, 0, 24)
            set(JAVA_INT, 4, TME_LEAVE)
        }
    private val cursorPoint = arena.allocate(8, 4)
    private val moveCursor = loadCursor.invokeExact(NULL, MemorySegment.ofAddress(32644)) as MemorySegment // IDC_SIZEWE
    private var dragOffset: Double? = null
    private var dragStart = 0
    private var pixels = ByteArray(0)
    private var window = NULL
    private var hovered = false
    private var scheduleWasOpenOnPress = false
    private var frame: Frame? = null
    private var attempted: Frame? = null
    private var retryAt = TimeSource.Monotonic.markNow()

    init {
        runCatching {
            register()
        }.onFailure { failure ->
            arena.close()
            throw failure
        }
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
        if (window != NULL && frame?.slot?.taskbar != slot.taskbar) detach()
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
            if ((getCursorPos.invokeExact(cursorPoint) as Int) == 0) return@withScreenDpi null
            cursorPoint.get(JAVA_INT, if (layout.vertical) 4L else 0L)
        }

    private fun dragSlot(slot: WindowsTaskbarSlot): WindowsTaskbarSlot {
        if (moveEnabled() && (getAsyncKeyState.invokeExact(VK_ESCAPE) as Short) < 0) {
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
        if ((getAsyncKeyState.invokeExact(VK_LBUTTON) as Short) >= 0) {
            dragOffset = null
            onMoveFinished(percent.takeIf { moved })
        }
        val bounds = if (moved) layout.boundsAt(percent) else null
        return if (bounds != null) slot.copy(bounds = bounds) else slot
    }

    fun pump() {
        while ((peekMessage.invokeExact(message, NULL, 0, 0, PM_REMOVE) as Int) != 0) {
            dispatchMessage.invokeExact(message) as Long
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
            Arena.ofConfined().use { scratch ->
                val caption = scratch.wideString(title)
                if (window == NULL) {
                    // Create a real child with Explorer's DPI context. SetParent can reset the
                    // child process's DPI awareness when crossing process boundaries.
                    window =
                        createWindow.invokeExact(
                            WS_EX_LAYERED,
                            className,
                            caption,
                            WS_CHILD,
                            bounds.x,
                            bounds.y,
                            bounds.width,
                            bounds.height,
                            slot.taskbar,
                            NULL,
                            instance,
                            NULL,
                        ) as MemorySegment
                    if (window == NULL) return@use false
                }
                setWindowText.invokeExact(window, caption) as Int
                val positioned =
                    setWindowPos.invokeExact(
                        window,
                        NULL,
                        bounds.x,
                        bounds.y,
                        bounds.width,
                        bounds.height,
                        SWP_NOACTIVATE,
                    ) as Int
                if (positioned == 0 || !paintWidget(window, bounds.width, bounds.height, pixels)) return@use false
                showWindow.invokeExact(window, SW_SHOWNOACTIVATE) as Int
                true
            }
        }

    private fun register() {
        val procedure =
            MethodHandles
                .lookup()
                .findVirtual(WindowsTaskbarWidget::class.java, "windowProc", windowProc.toMethodType())
                .bindTo(this)
        // WNDCLASSEXW on x64. The other optional handles remain zero.
        val wndClass = arena.allocate(80, 8)
        wndClass.set(JAVA_INT, 0, 80)
        wndClass.set(ADDRESS, 8, Win32.linker.upcallStub(procedure, windowProc, arena))
        wndClass.set(ADDRESS, 24, instance)
        val handCursor = loadCursor.invokeExact(NULL, MemorySegment.ofAddress(32649)) as MemorySegment // IDC_HAND
        wndClass.set(ADDRESS, 40, handCursor)
        wndClass.set(ADDRESS, 64, className)
        val atom = registerClass.invokeExact(wndClass) as Short
        check(atom.toInt() != 0) { "Unable to register the taskbar widget window" }
    }

    private fun handleSetCursor(
        handle: MemorySegment,
        wParam: Long,
        lParam: Long,
    ): Long {
        if (!moveEnabled()) return defWindowProc.invokeExact(handle, WM_SETCURSOR, wParam, lParam) as Long
        setCursor.invokeExact(moveCursor) as MemorySegment
        return 1L
    }

    private fun trackHover(handle: MemorySegment) {
        if (handle != window || hovered) return
        mouseTracking.set(ADDRESS, 8, handle)
        hovered = (trackMouseEvent.invokeExact(mouseTracking) as Int) != 0
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
        handle: MemorySegment,
        code: Int,
        wParam: Long,
        lParam: Long,
    ): Long {
        // Exceptions must never cross the upcall boundary.
        return runCatching {
            when (code) {
                WM_SETCURSOR -> handleSetCursor(handle, wParam, lParam)
                WM_MOUSEACTIVATE -> MA_NOACTIVATE
                WM_MOUSEMOVE -> {
                    trackHover(handle)
                    0L
                }
                WM_MOUSELEAVE -> {
                    if (handle == window) hovered = false
                    0L
                }
                WM_LBUTTONDOWN -> {
                    handleLeftPress()
                    0L
                }
                WM_LBUTTONUP -> {
                    if (!moveEnabled()) frame?.slot?.let { onClick(scheduleWasOpenOnPress, it) }
                    scheduleWasOpenOnPress = false
                    0L
                }
                WM_RBUTTONUP -> {
                    handleRightRelease()
                    0L
                }
                WM_NCDESTROY -> {
                    if (handle == window) {
                        window = NULL
                        hovered = false
                        scheduleWasOpenOnPress = false
                        frame = null
                        attempted = null
                    }
                    defWindowProc.invokeExact(handle, code, wParam, lParam) as Long
                }
                else -> defWindowProc.invokeExact(handle, code, wParam, lParam) as Long
            }
        }.getOrElse {
            logger.warn(it) { "Unable to handle a Windows prayer widget event" }
            0L
        }
    }

    private fun detach() {
        val previous = window
        dragOffset = null
        window = NULL
        hovered = false
        scheduleWasOpenOnPress = false
        frame = null
        if (previous != NULL) destroyWindow.invokeExact(previous) as Int
    }

    override fun close() {
        detach()
        // The class and its WndProc must be gone before the upcall's memory can be released.
        check((unregisterClass.invokeExact(className, instance) as Int) != 0) {
            "Unable to unregister the taskbar widget window"
        }
        arena.close()
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
        private val windowProc = FunctionDescriptor.of(JAVA_LONG, ADDRESS, JAVA_INT, JAVA_LONG, JAVA_LONG)
        private val getModuleHandle = Win32.kernel32("GetModuleHandleW", FunctionDescriptor.of(ADDRESS, ADDRESS))
        private val registerClass = Win32.user32("RegisterClassExW", FunctionDescriptor.of(JAVA_SHORT, ADDRESS))
        private val unregisterClass =
            Win32.user32("UnregisterClassW", FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS))
        private val defWindowProc =
            Win32.user32(
                "DefWindowProcW",
                FunctionDescriptor.of(
                    JAVA_LONG,
                    ADDRESS,
                    JAVA_INT,
                    JAVA_LONG,
                    JAVA_LONG,
                ),
            )
        private val destroyWindow = Win32.user32("DestroyWindow", FunctionDescriptor.of(JAVA_INT, ADDRESS))
        private val showWindow = Win32.user32("ShowWindow", FunctionDescriptor.of(JAVA_INT, ADDRESS, JAVA_INT))
        private val setWindowText = Win32.user32("SetWindowTextW", FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS))
        private val loadCursor = Win32.user32("LoadCursorW", FunctionDescriptor.of(ADDRESS, ADDRESS, ADDRESS))
        private val getCursorPos = Win32.user32("GetCursorPos", FunctionDescriptor.of(JAVA_INT, ADDRESS))
        private val getAsyncKeyState = Win32.user32("GetAsyncKeyState", FunctionDescriptor.of(JAVA_SHORT, JAVA_INT))
        private val setCursor = Win32.user32("SetCursor", FunctionDescriptor.of(ADDRESS, ADDRESS))
        private val trackMouseEvent = Win32.user32("TrackMouseEvent", FunctionDescriptor.of(JAVA_INT, ADDRESS))
        private val setWindowPos =
            Win32.user32(
                "SetWindowPos",
                FunctionDescriptor.of(
                    JAVA_INT,
                    ADDRESS,
                    ADDRESS,
                    JAVA_INT,
                    JAVA_INT,
                    JAVA_INT,
                    JAVA_INT,
                    JAVA_INT,
                ),
            )
        private val createWindow =
            Win32.user32(
                "CreateWindowExW",
                FunctionDescriptor.of(
                    ADDRESS,
                    JAVA_INT,
                    ADDRESS,
                    ADDRESS,
                    JAVA_INT,
                    JAVA_INT,
                    JAVA_INT,
                    JAVA_INT,
                    JAVA_INT,
                    ADDRESS,
                    ADDRESS,
                    ADDRESS,
                    ADDRESS,
                ),
            )
        private val peekMessage =
            Win32.user32(
                "PeekMessageW",
                FunctionDescriptor.of(
                    JAVA_INT,
                    ADDRESS,
                    ADDRESS,
                    JAVA_INT,
                    JAVA_INT,
                    JAVA_INT,
                ),
            )
        private val dispatchMessage = Win32.user32("DispatchMessageW", FunctionDescriptor.of(JAVA_LONG, ADDRESS))
    }
}

private fun paintWidget(
    hwnd: MemorySegment,
    width: Int,
    height: Int,
    pixels: ByteArray,
): Boolean =
    Arena.ofConfined().use { arena ->
        val dc = bitmapDc.invokeExact(NULL) as MemorySegment
        if (dc == NULL) return false
        try {
            val header = arena.allocate(40, 4) // BITMAPINFOHEADER, 32-bit BI_RGB
            header.set(JAVA_INT, 0, 40)
            header.set(JAVA_INT, 4, width)
            header.set(JAVA_INT, 8, -height)
            header.set(JAVA_SHORT, 12, 1)
            header.set(JAVA_SHORT, 14, 32)
            val bits = arena.allocate(ADDRESS)
            val bitmap = dib.invokeExact(dc, header, 0, bits, NULL, 0) as MemorySegment
            if (bitmap == NULL) return false
            try {
                bits.get(ADDRESS, 0).reinterpret(pixels.size.toLong()).copyFrom(MemorySegment.ofArray(pixels))
                val previous = selectObject.invokeExact(dc, bitmap) as MemorySegment
                if (previous == NULL) return false
                try {
                    updateLayer(hwnd, dc, arena, width, height)
                } finally {
                    selectObject.invokeExact(dc, previous) as MemorySegment
                }
            } finally {
                deleteObject.invokeExact(bitmap) as Int
            }
        } finally {
            deleteDc.invokeExact(dc) as Int
        }
    }

private fun updateLayer(
    hwnd: MemorySegment,
    dc: MemorySegment,
    arena: Arena,
    width: Int,
    height: Int,
): Boolean {
    val source = arena.allocate(8, 4) // POINT(0,0)
    val size = arena.allocate(8, 4)
    size.set(JAVA_INT, 0, width)
    size.set(JAVA_INT, 4, height)
    val blend = arena.allocate(4)
    blend.set(JAVA_BYTE, 2, (-1).toByte()) // full source opacity
    blend.set(JAVA_BYTE, 3, 1) // AC_SRC_ALPHA
    return (updateLayer.invokeExact(hwnd, NULL, NULL, size, dc, source, 0, blend, 2) as Int) != 0 // ULW_ALPHA
}

private val bitmapDc by lazy { Win32.gdi32("CreateCompatibleDC", FunctionDescriptor.of(ADDRESS, ADDRESS)) }
private val deleteDc by lazy { Win32.gdi32("DeleteDC", FunctionDescriptor.of(JAVA_INT, ADDRESS)) }
private val dib by lazy {
    Win32.gdi32(
        "CreateDIBSection",
        FunctionDescriptor.of(
            ADDRESS,
            ADDRESS,
            ADDRESS,
            JAVA_INT,
            ADDRESS,
            ADDRESS,
            JAVA_INT,
        ),
    )
}
private val selectObject by lazy { Win32.gdi32("SelectObject", FunctionDescriptor.of(ADDRESS, ADDRESS, ADDRESS)) }
private val deleteObject by lazy { Win32.gdi32("DeleteObject", FunctionDescriptor.of(JAVA_INT, ADDRESS)) }
private val updateLayer by lazy {
    Win32.user32(
        "UpdateLayeredWindow",
        FunctionDescriptor.of(
            JAVA_INT,
            ADDRESS,
            ADDRESS,
            ADDRESS,
            ADDRESS,
            ADDRESS,
            ADDRESS,
            JAVA_INT,
            ADDRESS,
            JAVA_INT,
        ),
    )
}

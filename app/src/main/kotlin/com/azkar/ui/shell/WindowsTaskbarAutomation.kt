package com.azkar.ui.shell

import com.sun.jna.Pointer
import com.sun.jna.platform.win32.WinDef.HWND
import java.awt.Rectangle
import java.lang.foreign.Arena
import java.lang.foreign.FunctionDescriptor
import java.lang.foreign.Linker
import java.lang.foreign.MemorySegment
import java.lang.foreign.MemorySegment.NULL
import java.lang.foreign.SymbolLookup
import java.lang.foreign.ValueLayout.ADDRESS
import java.lang.foreign.ValueLayout.JAVA_BYTE
import java.lang.foreign.ValueLayout.JAVA_INT
import java.lang.foreign.ValueLayout.JAVA_SHORT
import java.lang.invoke.MethodHandle
import java.util.UUID

/** UI Automation calls stay on a COM MTA thread that owns no windows. */
internal class WindowsTaskbarAutomation private constructor(
    private val automation: AutomationObject,
) {
    fun controls(taskbar: HWND): List<Rectangle> =
        Arena.ofConfined().use { arena ->
            automation
                .objectResult(arena, 6, MemorySegment.ofAddress(Pointer.nativeValue(taskbar.pointer)))
                ?.use { root ->
                    // ElementFromHandle
                    automation.objectResult(arena, 18)?.use { condition ->
                        // ControlViewCondition
                        automation.objectResult(arena, 20)?.use { request ->
                            // CreateCacheRequest
                            readControls(arena, root, condition, request)
                        }
                    }
                }.orEmpty()
        }

    private fun readControls(
        arena: Arena,
        root: AutomationObject,
        condition: AutomationObject,
        request: AutomationObject,
    ): List<Rectangle> {
        // Cache the three fields in one cross-process call (UIA property IDs).
        val fields = intArrayOf(30003, 30022, 30001)
        // AddProperty and put_AutomationElementMode(AutomationElementMode_None).
        if (fields.any { request.setInt(3, it) < 0 } || request.setInt(11, 0) < 0) return emptyList()
        val output = arena.allocate(ADDRESS)
        val hr =
            findAll.invokeExact(
                root.function(8),
                root.pointer,
                4,
                condition.pointer,
                request.pointer,
                output,
            ) as Int
        // FindAllBuildCache(TreeScope_Descendants)
        return if (hr < 0) emptyList() else AutomationObject(output.get(ADDRESS, 0)).use { it.controlBounds(arena) }
    }

    companion object {
        private val initialize = Win32.ole32("CoInitializeEx", FunctionDescriptor.of(JAVA_INT, ADDRESS, JAVA_INT))
        private val uninitialize = Win32.ole32("CoUninitialize", FunctionDescriptor.ofVoid())
        private val createInstance =
            Win32.ole32(
                "CoCreateInstance",
                FunctionDescriptor.of(
                    JAVA_INT,
                    ADDRESS,
                    ADDRESS,
                    JAVA_INT,
                    ADDRESS,
                    ADDRESS,
                ),
            )
        private val findAll =
            Win32.linker.downcallHandle(
                FunctionDescriptor.of(JAVA_INT, ADDRESS, JAVA_INT, ADDRESS, ADDRESS, ADDRESS),
            )

        fun <T> withApartment(action: (WindowsTaskbarAutomation) -> T): T =
            Arena.ofConfined().use { arena ->
                // COINIT_MULTITHREADED and CLSCTX_INPROC_SERVER
                check((initialize.invokeExact(NULL, 0) as Int) >= 0) { "Unable to initialize taskbar automation" }
                try {
                    val clsid = arena.guid("e22ad333-b25f-460c-83d0-0581107395c9") // CUIAutomation8
                    val iid = arena.guid("34723aff-0c9d-49d0-9896-7ab52df8cd8a") // IUIAutomation2
                    val output = arena.allocate(ADDRESS)
                    check((createInstance.invokeExact(clsid, NULL, 1, iid, output) as Int) >= 0) {
                        "Unable to connect to Windows UI Automation"
                    }
                    AutomationObject(output.get(ADDRESS, 0)).use {
                        it.setInt(61, 800) // ConnectionTimeout
                        it.setInt(63, 1000) // TransactionTimeout
                        action(WindowsTaskbarAutomation(it))
                    }
                } finally {
                    uninitialize.invokeExact()
                }
            }
    }
}

private class AutomationObject(
    val pointer: MemorySegment,
) : AutoCloseable {
    private val vtable = pointer.reinterpret(ADDRESS.byteSize()).get(ADDRESS, 0)

    fun function(index: Int): MemorySegment =
        vtable.reinterpret((index + 1) * ADDRESS.byteSize()).getAtIndex(ADDRESS, index.toLong())

    fun objectResult(
        arena: Arena,
        index: Int,
        argument: MemorySegment = NULL,
    ): AutomationObject? {
        val out = arena.allocate(ADDRESS)
        val result =
            if (argument ==
                NULL
            ) {
                getValue(index, out)
            } else {
                getObject.invokeExact(function(index), pointer, argument, out) as Int
            }
        val address = out.get(ADDRESS, 0)
        return if (result < 0 || address == NULL) null else AutomationObject(address)
    }

    fun setInt(
        index: Int,
        value: Int,
    ): Int = setInt.invokeExact(function(index), pointer, value) as Int

    fun controlBounds(arena: Arena): List<Rectangle> {
        val length = arena.allocate(JAVA_INT)
        if (getValue(3, length) < 0) return emptyList() // IUIAutomationElementArray::get_Length
        val out = arena.allocate(ADDRESS)
        val rect = arena.allocate(16, 4)
        val value = arena.allocate(JAVA_INT)
        return buildList {
            for (index in 0 until length.get(JAVA_INT, 0)) {
                if ((getElement.invokeExact(function(4), pointer, index, out) as Int) < 0) continue
                val element = out.get(ADDRESS, 0)
                if (element != NULL) AutomationObject(element).use { it.visibleControl(value, rect)?.let(::add) }
            }
        }
    }

    private fun visibleControl(
        value: MemorySegment,
        rect: MemorySegment,
    ): Rectangle? {
        val isTaskbarControl = getValue(53, value) >= 0 && value.get(JAVA_INT, 0) in taskbarControlTypes
        val isOnscreen = isTaskbarControl && getValue(70, value) >= 0 && value.get(JAVA_INT, 0) == 0
        return if (isOnscreen && getValue(75, rect) >= 0) rect.asRectangle() else null
    }

    private fun getValue(
        index: Int,
        out: MemorySegment,
    ): Int = getValue.invokeExact(function(index), pointer, out) as Int

    override fun close() {
        release.invokeExact(function(2), pointer) as Int
    }

    companion object {
        private val release = Win32.linker.downcallHandle(FunctionDescriptor.of(JAVA_INT, ADDRESS))
        private val getValue = Win32.linker.downcallHandle(FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS))
        private val getObject = Win32.linker.downcallHandle(FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS, ADDRESS))
        private val setInt = Win32.linker.downcallHandle(FunctionDescriptor.of(JAVA_INT, ADDRESS, JAVA_INT))
        private val getElement =
            Win32.linker.downcallHandle(
                FunctionDescriptor.of(JAVA_INT, ADDRESS, JAVA_INT, ADDRESS),
            )

        // Button, CheckBox, ComboBox, Edit, Hyperlink, ListItem, MenuItem, RadioButton, TabItem, SplitButton.
        private val taskbarControlTypes = setOf(50000, 50002, 50003, 50004, 50005, 50007, 50011, 50013, 50019, 50031)
    }
}

private fun Arena.guid(text: String): MemorySegment {
    val id = UUID.fromString(text)
    val buffer = allocate(16, 4)
    val high = id.mostSignificantBits
    buffer.set(JAVA_INT, 0, (high ushr 32).toInt())
    buffer.set(JAVA_SHORT, 4, (high ushr 16).toShort())
    buffer.set(JAVA_SHORT, 6, high.toShort())
    for (i in 0..7) {
        buffer.set(
            JAVA_BYTE,
            8L + i,
            (
                id.leastSignificantBits ushr
                    (56 - 8 * i)
            ).toByte(),
        )
    }
    return buffer
}

private fun MemorySegment.asRectangle(): Rectangle {
    val left = get(JAVA_INT, 0)
    val top = get(JAVA_INT, 4)
    return Rectangle(left, top, get(JAVA_INT, 8) - left, get(JAVA_INT, 12) - top)
}

private object Win32 {
    val linker: Linker = Linker.nativeLinker()
    private val ole by lazy { SymbolLookup.libraryLookup("ole32.dll", Arena.global()) }

    fun ole32(
        name: String,
        signature: FunctionDescriptor,
    ): MethodHandle = linker.downcallHandle(ole.findOrThrow(name), signature)
}

package com.azkar.ui.shell

import com.azkar.ui.theme.AppTypography
import de.infix.testBalloon.framework.core.testSuite
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.assertj.core.api.Assertions.assertThat
import java.awt.Dimension
import java.awt.Insets
import java.awt.Point
import java.awt.Rectangle
import java.lang.foreign.MemorySegment
import java.util.Locale

@Suppress("unused")
val DesktopPrayerPanelSuite by testSuite("The desktop prayer panel") {
    test("the same prayer panel is offered on Windows 10 and 11") {
        assertThat(windowsPrayerPanel("Windows 10")).isNotNull()
        assertThat(windowsPrayerPanel("Windows 11")).isNotNull()
        assertThat(windowsPrayerPanel("Windows 8.1")).isNull()
        assertThat(windowsPrayerPanel("Windows Server 2025")).isNull()
    }

    test("the GNOME panel is offered on Ubuntu 24 and newer") {
        val desktop = "ubuntu:GNOME"
        assertThat(isGnomePrayerPanelSupported(desktop, "ID=ubuntu\nVERSION_ID=\"24.04\"")).isTrue()
        assertThat(isGnomePrayerPanelSupported(desktop, "ID=ubuntu\nVERSION_ID=26.04")).isTrue()
        assertThat(isGnomePrayerPanelSupported(desktop, "ID=ubuntu\nVERSION_ID=22.04")).isFalse()
        assertThat(isGnomePrayerPanelSupported(desktop, "ID=linuxmint\nID_LIKE=ubuntu\nVERSION_ID=24")).isFalse()
        assertThat(isGnomePrayerPanelSupported(desktop, "ID=ubuntu")).isFalse()
    }

    test("the GNOME panel is offered on Fedora GNOME sessions") {
        val fedora = "ID=fedora\nVERSION_ID=45"
        assertThat(isGnomePrayerPanelSupported("GNOME", fedora)).isTrue()
        assertThat(isGnomePrayerPanelSupported("GNOME-Classic:GNOME", fedora)).isTrue()
        assertThat(isGnomePrayerPanelSupported("KDE", fedora)).isFalse()
        assertThat(isGnomePrayerPanelSupported("", fedora)).isFalse()
        assertThat(isGnomePrayerPanelSupported("GNOME", "ID=rhel\nID_LIKE=fedora")).isFalse()
    }

    test("the widget fits between Widgets and the centered taskbar controls") {
        val bar = Rectangle(0, 781, 1305, 48)
        val controls = listOf(Rectangle(364, 781, 560, 48), Rectangle(1063, 781, 242, 48), Rectangle(6, 781, 152, 48))
        assertThat(windowsTaskbarGap(bar, controls, Dimension(174, 40), 6)).isEqualTo(Rectangle(164, 785, 174, 40))
    }

    test("a crowded taskbar leaves its controls accessible") {
        val bar = Rectangle(0, 781, 1305, 48)
        val controls = listOf(Rectangle(0, 781, 900, 48), Rectangle(1030, 781, 275, 48))
        assertThat(windowsTaskbarGap(bar, controls, Dimension(174, 40), 6)).isNull()
    }

    test("the widget fits a narrower gap when a running app shifts Start left") {
        val bar = Rectangle(0, 781, 1305, 48)
        val controls = listOf(Rectangle(342, 781, 963, 48), Rectangle(6, 781, 152, 48))
        assertThat(windowsTaskbarGap(bar, controls, Dimension(174, 40), 6, minimumWidth = 148))
            .isEqualTo(Rectangle(164, 785, 172, 40))
    }

    test("left-aligned Start leaves the widget near the right-side tray") {
        val bar = Rectangle(0, 781, 1305, 48)
        val controls = listOf(Rectangle(0, 781, 318, 48), Rectangle(940, 781, 365, 48))
        assertThat(windowsTaskbarGap(bar, controls, Dimension(174, 40), 6, position = 100))
            .isEqualTo(Rectangle(760, 785, 174, 40))
        assertThat(windowsTaskbarGap(bar, controls, Dimension(174, 40), 6, position = 50))
            .isEqualTo(Rectangle(566, 785, 174, 40))
    }

    test("left-aligned controls leave room by the tray on a scaled display") {
        val bar = Rectangle(-1600, 0, 1600, 60)
        val controls = listOf(Rectangle(-1600, 0, 950, 60), Rectangle(-240, 0, 240, 60))
        val size = Dimension(windowsPixels(174, 120), windowsPixels(40, 120))
        assertThat(windowsTaskbarGap(bar, controls, size, windowsPixels(6, 120)))
            .isEqualTo(Rectangle(-642, 5, 218, 50))
    }

    test("the Explorer context menu preserves the last taskbar layout") {
        val bar = Rectangle(0, 781, 1305, 48)
        val weather = Rectangle(6, 781, 152, 48)
        val start = Rectangle(476, 781, 45, 48)
        val previous = WindowsTaskbarLayout(bar, listOf(weather, start), dpi = 96)
        val menuRows = listOf(Rectangle(945, 737, 149, 28), Rectangle(945, 772, 149, 28))

        val controls = taskbarControlBounds(bar, menuRows, previous)
        assertThat(WindowsTaskbarLayout(bar, controls, dpi = 96).boundsAt(null)).isEqualTo(previous.boundsAt(null))
        assertThat(taskbarControlBounds(bar, menuRows, null)).isEmpty()
        assertThat(taskbarControlBounds(Rectangle(0, 0, 1305, 48), menuRows, previous)).isEmpty()
        assertThat(taskbarControlBounds(bar, listOf(start) + menuRows, previous)).containsExactly(start)
    }

    test("a vertical taskbar reserves a gap above the notification area on either edge") {
        for (x in listOf(-48, 1257)) {
            val layout =
                WindowsTaskbarLayout(
                    Rectangle(x, 20, 48, 800),
                    listOf(Rectangle(x, 240, 48, 300), Rectangle(x, 660, 48, 160)),
                    dpi = 96,
                )
            assertThat(layout.boundsAt(100)).isEqualTo(Rectangle(4, 580, 40, 54))
            assertThat(layout.boundsAt(0)).isEqualTo(Rectangle(4, 6, 40, 54))
        }
    }

    test("Compose renders a transparent taskbar bitmap and mirrors the clock for Arabic") {
        withContext(Dispatchers.Main) {
            val english = WindowsTaskbarContent("Maghrib", "4:44 PM", "43m", Locale.ENGLISH, AppTypography())
            val arabic = english.copy(name = "المغرب", time = "٤:٤٤ م", locale = Locale.forLanguageTag("ar"))
            val cases = listOf(english to 96, arabic to 144)
            for ((text, dpi) in cases) {
                val width = windowsPixels(148, dpi)
                val height = windowsPixels(40, dpi)
                val rtl = text == arabic
                val slot = WindowsTaskbarSlot(MemorySegment.NULL, Rectangle(width, height), dpi, dark = !rtl)
                val pixels = renderWindowsTaskbar(text, slot, highlighted = false)
                assertThat(pixels).hasSize(width * height * 4)
                assertThat(pixels[3].toInt()).isZero()
                val surfaceAlpha = (height / 2 * width) * 4 + 3
                assertThat(pixels[surfaceAlpha].toInt()).isEqualTo(1)
                val highlighted = renderWindowsTaskbar(text, slot, highlighted = true)
                assertThat(highlighted[surfaceAlpha].toInt()).isGreaterThan(1)

                val iconInset = windowsPixels(8, dpi)
                val iconSize = windowsPixels(14, dpi)
                val iconX = if (rtl) width - iconInset - iconSize else iconInset
                val baseline = height / 2 * width
                val blue =
                    (iconX until iconX + iconSize).any { x ->
                        val offset = (baseline + x) * 4
                        pixels[offset].toUByte() > pixels[offset + 2].toUByte()
                    }
                assertThat(blue).isTrue()
            }
        }
    }

    test("the schedule opens at the widget and fits inside the screen work area") {
        val screen = Rectangle(0, 0, 900, 600)
        val insets = Insets(0, 0, 48, 0)
        val schedule = prayerScheduleBounds(screen, insets, Point(180, 570), Dimension(576, 568))
        assertThat(schedule).isEqualTo(Rectangle(8, 8, 576, 536))
        assertThat(prayerScheduleBounds(screen, Insets(48, 0, 0, 0), Point(330, 20), Dimension(288, 284)))
            .isEqualTo(Rectangle(186, 56, 288, 284))
    }
}

package com.azkar.ui.theme

import de.infix.testBalloon.framework.core.testSuite
import org.assertj.core.api.Assertions.assertThat

/**
 * The parsers behind [readSystemTheme].
 *
 * The reading of a real desktop is a subprocess and two files on disk, so it is the parsing that
 * deserves pinning down: what GNOME prints, what Plasma and GTK write, and the one value that means
 * "no preference" rather than "light". That last one is the trap — treating `'default'` as light
 * would silently contradict a desktop that is dark through some other route.
 *
 * TestBalloon generates the test entry point, but the IDE does not see that usage.
 */
@Suppress("unused")
val SystemThemeSuite by testSuite("Reading the desktop's theme") {
    test("GNOME's quoted values are read as the palette they name") {
        assertThat("'prefer-dark'".gsettingsColorScheme()).isEqualTo(SystemTheme.DARK)
        assertThat("'prefer-light'".gsettingsColorScheme()).isEqualTo(SystemTheme.LIGHT)
    }

    test("GNOME's unset value is no answer rather than a light desktop") {
        // `gsettings get org.gnome.desktop.interface color-scheme` prints exactly this when the
        // preference has never been touched, and the desktop may still be dark through a theme.
        assertThat("'default'".gsettingsColorScheme()).isNull()
        assertThat("".gsettingsColorScheme()).isNull()
        assertThat(null.gsettingsColorScheme()).isNull()
    }

    test("an unquoted value still reads, so a future gsettings cannot quietly break it") {
        assertThat("prefer-dark".gsettingsColorScheme()).isEqualTo(SystemTheme.DARK)
    }

    test("a theme name that says dark is dark, whatever its capitalisation") {
        assertThat("BreezeDark".themeNameOrNull()).isEqualTo(SystemTheme.DARK)
        assertThat("adwaita-dark".themeNameOrNull()).isEqualTo(SystemTheme.DARK)
    }

    test("a theme name that does not say dark is the light theme") {
        assertThat("Breeze".themeNameOrNull()).isEqualTo(SystemTheme.LIGHT)
        assertThat("adwaita".themeNameOrNull()).isEqualTo(SystemTheme.LIGHT)
    }

    test("a missing theme name is no answer") {
        assertThat("".themeNameOrNull()).isNull()
        assertThat("   ".themeNameOrNull()).isNull()
        assertThat(null.themeNameOrNull()).isNull()
    }

    test("an unreadable desktop is treated as light rather than as dark") {
        // The header toggle offers "switch to light" while the app is dark, so a palette that cannot
        // be established has to land on light or the button would read as already pressed.
        assertThat(SystemTheme.UNKNOWN.isDark).isFalse()
        assertThat(SystemTheme.DARK.isDark).isTrue()
    }
}

/** A Plasma config as it actually looks: the look and feel in one section, the colour scheme in another. */
private val kdeConfig =
    """
    [KDE]
    LookAndFeelPackage=org.kde.breezedark.desktop

    [General]
    ColorScheme=BreezeLight
    """.trimIndent()

// TestBalloon generates the test entry point, but the IDE does not see that usage.
@Suppress("unused")
val IniValueSuite by testSuite("Reading a setting out of an INI file") {
    test("the first key that is present wins, however late the other one is") {
        assertThat(iniValue(kdeConfig, listOf("LookAndFeelPackage", "ColorScheme")))
            .isEqualTo("org.kde.breezedark.desktop")
    }

    test("a later key is found when the earlier one is absent") {
        assertThat(iniValue(kdeConfig, listOf("MissingKey", "ColorScheme"))).isEqualTo("BreezeLight")
    }

    test("a missing key, a missing file and an empty value are all no answer") {
        assertThat(iniValue(kdeConfig, listOf("NothingLikeThis"))).isNull()
        assertThat(iniValue(null, listOf("LookAndFeelPackage"))).isNull()
        assertThat(iniValue("ColorScheme=", listOf("ColorScheme"))).isNull()
    }

    test("a commented out value is not a value") {
        val commented = "[KDE]\n#LookAndFeelPackage=org.kde.breezedark.desktop"

        assertThat(iniValue(commented, listOf("LookAndFeelPackage"))).isNull()
    }

    test("a section header is not mistaken for a key") {
        assertThat(iniValue("[LookAndFeelPackage]\n", listOf("LookAndFeelPackage"))).isNull()
    }

    test("surrounding whitespace is trimmed off") {
        assertThat(iniValue("  ColorScheme  =  BreezeDark  ", listOf("ColorScheme")))
            .isEqualTo("BreezeDark")
    }
}

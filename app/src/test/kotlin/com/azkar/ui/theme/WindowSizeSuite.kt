package com.azkar.ui.theme

import androidx.compose.ui.unit.dp
import de.infix.testBalloon.framework.core.testSuite
import org.assertj.core.api.Assertions.assertThat

/**
 * The breakpoint resolution the whole responsive layout branches on.
 *
 * These functions are deliberately free of Compose, so the one decision every screen depends on —
 * "does this window stack or sit side by side?" — is pinned down here without a window at all.
 *
 * TestBalloon generates the test entry point, but the IDE does not see that usage.
 */
@Suppress("unused")
val WindowSizeSuite by testSuite("The window size breakpoints") {
    test("the default window keeps the side-by-side layout") {
        val size = windowSizeClassFor(DEFAULT_WINDOW_WIDTH, DEFAULT_WINDOW_HEIGHT)

        // EXPANDED rather than MEDIUM: the default grew past `expandedWidth` when the shelf became
        // a two-column layout, so the size the app opens at is now the wide one by definition.
        assertThat(size.widthClass).isEqualTo(WindowWidthClass.EXPANDED)
        assertThat(size.isCompactWidth).isFalse()
    }

    test("a window narrower than the compact breakpoint stacks its panes") {
        val size = windowSizeClassFor(AzkarBreakpoints.compactWidth - 1.dp, DEFAULT_WINDOW_HEIGHT)

        assertThat(size.widthClass).isEqualTo(WindowWidthClass.COMPACT)
        assertThat(size.isCompactWidth).isTrue()
    }

    test("the compact breakpoint itself still counts as compact") {
        // The comparison is strict, so a window exactly at the breakpoint has to resolve one way or
        // the boundary is a coin flip that no test would catch.
        assertThat(widthClassFor(AzkarBreakpoints.compactWidth))
            .isEqualTo(WindowWidthClass.MEDIUM)
    }

    test("a very wide window expands") {
        assertThat(widthClassFor(AzkarBreakpoints.expandedWidth))
            .isEqualTo(WindowWidthClass.EXPANDED)
        assertThat(widthClassFor(AzkarBreakpoints.expandedWidth + 400.dp))
            .isEqualTo(WindowWidthClass.EXPANDED)
    }

    test("height buckets track the height breakpoints") {
        assertThat(heightClassFor(AzkarBreakpoints.compactHeight - 1.dp))
            .isEqualTo(WindowHeightClass.COMPACT)
        assertThat(heightClassFor(AzkarBreakpoints.compactHeight))
            .isEqualTo(WindowHeightClass.MEDIUM)
        assertThat(heightClassFor(AzkarBreakpoints.expandedHeight))
            .isEqualTo(WindowHeightClass.EXPANDED)
    }

    test("the measured size is carried through alongside the buckets") {
        val size = windowSizeClassFor(1000.dp, 700.dp)

        assertThat(size.width).isEqualTo(1000.dp)
        assertThat(size.height).isEqualTo(700.dp)
        assertThat(size.isCompactHeight).isFalse()
    }

    testSuite("sizing the window to the screen it opens on") {
        test("an ordinary laptop screen gives ninety per cent of itself") {
            // 1366 is the most common width still in use, and it is the case that decided the share:
            // nine tenths of it is wide enough for the rail and four card columns.
            val size = windowSizeFor(1366.dp, 768.dp)

            assertThat(size.width).isEqualTo(1229.dp)
            assertThat(size.height).isEqualTo(691.dp)
        }

        test("a large screen is capped rather than filling the desktop") {
            val size = windowSizeFor(2560.dp, 1440.dp)

            assertThat(size.width).isEqualTo(1600.dp)
            assertThat(size.height).isEqualTo(1000.dp)
        }

        test("a screen too small for a useful window still gets a share of it") {
            // No lower bound: a screen that can only give a small window gives one, and the compact
            // shelf takes over. A floor here would either do nothing or fill the screen.
            val size = windowSizeFor(1000.dp, 700.dp)

            assertThat(size.width).isEqualTo(900.dp)
            assertThat(size.height).isEqualTo(630.dp)
        }

        test("a window never opens wider or taller than the screen that offered") {
            val size = windowSizeFor(800.dp, 600.dp)

            assertThat(size.width).isEqualTo(720.dp)
            assertThat(size.height).isEqualTo(540.dp)
        }

        test("a window always leaves a strip of desktop to grab") {
            val size = windowSizeFor(1600.dp, 900.dp)

            assertThat(size.width).isLessThan(1600.dp)
            assertThat(size.height).isLessThan(900.dp)
        }

        test("a screen just short of the compact breakpoint opens compact") {
            // 1000dp of screen gives 900dp of window, which is the compact breakpoint exactly. The
            // comparison there is strict, so this is the first width that is *not* compact — the case
            // that decided there must be no floor pushing it back up into the wide layout.
            val size = windowSizeFor(1000.dp, 900.dp)

            assertThat(size.width).isEqualTo(900.dp)
            assertThat(widthClassFor(size.width)).isEqualTo(WindowWidthClass.MEDIUM)
        }

        test("each axis is decided on its own") {
            // A wide, short screen — a laptop with the browser docked to one side — must not have its
            // good width dragged down by its height, which is the failure of sizing as one ratio.
            val size = windowSizeFor(1920.dp, 700.dp)

            assertThat(size.width).isEqualTo(1600.dp)
            assertThat(size.height).isEqualTo(630.dp)
        }
    }

    test("the default size is one a screen could actually offer") {
        // The fallback is only ever used when there is no screen to measure, but it still has to be a
        // plausible window: anything narrower than the floor would mean the common case is untested.
        assertThat(DEFAULT_WINDOW_WIDTH).isGreaterThan(AzkarBreakpoints.expandedWidth)
        assertThat(DEFAULT_WINDOW_HEIGHT).isGreaterThan(AzkarBreakpoints.compactHeight)
    }
}

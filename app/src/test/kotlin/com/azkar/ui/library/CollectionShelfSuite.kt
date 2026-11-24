package com.azkar.ui.library

import androidx.compose.ui.unit.dp
import de.infix.testBalloon.framework.core.testSuite
import org.assertj.core.api.Assertions.assertThat

private const val GAP = 12
private const val MIN_WIDTH = 190

/**
 * The shelf's grid arithmetic.
 *
 * The grid used to be a `LazyVerticalGrid`, which worked out its own columns and its own height. It
 * is now a plain grid inside a column that scrolls, so it has to work both out for itself, and this
 * is the arithmetic it does — the same arithmetic the collection's members are laid out with, which
 * is why the narrowest cell is a parameter here rather than a constant of this file's.
 *
 * TestBalloon generates the test entry point, but the IDE does not see that usage.
 */
@Suppress("unused")
val CollectionShelfSuite by testSuite("Laying the collection cards out") {
    test("a 1440dp window's shelf fits seven cards, where the rail left room for five") {
        // The window less the pane's own inset. The rail used to take 300dp and a 14dp gutter off
        // the right of it, which is exactly the difference between five columns and seven.
        val available = (1440 - 16 - 16).dp
        assertThat(fittedColumns(available = available, gap = GAP.dp, minWidth = MIN_WIDTH.dp)).isEqualTo(7)
    }

    test("the cards and the gaps between them exactly fill the width they were given") {
        val available = 1408.dp
        val columns = fittedColumns(available = available, gap = GAP.dp, minWidth = MIN_WIDTH.dp)
        val width = fittedCellWidth(available = available, columns = columns, gap = GAP.dp)
        val occupied = columns * width.value + (columns - 1) * GAP
        assertThat(occupied).isEqualTo(available.value)
    }

    test("a narrower window drops to fewer columns rather than narrower cards") {
        val available = 1000.dp
        val columns = fittedColumns(available = available, gap = GAP.dp, minWidth = MIN_WIDTH.dp)
        assertThat(columns).isLessThan(7)
        assertThat(fittedCellWidth(available = available, columns = columns, gap = GAP.dp).value)
            .isGreaterThanOrEqualTo(190f)
    }

    test("a window too narrow for two still shows one card rather than none") {
        val columns = fittedColumns(available = 40.dp, gap = GAP.dp, minWidth = MIN_WIDTH.dp)
        assertThat(columns).isEqualTo(1)
        assertThat(fittedCellWidth(available = 40.dp, columns = columns, gap = GAP.dp)).isEqualTo(40.dp)
    }

    test("a single card keeps the whole width, with no gap taken off the end") {
        assertThat(fittedCellWidth(available = 300.dp, columns = 1, gap = GAP.dp)).isEqualTo(300.dp)
    }

    test("zero or negative columns cannot happen, and would divide by zero if they did") {
        assertThat(fittedCellWidth(available = 500.dp, columns = 0, gap = GAP.dp)).isEqualTo(500.dp)
    }
}

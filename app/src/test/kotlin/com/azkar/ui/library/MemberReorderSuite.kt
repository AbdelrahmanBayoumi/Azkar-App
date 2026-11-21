package com.azkar.ui.library

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import de.infix.testBalloon.framework.core.testSuite
import org.assertj.core.api.Assertions.assertThat

/**
 * A tile's width and height, in the pixel units these tests measure in.
 *
 * Not dp: [MemberReorderState] works in whatever the layout measured, and a test that converted
 * would be testing its own conversion.
 */
private const val TILE_WIDTH = 100f

private const val TILE_HEIGHT = 80f

/** The gap a layout puts between two tiles, matching the page's `MEMBER_GAP`. */
private const val GAP = 12f

/** The index no tile has, as [MemberReorderState] reports it. */
private const val NO_TILE = -1

/**
 * A list of [count] tiles stacked down the screen, and the state that has measured them.
 *
 * Rows are one height plus the gap, so a tile's top is `index * (TILE_HEIGHT + GAP)` — the
 * arithmetic every assertion below is checked against.
 *
 * The tiles are laid out here rather than measured from a real page. [MemberReorderState.measure] is
 * fed rectangles the same way `onGloballyPositioned` would, so a layout with the wrong pitch or the
 * wrong number of columns fails here rather than at the far end of a gesture.
 */
private fun listOf(count: Int): MemberReorderState =
    MemberReorderState().apply {
        repeat(count) { index ->
            val top = index * (TILE_HEIGHT + GAP)
            measure(index, Rect(0f, top, TILE_WIDTH, top + TILE_HEIGHT), count)
        }
    }

/** The same, but with two tiles per row — the grid the page actually shows. */
private fun twoColumnGrid(): MemberReorderState =
    MemberReorderState().apply {
        val count = 4
        columns = 2
        repeat(count) { index ->
            val left = (index % columns) * (TILE_WIDTH + GAP)
            val top = (index / columns) * (TILE_HEIGHT + GAP)
            measure(index, Rect(left, top, left + TILE_WIDTH, top + TILE_HEIGHT), count)
        }
    }

/** The top-left corner of a tile in a list laid out by [listOf]. */
private fun topOf(index: Int) = index * (TILE_HEIGHT + GAP)

/** One tile's worth of vertical space: its height and the gap below it. */
private const val PITCH = TILE_HEIGHT + GAP

/** One tile's worth of horizontal space: its width and the gap beside it. */
private const val CELL = TILE_WIDTH + GAP

/**
 * What a drag of a member tile does to the order, asserted on the arithmetic rather than on a picture.
 *
 * Every test here drives [MemberReorderState] directly, and that is the reason the suite exists. A
 * drag is a sequence of pointer positions, and the only part of it that can be asserted without a
 * picture is the part that decides *where the tile lands* — which is what this class is. The pointer
 * plumbing that reaches it is thin, and it is the part a test has to work hardest to drive, so it
 * gets exactly one test in `MemberDragSuite` rather than one per rule here.
 *
 * TestBalloon generates the test entry point, but the IDE does not see that usage.
 */
@Suppress("unused")
val MemberReorderSuite by testSuite("dragging a member to a new place") {
    // ---- which tile is under the pointer --------------------------------------------------------

    test("a press on a tile picks up that tile") {
        val state = listOf(4)

        val picked = state.indexAt(Offset(TILE_WIDTH / 2f, topOf(2) + TILE_HEIGHT / 2f))

        assertThat(picked)
            .describedAs("the pointer was in the third row, so that is the tile it has hold of")
            .isEqualTo(2)
    }

    test("a press on nothing picks up nothing") {
        // Nothing has been measured, so there is nothing to have hold of. Reported as no tile rather
        // than as the origin, which would silently mean the first one.
        assertThat(MemberReorderState().indexAt(Offset(10f, 10f)))
            .describedAs("an unmeasured page has no tile under the pointer")
            .isEqualTo(NO_TILE)
    }

    // ---- where the tile lands -------------------------------------------------------------------

    test("a tile dragged down two rows reports the move that makes") {
        val state = listOf(4)

        state.begin(0, GAP)
        state.drag(Offset(0f, 2 * PITCH))

        assertThat(state.release())
            .describedAs("two rows down a four-row list is the third tile, and the move is 0 to 2")
            .isEqualTo(0 to 2)
    }

    test("a tile put back where it came from reports no move at all") {
        val state = listOf(4)

        state.begin(1, GAP)
        // Enough to cross the touch slop and make it a drag, nowhere near enough to change rows.
        state.drag(Offset(0f, 6f))

        assertThat(state.release())
            .describedAs("a drag that ends where it began is the same as no drag, and the collection is told so")
            .isNull()
    }

    test("a cancelled drag reports no move and leaves nothing displaced") {
        val state = listOf(4)

        state.begin(0, GAP)
        state.drag(Offset(0f, 3 * PITCH))
        assertThat(state.offsetFor(1))
            .describedAs("mid-drag the rows have moved out of the way")
            .isNotEqualTo(Offset.Zero)

        state.cancel()

        assertThat(state.release()).describedAs("a cancelled drag was not a move").isNull()
        assertThat(state.offsetFor(1))
            .describedAs("and every tile the drag had displaced goes back where it was")
            .isEqualTo(Offset.Zero)
    }

    test("a drag only ends the drag once") {
        val state = listOf(4)

        state.begin(0, GAP)
        state.drag(Offset(0f, PITCH))

        assertThat(state.release()).isEqualTo(0 to 1)
        assertThat(state.release())
            .describedAs("the state is not holding a drag any more, so there is no second move to report")
            .isNull()
    }

    // ---- where the pressed tile goes --------------------------------------------------------------

    test("a tile picked up by its handle does not jump") {
        val state = listOf(4)

        // The handle sits at the tile's bottom-right, nowhere near the corner the tiles are placed by.
        state.begin(0, GAP, grabbedAt = Offset(TILE_WIDTH - 4f, topOf(0) + TILE_HEIGHT - 6f))

        assertThat(state.draggedBy)
            .describedAs("the tile is already offset by where inside it the pointer took hold")
            .isEqualTo(Offset(TILE_WIDTH - 4f, TILE_HEIGHT - 6f))
        assertThat(state.release())
            .describedAs("so it lands back in its own row rather than in the next one it was pressed over")
            .isNull()
    }

    // ---- where the tiles in the way go -------------------------------------------------------------

    test("a tile dragged down two rows pushes exactly the rows between it up by one") {
        val state = listOf(4)

        state.begin(0, GAP)
        state.drag(Offset(0f, 2 * PITCH))

        assertThat(state.offsetFor(0)).describedAs("the dragged tile follows the pointer").isEqualTo(
            Offset(
                0f,
                2 * PITCH,
            ),
        )
        assertThat(
            state.offsetFor(1),
        ).describedAs("the first row steps up into its place").isEqualTo(Offset(0f, -PITCH))
        assertThat(
            state.offsetFor(2),
        ).describedAs("the second row steps up into its place").isEqualTo(Offset(0f, -PITCH))
        assertThat(
            state.offsetFor(3),
        ).describedAs("the row the drag has not reached does not move").isEqualTo(Offset.Zero)
    }

    test("a tile dragged back up pushes the rows above it down by one") {
        val state = listOf(4)

        state.begin(3, GAP)
        state.drag(Offset(0f, -2 * PITCH))

        assertThat(state.offsetFor(3)).describedAs("the dragged tile follows the pointer").isEqualTo(
            Offset(
                0f,
                -2 * PITCH,
            ),
        )
        assertThat(state.offsetFor(2)).describedAs("the row below the gap steps down").isEqualTo(Offset(0f, PITCH))
        assertThat(state.offsetFor(1)).describedAs("the next row down steps down too").isEqualTo(Offset(0f, PITCH))
        assertThat(
            state.offsetFor(0),
        ).describedAs("the row the drag has not reached does not move").isEqualTo(Offset.Zero)
    }

    test("only the dragged tile is raised") {
        val state = listOf(4)

        state.begin(0, GAP)
        state.drag(Offset(0f, PITCH))

        assertThat(listOf(0, 1, 2, 3).filter { state.liftedFor(it) })
            .describedAs("a shadow and a scale belong to the tile in the hand, not to the ones making way")
            .containsExactly(0)
    }

    // ---- the grid ------------------------------------------------------------------------------

    test("a grid tile dragged sideways steps its neighbour left rather than down") {
        val state = twoColumnGrid()

        state.begin(0, GAP)
        state.drag(Offset(CELL, 0f))

        assertThat(state.offsetFor(1))
            .describedAs("the tile to the right has a cell to its left, so that is where it goes")
            .isEqualTo(Offset(-CELL, 0f))
        assertThat(state.offsetFor(2))
            .describedAs("the row below is untouched: swapping two tiles in one row moves nothing in the next")
            .isEqualTo(Offset.Zero)
        assertThat(state.release()).isEqualTo(0 to 1)
    }

    test("a grid tile dragged down a whole row lands on the tile below it") {
        val state = twoColumnGrid()

        state.begin(0, GAP)
        state.drag(Offset(0f, PITCH))

        assertThat(state.offsetFor(1))
            .describedAs("the tile that was beside it slides into the gap it leaves")
            .isEqualTo(Offset(-CELL, 0f))
        // Read after the displacement, not before: release ends the drag and puts every tile back.
        assertThat(state.release()).describedAs("one row down a two-column grid is the third tile").isEqualTo(0 to 2)
    }

    // ---- the geometry a drag runs on ---------------------------------------------------------------

    test("the geometry a drag runs on is the geometry it started with") {
        val state = listOf(4)
        state.begin(0, GAP)
        state.drag(Offset(0f, PITCH))

        // The rows in between are being drawn somewhere else, so a layout pass that reported their
        // real positions would move the target back under the pointer — and then forward again.
        state.measure(1, Rect(0f, 9000f, TILE_WIDTH, 9000f + TILE_HEIGHT), 4)
        state.measure(2, Rect(0f, 9000f + PITCH, TILE_WIDTH, 9000f + PITCH + TILE_HEIGHT), 4)

        assertThat(state.release())
            .describedAs("a displaced tile reporting where it is drawn must not move the target")
            .isEqualTo(0 to 1)
    }

    test("tiles measured again between drags are the tiles the next drag runs on") {
        val state = listOf(3)
        state.begin(0, GAP)
        state.drag(Offset(0f, PITCH))
        assertThat(state.release()).isEqualTo(0 to 1)

        // The collection has been written and read back, so the tiles have moved for real.
        val settled = listOf(3)
        settled.begin(2, GAP)
        settled.drag(Offset(0f, -PITCH))

        assertThat(settled.release())
            .describedAs("a drag that begins after the first one has settled measures a fresh layout")
            .isEqualTo(2 to 1)
    }

    test("a drag on a page whose tiles were never measured does not move anything") {
        val state = MemberReorderState()

        state.begin(0, GAP)
        state.drag(Offset(0f, 200f))

        assertThat(state.isDragging).describedAs("there was no tile to take hold of").isFalse()
        assertThat(state.release()).isNull()
    }
}

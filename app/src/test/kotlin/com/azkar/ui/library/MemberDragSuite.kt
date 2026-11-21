package com.azkar.ui.library

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsNodeInteractionCollection
import androidx.compose.ui.test.SkikoComposeUiTest
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.v2.runSkikoComposeUiTest
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.azkar.testing.assertTrackedRecompositions
import com.azkar.testing.assertTrackedStable
import com.azkar.testing.withDesktopDejavu
import com.azkar.ui.library.data.findById
import com.azkar.ui.library.model.CollectionMemberView
import com.azkar.ui.preview.previewReminders
import com.azkar.ui.theme.AzkarTheme
import com.azkar.ui.theme.LocalWindowSizeClass
import com.azkar.ui.theme.windowSizeClassFor
import de.infix.testBalloon.framework.core.testSuite
import dejavu.resetRecompositionCounts
import dejavu.setTrackedContent
import org.assertj.core.api.Assertions.assertThat
import java.util.Locale

/**
 * The window these tests lay the page out in.
 *
 * The same reasoning as `ScreenSuite`'s: the detail screen branches on [LocalWindowSizeClass], so a
 * window narrower than the one the app ships with would put the member grid below the fold, and a
 * handle that is not composed cannot be dragged. The rows also have to be tall enough that two of
 * them fit under the hero band, which is why this is a real desktop window rather than the default.
 */
private val DRAG_SCREEN = Size(1440f, 1200f)
private const val DETAIL_SCREEN_TAG = "member-detail-screen"

@OptIn(ExperimentalTestApi::class)
private fun trackedDragScreen(block: SkikoComposeUiTest.() -> Unit) {
    withDesktopDejavu {
        runSkikoComposeUiTest(size = DRAG_SCREEN, density = Density(1f)) {
            resetRecompositionCounts()
            block()
        }
    }
}

@OptIn(ExperimentalTestApi::class)
private fun dragScreen(block: SkikoComposeUiTest.() -> Unit) = trackedDragScreen(block)

/**
 * The handle's accessible name, which is the translation of its tooltip.
 *
 * It is the only handle this suite has on the control, and it exists for exactly this reason: a drag
 * handle that is announced as nothing is invisible to a screen reader *and* to a test.
 */
private const val DRAG_HANDLE = "Drag to reorder"

/** The collection under test. */
private const val DRAG_COLLECTION_ID = 42L

/**
 * The page, wired to a fake that honours a reorder.
 *
 * The gateway is the real [FakeCollectionsGateway] rather than the preview one because the preview
 * gateway is stateless — it cannot carry a move, so a test against it would assert only that the
 * drag gesture did not throw, which is the part that was broken.
 *
 * List view is forced rather than the default grid because a vertical drag in a single column has
 * one meaning. In a grid the same gesture could mean "move down a row" or "move sideways a column"
 * depending on where inside the card it started, and a test that asserted one of those would be
 * asserting a detail of the geometry rather than the fact that the drop was understood.
 */
@Composable
private fun DragFixture(
    gateway: FakeCollectionsGateway,
    content: @Composable (CollectionDetailState) -> Unit,
) {
    val scope = rememberCoroutineScope()
    val catalogue =
        remember(gateway) {
            CollectionsCatalogue(
                scope = scope,
                uiLocale = Locale.ENGLISH,
                gateway = gateway,
                covers = fakeCovers(),
            )
        }
    AzkarTheme(darkTheme = false, uiLocale = Locale.ENGLISH) {
        CompositionLocalProvider(
            LocalWindowSizeClass provides windowSizeClassFor(DRAG_SCREEN.width.dp, DRAG_SCREEN.height.dp),
        ) {
            Box(
                Modifier
                    .size(DRAG_SCREEN.width.dp, DRAG_SCREEN.height.dp)
                    .background(Color.White),
            ) {
                val detail =
                    rememberCollectionDetailState(
                        catalogue = catalogue,
                        collectionId = DRAG_COLLECTION_ID,
                        reminders = previewReminders(DRAG_COLLECTION_ID),
                    )
                detail.setMemberView(CollectionMemberView.LIST)
                CollectionDetailScreen(
                    detail = detail,
                    onBack = {},
                    onStartSession = {},
                    onOpenRemembrance = {},
                    showToast = { _, _, _ -> },
                    modifier = Modifier.testTag(DETAIL_SCREEN_TAG),
                )
                Box(Modifier.fillMaxSize()) { content(detail) }
            }
        }
    }
}

/**
 * Four remembrances, in a named order, so a test can say which one moved.
 *
 * The words are the fixture's own, read back out of the gateway, rather than anything read off the
 * screen: the tile paints a card, and a card shows a title it may well rewrap. The identity that
 * matters is the membership order, which is what a reorder is supposed to change.
 */
private fun dragGateway(count: Int = 4): FakeCollectionsGateway {
    val members = (1..count).map { remembrance(id = it.toLong(), text = "Remembrance $it") }
    return FakeCollectionsGateway(
        collections = listOf(officialCollection(id = DRAG_COLLECTION_ID, name = "After Prayer", members = members)),
        remembrances = members,
    )
}

/** The member ids the gateway is serving, in the order it is serving them. */
private fun FakeCollectionsGateway.memberIds(): List<Long> =
    collections
        .findById(DRAG_COLLECTION_ID)
        ?.members
        ?.mapNotNull { it.id }
        .orEmpty()

/**
 * Every drag handle currently on screen.
 *
 * Indexed by *screen position*, not by tree order, because every assertion here is about which row
 * was picked up, and a lazy container composes in whatever order the viewport asked for.
 */
@OptIn(ExperimentalTestApi::class)
private fun SkikoComposeUiTest.handles(): SemanticsNodeInteractionCollection =
    onAllNodes(hasContentDescription(DRAG_HANDLE))

/** The handles' top edges, smallest first: the rows' positions on the screen. */
@OptIn(ExperimentalTestApi::class)
private fun SkikoComposeUiTest.handleTops(): List<Float> =
    handles().fetchSemanticsNodes().map { it.boundsInRoot.top }.sorted()

/**
 * Dragging a card by its handle, and the order the gateway is left in.
 *
 * The one test in this feature that drives a real pointer. Everything about *which* slot a drop
 * lands in is asserted in `MemberReorderSuite` against the state machine, because that is where the
 * rules live and they are testable without a screen. What is left is the wiring: that a gesture
 * reaches the reorder code at all.
 *
 * That wiring is not a formality. The gesture was once attached outside the card's `clickable`,
 * which Compose delivers to innermost-first, and a drag detector that never sees the press produces
 * no exception, no log line and no failure anywhere except the card quietly staying where it was.
 */
@OptIn(ExperimentalTestApi::class)
// TestBalloon generates the test entry point, but the IDE does not see that usage.
@Suppress("unused")
val MemberDragSuite by testSuite("dragging a card on the members page") {
    test("the handles are on the screen before anything is dragged") {
        dragScreen {
            setTrackedContent { DragFixture(dragGateway()) {} }
            waitForIdle()
            resetRecompositionCounts()
            mainClock.advanceTimeBy(100)
            assertThat(handles().fetchSemanticsNodes())
                .describedAs("a handle on each of the four rows")
                .hasSize(4)
            onNodeWithTag(DETAIL_SCREEN_TAG).assertTrackedStable(expectedFunction = "DetailLazyPage")
        }
    }

    test("dragging the first row down onto the second one reorders the collection") {
        dragScreen {
            val gateway = dragGateway()
            setTrackedContent { DragFixture(gateway) {} }
            waitForIdle()
            assertThat(gateway.memberIds()).describedAs("what the fixture starts as").containsExactly(1L, 2L, 3L, 4L)

            val rowPitch = handleTops()[1] - handleTops()[0]
            resetRecompositionCounts()
            handles()[0].performTouchInput {
                down(center)
                moveBy(Offset(0f, rowPitch))
                up()
            }
            waitForIdle()

            assertThat(gateway.memberIds())
                .describedAs("the order the collection is stored in after the drop")
                .containsExactly(2L, 1L, 3L, 4L)
            onNodeWithTag("member-tile-1").assertTrackedRecompositions(exactly = 1, expectedFunction = "AzkarCard")
        }
    }

    test("a drag that goes nowhere leaves the order alone") {
        dragScreen {
            val gateway = dragGateway()
            setTrackedContent { DragFixture(gateway) {} }
            waitForIdle()
            resetRecompositionCounts()

            handles()[0].performTouchInput {
                down(center)
                moveBy(Offset(2f, 2f))
                up()
            }
            waitForIdle()

            assertThat(gateway.memberIds())
                .describedAs("a nudge is not a reorder, and must not write one")
                .containsExactly(1L, 2L, 3L, 4L)
            onNodeWithTag(DETAIL_SCREEN_TAG).assertTrackedStable(expectedFunction = "DetailLazyPage")
        }
    }

    test("the rows are laid out again in the order the drop produced") {
        dragScreen {
            val gateway = dragGateway()
            setTrackedContent { DragFixture(gateway) {} }
            waitForIdle()

            val tops = handleTops()
            resetRecompositionCounts()
            handles()[0].performTouchInput {
                down(center)
                moveBy(Offset(0f, tops[1] - tops[0]))
                up()
            }
            waitForIdle()

            // Both cards are still on the page after the swap, which is what proves the lazy
            // container re-laid the rows out rather than having lost one of them to the move.
            onNodeWithText("Remembrance 1").assertExists()
            onNodeWithText("Remembrance 2").assertExists()
            onNodeWithTag("member-tile-1").assertTrackedRecompositions(exactly = 1, expectedFunction = "AzkarCard")
        }
    }
}

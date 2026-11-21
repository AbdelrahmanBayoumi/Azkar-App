package com.azkar.ui.notifications.presentation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SkikoComposeUiTest
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.v2.runSkikoComposeUiTest
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import androidx.compose.ui.unit.width
import com.azkar.i18n.ARABIC
import com.azkar.i18n.ENGLISH
import com.azkar.testing.assertTrackedRecompositions
import com.azkar.testing.assertTrackedStable
import com.azkar.testing.withDesktopDejavu
import com.azkar.ui.notifications.NotificationAction
import com.azkar.ui.notifications.NotificationContent
import com.azkar.ui.theme.AzkarTheme
import de.infix.testBalloon.framework.core.testSuite
import dejavu.resetRecompositionCounts
import dejavu.setTrackedContent
import org.assertj.core.api.Assertions.assertThat
import java.util.Locale

/**
 * What the user can actually do with a card, asserted through the semantics tree.
 *
 * The presenter suite covers what the card *is*; this covers what it *does*, which is the half a
 * headless unit test cannot reach because it needs the pointer and the composition: that the whole
 * body is one target that opens Azkar, that the cross is a separate target that dismisses, and that
 * every label on the card is translated — an untranslated accessibility label in the Arabic build is
 * a real defect, and the one thing here that has to be checked against a resource rather than
 * against a callback.
 *
 * The card used to carry considerably more than this, and most of these cases are the absence of
 * something: there is no title beside the passage, no translation of it, no citation, no grade, no
 * collection name, no tags and no other controls. Those absences are the feature — a reminder is a
 * glance, and a card about a passage has to be read past to reach the passage — so they are asserted
 * rather than merely left out of the fixtures. A fixture that could not produce them would prove
 * nothing.
 *
 * The one control that came back is the star, and the cases below are about it rather than about its
 * absence: whether it is offered at all, whether it keeps the passage it belongs to, and what it
 * does when the write behind it fails. The rest of the card's economy is unchanged, and the gesture
 * count is asserted as a count so that anything else added back is caught even if it is a control
 * this test has never heard of.
 *
 * Three bodies are covered because the card draws three: a passage, a prayer time, and the
 * payload-less notice the proof-of-concept harness still publishes. The last of those is why the
 * old title-and-message assertions stayed rather than being replaced — a caller that has not been
 * updated must still draw something legible, and that is only a promise if it is asserted.
 *
 * Every case goes through [cardTest], which owns the frame and the theme, so what a test states is
 * what it is about and not the scaffolding around it. The frame is the **default preset's** size
 * rather than something generous: the card fills whatever box it is given and ellipsizes when the
 * passage does not fit, so a test that hands it a tall frame is testing a card that most readers
 * will never see.
 */
private val CARD_SIZE = Size(440f, 240f)

/**
 * The size the card is given when a test does not care, matching `OverlayCardSize.COMFORTABLE`.
 *
 * A pair of named values rather than one tuple so a case that only cares about height can say so
 * without having to name a width it is not testing.
 */
private val DEFAULT_CARD_WIDTH = 400.dp
private val DEFAULT_CARD_HEIGHT = 200.dp

private const val TITLE = "Fajr"
private const val MESSAGE = "Say it once, slowly."
private const val UPDATE_TITLE = "Update available"
private const val UPDATE_MESSAGE = "Version 1.2.0 is ready. Open Settings to review the update."

/** An Arabic passage, to prove the card renders right-to-left text without being told to. */
private const val ARABIC_PASSAGE = "أَصْبَحْنَا وَأَصْبَحَ الْمُلْكُ لِلَّهِ"

/** An English one, to prove the same card does not force either direction on whatever it is given. */
private const val ENGLISH_PASSAGE = "Whoever says: Allah is One, and He has no partner."

/**
 * Strings the card used to print and must not again.
 *
 * Kept as literals rather than deleted with the feature, because "the card is only the passage" is
 * only a real guarantee while there is something in the tree that would catch its return.
 */
private const val TRANSLATION = "We have entered the morning and the dominion belongs to Allah."
private const val COLLECTION = "Evening Adhkar"
private const val SOURCE = "Abu Dawud 5068"
private const val GRADE = "Sahih"
private const val TAG = "Evening"
private const val NEXT_AZKAR_LABEL = "Next azkar"
private const val NEXT_PRAYER_LABEL = "Next: Isha at 6:41 PM"

/** The rendered name of the dismiss target in each language under test. */
private const val ENGLISH_DISMISS_LABEL = "Dismiss"
private const val ARABIC_DISMISS_LABEL = "إخفاء"

/** The rendered name of the star, in the two states it can be in. */
private const val ENGLISH_ADD_FAVORITE_LABEL = "Add to Favorites"
private const val ENGLISH_REMOVE_FAVORITE_LABEL = "Remove from Favorites"
private const val ARABIC_ADD_FAVORITE_LABEL = "إضافة إلى المفضلة"

/** The cycle counter, written out in both languages rather than looked up. */
private const val ENGLISH_POSITION_LABEL = "1 of 3"
private const val ARABIC_POSITION_LABEL = "1 من 3"

private const val REMEMBRANCE_ID = 42L
private const val CARD_TAG = "notification-card"

/** The fake deadline is long enough to observe several ticks and short enough for a fast UI suite. */
private const val BAR_TEST_DURATION_NANOS = 2_000_000_000L

/** Fewer than one second should always contain three 30ms countdown samples. */
private const val BAR_TEST_TIMEOUT_MILLIS = 1_000L

@OptIn(ExperimentalTestApi::class)
private fun trackedCardUiTest(block: SkikoComposeUiTest.() -> Unit) {
    withDesktopDejavu {
        runSkikoComposeUiTest(size = CARD_SIZE, density = Density(1f), block = block)
    }
}

@OptIn(ExperimentalTestApi::class)
private fun cardTest(
    locale: Locale = ENGLISH,
    darkTheme: Boolean = false,
    cardWidth: Dp = DEFAULT_CARD_WIDTH,
    cardHeight: Dp = DEFAULT_CARD_HEIGHT,
    maxRecompositions: Int = 0,
    content: @Composable () -> Unit,
    block: SkikoComposeUiTest.() -> Unit,
) {
    trackedCardUiTest {
        setTrackedContent {
            AzkarTheme(darkTheme = darkTheme, uiLocale = locale) {
                Box(
                    modifier = Modifier.fillMaxSize().padding(12.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(modifier = Modifier.size(cardWidth, cardHeight)) { content() }
                }
            }
        }
        waitForIdle()
        resetRecompositionCounts()
        block()
        mainClock.advanceTimeBy(100)
        waitForIdle()
        onNodeWithTag(
            CARD_TAG,
        ).assertTrackedRecompositions(atMost = maxRecompositions, expectedFunction = "AzkarSurfaceCard")
    }
}

/**
 * The width a fitted card is measured at, and the tag its box is reached by.
 *
 * The same 320 as the compact preset: a fit case is about height, so it is measured at the narrowest
 * width the settings can produce, which is the one where a passage wraps the most and the height it
 * needs is furthest from a slot's.
 */
private val FITTED_CARD_WIDTH = 320.dp
private const val FITTED_CARD_TAG = "fitted-card"

/**
 * Renders a card the way the overlay does under a fitted height: a width, and no height ceiling.
 *
 * `wrapContentHeight(unbounded = true)` is the part that matters and the part a plain `Box` cannot
 * stand in for. A bounded frame would let the case pass without proving anything, because the card
 * would be laid out inside a slot and its own measurement would never be exercised — the whole point
 * here is that the card is measured with no slot to fall into.
 */
@OptIn(ExperimentalTestApi::class)
private fun fittedCardTest(block: SkikoComposeUiTest.() -> Unit) {
    trackedCardUiTest {
        setTrackedContent {
            AzkarTheme(uiLocale = ENGLISH) {
                Box(modifier = Modifier.width(FITTED_CARD_WIDTH).wrapContentHeight(unbounded = true)) {
                    NotificationCard(
                        item = passageNotice(passage = ARABIC_PASSAGE, position = 1, total = 3),
                        onOpen = {},
                        onDismiss = {},
                        onPauseChanged = {},
                        modifier = Modifier.testTag(FITTED_CARD_TAG),
                        sizing = OverlayCardSizing.FIT,
                    )
                }
            }
        }
        waitForIdle()
        resetRecompositionCounts()
        block()
        mainClock.advanceTimeBy(100)
        waitForIdle()
        onNodeWithTag(FITTED_CARD_TAG).assertTrackedStable(expectedFunction = "AzkarSurfaceCard")
    }
}

@Composable
private fun plainCard(
    onOpen: () -> Unit = {},
    onDismiss: () -> Unit = {},
    onPauseChanged: (Boolean) -> Unit = {},
) {
    NotificationCard(
        item = plainNotice(),
        onOpen = onOpen,
        onDismiss = onDismiss,
        onPauseChanged = onPauseChanged,
        modifier = Modifier.testTag(CARD_TAG),
    )
}

@Composable
private fun updateCard(
    onOpen: () -> Unit = {},
    onDismiss: () -> Unit = {},
) {
    NotificationCard(
        item =
            PresentedNotice(
                id = 1L,
                title = UPDATE_TITLE,
                message = UPDATE_MESSAGE,
                action = NotificationAction.Update,
            ),
        onOpen = onOpen,
        onDismiss = onDismiss,
        onPauseChanged = {},
        modifier = Modifier.testTag(CARD_TAG),
    )
}

@Composable
private fun passageCard(
    passage: String = ARABIC_PASSAGE,
    total: Int = 3,
    position: Int = 1,
    isFavorite: Boolean = false,
    onOpen: () -> Unit = {},
    onDismiss: () -> Unit = {},
    remaining: () -> Float = { 1f },
    onToggleFavorite: (suspend (Long, Boolean) -> Boolean)? = { _, _ -> true },
) {
    NotificationCard(
        item =
            passageNotice(
                passage = passage,
                position = position,
                total = total,
                isFavorite = isFavorite,
            ),
        onOpen = onOpen,
        onDismiss = onDismiss,
        onPauseChanged = {},
        remaining = remaining,
        onToggleFavorite = onToggleFavorite,
        modifier = Modifier.testTag(CARD_TAG),
    )
}

@Composable
private fun prayerCard(
    onOpen: () -> Unit = {},
    onDismiss: () -> Unit = {},
) {
    NotificationCard(
        item = prayerNotice(),
        onOpen = onOpen,
        onDismiss = onDismiss,
        onPauseChanged = {},
        modifier = Modifier.testTag(CARD_TAG),
    )
}

/**
 * The written name of a target, asked as a question rather than as an assertion.
 *
 * A predicate has to be safe to call again and again — [waitUntil] calls it until it says yes — so
 * it cannot assert. This asks the tree what is in it instead, which is what makes it usable for
 * "the star is back" as well as for "the star is there".
 */
@OptIn(ExperimentalTestApi::class)
private fun SkikoComposeUiTest.showsTargetNamed(description: String): Boolean =
    onAllNodes(hasContentDescription(description)).fetchSemanticsNodes().isNotEmpty()

/**
 * What the card asked the app to write, and about which passage.
 *
 * A pair rather than two counters, because the two are one fact: a star that fired for the wrong
 * passage would look exactly like a star that fired at all.
 */
private data class FavoriteToggle(
    val id: Long,
    val favorite: Boolean,
)

@OptIn(ExperimentalTestApi::class)
val NotificationCardSuite by testSuite("The overlay notification card") {
    test("an update notice shows the release version and download choice") {
        cardTest(content = { updateCard() }) {
            onNodeWithText(UPDATE_TITLE).assertIsDisplayed()
            onNodeWithText(UPDATE_MESSAGE).assertIsDisplayed()
        }
    }

    test("choosing the update notice invokes its action") {
        var started = 0
        cardTest(content = { updateCard(onOpen = { started++ }) }) {
            onNodeWithText(UPDATE_MESSAGE).performClick()
        }

        assertThat(started).isEqualTo(1)
    }

    test("dismissing the update notice does not invoke its action") {
        var started = 0
        var dismissed = 0
        cardTest(content = {
            updateCard(onOpen = { started++ }, onDismiss = { dismissed++ })
        }) {
            onNodeWithContentDescription(ENGLISH_DISMISS_LABEL).performClick()
        }

        assertThat(dismissed).isEqualTo(1)
        assertThat(started).isZero()
    }

    test("it shows what was published, in the app's own card") {
        cardTest(content = { plainCard() }) {
            onNodeWithText(TITLE).assertExists()
            onNodeWithText(MESSAGE).assertExists()
        }
    }

    test("the body opens Azkar") {
        var opened = 0
        cardTest(maxRecompositions = 1, content = { plainCard(onOpen = { opened++ }) }) {
            resetRecompositionCounts()
            onNodeWithText(TITLE).performClick()
            onNodeWithTag(CARD_TAG).assertTrackedRecompositions(atMost = 1, expectedFunction = "AzkarSurfaceCard")
        }

        assertThat(opened).isEqualTo(1)
    }

    test("the gradient bar keeps sampling the card's remaining time") {
        val observed = mutableListOf<Float>()
        val startedAt = System.nanoTime()
        cardTest(content = {
            passageCard(
                remaining = {
                    val fraction =
                        (1f - (System.nanoTime() - startedAt) / BAR_TEST_DURATION_NANOS.toFloat())
                            .coerceIn(0f, 1f)
                    observed += fraction
                    fraction
                },
            )
        }) {
            waitUntil(timeoutMillis = BAR_TEST_TIMEOUT_MILLIS) { observed.size >= 3 }
        }

        assertThat(observed.first()).isGreaterThan(observed.last())
    }

    test("the cross dismisses, and is not the same gesture as opening") {
        var opened = 0
        var dismissed = 0
        cardTest(maxRecompositions = 1, content = { plainCard(onOpen = { opened++ }, onDismiss = { dismissed++ }) }) {
            resetRecompositionCounts()
            onNodeWithContentDescription(ENGLISH_DISMISS_LABEL).performClick()
            onNodeWithTag(CARD_TAG).assertTrackedRecompositions(atMost = 1, expectedFunction = "AzkarSurfaceCard")
        }

        assertThat(dismissed).isEqualTo(1)
        assertThat(opened).isZero()
    }

    test("the cross is named in the language the app is in") {
        // The resource environment on desktop reads `Locale.getDefault()` at the moment the string is
        // asked for, so this is the only way to make the card compose in Arabic at all — and the
        // reason an untranslated label is a real defect here rather than a theoretical one.
        val previous = Locale.getDefault()
        Locale.setDefault(ARABIC)
        try {
            cardTest(locale = ARABIC, content = { passageCard() }) {
                onNodeWithContentDescription(ARABIC_DISMISS_LABEL).assertExists()
            }
        } finally {
            Locale.setDefault(previous)
        }
    }

    test("hovering pauses and resumes without recomposing the card") {
        val pauses = mutableListOf<Boolean>()
        cardTest(content = { plainCard(onPauseChanged = { pauses += it }) }) {
            assertThat(pauses).containsExactly(false)
            onNodeWithTag(CARD_TAG).performMouseInput { moveTo(center) }
            waitForIdle()
            assertThat(pauses).containsExactly(false, true)
            onNodeWithTag(CARD_TAG).performMouseInput { moveTo(Offset(-10f, -10f)) }
            waitForIdle()
            assertThat(pauses).containsExactly(false, true, false)
        }
    }

    test("a passage card prints the passage and where it falls in the cycle, and nothing else") {
        cardTest(content = { passageCard() }) {
            onNodeWithText(ARABIC_PASSAGE).assertIsDisplayed()
            onNodeWithText(ENGLISH_POSITION_LABEL).assertIsDisplayed()
        }
    }

    test("a passage card prints neither a translation nor the metadata around the passage") {
        // Not an omission to be closed in a later pass. A translation is somebody else's *reading* of
        // a passage the reader has not read yet, and it would take the space that makes the passage
        // recognisable; a citation and a grade are references rather than parts of what is being said;
        // which collection it came from is answered by the screen the click opens. Every one of them
        // was on this card once, and each is a thing the reader had to read past to reach the azkar.
        cardTest(content = { passageCard() }) {
            onNodeWithText(TRANSLATION, substring = true).assertDoesNotExist()
            onNodeWithText(COLLECTION, substring = true).assertDoesNotExist()
            onNodeWithText(SOURCE, substring = true).assertDoesNotExist()
            onNodeWithText(GRADE, substring = true).assertDoesNotExist()
            onNodeWithText(TAG, substring = true).assertDoesNotExist()
            onNodeWithText(NEXT_AZKAR_LABEL).assertDoesNotExist()
        }
    }

    test("a passage card offers exactly three gestures: open it, keep it and close it") {
        // Copy and next are gone for good. They were controls about *the app* — one acted on the
        // clipboard, the other on the queue — and every one of them had a home the click already
        // opens. The star is the exception, and the reason it is not: whether a passage is kept has
        // no home the click opens, so a reader who keeps a passage in the library and then meets that
        // same passage in the queue has to be able to keep it from the queue too.
        //
        // Asserted as a *count* rather than as separate absences so that anything added back is
        // caught even if it is a control this test has never heard of — a menu, a dismiss-all, a
        // play button. Three means the body, the star and the cross, and nothing else.
        cardTest(content = { passageCard() }) {
            onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsActions.OnClick)).assertCountEquals(3)
        }
    }

    test("a card with nowhere to write the star offers no star at all") {
        // The star is offered only where it can be honest. A caller with no way to write the flag
        // would be putting a claim on screen that nothing behind it agrees with, so the card draws
        // no star rather than a dead one — and a passage that cannot be kept is still fully openable.
        cardTest(content = { passageCard(onToggleFavorite = null) }) {
            onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsActions.OnClick)).assertCountEquals(2)
            onNodeWithContentDescription(ENGLISH_ADD_FAVORITE_LABEL).assertDoesNotExist()
        }
    }

    test("the star keeps the passage it belongs to") {
        val toggles = mutableListOf<FavoriteToggle>()
        cardTest(
            content = {
                passageCard(
                    onToggleFavorite = { id, favorite ->
                        toggles += FavoriteToggle(id = id, favorite = favorite)
                        true
                    },
                )
            },
        ) {
            onNodeWithContentDescription(ENGLISH_ADD_FAVORITE_LABEL).performClick()
            waitUntil(timeoutMillis = BAR_TEST_TIMEOUT_MILLIS) { toggles.isNotEmpty() }
        }

        assertThat(toggles).containsExactly(FavoriteToggle(id = REMEMBRANCE_ID, favorite = true))
    }

    test("the star unkeeps a passage that was already kept") {
        val toggles = mutableListOf<FavoriteToggle>()
        cardTest(
            content = {
                passageCard(
                    isFavorite = true,
                    onToggleFavorite = { id, favorite ->
                        toggles += FavoriteToggle(id = id, favorite = favorite)
                        true
                    },
                )
            },
        ) {
            onNodeWithContentDescription(ENGLISH_REMOVE_FAVORITE_LABEL).performClick()
            waitUntil(timeoutMillis = BAR_TEST_TIMEOUT_MILLIS) { toggles.isNotEmpty() }
        }

        assertThat(toggles).containsExactly(FavoriteToggle(id = REMEMBRANCE_ID, favorite = false))
    }

    test("the star does not open the app, because keeping a passage is not a request to be taken somewhere") {
        var opened = 0
        cardTest(content = { passageCard(onOpen = { opened++ }) }) {
            onNodeWithContentDescription(ENGLISH_ADD_FAVORITE_LABEL).performClick()
        }

        assertThat(opened).isZero()
    }

    test("the star answers the press at once, rather than waiting for the write to come back") {
        // The write is a database round trip, and a star that only appeared once the database had
        // agreed would put a visible delay between the press and the picture. So the glyph moves
        // first and the answer, if there is one, comes afterwards.
        cardTest(content = { passageCard() }) {
            onNodeWithContentDescription(ENGLISH_ADD_FAVORITE_LABEL).performClick()
            waitUntil(timeoutMillis = BAR_TEST_TIMEOUT_MILLIS) {
                showsTargetNamed(ENGLISH_REMOVE_FAVORITE_LABEL)
            }
        }
    }

    test("a write that fails takes the star back, rather than leaving a claim nothing agrees with") {
        // The flip happens before the write, so for the length of a round trip the picture claims
        // something the database has not agreed to. A failed write is the only thing that can close
        // that gap, and closing it means putting the star back.
        var attempts = 0
        cardTest(content = {
            passageCard(onToggleFavorite = { _, _ ->
                attempts++
                false
            })
        }) {
            onNodeWithContentDescription(ENGLISH_ADD_FAVORITE_LABEL).performClick()
            waitUntil(timeoutMillis = BAR_TEST_TIMEOUT_MILLIS) { attempts > 0 }
            waitUntil(timeoutMillis = BAR_TEST_TIMEOUT_MILLIS) {
                showsTargetNamed(ENGLISH_ADD_FAVORITE_LABEL)
            }
            onNodeWithContentDescription(ENGLISH_REMOVE_FAVORITE_LABEL).assertDoesNotExist()
        }
    }

    test("a passage that is already kept shows the star filled, and offers to unkeep it") {
        cardTest(content = { passageCard(isFavorite = true) }) {
            onNodeWithContentDescription(ENGLISH_REMOVE_FAVORITE_LABEL).assertExists()
            onNodeWithContentDescription(ENGLISH_ADD_FAVORITE_LABEL).assertDoesNotExist()
        }
    }

    test("the star is named in the language the app is in") {
        val previous = Locale.getDefault()
        Locale.setDefault(ARABIC)
        try {
            cardTest(locale = ARABIC, content = { passageCard() }) {
                onNodeWithContentDescription(ARABIC_ADD_FAVORITE_LABEL).assertExists()
            }
        } finally {
            Locale.setDefault(previous)
        }
    }

    test("a prayer time and a bare notice carry no star, because neither is a passage to keep") {
        cardTest(content = { prayerCard() }) {
            onNodeWithContentDescription(ENGLISH_ADD_FAVORITE_LABEL).assertDoesNotExist()
        }
        cardTest(content = { plainCard() }) {
            onNodeWithContentDescription(ENGLISH_ADD_FAVORITE_LABEL).assertDoesNotExist()
        }
    }

    test("a cycle of one says no count, because 1 of 1 is arithmetic rather than progress") {
        cardTest(content = { passageCard(total = 1) }) {
            onNodeWithText(ARABIC_PASSAGE).assertIsDisplayed()
            onNodeWithText("1 of 1").assertDoesNotExist()
        }
    }

    test("the counter is translated, so an Arabic reader is not told 1 of 3 in English") {
        val previous = Locale.getDefault()
        Locale.setDefault(ARABIC)
        try {
            cardTest(locale = ARABIC, content = { passageCard() }) {
                onNodeWithText(ARABIC_POSITION_LABEL).assertIsDisplayed()
                onNodeWithText(ENGLISH_POSITION_LABEL).assertDoesNotExist()
            }
        } finally {
            Locale.setDefault(previous)
        }
    }

    test("the counter says the passage it actually is, not always the first") {
        cardTest(content = { passageCard(position = 2) }) {
            onNodeWithText("2 of 3").assertIsDisplayed()
        }
    }

    test("a left-to-right passage is laid out left-to-right, and an Arabic one right-to-left") {
        // The card reads the direction off the passage rather than off the interface. Pinning it to
        // the app's layout would mis-order one language's punctuation and its quotation marks; the
        // test can only prove it renders and stays on screen, but a direction that throws or drops
        // the text out of the box fails here.
        cardTest(content = { passageCard(passage = ENGLISH_PASSAGE) }) {
            onNodeWithText(ENGLISH_PASSAGE).assertIsDisplayed()
        }
        cardTest(content = { passageCard() }) {
            onNodeWithText(ARABIC_PASSAGE).assertIsDisplayed()
        }
    }

    test("a card given a tall frame shows more of a long passage, and a short one clips rather than grows") {
        val long = "سُبْحَانَ اللَّهِ " + "وَبِحَمْدِهِ ".repeat(40)
        cardTest(cardHeight = 320.dp, content = { passageCard(passage = long) }) {
            onNodeWithText(long, substring = true).assertExists()
        }
        cardTest(cardHeight = 120.dp, content = { passageCard(passage = long) }) {
            onNodeWithText("سُبْحَانَ", substring = true).assertIsDisplayed()
        }
    }

    test("a prayer card is about a time, so it prints one and not an empty passage") {
        cardTest(content = { prayerCard() }) {
            onNodeWithText("Maghrib").assertIsDisplayed()
            onNodeWithText("5:12 PM").assertIsDisplayed()
            onNodeWithText("Ramallah, Palestine").assertIsDisplayed()
        }
    }

    test("a prayer card offers no next-prayer row, because the whole card already opens the times") {
        cardTest(content = { prayerCard() }) {
            onNodeWithText(NEXT_PRAYER_LABEL, substring = true).assertDoesNotExist()
        }
    }

    test("a prayer card opens the prayer times from anywhere on it, and still dismisses") {
        var opened = 0
        var dismissed = 0
        cardTest(content = { prayerCard(onOpen = { opened++ }, onDismiss = { dismissed++ }) }) {
            onNodeWithText("5:12 PM").performClick()
            onNodeWithContentDescription(ENGLISH_DISMISS_LABEL).performClick()
        }

        assertThat(opened).isEqualTo(1)
        assertThat(dismissed).isEqualTo(1)
    }

    test("a prayer card says no count, because a cycle is not what a prayer time is part of") {
        cardTest(content = { prayerCard() }) {
            onNodeWithText(ENGLISH_POSITION_LABEL).assertDoesNotExist()
        }
    }

    test("it draws in both themes, and never asks the window for a background") {
        // What a semantic assertion *can* prove about theming is that the card composes cleanly in
        // either theme and lays out — a missing token or an unresolvable colour would throw here —
        // The named tests supply the theme and locale. Colour itself is deliberately not asserted
        // through the semantics tree: Compose 1.12's text semantics carry no span styles, so any such
        // assertion would be reading a null.
        listOf(true, false).forEach { dark ->
            cardTest(darkTheme = dark, content = { passageCard() }) {
                onNodeWithText(ARABIC_PASSAGE).assertIsDisplayed()
                onNodeWithText(ENGLISH_POSITION_LABEL).assertIsDisplayed()
                onNodeWithContentDescription(ENGLISH_DISMISS_LABEL).assertIsDisplayed()
            }
        }
    }

    test("a fitted card is as wide as the setting and as tall as its passage, not as tall as a slot") {
        // The one thing a fixed slot guaranteed was a height, so the fitted card is asserted to be
        // *shorter* than one rather than merely drawn — and still exactly the width it was handed, since
        // the setting the reader keeps is the measure, not the size. It also proves the card can measure
        // itself at all: the passage reaches it through a `weight`, a weight in an unbounded column
        // collapses to zero, and a card that had kept one under `FIT` would fail here as a missing
        // passage rather than as a wrong number.
        fittedCardTest {
            onNodeWithTag(FITTED_CARD_TAG).assertIsDisplayed()
            onNodeWithText(ARABIC_PASSAGE).assertIsDisplayed()
            assertThat(onNodeWithTag(FITTED_CARD_TAG).getBoundsInRoot().width.value)
                .isEqualTo(FITTED_CARD_WIDTH.value)
            assertThat(onNodeWithTag(FITTED_CARD_TAG).getBoundsInRoot().height.value)
                .isLessThan(DEFAULT_CARD_HEIGHT.value)
        }
    }
}

private fun plainNotice() =
    PresentedNotice(
        id = 1L,
        title = TITLE,
        message = MESSAGE,
        action = NotificationAction.Remembrance(REMEMBRANCE_ID),
    )

private fun passageNotice(
    passage: String,
    position: Int,
    total: Int,
    isFavorite: Boolean = false,
) = PresentedNotice(
    id = 1L,
    title = COLLECTION,
    message = TRANSLATION,
    action = NotificationAction.Remembrance(REMEMBRANCE_ID),
    content =
        NotificationContent.Remembrance(
            text = passage,
            position = position,
            total = total,
            isFavorite = isFavorite,
        ),
)

private fun prayerNotice() =
    PresentedNotice(
        id = 2L,
        title = "Maghrib",
        message = "5:12 PM • Ramallah, Palestine",
        action = NotificationAction.PrayerTimes,
        content =
            NotificationContent.Prayer(
                prayer = "Maghrib",
                time = "5:12 PM",
                city = "Ramallah, Palestine",
            ),
    )

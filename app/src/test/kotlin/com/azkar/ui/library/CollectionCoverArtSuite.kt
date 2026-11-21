package com.azkar.ui.library

import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import com.azkar.data.storage.CoverStorage
import com.azkar.testing.assertTrackedRecompositions
import com.azkar.testing.assertTrackedStable
import com.azkar.ui.theme.AzkarTheme
import de.infix.testBalloon.framework.core.testSuite
import dejavu.resetRecompositionCounts
import dejavu.runRecompositionTrackingUiTest
import dejavu.setTrackedContent
import org.assertj.core.api.Assertions.assertThat
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.nio.file.Files
import java.nio.file.Path
import java.util.Locale
import javax.imageio.ImageIO

private const val COVER_PHOTO = "Cover photo"
private const val COVER_TAG = "cover"
private const val ICON_KEY = "sunrise"

/** The loader reads from a background dispatcher, so the photo is correct some time after frame one. */
private const val PHOTO_TIMEOUT_MILLIS = 10_000L

/**
 * A collection cover is a file the app copied into its own directory, and two links stand between
 * the stored name and the pixels on a shelf card: the cache has to resolve the name to a file, and
 * the art has to hand that file to the image loader.
 *
 * The second link is the one that was broken. `CollectionCard` defaulted its `covers` argument to
 * `null` and the shelf never passed it, so every card resolved its own cover to nothing and painted
 * the generated wash instead -- a collection could be created with a picture, the picture could sit
 * on disk and in the database, and the shelf would never show it. These tests cover the default
 * itself rather than the symptom, and they pair the two cases that have to stay opposite: a cover
 * that resolves reaches the screen, and one that does not leaves the wash behind rather than a
 * hole in the grid.
 *
 * TestBalloon generates the test entry point, but the IDE does not see that usage.
 */
@Suppress("unused")
val CollectionCoverArtSuite by testSuite("Collection cover art") {
    test("a stored cover resolves to the file the app copied in") {
        withCovers { storage, covers ->
            val stored = storage.store(picture())

            assertThat(covers.file(stored)?.file?.name).isEqualTo(stored)
        }
    }

    test("a cover whose file was deleted behind the app's back resolves to nothing") {
        withCovers { storage, covers ->
            val stored = storage.store(picture())
            Files.delete(storage.resolve(stored))

            assertThat(covers.file(stored)).isNull()
        }
    }

    test("a name that was never stored resolves to nothing") {
        withCovers { storage, covers ->
            assertThat(covers.file("no-such-cover.png")).isNull()
        }
    }

    test("a stored cover is drawn over the generated wash") {
        withCovers { storage, covers ->
            val stored = storage.store(picture())

            renderArt(covers = covers, coverImagePath = stored) {
                awaitPhoto()
                onNodeWithContentDescription(COVER_PHOTO).assertIsDisplayed()
                onNodeWithTag(
                    COVER_TAG,
                ).assertTrackedRecompositions(atMost = 2, expectedFunction = "CollectionCoverArt")
            }
        }
    }

    test("a collection with no cover draws the wash and no photo") {
        withCovers { _, covers ->
            renderArt(covers = covers, coverImagePath = null) {
                onNodeWithTag(COVER_TAG).assertIsDisplayed()
                onNodeWithContentDescription(COVER_PHOTO).assertDoesNotExist()
                mainClock.advanceTimeBy(1_000)
                waitForIdle()
                onNodeWithTag(COVER_TAG).assertTrackedStable(expectedFunction = "CollectionCoverArt")
            }
        }
    }

    test("a cover that will not decode still leaves a complete tile rather than a hole") {
        withCovers { storage, covers ->
            // A PNG header with no image behind it, which is what a truncated download or a
            // half-finished copy looks like. The tile has to survive it, because the cover is
            // decoration sitting over a collection rather than the collection itself.
            val truncatedPng = Files.createTempFile("azkar-cover-art-source", ".png")
            Files.write(truncatedPng, byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 1, 2, 3))
            val stored = storage.store(truncatedPng)

            renderArt(covers = covers, coverImagePath = stored) {
                expectNoPhoto()
                onNodeWithTag(COVER_TAG).assertIsDisplayed()
                onNodeWithContentDescription(COVER_PHOTO).assertDoesNotExist()
                onNodeWithTag(
                    COVER_TAG,
                ).assertTrackedRecompositions(atMost = 2, expectedFunction = "CollectionCoverArt")
            }
        }
    }
}

/**
 * A throwaway cover directory, so nothing in this suite can reach `~/.azkar`.
 *
 * Both halves are handed over rather than just the cache: the cache is what the art is handed and
 * so is what is under test, but the storage is what a test has to write through to give it
 * something to find, and wrapping the pair in a fixture class would only hide which is which.
 */
private fun withCovers(content: (storage: CoverStorage, covers: CollectionCoverCache) -> Unit) {
    val storage = CoverStorage(Files.createTempDirectory("azkar-cover-art"))
    content(storage, CollectionCoverCache(storage))
}

/**
 * A real, decodable PNG on disk.
 *
 * Written out with ImageIO rather than pasted in as a byte constant because the loader actually
 * decodes what it is handed: a hand-written header would exercise the storage copy and nothing
 * past it, and the decode is the half that was missing.
 */
private fun picture(): Path {
    val image = BufferedImage(2, 2, BufferedImage.TYPE_INT_ARGB)
    val bytes = ByteArrayOutputStream().also { ImageIO.write(image, "png", it) }.toByteArray()
    val file = Files.createTempDirectory("azkar-cover-art-source").resolve("sunset.png")
    Files.write(file, bytes)
    return file
}

private fun renderArt(
    covers: CollectionCoverCache,
    coverImagePath: String?,
    check: ComposeUiTest.() -> Unit,
) {
    runRecompositionTrackingUiTest {
        setTrackedContent {
            AzkarTheme(darkTheme = false, uiLocale = Locale.ENGLISH) {
                CollectionCoverArt(
                    iconKey = ICON_KEY,
                    modifier = Modifier.size(120.dp).testTag(COVER_TAG),
                    coverImagePath = coverImagePath,
                    covers = covers,
                )
            }
        }
        waitForIdle()
        resetRecompositionCounts()
        check()
    }
}

/**
 * Waits for the loader rather than for a number of frames.
 *
 * The picture arrives on a background dispatcher, so the tile becomes correct an indeterminate time
 * after the first frame and there is no virtual clock here to advance. Timing out is the assertion:
 * a cover that resolves has to reach the screen inside the window or the shelf has the same bug it
 * had before, one indirection further away.
 */
private fun ComposeUiTest.awaitPhoto() {
    waitUntil(timeoutMillis = PHOTO_TIMEOUT_MILLIS) { photoNodes().isNotEmpty() }
}

/**
 * The mirror of [awaitPhoto], for a file that must *not* turn into a picture.
 *
 * Spelled as a bounded watch rather than a single check because "no photo yet" and "no photo ever"
 * are the same observation at frame one. The window is far shorter than [PHOTO_TIMEOUT_MILLIS] on
 * purpose: reading a file from the app's own covers directory is instant, so a picture that has not
 * arrived in a second was never going to.
 */
private fun ComposeUiTest.expectNoPhoto() {
    val deadline = System.nanoTime() + NO_PHOTO_WINDOW_MILLIS * NANOS_PER_MILLI
    while (System.nanoTime() < deadline) {
        assertThat(photoNodes()).describedAs("a cover that cannot decode drew a photo").isEmpty()
        Thread.sleep(POLL_MILLIS)
    }
}

private fun ComposeUiTest.photoNodes() = onAllNodes(hasContentDescription(COVER_PHOTO)).fetchSemanticsNodes()

private const val NANOS_PER_MILLI = 1_000_000L
private const val NO_PHOTO_WINDOW_MILLIS = 1_000L
private const val POLL_MILLIS = 25L

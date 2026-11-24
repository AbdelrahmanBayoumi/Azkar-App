package com.azkar.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SkikoComposeUiTest
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.v2.runSkikoComposeUiTest
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.azkar.settings.FakePreferencesDataStore
import com.azkar.settings.SettingsStore
import com.azkar.testing.assertTrackedRecompositions
import com.azkar.testing.assertTrackedStable
import com.azkar.testing.withDesktopDejavu
import com.azkar.ui.home.HomeGateway
import com.azkar.ui.home.HomeScreen
import com.azkar.ui.library.CollectionDetailScreen
import com.azkar.ui.library.CollectionSessionScreen
import com.azkar.ui.library.CollectionsScreen
import com.azkar.ui.library.RemembranceReaderScreen
import com.azkar.ui.library.rememberCollectionDetailState
import com.azkar.ui.library.rememberCollectionSessionState
import com.azkar.ui.library.rememberRemembranceReaderState
import com.azkar.ui.notifications.DesktopAudioPlayer
import com.azkar.ui.notifications.GeocodingClient
import com.azkar.ui.notifications.LocationDetector
import com.azkar.ui.notifications.NotificationPreferencesRepository
import com.azkar.ui.notifications.StartupRegistration
import com.azkar.ui.preview.PREVIEW_COLLECTION_ID
import com.azkar.ui.preview.PreviewHomeGateway
import com.azkar.ui.preview.previewAzkarAppState
import com.azkar.ui.preview.previewReminders
import com.azkar.ui.preview.rememberPreviewCatalogue
import com.azkar.ui.shell.AzkarAppState
import com.azkar.ui.shell.SettingsGroup
import com.azkar.ui.shell.SettingsScreen
import com.azkar.ui.shell.ThemePreference
import com.azkar.ui.theme.AzkarTheme
import com.azkar.ui.theme.LocalWindowSizeClass
import com.azkar.ui.theme.windowSizeClassFor
import com.azkar.updates.UpdateManager
import de.infix.testBalloon.framework.core.testSuite
import dejavu.resetRecompositionCounts
import dejavu.setTrackedContent
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import org.assertj.core.api.Assertions.assertThat
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import java.util.Locale

/**
 * The window the screens are laid out in.
 *
 * A real one, and deliberately so: `CollectionsScreen` and `CollectionDetailScreen` both branch on
 * [LocalWindowSizeClass], so a compact window would silently exercise different code than a wide one
 * and the suite would pass while proving nothing about the desktop layout the app actually ships.
 */
private val SCREEN_SIZE = Size(1440f, 900f)

/**
 * The surface every screen test renders into.
 *
 * `runComposeUiTest`'s default window is 1024x768, which is smaller than this app's own default
 * window — and at that height the library's collection grid falls below the fold, so the nodes a test
 * wants to assert on exist but are neither displayed nor clickable. `runSkikoComposeUiTest` takes
 * the surface size directly, which is what lets these tests use the same window the app ships with.
 */
@OptIn(ExperimentalTestApi::class)
private fun screenTest(block: SkikoComposeUiTest.() -> Unit) =
    withDesktopDejavu {
        runSkikoComposeUiTest(size = SCREEN_SIZE, density = Density(1f), block = block)
    }

private const val FIRST_COLLECTION_NAME = "Morning Azkar"

@OptIn(ExperimentalTestApi::class)
private fun SkikoComposeUiTest.assertSettledAndTracked(
    tag: String,
    expectedFunction: String? = null,
    changeInput: () -> Unit,
) {
    waitForIdle()
    resetRecompositionCounts()
    mainClock.advanceTimeBy(200)
    waitForIdle()
    onNodeWithTag(tag).assertTrackedStable(expectedFunction)
    runOnIdle(changeInput)
    waitForIdle()
    onNodeWithTag(tag).assertTrackedRecompositions(exactly = 1, expectedFunction = expectedFunction)
}

private const val SECOND_COLLECTION_NAME = "My night route"

/**
 * The detail screen's way out is a breadcrumb labelled with its destination rather than a generic
 * back arrow, so the label is part of the contract this suite asserts on.
 */
private const val BREADCRUMB_TO_LIBRARY = "All Collections"

/**
 * The session screen names its destination the same way.
 */
private const val SESSION_BREADCRUMB = "Back to Collections"

/**
 * Labels this suite clicks rather than renders.
 *
 * These are translation resources, unlike the fixture's collection names, so a reword in `strings.xml`
 * breaks them. They are here anyway because there is no other handle on these controls: a theme chip
 * is three unlabelled-glyph-free boxes distinguished only by their text, and a breadcrumb is only
 * findable by the destination it names. [ScreenFixture] pins the locale to English for every test in
 * this file, which is what makes pinning the English string honest rather than brittle — a
 * developer running this suite in another locale gets the same assertions, not different ones.
 *
 * Where a fixture literal exists it is preferred, and is used wherever one does: see
 * [FIRST_COLLECTION_NAME].
 */
private const val THEME_OPTION_DARK = "Dark"

/**
 * The round dismiss in the settings page header.
 *
 * It used to be a labelled "Back" button. The rail now names every block on the page, so the header only
 * has to offer a way out — and a way out is not owed a sentence, which is why this test finds it by its
 * accessible name rather than by text a reader sees.
 */
private const val SETTINGS_CLOSE = "Close"

private const val SESSION_SKIP = "Skip"

private const val TRANSLATION_HEADING = "Translation"

/**
 * The first card in `previewRailCards()`.
 *
 * A fixture literal for the same reason as [FIRST_COLLECTION_NAME]: the rail is built from the
 * preview gateway, not from translation resources, so the name only changes if the fixture is
 * rebuilt. Its id is the assertion that matters; the name is only how the card is found.
 */
private const val HOME_RAIL_FIRST_COLLECTION = "Morning Azkar"

private const val HOME_RAIL_FIRST_COLLECTION_ID = 1L

/**
 * The strings a test asserts on come from the fixture, not from `strings.xml`.
 *
 * `collectionsSearchCollections` and the screen titles are translation resources, and a test that
 * pins them breaks the moment somebody rewords one. `previewGateway()`'s collection names are
 * literals that only change if the fixture is rebuilt, so asserting those ties the test to the data
 * rather than to the copy.
 */
private val REMINDERS = previewReminders(PREVIEW_COLLECTION_ID)

/**
 * The shell a screen needs, and nothing more.
 *
 * [rememberPreviewCatalogue] is what makes these tests hermetic: it wires the catalogue to the
 * preview gateway rather than to the seeded database, so a screen reads fixtures and the suite never
 * touches Koin or opens a connection. Only `HomeScreen` cannot be built this way — it resolves its
 * own gateway and settings store from the container — which is why it has its own fixture below.
 *
 * The theme is pinned to light so a developer's desktop preference cannot decide which assertions
 * run, and the window size class is published for the same reason [SCREEN_SIZE] exists.
 */
@Composable
private fun ScreenFixture(
    uiLocale: Locale = Locale.ENGLISH,
    darkTheme: Boolean = false,
    content: @Composable () -> Unit,
) {
    AzkarTheme(darkTheme = darkTheme, uiLocale = uiLocale) {
        CompositionLocalProvider(
            LocalWindowSizeClass provides windowSizeClassFor(SCREEN_SIZE.width.dp, SCREEN_SIZE.height.dp),
        ) {
            Box(
                Modifier
                    .size(SCREEN_SIZE.width.dp, SCREEN_SIZE.height.dp)
                    .background(Color.White),
            ) {
                Box(Modifier.fillMaxSize()) { content() }
            }
        }
    }
}

/**
 * Matches the collection *card*, and only the collection card.
 *
 * This indirection is not decoration. A collection can carry a name that also appears elsewhere on
 * the library screen, and matching on the text alone finds every one of them. The cards are the only
 * clickable nodes on this screen carrying a name and *no* semantics role, so excluding role is what
 * separates them: a filter chip's click narrows the shelf rather than opening anything, and a test
 * that clicks "the collection" and lands on one of those passes for the wrong reason, or fails for a
 * reason that has nothing to do with the card. It also means the assertion survives a reworded badge
 * or a renumbered progress string, which a "match on the card's full merged text" approach would not.
 */
private fun collectionCard(name: String): SemanticsMatcher =
    hasText(name) and
        hasClickAction() and
        SemanticsMatcher.keyNotDefined(SemanticsProperties.Role)

/**
 * Runs [block] with a Koin container that can satisfy `HomeScreen`, and no screen of its own.
 *
* Home is the one screen this suite cannot build the ordinary way. Every other screen takes its
 * dependencies as parameters, so [ScreenFixture] is the whole of the wiring. `HomeScreen` resolves its
 * own `SettingsStore` and `HomeGateway` from the container, which is a real difference in how the
 * screen is constructed and not something to work around — so this stands up the smallest container
 * that can satisfy it: the store on settings it will never meaningfully write, and the preview
 * gateway that returns fixtures instead of querying.
 *
 * `SettingsScreen` joins it for a second, unrelated reason: the notification and prayer tabs resolve
 * their own settings holder, so a test that composes that screen needs the same store and the two
 * platform capabilities behind it. The container is a list of what is needed rather than a list of
 * what is convenient, which is why the detector is here even though nothing in the suite presses its
 * button.
 *
 * It deliberately does **not** render anything. An earlier version of this took a
 * `SkikoComposeUiTest` receiver and set the screen up itself, but that left every test free to call
 * `setContent` a second time with its own callbacks — and a second `setContent` mutates composition
 * state from inside composition, which Compose rejects outright with *"Unsupported concurrent change
 * during composition"*. Owning the container and nothing else keeps the test in charge of what it
 * renders.
 *
 * The container is torn down in a `finally`. A test that leaves Koin running leaks that state into
 * every suite that happens to run after it, which is the kind of failure that only shows up on a full
 * run and never in isolation. Nothing else has to be undone: the settings are in memory, so there is
 * no file to unlink and therefore no teardown that could race a reader.
 *
 * It is a top-level function rather than one declared inside `testSuite { }` because Kotlin does not
 * allow a `private` local function, and a helper local to the suite body could not be.
 */
private fun withScreenContainer(block: () -> Unit) {
    val storeScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    try {
        startKoin {
            modules(
                module {
                    single { SettingsStore(FakePreferencesDataStore(), storeScope) }
                    single { NotificationPreferencesRepository(get()) }
                    single { DesktopAudioPlayer() }
                    single<HomeGateway> { PreviewHomeGateway() }
                    // The settings page's two moved tabs resolve these themselves rather than taking
                    // them as parameters, so any test that composes that screen needs them here too.
                    // Nothing calls the detector — no test presses "Auto-detect" — but Koin has to be
                    // able to hand one over before the screen finishes composing.
                    single { HttpClient(CIO) }
                    single { LocationDetector(get()) }
                    single { GeocodingClient(get()) }
                    single { StartupRegistration() }
                    single { UpdateManager(get(), get(), storeScope) }
                },
            )
        }
        block()
    } finally {
        stopKoin()
        storeScope.cancel()
    }
}

/**
 * Smoke tests for the screens themselves.
 *
 * [com.azkar.ui.components.ControlBehaviourSuite] proves the controls work. This proves the screens
 * *wire them up*: clicking a collection card reaches the callback with the right id, that the search field is bound
 * to the catalogue rather than to a local field, that a toggle actually changes what is rendered.
 * Those are the wiring mistakes that survive every visual check, because the screen still looks
 * correct: it is only the journey from a tap to a navigation that is broken.
 *
 * Everything is asserted through semantics and callbacks, never through pixels.
 */
@OptIn(ExperimentalTestApi::class)
// TestBalloon generates the test entry point, but the IDE does not see that usage.
@Suppress("unused")
val ScreenSuite by testSuite("Screen smoke") {
    test("the library lists the collections the fixture holds") {
        screenTest {
            val padding = mutableIntStateOf(0)
            setTrackedContent {
                ScreenFixture {
                    CollectionsScreen(
                        modifier = Modifier.testTag("collections-screen").padding(padding.intValue.dp),
                        onOpenCollection = {},
                        onOpenRemembrance = {},
                        onManageTags = {},
                        showToast = { _, _, _ -> },
                        catalogue = rememberPreviewCatalogue(Locale.ENGLISH),
                    )
                }
            }
            onNode(collectionCard(FIRST_COLLECTION_NAME)).assertIsDisplayed()
            onNode(collectionCard(SECOND_COLLECTION_NAME)).assertIsDisplayed()
            assertSettledAndTracked("collections-screen", "ShelfColumn") { padding.intValue = 1 }
        }
    }

    /**
     * The search field is the one control on this screen that has to stay bound to the catalogue.
     * A copy of it that reads and writes a local field renders perfectly and filters nothing, which is
     * exactly the kind of break a screenshot cannot see — the placeholder still looks right.
     */
    test("searching the library narrows the list to the matching collection") {
        screenTest {
            setTrackedContent {
                ScreenFixture {
                    CollectionsScreen(
                        modifier = Modifier.testTag("collections-screen"),
                        onOpenCollection = {},
                        onOpenRemembrance = {},
                        onManageTags = {},
                        showToast = { _, _, _ -> },
                        catalogue = rememberPreviewCatalogue(Locale.ENGLISH),
                    )
                }
            }
            waitForIdle()
            resetRecompositionCounts()
            onNode(hasSetTextAction()).performTextInput("night")
            waitForIdle()
            onNodeWithTag(
                "collections-screen",
            ).assertTrackedRecompositions(atLeast = 1, atMost = 1, expectedFunction = "ShelfColumn")
            onNode(collectionCard(SECOND_COLLECTION_NAME)).assertIsDisplayed()
            onAllNodes(collectionCard(FIRST_COLLECTION_NAME)).assertCountEquals(0)
        }
    }

    /**
     * The id is the assertion, not the click. Every collection on this screen is a card with a
     * callback, and a screen that opened the wrong one would look identical while being useless.
     */
    test("opening a collection card reports that collection's id") {
        screenTest {
            val opened = mutableListOf<Long>()
            setTrackedContent {
                ScreenFixture {
                    CollectionsScreen(
                        modifier = Modifier.testTag("collections-screen"),
                        onOpenCollection = { opened += it },
                        onOpenRemembrance = {},
                        onManageTags = {},
                        showToast = { _, _, _ -> },
                        catalogue = rememberPreviewCatalogue(Locale.ENGLISH),
                    )
                }
            }
            waitForIdle()
            resetRecompositionCounts()
            onNode(collectionCard(FIRST_COLLECTION_NAME)).performClick()
            waitForIdle()
            onNodeWithTag("collections-screen").assertTrackedStable("ShelfColumn")
            assertThat(
                opened,
            ).describedAs("the card must report the id it stands for").containsExactly(PREVIEW_COLLECTION_ID)
        }
    }

    test("the detail screen names the collection it was opened for") {
        screenTest {
            val padding = mutableIntStateOf(0)
            setTrackedContent {
                val catalogue = rememberPreviewCatalogue(Locale.ENGLISH)
                ScreenFixture {
                    CollectionDetailScreen(
                        modifier = Modifier.testTag("collection-detail-screen").padding(padding.intValue.dp),
                        detail =
                            rememberCollectionDetailState(
                                catalogue = catalogue,
                                collectionId = PREVIEW_COLLECTION_ID,
                                reminders = REMINDERS,
                            ),
                        onBack = {},
                        onStartSession = {},
                        onOpenRemembrance = {},
                        showToast = { _, _, _ -> },
                    )
                }
            }
            waitForIdle()
            onNodeWithText(FIRST_COLLECTION_NAME).assertIsDisplayed()
            assertSettledAndTracked("collection-detail-screen", "DetailLazyPage") { padding.intValue = 1 }
        }
    }

    test("leaving the detail screen calls back") {
        screenTest {
            var backs = 0
            setTrackedContent {
                val catalogue = rememberPreviewCatalogue(Locale.ENGLISH)
                ScreenFixture {
                    CollectionDetailScreen(
                        modifier = Modifier.testTag("collection-detail-screen"),
                        detail =
                            rememberCollectionDetailState(
                                catalogue = catalogue,
                                collectionId = PREVIEW_COLLECTION_ID,
                                reminders = REMINDERS,
                            ),
                        onBack = { backs++ },
                        onStartSession = {},
                        onOpenRemembrance = {},
                        showToast = { _, _, _ -> },
                    )
                }
            }
            waitForIdle()
            resetRecompositionCounts()
            // The way out is a breadcrumb labelled with where it goes, not a generic "Back". Asserting
            // the label rather than the symbol keeps the test honest about what the control is.
            onNodeWithText(BREADCRUMB_TO_LIBRARY).performClick()
            waitForIdle()
            onNodeWithTag("collection-detail-screen").assertTrackedStable("DetailLazyPage")
            assertThat(backs).describedAs("the screen has to be able to be left").isEqualTo(1)
        }
    }

    /**
     * The strongest wiring assertion in this suite: the chip is not just painted in the right place,
     * it is the one bound to the state. A settings screen that rendered three chips and wired none of
     * them looks completely correct and does nothing.
     *
     * Both halves are checked, and both are needed. `themePreference` proves the click reached the
     * state; `persisted` proves the state reached the thing that writes it to disk. A screen that
     * updates the label and forgets to persist passes one and fails the other.
     */
    test("choosing a theme in settings reaches the state and persists it") {
        withScreenContainer {
            screenTest {
                val persisted = mutableListOf<ThemePreference>()
                setTrackedContent {
                    val state =
                        AzkarAppState(
                            initialThemePreference = ThemePreference.SYSTEM,
                            initialLocale = Locale.ENGLISH,
                            persistTheme = { persisted += it },
                            persist = {},
                            persistTypography = {},
                        )
                    ScreenFixture {
                        SettingsScreen(
                            state = state,
                            onClose = {},
                            modifier = Modifier.testTag("settings-screen"),
                        )
                    }
                }
                waitForIdle()
                resetRecompositionCounts()
                onNodeWithText("Theme").performClick()
                waitForIdle()
                onNodeWithTag("settings-screen")
                    .assertTrackedRecompositions(atLeast = 1, atMost = 3, expectedFunction = "SettingsScreenContent")
                resetRecompositionCounts()
                onNodeWithText(THEME_OPTION_DARK).performClick()
                waitForIdle()
                onNodeWithTag("settings-screen").assertTrackedStable("SettingsScreenContent")
                assertThat(
                    persisted,
                ).describedAs(
                    "the choice must reach the store, not just the label",
                ).containsExactly(ThemePreference.DARK)
            }
        }
    }

    test("scrolling to a settings section keeps screen recompositions bounded") {
        withScreenContainer {
            screenTest {
                setTrackedContent {
                    ScreenFixture {
                        SettingsScreen(
                            state = previewAzkarAppState(),
                            onClose = {},
                            modifier = Modifier.testTag("settings-screen"),
                        )
                    }
                }
                waitForIdle()
                val before = onNodeWithText("Text size", ignoreCase = true).fetchSemanticsNode().boundsInRoot.top
                resetRecompositionCounts()
                onNode(hasText("Text") and hasClickAction()).performClick()
                waitForIdle()
                onNodeWithText("Text size", ignoreCase = true).assertIsDisplayed()
                val after = onNodeWithText("Text size", ignoreCase = true).fetchSemanticsNode().boundsInRoot.top
                assertThat(after).describedAs("section navigation must scroll the settings list").isLessThan(before)
                onNodeWithTag("settings-screen")
                    .assertTrackedRecompositions(atLeast = 1, atMost = 3, expectedFunction = "SettingsScreenContent")
            }
        }
    }

    test("prayer audio navigation keeps the chosen section selected") {
        withScreenContainer {
            screenTest {
                setTrackedContent {
                    ScreenFixture {
                        SettingsScreen(
                            state = previewAzkarAppState(),
                            modifier = Modifier.testTag("settings-screen"),
                            onClose = {},
                            initialGroup = SettingsGroup.PRAYER,
                        )
                    }
                }
                waitForIdle()
                resetRecompositionCounts()
                onNodeWithText("Calculation").assertIsSelected()
                onNodeWithText("Sounds").assertIsNotSelected().performClick()
                waitForIdle()
                onNodeWithText("Sounds").assertIsSelected()
                onNodeWithTag(
                    "settings-screen",
                ).assertTrackedRecompositions(atLeast = 1, atMost = 3, expectedFunction = "SettingsScreenContent")
                resetRecompositionCounts()
                onNodeWithText("Calculation").assertIsNotSelected().performClick()
                waitForIdle()
                onNodeWithText("Calculation").assertIsSelected()
                onNodeWithTag(
                    "settings-screen",
                ).assertTrackedRecompositions(atLeast = 1, atMost = 3, expectedFunction = "SettingsScreenContent")
            }
        }
    }

    test("leaving settings calls back") {
        withScreenContainer {
            screenTest {
                var closes = 0
                setTrackedContent {
                    ScreenFixture {
                        SettingsScreen(
                            state = previewAzkarAppState(),
                            onClose = { closes++ },
                            modifier = Modifier.testTag("settings-screen"),
                        )
                    }
                }
                waitForIdle()
                resetRecompositionCounts()
                onNodeWithContentDescription(SETTINGS_CLOSE).performClick()
                waitForIdle()
                onNodeWithTag("settings-screen").assertTrackedStable("SettingsScreenContent")
                assertThat(
                    closes,
                ).describedAs("settings is pushed onto a screen and has to pop back off it").isEqualTo(1)
            }
        }
    }

    test("the session names the collection it was started for") {
        screenTest {
            val padding = mutableIntStateOf(0)
            setTrackedContent {
                val catalogue = rememberPreviewCatalogue(Locale.ENGLISH)
                ScreenFixture {
                    CollectionSessionScreen(
                        modifier = Modifier.testTag("collection-session-screen").padding(padding.intValue.dp),
                        session = rememberCollectionSessionState(catalogue, PREVIEW_COLLECTION_ID, REMINDERS),
                        onBack = {},
                        onOpenRemembrance = {},
                        showToast = { _, _, _ -> },
                    )
                }
            }
            waitForIdle()
            onNodeWithText(FIRST_COLLECTION_NAME).assertIsDisplayed()
            assertSettledAndTracked("collection-session-screen", "CollectionSessionContent") { padding.intValue = 1 }
        }
    }

    test("leaving the session calls back") {
        screenTest {
            var backs = 0
            setTrackedContent {
                val catalogue = rememberPreviewCatalogue(Locale.ENGLISH)
                ScreenFixture {
                    CollectionSessionScreen(
                        modifier = Modifier.testTag("collection-session-screen"),
                        session = rememberCollectionSessionState(catalogue, PREVIEW_COLLECTION_ID, REMINDERS),
                        onBack = { backs++ },
                        onOpenRemembrance = {},
                        showToast = { _, _, _ -> },
                    )
                }
            }
            waitForIdle()
            resetRecompositionCounts()
            onNodeWithText(SESSION_BREADCRUMB).performClick()
            waitForIdle()
            onNodeWithTag("collection-session-screen").assertTrackedStable("CollectionSessionContent")
            assertThat(backs).isEqualTo(1)
        }
    }

    /**
     * Asserted against [com.azkar.ui.library.CollectionSessionState.position] rather than the "1 / 54" counter drawn on
     * screen.
     *
     * The counter is a formatted, translated string, so a test written against it breaks on a
     * reword and — worse — can pass while the state is wrong if the format changes to still contain
     * the digits it looked for. The position is the thing the button actually moves, so asserting it
     * tests the wiring instead of the typesetting.
     */
    test("skipping in a session advances to the next passage") {
        screenTest {
            var position = -1
            setTrackedContent {
                val catalogue = rememberPreviewCatalogue(Locale.ENGLISH)
                val session = rememberCollectionSessionState(catalogue, PREVIEW_COLLECTION_ID, REMINDERS)
                position = session.position
                ScreenFixture {
                    CollectionSessionScreen(
                        modifier = Modifier.testTag("collection-session-screen"),
                        session = session,
                        onBack = {},
                        onOpenRemembrance = {},
                        showToast = { _, _, _ -> },
                    )
                }
            }
            waitForIdle()
            val start = position
            resetRecompositionCounts()
            onNodeWithContentDescription(SESSION_SKIP).performClick()
            waitForIdle()
            onNodeWithTag(
                "collection-session-screen",
            ).assertTrackedRecompositions(atLeast = 1, atMost = 1, expectedFunction = "CollectionSessionContent")
            assertThat(
                position,
            ).describedAs("skip must move on, not merely re-render the same one").isEqualTo(start + 1)
        }
    }

    /**
     * The reader resolves its passage out of the catalogue by id, and the catalogue is empty on the
     * first composition.
     *
     * `rememberPreviewCatalogue` deliberately does not call the catalogue's `loadData` — its own KDoc
     * says so, and leaves it to each screen's effects, because a screen that forgets to load is a bug
     * the preview is supposed to be able to show. That has a consequence here that is worth stating
     * plainly: **the screen has to mount before its data can arrive.**
     *
     * The obvious way to write this test — look up the id, and only render the screen once there is
     * one — deadlocks. Nothing mounts, so no effect runs, so the catalogue never loads, so there is
     * never an id. The reader is given a placeholder id and mounted unconditionally; its effect loads
     * the catalogue, the id resolves, and the screen re-resolves against the real passage. A screen
     * that only rendered once its data already existed would fail here rather than pass quietly.
     */
    test("the reader shows the passage's translation once the catalogue has loaded") {
        screenTest {
            val padding = mutableIntStateOf(0)
            setTrackedContent {
                val catalogue = rememberPreviewCatalogue(Locale.ENGLISH)
                val passageId =
                    catalogue.allCollections
                        .firstOrNull { it.id == PREVIEW_COLLECTION_ID }
                        ?.members
                        ?.firstOrNull()
                        ?.id ?: 0L
                ScreenFixture {
                    RemembranceReaderScreen(
                        modifier = Modifier.testTag("remembrance-reader-screen").padding(padding.intValue.dp),
                        detail = rememberRemembranceReaderState(catalogue = catalogue, remembranceId = passageId),
                        onBack = {},
                        showToast = { _, _, _ -> },
                    )
                }
            }
            waitForIdle()
            onNodeWithText(TRANSLATION_HEADING).assertIsDisplayed()
            assertSettledAndTracked("remembrance-reader-screen", "RemembranceReaderScreen") { padding.intValue = 1 }
        }
    }

    /**
     * The rail's cards are the one thing on Home that a tap has to carry somewhere.
     *
     * The assertion is on the id rather than the name, because the name is only how the card was found
     * — the callback could report the wrong collection and the screen would still look right.
     */
    test("opening a home rail card reports that collection's id") {
        withScreenContainer {
            screenTest {
                val opened = mutableListOf<Long>()
                var libraryOpens = 0
                setTrackedContent {
                    ScreenFixture {
                        HomeScreen(
                            modifier = Modifier.testTag("home-screen"),
                            onOpenLibrary = { libraryOpens++ },
                            onOpenRemembrance = {},
                            onOpenCollection = { opened += it },
                            reminders = REMINDERS,
                        )
                    }
                }
                waitForIdle()
                resetRecompositionCounts()
                onNode(collectionCard(HOME_RAIL_FIRST_COLLECTION)).performClick()
                waitForIdle()
                onNodeWithTag("home-screen").assertTrackedStable("HomeContent")
                assertThat(opened)
                    .describedAs("the rail card must report the collection it stands for")
                    .containsExactly(HOME_RAIL_FIRST_COLLECTION_ID)
                assertThat(libraryOpens).describedAs("opening a card is not the same as opening the library").isZero()
            }
        }
    }
}

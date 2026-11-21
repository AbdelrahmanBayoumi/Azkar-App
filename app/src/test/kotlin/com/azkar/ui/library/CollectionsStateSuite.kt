package com.azkar.ui.library

import com.azkar.TestDispatchers
import com.azkar.domain.model.CollectionKind
import com.azkar.domain.model.HadithGrade
import com.azkar.domain.model.Remembrance
import com.azkar.ui.library.data.findById
import com.azkar.ui.library.model.CollectionScope
import com.azkar.ui.library.model.CollectionSort
import de.infix.testBalloon.framework.core.testSuite
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import java.util.Locale

/** The Arabic a reader sees, read the same way production reads it. */
private fun Remembrance.arabicText(): String? = translations[ARABIC]?.translationPair?.text

/** The English a reader sees, read the same way production reads it. */
private fun Remembrance.englishText(): String? = translations[Locale.ENGLISH]?.translationPair?.text

private val ARABIC: Locale = Locale.forLanguageTag("ar")

/**
 * The shelf's state machine: what a load puts in it, and what every mutation does to the screen.
 *
 * The rules pinned down here are the ones a user would call a bug. A star that stays lit after a
 * failed write, a rename that moves a tile to the bottom of a name-sorted shelf, a delete that takes
 * the tile away even though the write failed, a search box that answers a half-typed word.
 */
@OptIn(ExperimentalCoroutinesApi::class)
// TestBalloon generates the test entry point, but the IDE does not see that usage.
@Suppress("unused")
val CollectionsStateSuite by testSuite("The collections shelf state") {
    val morning = remembrance(id = 1L, text = "Whoever says good morning")
    val evening = remembrance(id = 2L, text = "Whoever says good evening")
    val travel = remembrance(id = 3L, text = "The traveller's supplication")

    /**
     * A state wired to a fake and running on an unconfined scope.
     *
     * Unconfined is what lets an assertion read immediately after a call: every mutation launches
     * into a scope that runs the body on the calling thread, so "the write was issued" and "the
     * screen was told" are both observable without pumping a scheduler first. The three tests that
     * need a clock pass a `TestScope` instead, because waiting out a delay — or letting two loads
     * race — is the one thing that genuinely needs one.
     */
    fun stateWith(
        gateway: FakeCollectionsGateway,
        scope: CoroutineScope = CoroutineScope(TestDispatchers.unconfined),
    ): CollectionsState =
        CollectionsState(
            scope = scope,
            catalogue =
                CollectionsCatalogue(
                    scope = scope,
                    uiLocale = Locale.ENGLISH,
                    gateway = gateway,
                    covers = fakeCovers(),
                ),
        )

    /** A loaded state and the fake behind it, which most tests need both of. */
    fun loaded(
        gateway: FakeCollectionsGateway,
        scope: CoroutineScope = CoroutineScope(TestDispatchers.unconfined),
    ): Pair<CollectionsState, FakeCollectionsGateway> {
        val state = stateWith(gateway, scope)
        state.loadData()
        return state to gateway
    }

    fun morningCollection(
        id: Long = 10L,
        name: String = "Morning Azkar",
        members: List<Remembrance> = emptyList(),
    ) = officialCollection(id = id, name = name, members = members)

    fun favoritesCollection(members: List<Remembrance> = emptyList()) =
        officialCollection(id = 10L, name = "Favorites", members = members)

    // ---- loading -------------------------------------------------------------------------------

    test("a load fills the shelf and leaves the spinner") {
        val gateway =
            FakeCollectionsGateway(
                collections = listOf(morningCollection()),
                remembrances = listOf(morning),
            )

        val (state, _) = loaded(gateway)

        assertThat(state.allCollections.map { it.name }).containsExactly("Morning Azkar")
        assertThat(state.allRemembrances).hasSize(1)
        assertThat(state.loading).isFalse()
        assertThat(state.isEmpty).isFalse()
        // A read that worked says nothing the shelf it filled does not already say, so the footer
        // stays empty — there is no "updated" stamp to keep the reader's eye on.
        assertThat(state.infoLine).isEmpty()
    }

    test("a load failure empties the shelf instead of leaving a stale one on screen") {
        val gateway = FakeCollectionsGateway(collections = listOf(morningCollection()))
        val state = stateWith(gateway)
        gateway.failure = IllegalStateException("the database is gone")

        state.loadData()

        // Half a shelf — the collections without the remembrances behind them — is worse than none:
        // every tile on it would open onto an empty page with nothing to say why.
        assertThat(state.allCollections).isEmpty()
        assertThat(state.allRemembrances).isEmpty()
        assertThat(state.loading).isFalse()
    }

    test("a load that a later load overtakes is discarded rather than applied late") {
        runTest {
            val gateway = FakeCollectionsGateway(collections = listOf(morningCollection()))
            // Hold the first re-read open. Two writes back to back each queue a quiet reload, and
            // a FIFO test dispatcher runs them in the order they were issued — so without a gate the
            // stale read always lands last and the guard under test would pass either way.
            gateway.readGates[1] = CompletableDeferred()
            val state = stateWith(gateway, this)
            state.loadData()
            advanceUntilIdle()

            // Membership edits, not display toggles: a toggle patches the one tile it touched and
            // reads nothing back, so it is not a read to overtake.
            state.catalogue.addMembers(10L, listOf(1L)) {}
            advanceUntilIdle()

            gateway.collections = listOf(morningCollection(name = "Evening Azkar"))
            state.catalogue.addMembers(10L, listOf(2L)) {}
            advanceUntilIdle()

            // Only now is the first read allowed to finish, carrying the shelf as it was before the
            // second write.
            gateway.releaseRead(1)
            advanceUntilIdle()

            // Without the guard the screen settles on whichever read finished last, which is not
            // necessarily the one the user asked for.
            assertThat(state.allCollections.map { it.name }).containsExactly("Evening Azkar")
        }
    }

    test("every screen asking for the shelf gets the one read the catalogue already made") {
        val gateway = FakeCollectionsGateway(collections = listOf(morningCollection()))
        val scope = CoroutineScope(TestDispatchers.unconfined)
        // One catalogue, as the shell builds it, with the three screens' views over it: the shelf,
        // a collection's detail page and its session screen.
        val catalogue =
            CollectionsCatalogue(scope = scope, uiLocale = Locale.ENGLISH, gateway = gateway, covers = fakeCovers())

        val shelf = CollectionsState(scope = scope, catalogue = catalogue)
        val detail = CollectionsState(scope = scope, catalogue = catalogue)
        val session = CollectionsState(scope = scope, catalogue = catalogue)
        shelf.loadData()
        detail.loadData()
        session.loadData()

        // Each of these used to build a holder of its own and read the whole database, so walking
        // into a collection and back out re-queried all 270 remembrances and every member of every
        // collection — three times over, because all three screens were on the stack.
        assertThat(gateway.snapshotReads).isEqualTo(1)
    }

    // ---- recitation progress -------------------------------------------------------------------

    testSuite("how far through each collection the user is") {
        test("every card carries the count its own collection was loaded with") {
            val gateway =
                FakeCollectionsGateway(
                    collections = listOf(morningCollection(members = listOf(morning, evening))),
                    remembrances = listOf(morning, evening),
                )
            gateway.recitedCounts = mapOf(10L to 1)

            val (state, _) = loaded(gateway)

            assertThat(state.shelfTiles.single().recitedCount).isEqualTo(1)
            assertThat(state.shelfTiles.single().completion).isEqualTo(0.5f)
        }

        test("a collection with nothing recorded reads as zero rather than as absent") {
            val gateway = FakeCollectionsGateway(collections = listOf(morningCollection(members = listOf(morning))))
            gateway.recitedCounts = emptyMap()

            val (state, _) = loaded(gateway)

            // Zero, not missing: the card draws nothing at all below one, so the distinction the map
            // makes between "not asked about" and "counted nothing" cannot reach the screen.
            assertThat(state.recitedCounts).isEmpty()
            assertThat(state.shelfTiles.single().recitedCount).isZero()
            assertThat(state.shelfTiles.single().completion).isZero()
        }

        test("a load failure takes the counts with the shelf") {
            val gateway = FakeCollectionsGateway(collections = listOf(morningCollection()))
            gateway.recitedCounts = mapOf(10L to 4)
            val state = stateWith(gateway)
            gateway.failure = IllegalStateException("the database is gone")

            state.loadData()

            assertThat(state.recitedCounts).isEmpty()
        }

        test("an empty collection's card never reads as complete") {
            val gateway = FakeCollectionsGateway(collections = listOf(morningCollection()))

            val (state, _) = loaded(gateway)

            // Nothing to recite is not the same as having recited everything, and a card that said
            // so would put a finished collection on the shelf beside the finished ones.
            assertThat(state.shelfTiles.single().completion).isZero()
        }
    }

    // ---- favourites ----------------------------------------------------------------------------

    test("starring a remembrance applies at once, before the write is confirmed") {
        val gateway = FakeCollectionsGateway(remembrances = listOf(morning, evening))
        val (state, _) = loaded(gateway)

        state.toggleFavorite(1L)

        assertThat(state.allRemembrances.findById(1L)?.isFavorite).isTrue()
        assertThat(gateway.favoriteWrites).containsExactly(1L to true)
    }

    test("a failed star write rolls the tile back") {
        val gateway = FakeCollectionsGateway(remembrances = listOf(morning))
        val (state, _) = loaded(gateway)
        gateway.failure = IllegalStateException("disk full")

        state.toggleFavorite(1L)

        // Leaving the star lit would be a claim the screen cannot take back: nothing else in the
        // shelf would ever change it, so the only way to find out it was wrong is to leave and
        // come back.
        assertThat(state.allRemembrances.findById(1L)?.isFavorite).isFalse()
    }

    test("starring mirrors favorites and removes the now-filed remembrance from Uncategorized") {
        val gateway =
            FakeCollectionsGateway(
                collections =
                    listOf(
                        favoritesCollection(),
                        morningCollection(id = 11L),
                        customCollection(id = 12L, name = "Uncategorized"),
                    ),
                remembrances = listOf(morning),
                favoritesCollectionId = 10L,
                uncategorizedCollectionId = 12L,
            )
        val (state, _) = loaded(gateway)

        state.toggleFavorite(1L)

        // The favorite shelf is a membership and therefore disqualifies this untagged remembrance
        // from Uncategorized. A reload reconciles both collection mirrors after the favorite write.
        assertThat(
            state.allCollections
                .findById(10L)
                ?.members
                ?.map { it.id },
        ).containsExactly(1L)
        assertThat(state.allCollections.findById(12L)?.members).isEmpty()
        assertThat(gateway.snapshotReads).isEqualTo(2)
    }

    test("un-starring removes the remembrance from the favourites collection") {
        val starred = remembrance(id = 1L, text = "Whoever says good morning", favorite = true)
        val gateway =
            FakeCollectionsGateway(
                collections = listOf(favoritesCollection(members = listOf(starred))),
                remembrances = listOf(starred),
                favoritesCollectionId = 10L,
            )
        val (state, _) = loaded(gateway)

        state.toggleFavorite(1L)

        assertThat(state.allCollections.findById(10L)?.members).isEmpty()
    }

    test("starring leaves the other collections' membership alone") {
        val gateway =
            FakeCollectionsGateway(
                collections = listOf(morningCollection()),
                remembrances = listOf(morning),
            )
        val (state, _) = loaded(gateway)

        state.toggleFavorite(1L)

        assertThat(state.allCollections.findById(10L)?.members).isEmpty()
    }

    test("starring fills the star on the collection page's own copy of the member") {
        val gateway =
            FakeCollectionsGateway(
                collections = listOf(morningCollection(members = listOf(morning, evening))),
                remembrances = listOf(morning, evening),
            )
        val (state, _) = loaded(gateway)

        state.toggleFavorite(1L)

        // A collection carries its own Remembrance instances rather than pointing at the flat list,
        // so patching the flat list alone leaves every collection page still holding the old value.
        // That page is the one the star is pressed on, and a star that does not fill in there is a
        // broken button as far as the user is concerned — the write going through is invisible.
        assertThat(
            state.allCollections
                .findById(10L)
                ?.members
                ?.first { it.id == 1L }
                ?.isFavorite,
        ).isTrue()
        // And the member beside it in the same collection is not dragged along with it.
        assertThat(
            state.allCollections
                .findById(10L)
                ?.members
                ?.first { it.id == 2L }
                ?.isFavorite,
        ).isFalse()
        assertThat(gateway.favoriteWrites).containsExactly(1L to true)
    }

    test("un-starring empties the star on the collection page's own copy too") {
        val starred = remembrance(id = 1L, text = "Whoever says good morning", favorite = true)
        val gateway =
            FakeCollectionsGateway(
                collections = listOf(morningCollection(members = listOf(starred, evening))),
                remembrances = listOf(starred, evening),
            )
        val (state, _) = loaded(gateway)

        state.toggleFavorite(1L)

        assertThat(
            state.allCollections
                .findById(10L)
                ?.members
                ?.first { it.id == 1L }
                ?.isFavorite,
        ).isFalse()
    }

    test("starring something that is not in the catalogue does nothing") {
        val gateway = FakeCollectionsGateway(remembrances = listOf(morning))
        val (state, _) = loaded(gateway)

        state.toggleFavorite(99L)

        assertThat(gateway.favoriteWrites).isEmpty()
    }

    // ---- editing a remembrance's text -----------------------------------------------------------

    test("editing a text reaches the card that was edited, not just the flat list") {
        val gateway =
            FakeCollectionsGateway(
                collections = listOf(morningCollection(members = listOf(morning, evening))),
                remembrances = listOf(morning, evening),
            )
        val (state, _) = loaded(gateway)

        state.catalogue.saveRemembranceTexts(id = 1L, arabic = "أصلح", english = "Corrected")

        // The same trap the star fell into: a collection holds its own Remembrance instances, so a
        // patch that only rewrites allRemembrances leaves the page you typed on showing the old
        // words. The write succeeding is invisible, which is indistinguishable from a broken editor.
        assertThat(
            state.allCollections
                .findById(10L)
                ?.members
                ?.first { it.id == 1L }
                ?.arabicText(),
        ).isEqualTo("أصلح")
        assertThat(
            state.allRemembrances
                .findById(1L)
                ?.englishText(),
        ).isEqualTo("Corrected")
        // The member beside it is not edited along with it.
        assertThat(
            state.allCollections
                .findById(10L)
                ?.members
                ?.first { it.id == 2L }
                ?.arabicText(),
        ).isEqualTo("ذكر 2")
    }

    test("editing hands the gateway a remembrance that already carries an id, so the write updates") {
        val gateway =
            FakeCollectionsGateway(
                collections = listOf(morningCollection(members = listOf(morning))),
                remembrances = listOf(morning),
            )
        val (state, _) = loaded(gateway)

        state.catalogue.saveRemembranceTexts(id = 1L, arabic = "أصلح", english = null)

        // A save that arrives without an id is an insert, and the edit would silently become a
        // second copy of the remembrance rather than a correction to the first.
        assertThat(gateway.savedRemembrances).hasSize(1)
        assertThat(gateway.savedRemembrances.single().id).isEqualTo(1L)
    }

    test("a new remembrance is saved and added to the collection that opened its form") {
        val gateway = FakeCollectionsGateway(collections = listOf(customCollection(id = 10L, name = "My collection")))
        val (state, _) = loaded(gateway)
        val translations =
            mapOf(
                ARABIC to
                    Remembrance.Translations(
                        explanationPair = Remembrance.Translations.Pair(id = null, text = ""),
                        translationPair = Remembrance.Translations.Pair(id = null, text = "الحمد لله"),
                    ),
                Locale.ENGLISH to
                    Remembrance.Translations(
                        explanationPair = Remembrance.Translations.Pair(id = null, text = "Praise belongs to God"),
                        translationPair = Remembrance.Translations.Pair(id = null, text = "All praise is for God"),
                    ),
            )

        state.catalogue.createRemembrance(
            collectionId = 10L,
            grade = HadithGrade.HASAN,
            source = "Book 1",
            translations = translations,
            tagNames = listOf("morning"),
            tagLocale = ARABIC,
        )

        val saved = gateway.savedRemembrances.single()
        assertThat(saved.id).isEqualTo(11L)
        assertThat(saved.grade).isEqualTo(HadithGrade.HASAN)
        assertThat(saved.source).isEqualTo("Book 1")
        assertThat(saved.tags.single().name).isEqualTo("morning")
        assertThat(saved.tags.single().nameLocale).isEqualTo(ARABIC)
        assertThat(
            state.allCollections
                .findById(10L)
                ?.members
                ?.map { it.id },
        ).containsExactly(11L)
        assertThat(state.noticeKey).isEqualTo(CollectionNoticeKey.REMEMBRANCE_CREATED)
    }

    test("a new untagged remembrance is filed in Uncategorized instead of the open collection") {
        val gateway =
            FakeCollectionsGateway(
                collections =
                    listOf(
                        customCollection(id = 10L, name = "My collection"),
                        customCollection(id = 20L, name = "Uncategorized"),
                    ),
                uncategorizedCollectionId = 20L,
            )
        val (state, _) = loaded(gateway)

        state.catalogue.createRemembrance(
            collectionId = 10L,
            grade = HadithGrade.UNSPECIFIED,
            source = null,
            translations =
                mapOf(
                    ARABIC to
                        Remembrance.Translations(
                            explanationPair = Remembrance.Translations.Pair(id = null, text = ""),
                            translationPair = Remembrance.Translations.Pair(id = null, text = "الحمد لله"),
                        ),
                ),
            tagNames = emptyList(),
            tagLocale = ARABIC,
        )

        val createdId = requireNotNull(gateway.savedRemembrances.single().id)
        assertThat(state.allCollections.findById(10L)?.members).isEmpty()
        assertThat(
            state.allCollections
                .findById(20L)
                ?.members
                ?.map { it.id },
        ).containsExactly(createdId)
    }

    test("permanent remembrance deletion removes it from the catalogue and every collection") {
        val gateway =
            FakeCollectionsGateway(
                collections =
                    listOf(
                        customCollection(id = 10L, name = "Morning", members = listOf(morning)),
                        customCollection(id = 11L, name = "Saved", members = listOf(morning)),
                    ),
                remembrances = listOf(morning),
            )
        val (state, _) = loaded(gateway)

        state.catalogue.deleteRemembrance(1L)

        assertThat(state.allRemembrances).isEmpty()
        assertThat(state.allCollections.flatMap { it.members }).isEmpty()
        assertThat(state.noticeKey).isEqualTo(CollectionNoticeKey.REMEMBRANCE_DELETED)
    }

    test("removing the last membership of an untagged remembrance files it in Uncategorized") {
        val gateway =
            FakeCollectionsGateway(
                collections =
                    listOf(
                        customCollection(id = 10L, name = "Morning", members = listOf(morning)),
                        customCollection(id = 20L, name = "Uncategorized"),
                    ),
                remembrances = listOf(morning),
                uncategorizedCollectionId = 20L,
            )
        val (state, _) = loaded(gateway)

        state.catalogue.removeMembers(collectionId = 10L, remembranceIds = listOf(1L))

        assertThat(state.allCollections.findById(10L)?.members).isEmpty()
        assertThat(
            state.allCollections
                .findById(20L)
                ?.members
                ?.map { it.id },
        ).containsExactly(1L)
    }

    test("deleting the last containing collection files its untagged members in Uncategorized") {
        val gateway =
            FakeCollectionsGateway(
                collections =
                    listOf(
                        customCollection(id = 10L, name = "Morning", members = listOf(morning)),
                        customCollection(id = 20L, name = "Uncategorized"),
                    ),
                remembrances = listOf(morning),
                uncategorizedCollectionId = 20L,
            )
        val (state, _) = loaded(gateway)

        state.catalogue.deleteCollection(10L)

        assertThat(state.allCollections.findById(10L)).isNull()
        assertThat(
            state.allCollections
                .findById(20L)
                ?.members
                ?.map { it.id },
        ).containsExactly(1L)
    }

    test("the Uncategorized collection cannot be edited or deleted through the catalogue") {
        val gateway =
            FakeCollectionsGateway(
                collections = listOf(customCollection(id = 20L, name = "Uncategorized")),
                uncategorizedCollectionId = 20L,
            )
        val (state, _) = loaded(gateway)

        state.catalogue.editCollection(
            id = 20L,
            name = "Renamed",
            description = null,
            tagNames = emptyList(),
            coverImagePath = null,
        )
        state.catalogue.deleteCollection(20L)

        assertThat(gateway.collections.single().name).isEqualTo("Uncategorized")
        assertThat(state.noticeKey).isNull()
    }

    test("leaving the translation blank keeps it, while naming one replaces it") {
        val gateway =
            FakeCollectionsGateway(
                collections = listOf(morningCollection(members = listOf(morning))),
                remembrances = listOf(morning),
            )
        val (state, _) = loaded(gateway)

        state.catalogue.saveRemembranceTexts(id = 1L, arabic = "أصلح", english = null)

        // Null means "the user did not touch the translation", which is not the same request as
        // "the translation should now be empty" — and the dialog collapses a blank box to null for
        // exactly that reason.
        assertThat(state.allRemembrances.findById(1L)?.englishText()).isEqualTo("Whoever says good morning")
    }

    test("editing something that is not in the catalogue writes nothing") {
        val gateway = FakeCollectionsGateway(remembrances = listOf(morning))
        val (state, _) = loaded(gateway)

        state.catalogue.saveRemembranceTexts(id = 99L, arabic = "أصلح", english = null)

        assertThat(gateway.savedRemembrances).isEmpty()
    }

    test("the collection page shows the correction before the write has been read back") {
        runTest {
            val gateway =
                FakeCollectionsGateway(
                    collections = listOf(morningCollection(members = listOf(morning, evening))),
                    remembrances = listOf(morning, evening),
                )
            // A scheduler that holds the launch rather than running it on the calling thread, unlike
            // the unconfined scope every other test here uses. The point is to stand in the gap
            // between the edit and the reload that follows it: an unconfined scope finishes both
            // inside the call, so it can only ever report the state *after* the read, and an
            // optimistic patch would pass or fail on whether the gateway cooperated rather than on
            // whether the patch was there.
            val state = stateWith(gateway, this)
            state.loadData()
            advanceUntilIdle()

            state.catalogue.saveRemembranceTexts(id = 1L, arabic = "أصلح", english = null)

            // Nothing has been read back yet. This is what the user is looking at.
            assertThat(
                state.allCollections
                    .findById(10L)
                    ?.members
                    ?.first { it.id == 1L }
                    ?.arabicText(),
            ).isEqualTo("أصلح")
            assertThat(gateway.savedRemembrances).isEmpty()
        }
    }

    // ---- filtering -----------------------------------------------------------------------------

    test("the scope chip narrows the shelf") {
        val gateway =
            FakeCollectionsGateway(
                collections =
                    listOf(
                        morningCollection(),
                        customCollection(id = 20L, name = "For the car"),
                    ),
            )
        val (state, _) = loaded(gateway)

        state.scopeFilter = CollectionScope.CUSTOM

        assertThat(state.visibleCollections.map { it.name }).containsExactly("For the car")
    }

    test("the sort choice reaches the shelf") {
        val gateway =
            FakeCollectionsGateway(
                collections =
                    listOf(
                        customCollection(id = 20L, name = "Zzz"),
                        customCollection(id = 21L, name = "Aaa"),
                    ),
            )
        val (state, _) = loaded(gateway)

        state.selectSort(CollectionSort.NAME)

        assertThat(state.visibleCollections.map { it.name }).containsExactly("Aaa", "Zzz")
    }

    test("a tile carries the count and the icon the shelf draws") {
        val gateway =
            FakeCollectionsGateway(
                collections =
                    listOf(
                        morningCollection(members = listOf(morning, evening)),
                    ),
            )
        val (state, _) = loaded(gateway)

        val tile = state.shelfTiles.single()

        assertThat(tile.name).isEqualTo("Morning Azkar")
        assertThat(tile.memberCount).isEqualTo(2)
        assertThat(tile.iconKey).isEqualTo("mosque")
    }

    test("the shelf search narrows the shelf") {
        val gateway =
            FakeCollectionsGateway(
                collections =
                    listOf(
                        morningCollection(),
                        customCollection(id = 20L, name = "For the car"),
                    ),
            )
        val (state, _) = loaded(gateway)

        state.collectionQuery = "car"

        assertThat(state.visibleCollections.map { it.name }).containsExactly("For the car")
        assertThat(state.isEmpty).isFalse()
    }

    test("a shelf nothing matches reads as empty rather than as still loading") {
        val gateway = FakeCollectionsGateway(collections = listOf(morningCollection()))
        val (state, _) = loaded(gateway)

        state.collectionQuery = "nothing called that"

        assertThat(state.isEmpty).isTrue()
    }

    // ---- the remembrance search ----------------------------------------------------------------

    test("the remembrance search answers only once the user stops typing") {
        runTest {
            val gateway =
                FakeCollectionsGateway(
                    collections = listOf(morningCollection(members = listOf(morning))),
                    remembrances = listOf(morning),
                )
            val state = stateWith(gateway, this)
            state.loadData()

            state.onRemembranceQueryChanged("morning")

            // Grouping scans every member of every collection, so answering per keystroke would
            // make typing into a large catalogue progressively slower as the answer grows.
            assertThat(state.isSearchingRemembrances).isFalse()
            assertThat(state.searchGroups).isEmpty()

            // Running the clock out is what "the user stopped typing" looks like to the debounce.
            advanceUntilIdle()

            assertThat(state.isSearchingRemembrances).isTrue()
            assertThat(state.searchGroups.map { it.collectionName }).containsExactly("Morning Azkar")
        }
    }

    test("clearing the search shows the shelf again without waiting out the debounce") {
        runTest {
            val gateway =
                FakeCollectionsGateway(
                    collections = listOf(morningCollection(members = listOf(morning))),
                    remembrances = listOf(morning),
                )
            val state = stateWith(gateway, this)
            state.loadData()
            state.onRemembranceQueryChanged("morning")
            advanceUntilIdle()

            state.onRemembranceQueryChanged("")

            // The clearest possible query should be the fastest one to answer; making it wait out a
            // debounce would make clearing the field feel slower than typing it.
            assertThat(state.isSearchingRemembrances).isFalse()
            assertThat(state.visibleCollections).hasSize(1)
        }
    }

    test("leaving the screen cancels the pending search") {
        runTest {
            val gateway =
                FakeCollectionsGateway(
                    collections = listOf(morningCollection()),
                    remembrances = listOf(morning),
                )
            val state = stateWith(gateway, this)
            state.loadData()
            state.onRemembranceQueryChanged("morning")

            state.shutdown()
            advanceUntilIdle()

            // Nobody is reading the state any more, so running the debounce would only keep the
            // screen's scope alive a moment longer than it needs to be.
            assertThat(state.isSearchingRemembrances).isFalse()
        }
    }

    // ---- shelf mutations -----------------------------------------------------------------------

    test("a new collection lands on the shelf and reports itself") {
        val gateway = FakeCollectionsGateway()
        val (state, _) = loaded(gateway)
        var createdId: Long? = null

        state.createCollection(
            name = "For the car",
            description = "Long drives.",
            tagNames = emptyList(),
            coverImagePath = null,
            onCreated = { createdId = it },
        )

        assertThat(createdId).isNotNull()
        assertThat(state.allCollections.map { it.name }).containsExactly("For the car")
        assertThat(state.noticeKey).isEqualTo(CollectionNoticeKey.CREATED)
    }

    test("a new collection counts as the user's own straight away") {
        val gateway = FakeCollectionsGateway(collections = listOf(customCollection(id = 20L, name = "Existing")))
        val (state, _) = loaded(gateway)

        state.createCollection("For the car", null, emptyList(), null, onCreated = {})

        // The badge counts custom collections, and one the user just made is the most certainly
        // custom thing on the shelf.
        assertThat(state.membershipCounts.customCollectionIds).containsExactlyInAnyOrder(20L, 21L)
    }

    test("a failed create reports the failure and leaves the shelf alone") {
        val gateway = FakeCollectionsGateway()
        val (state, _) = loaded(gateway)
        gateway.failure = IllegalStateException("name already taken")

        state.createCollection("For the car", null, emptyList(), null, onCreated = {})

        assertThat(state.allCollections).isEmpty()
        assertThat(state.noticeKey).isEqualTo(CollectionNoticeKey.FAILED)
    }

    test("a rename keeps the collection where the shelf had it") {
        val gateway =
            FakeCollectionsGateway(
                collections =
                    listOf(
                        customCollection(id = 20L, name = "For the car"),
                        customCollection(id = 21L, name = "For the garden"),
                    ),
            )
        val (state, _) = loaded(gateway)
        state.selectSort(CollectionSort.NAME)

        state.editCollection(
            id = 21L,
            name = "Zzz renamed",
            description = null,
            tagNames = emptyList(),
            coverImagePath = null,
        )

        // A rename that re-sorts the shelf looks exactly like the save having worked on something
        // else, and the user has to hunt for the tile they just edited.
        assertThat(state.visibleCollections.map { it.name }).containsExactly("For the car", "Zzz renamed")
    }

    test("a rename reports itself") {
        val gateway = FakeCollectionsGateway(collections = listOf(customCollection(id = 20L, name = "For the car")))
        val (state, _) = loaded(gateway)

        state.editCollection(
            id = 20L,
            name = "Car",
            description = null,
            tagNames = emptyList(),
            coverImagePath = null,
        )

        assertThat(state.noticeKey).isEqualTo(CollectionNoticeKey.EDITED)
    }

    test("pinning lifts a collection above the rest of the shelf") {
        val gateway =
            FakeCollectionsGateway(
                collections =
                    listOf(
                        customCollection(id = 20L, name = "Aaa"),
                        customCollection(id = 21L, name = "Bbb"),
                    ),
            )
        val (state, _) = loaded(gateway)
        state.selectSort(CollectionSort.NAME)

        state.setDisplay(21L, CollectionDisplayPreference.PINNED, true)

        assertThat(state.visibleCollections.map { it.name }).containsExactly("Bbb", "Aaa")
        assertThat(state.noticeKey).isEqualTo(CollectionNoticeKey.PINNED)
    }

    test("unpinning puts a collection back into the sort") {
        val gateway =
            FakeCollectionsGateway(
                collections =
                    listOf(
                        customCollection(id = 20L, name = "Aaa"),
                        customCollection(
                            id = 21L,
                            name = "Bbb",
                            pinnedAt = FixtureInstant,
                        ),
                    ),
            )
        val (state, _) = loaded(gateway)
        state.selectSort(CollectionSort.NAME)

        state.setDisplay(21L, CollectionDisplayPreference.PINNED, false)

        assertThat(state.visibleCollections.map { it.name }).containsExactly("Aaa", "Bbb")
        assertThat(state.noticeKey).isEqualTo(CollectionNoticeKey.UNPINNED)
    }

    test("a duplicate arrives as one of the user's own, with the members it was copied from") {
        val gateway =
            FakeCollectionsGateway(collections = listOf(morningCollection(members = listOf(morning))))
        val (state, _) = loaded(gateway)

        state.duplicateCollection(id = 10L, newName = "My mornings")

        val copy = state.allCollections.last()
        assertThat(copy.name).isEqualTo("My mornings")
        assertThat(copy.kind).isEqualTo(CollectionKind.CUSTOM)
        assertThat(copy.members.map { it.id }).containsExactly(1L)
        assertThat(state.noticeKey).isEqualTo(CollectionNoticeKey.DUPLICATED)
    }

    test("a delete takes the collection off the shelf") {
        val gateway = FakeCollectionsGateway(collections = listOf(customCollection(id = 20L, name = "For the car")))
        val (state, _) = loaded(gateway)

        state.deleteCollection(20L)

        assertThat(state.allCollections).isEmpty()
        assertThat(state.noticeKey).isEqualTo(CollectionNoticeKey.DELETED)
    }

    test("a failed delete leaves the collection on screen and says so") {
        val gateway = FakeCollectionsGateway(collections = listOf(customCollection(id = 20L, name = "For the car")))
        val (state, _) = loaded(gateway)
        gateway.failure = IllegalStateException("the file is in use")

        state.deleteCollection(20L)

        // Removing the tile before the delete succeeded would hide the collection the user can
        // still see in the detail page they are about to go back from.
        assertThat(state.allCollections.map { it.name }).containsExactly("For the car")
        assertThat(state.noticeKey).isEqualTo(CollectionNoticeKey.FAILED)
    }

    // ---- membership ----------------------------------------------------------------------------

    test("adding remembrances reports how many were not already members") {
        val gateway =
            FakeCollectionsGateway(
                collections = listOf(customCollection(id = 20L, name = "For the car", members = listOf(morning))),
                remembrances = listOf(morning, evening, travel),
            )
        val (state, _) = loaded(gateway)
        var added: Int? = null

        state.catalogue.addMembers(collectionId = 20L, remembranceIds = listOf(1L, 2L)) { added = it }

        // "Added three" when one of them was already there would be a lie told in the only place the
        // user is looking.
        assertThat(added).isEqualTo(1)
        assertThat(state.noticeKey).isEqualTo(CollectionNoticeKey.MEMBERS_ADDED)
    }

    test("adding remembrances refreshes the shelf in the background") {
        val gateway =
            FakeCollectionsGateway(
                collections = listOf(customCollection(id = 20L, name = "For the car")),
                remembrances = listOf(morning, evening),
            )
        val (state, _) = loaded(gateway)

        state.catalogue.addMembers(collectionId = 20L, remembranceIds = listOf(1L, 2L)) {}

        // Membership lives in the database, so the tile's count can only come from a re-read — and
        // it has to be a *quiet* one: the user is mid-flow, and flashing the whole screen back into
        // a loading state to add one tile is worse than the moment's staleness.
        assertThat(state.allCollections.findById(20L)?.members).hasSize(2)
        assertThat(state.loading).isFalse()
    }

    test("removing a remembrance refreshes the shelf too") {
        val gateway =
            FakeCollectionsGateway(
                collections =
                    listOf(
                        customCollection(id = 20L, name = "For the car", members = listOf(morning, evening)),
                    ),
                remembrances = listOf(morning, evening),
            )
        val (state, _) = loaded(gateway)

        state.catalogue.removeMembers(collectionId = 20L, remembranceIds = listOf(1L))

        assertThat(
            state.allCollections
                .findById(20L)
                ?.members
                .orEmpty()
                .map { it.id },
        ).containsExactly(2L)
        assertThat(state.noticeKey).isEqualTo(CollectionNoticeKey.MEMBERS_REMOVED)
    }

    test("a failed removal reports the failure rather than quietly re-reading") {
        val gateway =
            FakeCollectionsGateway(
                collections = listOf(customCollection(id = 20L, name = "For the car", members = listOf(morning))),
                remembrances = listOf(morning),
            )
        val (state, _) = loaded(gateway)
        gateway.failure = IllegalStateException("the write was rejected")

        state.catalogue.removeMembers(collectionId = 20L, remembranceIds = listOf(1L))

        assertThat(state.noticeKey).isEqualTo(CollectionNoticeKey.FAILED)
    }

    test("the cover an edit carried lands on the tile") {
        val gateway =
            FakeCollectionsGateway(
                collections = listOf(customCollection(id = 20L, name = "For the car", members = listOf(morning))),
                remembrances = listOf(morning),
            )
        val (state, _) = loaded(gateway)

        state.catalogue.editCollection(
            id = 20L,
            name = "For the car",
            description = null,
            tagNames = emptyList(),
            coverImagePath = "cover.jpg",
        )

        assertThat(state.shelfTiles.single().coverImagePath).isEqualTo("cover.jpg")
    }

    test("clearing the cover takes it back off the tile") {
        val gateway =
            FakeCollectionsGateway(
                collections = listOf(customCollection(id = 20L, name = "For the car", coverImagePath = "cover.jpg")),
            )
        val (state, _) = loaded(gateway)

        state.catalogue.editCollection(
            id = 20L,
            name = "For the car",
            description = null,
            tagNames = emptyList(),
            coverImagePath = null,
        )

        assertThat(state.shelfTiles.single().coverImagePath).isNull()
    }

    // ---- notices and lookup --------------------------------------------------------------------

    test("a notice is raised once and then goes away") {
        val gateway = FakeCollectionsGateway()
        val (state, _) = loaded(gateway)

        state.createCollection("For the car", null, emptyList(), null, onCreated = {})
        assertThat(state.noticeKey).isEqualTo(CollectionNoticeKey.CREATED)

        state.onNotice = null
        assertThat(state.noticeKey).isNull()
    }

    test("the detail screen's lookup records that the collection was opened") {
        val gateway = FakeCollectionsGateway(collections = listOf(customCollection(id = 20L, name = "For the car")))
        val (state, _) = loaded(gateway)

        val found = state.findCollection(20L)

        // Opening is what the recently-updated order sorts by, so a collection the user reads has
        // to move up without anything having been edited.
        assertThat(found?.name).isEqualTo("For the car")
        assertThat(gateway.openedCollections).containsExactly(20L)
    }

    test("looking up a collection that is not there opens nothing") {
        val gateway = FakeCollectionsGateway(collections = listOf(customCollection(id = 20L, name = "For the car")))
        val (state, _) = loaded(gateway)

        assertThat(state.findCollection(99L)).isNull()
        assertThat(gateway.openedCollections).isEmpty()
    }
}

/** The key of the notice currently waiting to be shown, or `null` if there is none. */
private val CollectionsState.noticeKey: CollectionNoticeKey?
    get() = (onNotice as? CollectionNotice.Message)?.key

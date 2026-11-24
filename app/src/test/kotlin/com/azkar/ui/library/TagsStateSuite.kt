package com.azkar.ui.library

import com.azkar.TestDispatchers
import com.azkar.domain.model.CollectionSummary
import com.azkar.domain.model.TagSummary
import com.azkar.domain.model.TagUsage
import com.azkar.domain.model.TagUsageRemembrance
import com.azkar.ui.library.data.TagGateway
import com.azkar.ui.library.data.TagSaveResult
import de.infix.testBalloon.framework.core.testSuite
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import java.time.Instant
import java.util.Locale

/**
 * The vocabulary rules the Manage tags screen stands on: which tags a scope leaves standing, what
 * order the sort puts them in, and what the footer does when the list moves under it.
 *
 * The paging cases are the ones worth writing down. Every filter on this screen can change the row
 * count while the reader is on page four, and the two obvious mistakes — an empty page with no
 * explanation, and a "next" arrow that walks off the end — are both reachable without a database
 * being wrong.
 *
 * TestBalloon generates the test entry point, but the IDE does not see that usage.
 */
@Suppress("unused")
val TagsStateSuite by testSuite("The tag vocabulary") {
    /** Stamped with a chosen date, so a "newest" assertion cannot drift with the clock. */
    fun summary(
        id: Long,
        name: String,
        collections: Int = 0,
        remembrances: Int = 0,
        createdAt: Instant? = FixtureInstant,
        deletedAt: Instant? = null,
    ) = TagSummary(
        id = id,
        name = name,
        createdAt = createdAt,
        updatedAt = createdAt,
        collectionCount = collections,
        remembranceCount = remembrances,
        deletedAt = deletedAt,
    )

    /** Twenty-five tags: two full pages and a page of one, so every page boundary is exercised. */
    fun twentyFiveTags(): List<TagSummary> =
        List(25) { index -> summary(index.toLong() + 1L, String.format(Locale.ROOT, "tag %02d", index)) }

    fun fake(
        tags: List<TagSummary> = emptyList(),
        deleted: List<TagSummary> = emptyList(),
    ) = FakeTagGateway(tags = tags, deleted = deleted)

    /**
     * A loaded state and the fake behind it, on a scope that runs every write on the calling thread.
     *
     * Unconfined is what lets an assertion read straight after a call: "the write was issued" and "the
     * shelf was told" are then both observable without pumping a scheduler first.
     */
    fun loaded(
        gateway: FakeTagGateway,
        onTagsChanged: () -> Unit = {},
    ): TagsState =
        TagsState(
            scope = CoroutineScope(TestDispatchers.unconfined),
            gateway = gateway,
            onTagsChanged = onTagsChanged,
        ).also { it.load() }

    // ---- searching ---------------------------------------------------------------------------

    test("a blank search leaves the list alone rather than emptying it") {
        val tags = listOf(summary(1L, "morning"), summary(2L, "evening"))

        assertThat(filterTags(tags, "   ")).isEqualTo(tags)
    }

    test("a search reaches a name however it is capitalised") {
        val tags = listOf(summary(1L, "morning"), summary(2L, "evening"))

        assertThat(filterTags(tags, "MORNING").map { it.name }).containsExactly("morning")
    }

    test("surrounding whitespace in a search does not hide the list") {
        val tags = listOf(summary(1L, "morning"), summary(2L, "evening"))

        assertThat(filterTags(tags, "  morning  ")).hasSize(1)
    }

    // ---- scoping -----------------------------------------------------------------------------

    val used = summary(1L, "morning", collections = 1, remembrances = 52)
    val onPassagesOnly = summary(2L, "travel", remembrances = 3)
    val onCollectionsOnly = summary(3L, "hajj", collections = 2)
    val unused = summary(4L, "unfiled")
    val all = listOf(used, onPassagesOnly, onCollectionsOnly, unused)

    test("the all scope leaves the list whole") {
        assertThat(scopedTags(all, TagScope.ALL)).isEqualTo(all)
    }

    test("used counts a tag carried by anything at all, not just by a collection") {
        val scoped = scopedTags(all, TagScope.USED)

        // "Did I get round to using this label" is the question behind the scope, so a tag on only
        // passages and a tag on only collections both belong in it. Narrowing to collections would
        // leave the third row looking unused while its own figures say otherwise.
        assertThat(scoped.map { it.name }).containsExactly("morning", "travel", "hajj")
    }

    test("unused is what used leaves out, rather than what has no collections") {
        assertThat(scopedTags(all, TagScope.UNUSED).map { it.name }).containsExactly("unfiled")
    }

    test("the deleted scope empties the live list rather than filtering it") {
        assertThat(scopedTags(all, TagScope.DELETED)).isEmpty()
    }

    test("used and unused between them account for every tag, so nothing hides between the two") {
        // Spelled out rather than counted against the whole list, so that a scope added to the
        // dropdown without being applied here fails this instead of quietly passing.
        val names = (scopedTags(all, TagScope.USED) + scopedTags(all, TagScope.UNUSED)).map { it.name }

        assertThat(names).containsExactlyInAnyOrder("morning", "travel", "hajj", "unfiled")
    }

    // ---- sorting -----------------------------------------------------------------------------

    val aaa = summary(1L, "aaa", remembrances = 1, createdAt = LaterFixtureInstant)
    val bbb = summary(2L, "bbb", remembrances = 1)
    val ccc = summary(3L, "ccc", remembrances = 9)
    val ddd = summary(4L, "ddd", createdAt = null)
    val unsorted = listOf(ccc, aaa, ddd, bbb)

    test("sorting by name ignores case, because names are stored folded but read as typed") {
        val mixed = listOf(summary(1L, "banana"), summary(2L, "Apple"), summary(3L, "cherry"))

        assertThat(sortedTags(mixed, TagSort.NAME_ASC).map { it.name })
            .containsExactly("Apple", "banana", "cherry")
    }

    test("the descending name order is the ascending one turned about") {
        assertThat(sortedTags(unsorted, TagSort.NAME_DESC).map { it.name })
            .containsExactly("ddd", "ccc", "bbb", "aaa")
    }

    test("the most used order leads with the busiest tag and buries the unused ones") {
        assertThat(sortedTags(unsorted, TagSort.MOST_USED).map { it.name })
            .containsExactly("ccc", "aaa", "bbb", "ddd")
    }

    test("the least used order leads with the quietest tag") {
        assertThat(sortedTags(unsorted, TagSort.LEAST_USED).map { it.name })
            .containsExactly("ddd", "aaa", "bbb", "ccc")
    }

    test("tags tied on usage are ordered by name, so they cannot swap between presses") {
        // aaa and bbb are both on one passage. Pressing "most used" twice has to hand back the same
        // order, because nothing about the list changed between the presses.
        assertThat(sortedTags(listOf(bbb, aaa), TagSort.MOST_USED).map { it.name })
            .containsExactly("aaa", "bbb")
    }

    test("the newest order leads with the most recently added") {
        assertThat(sortedTags(unsorted, TagSort.NEWEST).map { it.name })
            .containsExactly("aaa", "bbb", "ccc", "ddd")
    }

    test("a tag that was never stamped sorts last under newest rather than first") {
        // "Added recently" is a claim about rows that were added, and a row with no date is not one.
        // Treating its missing date as the epoch would put it in the middle as if it were from 1970.
        assertThat(sortedTags(unsorted, TagSort.NEWEST).last().name).isEqualTo("ddd")
    }

    // ---- avatar colours ----------------------------------------------------------------------

    test("a tag keeps its colour across reads, because the slot is hashed from the name") {
        // Were the slot handed out in list order, deleting one tag would repaint every colour below
        // it, and the reader would watch rows they had not touched change colour.
        assertThat(tagAvatarSlot("morning", 8)).isEqualTo(tagAvatarSlot("morning", 8))
    }

    test("a negative name hash still lands inside the palette") {
        // Kotlin's remainder keeps the sign of the dividend, so a name whose hash is negative would
        // index past the end of the palette list without the floorMod.
        val slot = tagAvatarSlot("a name that hashes negative", 8)

        assertThat(slot).isBetween(0, 7)
    }

    test("every slot drawn from a spread of names stays inside the palette") {
        val slots = List(200) { index -> tagAvatarSlot("tag $index", 8) }

        assertThat(slots).allMatch { it in 0..7 }
    }

    test("two tags in different lists keep their colours when the lists are sorted apart") {
        val same = { list: List<TagSummary> -> list.associate { it.name to tagAvatarSlot(it.name, 8) } }

        assertThat(same(listOf(ccc, aaa, bbb))).isEqualTo(same(listOf(bbb, ccc, aaa)))
    }

    // ---- paging ------------------------------------------------------------------------------

    test("a list shorter than a page is one page") {
        val state = loaded(fake(tags = listOf(summary(1L, "morning"))))

        assertThat(state.pageCount).isEqualTo(1)
        assertThat(state.pagedRows).hasSize(1)
    }

    test("an empty list is still page one rather than page zero of nothing") {
        val state = loaded(fake())

        assertThat(state.pageCount).isEqualTo(1)
        assertThat(state.currentPage).isZero()
    }

    test("the page holds a screenful and the rest waits behind next") {
        val state = loaded(fake(tags = twentyFiveTags()))

        assertThat(state.pageCount).isEqualTo(3)
        assertThat(state.pagedRows).hasSize(TAGS_PAGE_SIZE)
    }

    test("the last page holds the remainder rather than a repeat of the first page") {
        val state = loaded(fake(tags = twentyFiveTags()))

        state.nextPage()
        state.nextPage()

        assertThat(state.pagedRows.map { it.name }).containsExactly("tag 24")
    }

    test("the pages between them are the rows in between, in order") {
        val state = loaded(fake(tags = twentyFiveTags()))

        state.nextPage()

        assertThat(state.pagedRows.first().name).isEqualTo("tag 12")
        assertThat(state.pagedRows.last().name).isEqualTo("tag 23")
    }

    test("next stops at the last page and previous stops at the first") {
        val state = loaded(fake(tags = twentyFiveTags()))

        state.previousPage()
        assertThat(state.currentPage).isZero()

        repeat(5) { state.nextPage() }
        assertThat(state.currentPage).isEqualTo(2)
    }

    test("a list that shrinks under the reader clamps the page instead of showing a blank one") {
        val store = fake(tags = twentyFiveTags())
        val state = loaded(store)
        state.nextPage()
        state.nextPage()
        assertThat(state.currentPage).isEqualTo(2)

        // Deleting the last thirteen tags leaves the reader on a page that no longer exists. The page
        // number is clamped for reading, so no write is needed to rescue them from a blank page.
        store.tags = store.tags.take(13)
        state.load()

        assertThat(state.pageCount).isEqualTo(2)
        assertThat(state.currentPage).isEqualTo(1)
        assertThat(state.pagedRows).hasSize(1)
    }

    test("a list emptied under the reader leaves them on the one page that exists") {
        val store = fake(tags = twentyFiveTags())
        val state = loaded(store)
        state.nextPage()

        store.tags = emptyList()
        state.load()

        assertThat(state.currentPage).isZero()
        assertThat(state.pagedRows).isEmpty()
    }

    // ---- filters and the page they move ------------------------------------------------------

    test("typing in the search returns to the first page") {
        val state = loaded(fake(tags = twentyFiveTags()))
        state.nextPage()

        state.onQueryChanged("tag 0")

        assertThat(state.currentPage).isZero()
    }

    test("choosing a scope returns to the first page, for the reason the search does") {
        val state = loaded(fake(tags = twentyFiveTags()))
        state.nextPage()

        state.selectScope(TagScope.UNUSED)

        assertThat(state.currentPage).isZero()
    }

    test("choosing a sort leaves the page alone, because the row count has not moved") {
        val state = loaded(fake(tags = twentyFiveTags()))
        state.nextPage()

        state.selectSort(TagSort.MOST_USED)

        assertThat(state.currentPage).isEqualTo(1)
    }

    test("the deleted list is paged on its own, so removed tags do not stretch the live list") {
        val gone =
            List(20) { index ->
                summary(500L + index, String.format(Locale.ROOT, "gone %02d", index), deletedAt = FixtureInstant)
            }
        val state = loaded(fake(tags = twentyFiveTags(), deleted = gone))
        assertThat(state.rows).hasSize(25)

        state.toggleDeleted()

        assertThat(state.rows).hasSize(20)
        assertThat(state.pageCount).isEqualTo(2)
    }

    // ---- live and deleted --------------------------------------------------------------------

    val deletedOne = summary(90L, "retired", collections = 2, deletedAt = FixtureInstant)

    test("the deleted tags stay out of the list until they are asked for") {
        val state =
            loaded(
                fake(tags = listOf(summary(1L, "morning")), deleted = listOf(deletedOne)),
            )

        assertThat(state.rows.map { it.name }).containsExactly("morning")
    }

    test("asking for the deleted ones takes the live ones out of the list") {
        val state =
            loaded(
                fake(tags = listOf(summary(1L, "morning")), deleted = listOf(deletedOne)),
            )

        state.toggleDeleted()

        assertThat(state.rows.map { it.name }).containsExactly("retired")
    }

    test("asking again puts the live ones back and the deleted ones away") {
        val state =
            loaded(
                fake(tags = listOf(summary(1L, "morning")), deleted = listOf(deletedOne)),
            )

        state.toggleDeleted()
        state.toggleDeleted()

        assertThat(state.rows.map { it.name }).containsExactly("morning")
    }

    test("the toggle returns to the first page, because the two lists are not the same length") {
        val state = loaded(fake(tags = twentyFiveTags()))
        state.nextPage()
        state.nextPage()
        assertThat(state.currentPage).isEqualTo(2)

        state.toggleDeleted()

        assertThat(state.currentPage).isZero()
    }

    test("an empty deleted list is its own answer, not the same one as having no tags") {
        val state = loaded(fake(tags = listOf(summary(1L, "morning"))))
        assertThat(state.isEmpty).isFalse()

        state.toggleDeleted()

        assertThat(state.noDeletedTags).isTrue()
        assertThat(state.isEmpty).isFalse()
        assertThat(state.searchMissedEverything).isFalse()
    }

    test("choosing the deleted scope shows them without the toggle having to be pressed") {
        val state =
            loaded(
                fake(tags = listOf(summary(1L, "morning")), deleted = listOf(deletedOne)),
            )

        state.selectScope(TagScope.DELETED)

        assertThat(state.rows.map { it.name }).containsExactly("retired")
    }

    test("the toggle is hidden once the deleted scope has been chosen, because it would say nothing") {
        val state =
            loaded(
                fake(tags = listOf(summary(1L, "morning")), deleted = listOf(deletedOne)),
            )
        assertThat(state.deletedToggleRelevant).isTrue()

        state.selectScope(TagScope.DELETED)

        assertThat(state.deletedToggleRelevant).isFalse()
    }

    test("the search reaches the deleted tags too, rather than only the live ones") {
        val state =
            loaded(
                fake(tags = listOf(summary(1L, "morning")), deleted = listOf(deletedOne)),
            )
        state.toggleDeleted()

        state.onQueryChanged("reti")

        assertThat(state.rows.map { it.name }).containsExactly("retired")
    }

    // ---- what the hero band reports ----------------------------------------------------------

    test("the hero counts every tag, the ones on collections, and the passages carrying a tag") {
        val state =
            loaded(
                fake(
                    tags =
                        listOf(
                            summary(1L, "morning", collections = 1, remembrances = 52),
                            summary(2L, "travel", collections = 1, remembrances = 3),
                            summary(3L, "on passages only", remembrances = 8),
                            summary(4L, "unfiled"),
                        ),
                ),
            )

        assertThat(state.stats.total).isEqualTo(4)
        assertThat(state.stats.usedInCollections).isEqualTo(2)
        // Taggings, not distinct passages: a passage is counted once per tag it carries.
        assertThat(state.stats.taggedRemembrances).isEqualTo(63)
    }

    test("the hero does not count the deleted tags among the ones in use") {
        val state =
            loaded(
                fake(tags = listOf(summary(1L, "morning", remembrances = 4)), deleted = listOf(deletedOne)),
            )

        assertThat(state.stats.total).isEqualTo(1)
        assertThat(state.stats.taggedRemembrances).isEqualTo(4)
    }

    // ---- empty answers -----------------------------------------------------------------------

    test("a search that matched nothing says so, which is not the same as having no tags") {
        val state = loaded(fake(tags = listOf(summary(1L, "morning"))))

        state.onQueryChanged("zebra")

        assertThat(state.searchMissedEverything).isTrue()
        assertThat(state.isEmpty).isFalse()
    }

    test("having no tags at all is reported as an empty library rather than a dead search") {
        val state = loaded(fake())

        assertThat(state.isEmpty).isTrue()
        assertThat(state.searchMissedEverything).isFalse()
    }

    test("a scope that leaves nothing standing is not reported as a failed search") {
        val state = loaded(fake(tags = listOf(summary(1L, "morning", remembrances = 1))))

        state.selectScope(TagScope.UNUSED)

        assertThat(state.rows).isEmpty()
        assertThat(state.searchMissedEverything).isFalse()
    }

    test("a failed read still leaves the screen, rather than spinning forever") {
        val failing =
            object : TagGateway {
                override suspend fun fetchSummaries(): List<TagSummary> = error("no database")

                override suspend fun fetchDeleted(): List<TagSummary> = error("no database")

                override suspend fun create(
                    name: String,
                    translations: Map<Locale, String>,
                    nameLocale: Locale,
                ): TagSaveResult = TagSaveResult.Saved

                override suspend fun rename(
                    id: Long,
                    newName: String,
                    translations: Map<Locale, String>?,
                    nameLocale: Locale?,
                ): TagSaveResult = TagSaveResult.Saved

                override suspend fun softDelete(id: Long) = Unit

                override suspend fun restore(id: Long) = Unit

                override suspend fun usage(id: Long): TagUsage? = null
            }
        val state =
            TagsState(CoroutineScope(TestDispatchers.unconfined), failing, onTagsChanged = {}).also { it.load() }

        assertThat(state.loading).isFalse()
        assertThat(state.onNotice).isEqualTo(CollectionNotice.Message(CollectionNoticeKey.FAILED))
    }

    // ---- the usage panel ----------------------------------------------------------------------

    test("the panel is closed until something asks for it") {
        val state = loaded(fake(tags = listOf(summary(1L, "morning"))))

        assertThat(state.usage).isNull()
    }

    test("the panel opens on the tag that asked for it, with what it is on") {
        val morning = summary(1L, "morning", collections = 1, remembrances = 2)
        val store = fake(tags = listOf(morning))
        store.answeringUsage(
            id = 1L,
            tag = morning,
            collections =
                listOf(
                    CollectionSummary(
                        id = 10L,
                        name = "Morning Azkar",
                        description = "",
                        iconKey = "sunrise",
                        coverImagePath = null,
                        memberCount = 4,
                        homePinnedAt = null,
                    ),
                ),
            remembrances =
                listOf(
                    TagUsageRemembrance(
                        id = 1L,
                        texts = mapOf(Locale.ENGLISH to "Whoever says good morning"),
                        source = null,
                    ),
                ),
        )
        val state = loaded(store)

        state.openUsage(1L)

        assertThat(state.usage?.tag?.name).isEqualTo("morning")
        assertThat(state.usage?.collections?.map { it.name }).containsExactly("Morning Azkar")
        assertThat(state.usage?.remembrances?.map { it.id }).containsExactly(1L)
        assertThat(state.usageLoading).isFalse()
    }

    test("closing the panel clears what it was holding") {
        val morning = summary(1L, "morning")
        val store = fake(tags = listOf(morning))
        store.answeringUsage(1L, morning)
        val state = loaded(store)
        state.openUsage(1L)

        state.closeUsage()

        assertThat(state.usage).isNull()
        assertThat(state.usageLoading).isFalse()
    }

    test("a tag the store has never heard of leaves the panel empty rather than half open") {
        val store = fake(tags = listOf(summary(1L, "morning")))
        val state = loaded(store)

        state.openUsage(999L)

        assertThat(state.usage).isNull()
        assertThat(state.usageLoading).isFalse()
    }

    // ---- writes ------------------------------------------------------------------------------

    test("a name the store refuses is handed back, not announced as a success") {
        val store = fake()
        store.refusedNames = setOf("morning")
        val state = loaded(store)

        var answer: TagSaveResult? = null
        runTest { answer = state.create("Morning") }

        assertThat(store.lastCreated).isEqualTo("Morning")
        assertThat(answer).isEqualTo(TagSaveResult.NameTaken)
    }

    test("a rename that is accepted tells the shelf, because its chips are a copy of the old name") {
        val store = fake(tags = listOf(summary(1L, "morning")))
        var told = 0
        val state = loaded(store) { told++ }

        runTest { state.rename(1L, "Early morning") }

        assertThat(store.lastRenamedTo).isEqualTo("Early morning")
        assertThat(told).isEqualTo(1)
    }

    test("a rename the store refuses leaves the shelf alone") {
        val store = fake(tags = listOf(summary(1L, "morning")))
        store.refusedNames = setOf("evening")
        var told = 0
        val state = loaded(store) { told++ }

        var answer: TagSaveResult? = null
        runTest { answer = state.rename(1L, "Evening") }

        assertThat(answer).isEqualTo(TagSaveResult.NameTaken)
        assertThat(told).isZero()
    }

    test("deleting a tag tells the shelf, so the chip it drew stops being offered") {
        val store = fake(tags = listOf(summary(1L, "morning")))
        var told = 0
        val state = loaded(store) { told++ }

        state.softDelete(1L)

        assertThat(store.softDeleted).isEqualTo(1L)
        assertThat(told).isEqualTo(1)
    }

    test("restoring a tag tells the shelf too, because a chip that came back needs redrawing") {
        val store = fake(deleted = listOf(summary(1L, "retired", deletedAt = FixtureInstant)))
        var told = 0
        val state = loaded(store) { told++ }

        state.restore(1L)

        assertThat(store.restored).isEqualTo(1L)
        assertThat(told).isEqualTo(1)
    }
}

/**
 * Sorting and searching a tag list whose rows carry translations.
 *
 * The tag list is the one place in the library where a translated row is most visible, because a tag
 * is a short label and a short label that stays in the wrong language reads as a bug rather than as
 * an untranslated name. Both rules below are the same rules the untagged list follows — the query
 * reaches the words on screen, and the name sort orders by them — and both have to be handed the
 * display name explicitly, because the stored name is folded lower-case and is the tag's *identity*
 * rather than its label.
 *
 * TestBalloon generates the test entry point, but the IDE does not see that usage.
 */
@Suppress("unused")
val TranslatedTagsSuite by testSuite("A tag list reads in the language on screen") {
    val arabic = Locale.forLanguageTag("ar")
    val english = Locale.ENGLISH

    /** A tag as the vocabulary stores it: folded, plus whatever wording has been added for it. */
    fun summary(
        id: Long,
        name: String,
        translations: Map<Locale, String> = emptyMap(),
    ) = TagSummary(
        id = id,
        name = name,
        createdAt = FixtureInstant,
        updatedAt = FixtureInstant,
        collectionCount = 1,
        remembranceCount = 1,
        translations = translations,
        deletedAt = null,
    )

    /** The label the Arabic interface would draw, which is what the list is sorted and searched by. */
    val arabicLabel: (TagSummary) -> String = { it.displayName(arabic) }

    val morning = summary(1L, "morning", mapOf(arabic to "أذكار الصباح", english to "Morning Azkar"))
    val evening = summary(2L, "evening", mapOf(arabic to "أذكار المساء", english to "Evening Azkar"))
    val travel = summary(3L, "travel", mapOf(arabic to "أذكار السفر", english to "Travel Azkar"))
    val untouch = summary(4L, "untouched")

    val tags = listOf(morning, evening, travel, untouch)

    testSuite("sorting by name") {
        test("the shelf is ordered by the words on screen, not by the stored slugs") {
            // Stored alphabetically the order is evening, morning, travel. In Arabic it is
            // السفر, الصباح, المساء — a different shelf, and the one the reader is actually on.
            // That order is by code point (س U+0633 < ص U+0635 < م U+0645), with Latin first because
            // it too sorts below them; a dictionary order would put المساء first.
            assertThat(sortedTags(tags, TagSort.NAME_ASC, arabicLabel).map { arabicLabel(it) })
                .containsExactly("Untouched", "أذكار السفر", "أذكار الصباح", "أذكار المساء")
        }

        test("the reverse sort is the reverse of the same order") {
            assertThat(sortedTags(tags, TagSort.NAME_DESC, arabicLabel).map { arabicLabel(it) })
                .containsExactly("أذكار المساء", "أذكار الصباح", "أذكار السفر", "Untouched")
        }

        test("an untranslated tag keeps its stored name in a translated list") {
            // Latin sorts below Arabic by code point, so the one tag with no wording leads — not
            // because it comes first in Arabic, which it does not, but because it is the only row
            // still reading in the language its name was typed in.
            val ordered = sortedTags(tags, TagSort.NAME_ASC, arabicLabel)

            assertThat(ordered.first().name).isEqualTo("untouched")
            assertThat(ordered.first().displayName(arabic)).isEqualTo("Untouched")
        }
    }

    testSuite("sorting by something other than name") {
        test("the tiebreaker is the label, so equal counts still read alphabetically on screen") {
            // Every row here carries the same counts, so the only thing ordering them is the name.
            val ordered = sortedTags(listOf(morning, evening, travel), TagSort.MOST_USED, arabicLabel)

            assertThat(ordered.map { arabicLabel(it) })
                .containsExactly("أذكار السفر", "أذكار الصباح", "أذكار المساء")
        }
    }

    testSuite("searching") {
        test("the Arabic label is found from the Arabic interface") {
            assertThat(filterTags(tags, "الصباح", arabicLabel).map { it.name }).containsExactly("morning")
        }

        test("the stored name is still found, so a search typed in the other language works too") {
            assertThat(filterTags(tags, "morning", arabicLabel).map { it.name }).containsExactly("morning")
        }

        test("a query that straddles the label and the stored name does not match") {
            // "أذكارmorning" matches neither word and must not be found by concatenating the two.
            assertThat(filterTags(tags, "أذكارmorning", arabicLabel)).isEmpty()
        }

        test("a word the label repeats is not treated as two matches") {
            // Joining the label and the stored name must not turn "morning" into a doubled string
            // that would then match a query a user did not type.
            assertThat(filterTags(tags, "morningmorning", arabicLabel)).isEmpty()
        }

        test("an empty query returns the whole list rather than nothing") {
            assertThat(filterTags(tags, "   ", arabicLabel)).isEqualTo(tags)
        }
    }

    test("the default label is the stored name, so an untagged caller is unaffected") {
        // Every other caller of these functions predates translations and passes no label; if the
        // default silently changed, the shelf would reorder itself for anyone not opting in.
        assertThat(sortedTags(tags, TagSort.NAME_ASC).map { it.name })
            .containsExactly("evening", "morning", "travel", "untouched")
    }
}

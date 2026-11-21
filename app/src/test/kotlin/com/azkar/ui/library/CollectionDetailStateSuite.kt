package com.azkar.ui.library

import com.azkar.TestDispatchers
import com.azkar.ui.library.model.CollectionMemberSort
import com.azkar.ui.library.model.CollectionMemberView
import de.infix.testBalloon.framework.core.testSuite
import kotlinx.coroutines.CoroutineScope
import org.assertj.core.api.Assertions.assertThat
import java.util.Locale

/**
 * The collection detail page's member grid: what it shows, and in what order.
 *
 * The grid grew a sort and a view of its own, and both of them are claims the page makes to the reader
 * about twenty-nine passages at once, which is the hardest kind of claim to keep. The rules worth
 * pinning down here are therefore the ones that cannot be seen in any single screenshot: that a
 * position on a card is a position in the *collection* and not in whatever the grid is currently
 * showing, and that a sort is a view of the collection rather than a rewrite of it.
 *
 * There is no longer a filter axis here. It had one — a row of category chips — and the rules it
 * needed were three of the five above. What it bought was a second way to hide a passage from a
 * reader who came here to read passages, on a page whose only two remaining controls are ones that
 * change the shape of the whole grid. A "by category" *order* survives, because ordering the members
 * under the headings they already carry is not the same act as showing only one of them.
 *
 * TestBalloon generates the test entry point, but the IDE does not see that usage.
 */
@Suppress("unused")
val CollectionDetailStateSuite by testSuite("The collection detail grid") {
    test("positions are stamped in the collection's own order, not the shelf's") {
        val detail = detailWith(members = listOf(1L, 2L, 3L))

        assertThat(detail.memberTiles().map { it.position }).containsExactly(1, 2, 3)
    }

    test("with nothing chosen for a sort or a view the grid shows every member, in order") {
        val detail = detailWith(members = listOf(1L, 2L, 3L))

        assertThat(detail.memberView).isEqualTo(CollectionMemberView.GRID)
        assertThat(detail.memberSort).isEqualTo(CollectionMemberSort.SEQUENCE)
        assertThat(detail.visibleMemberTiles().map { it.id }).containsExactly(1L, 2L, 3L)
    }

    test("the member a card leads with is not the one the collection is filed under") {
        // Every shipped row carries the publisher, so a card that led with it said the same thing as
        // the source badge printed above it, on every card, forever.
        val detail = detailWith(members = listOf(1L), categories = mapOf(1L to listOf(PUBLISHER, MORNING)))

        val tile = detail.memberTiles().single()

        assertThat(tile.category).isEqualTo(MORNING_LABEL)
        assertThat(tile.categories).contains(MORNING_LABEL, PUBLISHER_LABEL)
    }

    test("a remembrance the publisher is the only tag of still leads with the publisher") {
        val detail = detailWith(members = listOf(1L), categories = mapOf(1L to listOf(PUBLISHER)))

        assertThat(detail.memberTiles().single().category).isEqualTo(PUBLISHER_LABEL)
    }

    test("sorting by title is case-insensitive and does not reshuffle the shelf underneath it") {
        val detail =
            detailWith(
                members = listOf(1L, 2L, 3L),
                texts = mapOf(1L to "banana", 2L to "Apple", 3L to "cherry"),
            )

        detail.setMemberSort(CollectionMemberSort.TITLE)

        assertThat(detail.visibleMemberTiles().map { it.primaryPreview }).containsExactly("Apple", "banana", "cherry")
        // The collection's own order is still there to be had; a sort is a view, not a rewrite.
        assertThat(detail.memberTiles().map { it.id }).containsExactly(1L, 2L, 3L)
    }

    test("sorting by category is alphabetical, and falls back to the passage within one") {
        val detail =
            detailWith(
                members = listOf(1L, 2L, 3L),
                categories = mapOf(1L to listOf(EVENING), 2L to listOf(MORNING), 3L to listOf(MORNING)),
                texts = mapOf(1L to "aaa", 2L to "zzz", 3L to "bbb"),
            )

        detail.setMemberSort(CollectionMemberSort.CATEGORY)

        // "Evening" before "Morning", and inside "Morning" the passage breaks the tie rather than the
        // shelf's order — two members of one category would otherwise swap places every time the
        // collection was edited.
        assertThat(detail.visibleMemberTiles().map { it.id }).containsExactly(1L, 3L, 2L)
    }

    test("a trimmed case-insensitive query searches this collection and keeps collection positions") {
        val detail =
            detailWith(
                members = listOf(1L, 2L, 3L),
                texts = mapOf(1L to "First passage", 2L to "Evening prayer", 3L to "Third passage"),
            )

        detail.onMemberQueryChanged("  EVENING ")

        assertThat(detail.visibleMemberTiles().map { it.id }).containsExactly(2L)
        assertThat(detail.visibleMemberTiles().single().position).isEqualTo(2)
        assertThat(detail.memberTiles().map { it.id }).containsExactly(1L, 2L, 3L)
        assertThat(detail.memberCount).isEqualTo(3)
    }

    test("a query searches translations in another language") {
        val detail = detailWith(members = listOf(1L, 2L))

        detail.onMemberQueryChanged("ذكر 2")

        assertThat(detail.visibleMemberTiles().map { it.id }).containsExactly(2L)
    }

    test("a query searches sources and tag names") {
        val detail =
            detailWith(
                members = listOf(1L, 2L),
                categories = mapOf(2L to listOf(MORNING)),
                sources = mapOf(1L to "Collected Works", 2L to "Book of Supplications"),
            )

        detail.onMemberQueryChanged("book of supplications")
        assertThat(detail.visibleMemberTiles().map { it.id }).containsExactly(2L)

        detail.onMemberQueryChanged("morning")
        assertThat(detail.visibleMemberTiles().map { it.id }).containsExactly(2L)
    }

    test("a query with no matches hides tiles without making the collection itself empty") {
        val detail = detailWith(members = listOf(1L, 2L))

        detail.onMemberQueryChanged("no such remembrance")

        assertThat(detail.visibleMemberTiles()).isEmpty()
        assertThat(detail.memberTiles()).hasSize(2)
        assertThat(detail.memberCount).isEqualTo(2)
    }

    test("the grid and list toggles only change how the same members are drawn") {
        val detail = detailWith(members = listOf(1L, 2L, 3L))

        detail.setMemberView(CollectionMemberView.LIST)

        assertThat(detail.memberView).isEqualTo(CollectionMemberView.LIST)
        assertThat(detail.visibleMemberTiles().map { it.id }).containsExactly(1L, 2L, 3L)
    }

    test("a collection that no longer exists has no tiles and no count") {
        val detail = detailWith(members = listOf(1L, 2L))

        detail.catalogue.deleteCollection(COLLECTION_ID)

        assertThat(detail.memberTiles()).isEmpty()
        assertThat(detail.visibleMemberTiles()).isEmpty()
        assertThat(detail.memberCount).isZero()
    }
}

private const val COLLECTION_ID = 10L

/**
 * Tag names as the presenter sees them: folded, since that is what a stored name always is.
 *
 * These are the inputs. Where the assertions below compare a *label*, the expectation is the
 * capitalised form — this suite runs without loaded string resources, so
 * [com.azkar.ui.library.data.CollectionPresenter] has an empty label table, so every label comes from
 * capitalising the raw name.
 */
private const val PUBLISHER = "muslim data"
private const val MORNING = "morning"
private const val EVENING = "evening"

/** [PUBLISHER] as it reaches the user: first letter capitalised, rest left as stored. */
private const val PUBLISHER_LABEL = "Muslim data"

/** [MORNING] as it reaches the user. */
private const val MORNING_LABEL = "Morning"

/**
 * A detail state over a collection whose members carry the tags asked for.
 *
 * Tags are given per member id rather than as one list for the collection, because almost every rule
 * this suite pins down is about a *subset* of members sharing a category — a fixture that gave every
 * member the same tags could not tell a filter that works from one that matches everything.
 */
private fun detailWith(
    members: List<Long>,
    categories: Map<Long, List<String>> = emptyMap(),
    texts: Map<Long, String> = emptyMap(),
    sources: Map<Long, String?> = emptyMap(),
): CollectionDetailState {
    val remembrances =
        members.map { id ->
            remembrance(
                id = id,
                text = texts[id] ?: "Remembrance $id",
                source = sources[id] ?: "Muslim Data Azkars",
                tags = categories[id].orEmpty().map { tag(it.hashCode().toLong(), it) }.toSet(),
            )
        }
    val collection =
        customCollection(
            id = COLLECTION_ID,
            name = "Morning Azkar",
            members = remembrances,
        )
    val gateway = FakeCollectionsGateway(collections = listOf(collection), remembrances = remembrances)
    val scope = CoroutineScope(TestDispatchers.unconfined)
    val collections =
        CollectionsCatalogue(
            scope = scope,
            uiLocale = Locale.ENGLISH,
            gateway = gateway,
            covers = fakeCovers(),
        )
    collections.loadData()
    return CollectionDetailState(
        collections = collections,
        collectionId = COLLECTION_ID,
        reminders = CollectionReminderState(FakeReminderSources()),
    )
}

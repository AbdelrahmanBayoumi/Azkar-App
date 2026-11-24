package com.azkar.ui.library

import com.azkar.TestDispatchers
import com.azkar.domain.model.RemembranceCollection
import com.azkar.ui.library.model.CollectionSort
import de.infix.testBalloon.framework.core.testSuite
import kotlinx.coroutines.CoroutineScope
import org.assertj.core.api.Assertions.assertThat
import java.time.Instant
import java.util.Locale

/**
 * The order the user gives the shelf, and what the shelf does with it afterwards.
 *
 * Two things are pinned down here, and they are the two that used to disagree. The order a drag
 * writes is the order the shelf then shows — the user has just told the library which order they
 * want, and a sort that threw that away would throw it away the moment the page recomposed. And
 * that order is the one the reminder queue recites in, which is why the custom order does not hoist
 * a pinned card out of it: two different orders for the same shelf is how the app ends up promising
 * one thing and doing another.
 *
 * TestBalloon generates the test entry point, but the IDE does not see that usage.
 */
@Suppress("unused")
val CollectionOrderSuite by testSuite("The order the user gives the library") {
    val older = Instant.parse("2025-12-01T08:00:00Z")

    val morning =
        officialCollection(
            id = 10L,
            name = "Morning remembrances",
            description = "For the start of the day.",
            updatedAt = FixtureInstant,
        )
    val evening =
        officialCollection(
            id = 11L,
            name = "Evening remembrances",
            description = "For the end of the day.",
            updatedAt = LaterFixtureInstant,
        )
    val travel =
        officialCollection(
            id = 12L,
            name = "Travel supplications",
            description = "For the road.",
            updatedAt = older,
        )

    /**
     * A loaded shelf over a fake, on an unconfined scope.
     *
     * Unconfined is what lets an assertion read immediately after the call: the write and the read
     * that follows it both run on the calling thread, so the order the shelf shows is observable
     * without pumping a scheduler first.
     */
    fun loaded(
        gateway: FakeCollectionsGateway,
        scope: CoroutineScope = CoroutineScope(TestDispatchers.unconfined),
    ): Pair<CollectionsState, FakeCollectionsGateway> {
        val catalogue =
            CollectionsCatalogue(
                scope = scope,
                uiLocale = Locale.ENGLISH,
                gateway = gateway,
                covers = fakeCovers(),
            )
        val state = CollectionsState(scope = scope, catalogue = catalogue)
        state.loadData()
        return state to gateway
    }

    fun gatewayWith(vararg collections: RemembranceCollection) =
        FakeCollectionsGateway(collections = collections.toList())

    test("a drop writes the order the user gave, and the shelf then shows that order") {
        val gateway = gatewayWith(morning, evening, travel)
        val (state, _) = loaded(gateway)

        // The shelf opens on the fullest first; with no members in any of them the name decides,
        // which puts the evening before the morning.
        assertThat(state.visibleCollections.map { it.id }).containsExactly(11L, 10L, 12L)

        state.reorderVisible(from = 2, to = 0)

        assertThat(state.visibleCollections.map { it.id }).containsExactly(12L, 11L, 10L)
        assertThat(state.sort).isEqualTo(CollectionSort.CUSTOM)
    }

    test("the positions written are a complete order, not a partial one") {
        val gateway = gatewayWith(morning, evening, travel)
        val (state, _) = loaded(gateway)

        state.reorderVisible(from = 2, to = 0)

        // Every card has a slot, and no two share one: a partial write would leave two collections
        // in the same position and the shelf's order would stop being an order.
        val positions = gateway.collections.associate { it.id to it.position }
        assertThat(positions).containsExactlyInAnyOrderEntriesOf(mapOf(10L to 2, 11L to 1, 12L to 0))
    }

    test("narrowing the shelf and dropping leaves the collections out of sight where they were") {
        val gateway = gatewayWith(morning, evening, travel)
        val (state, _) = loaded(gateway)

        // Only two of the three match the search, and the third is not on screen to be moved.
        state.collectionQuery = "day"
        assertThat(state.visibleCollections.map { it.id }).containsExactly(11L, 10L)

        state.reorderVisible(from = 0, to = 1)

        // The collection the user could not see keeps its place after the ones they could — it is
        // not dragged to the front by a move that was not about it.
        val positions = gateway.collections.associate { it.id to it.position }
        assertThat(positions[10L]).isEqualTo(0)
        assertThat(positions[11L]).isEqualTo(1)
        assertThat(positions[12L]).isEqualTo(2)
    }

    test("the custom order does not hoist a pinned card out of it") {
        val pinnedTravel =
            officialCollection(
                id = 12L,
                name = "Travel supplications",
                description = "For the road.",
                updatedAt = older,
                pinnedAt = FixtureInstant,
            )
        val gateway = gatewayWith(morning, evening, pinnedTravel)
        val (state, _) = loaded(gateway)

        // Pinned first, then the rest by recency.
        assertThat(state.visibleCollections.map { it.id }).containsExactly(12L, 11L, 10L)

        state.reorderVisible(from = 1, to = 0)

        // The pinned card is second now, and stays second. A pinned card is pinned whatever order
        // the user chose, and hoisting it would make the shelf show one order while the queue — which
        // recites in the stored order — recited another.
        assertThat(state.visibleCollections.map { it.id }).containsExactly(11L, 12L, 10L)
    }

    test("a drop that does not move anything writes nothing") {
        val gateway = gatewayWith(morning, evening, travel)
        val (state, _) = loaded(gateway)

        state.reorderVisible(from = 1, to = 1)

        assertThat(state.sort).isEqualTo(CollectionSort.MOST_ITEMS)
        assertThat(gateway.collections.map { it.position }).containsOnlyNulls()
    }

    test("a drop outside the shelf is refused rather than guessed at") {
        val gateway = gatewayWith(morning, evening, travel)
        val (state, _) = loaded(gateway)

        state.reorderVisible(from = 0, to = 9)

        assertThat(state.sort).isEqualTo(CollectionSort.MOST_ITEMS)
        assertThat(gateway.collections.map { it.position }).containsOnlyNulls()
    }
}

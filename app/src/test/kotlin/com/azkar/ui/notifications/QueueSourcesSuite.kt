package com.azkar.ui.notifications

import com.azkar.ui.library.customCollection
import com.azkar.ui.library.officialCollection
import com.azkar.ui.library.remembrance
import de.infix.testBalloon.framework.core.testSuite
import org.assertj.core.api.Assertions.assertThat

/**
 * Which passages the queue can draw from, and in what order.
 *
 * The order is the part worth pinning. The queue used to recite in an order the user arranged
 * somewhere else entirely, and the library showed a different order again — so the same app could
 * promise one thing and do another. These are the rules that make the shelf's order the queue's
 * order, and the rules that make a passage picked on its own a source like any other.
 *
 * TestBalloon generates the test entry point, but the IDE does not see that usage.
 */
@Suppress("unused")
val QueueSourcesSuite by testSuite("Choosing what the queue recites") {
    val morning = remembrance(id = 1L, text = "Whoever says good morning")
    val evening = remembrance(id = 2L, text = "Whoever says good evening")
    val travel = remembrance(id = 3L, text = "The traveller's supplication")

    val morningCollection =
        officialCollection(id = 10L, name = "Morning Azkar", members = listOf(morning))
    val eveningCollection =
        officialCollection(id = 11L, name = "Evening Azkar", members = listOf(evening))
    val travelCollection =
        customCollection(id = 20L, name = "For the road", members = listOf(travel))

    test("a collection that is not a source contributes nothing") {
        val selection =
            QueueSources.select(
                collections = listOf(morningCollection, eveningCollection),
                config = ReminderQueueConfig(collectionIds = listOf(10L)),
            )

        assertThat(selection.orderedIds).containsExactly(1L)
    }

    test("the sources are recited in the order the library lists them") {
        val selection =
            QueueSources.select(
                collections = listOf(morningCollection, eveningCollection),
                config = ReminderQueueConfig(collectionIds = listOf(11L, 10L)),
            )

        // The config's order does not decide: the library's order does, because that is the order
        // the user can see and the one they arranged.
        assertThat(selection.orderedIds).containsExactly(1L, 2L)
    }

    test("a passage in two collections appears once, at the first of them") {
        val shared = officialCollection(id = 10L, name = "Morning Azkar", members = listOf(morning))
        val alsoShared =
            customCollection(id = 20L, name = "For the road", members = listOf(morning, travel))

        val selection =
            QueueSources.select(
                collections = listOf(shared, alsoShared),
                config = ReminderQueueConfig(collectionIds = listOf(10L, 20L)),
            )

        assertThat(selection.orderedIds).containsExactly(1L, 3L)
        assertThat(selection.owningCollections[1L]).containsExactly(10L, 20L)
    }

    test("the reserved picks shelf is a source without being listed in the config") {
        val picks = customCollection(id = 30L, name = "Reminder picks", members = listOf(travel))

        val selection =
            QueueSources.select(
                collections = listOf(picks),
                config = ReminderQueueConfig(collectionIds = emptyList()),
                picksCollectionId = 30L,
            )

        assertThat(selection.orderedIds).containsExactly(3L)
    }

    test("no sources at all is an empty selection, not a failure") {
        val selection =
            QueueSources.select(
                collections = listOf(morningCollection, travelCollection),
                config = ReminderQueueConfig(collectionIds = emptyList()),
            )

        assertThat(selection.orderedIds).isEmpty()
    }
}

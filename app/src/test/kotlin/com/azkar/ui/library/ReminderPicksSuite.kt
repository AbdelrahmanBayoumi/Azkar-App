package com.azkar.ui.library

import com.azkar.TestDispatchers
import com.azkar.ui.library.data.findById
import de.infix.testBalloon.framework.core.testSuite
import kotlinx.coroutines.CoroutineScope
import org.assertj.core.api.Assertions.assertThat
import java.util.Locale

/**
 * The shelf that holds the passages the user asked to be reminded about, one at a time.
 *
 * The rule under test is that the shelf exists only while there is something in it. A shelf of the
 * app's own, sitting on the user's shelf saying nothing, is a row that costs the user a glance and
 * gives nothing back — so it appears with the first pick and leaves with the last one.
 *
 * TestBalloon generates the test entry point, but the IDE does not see that usage.
 */
@Suppress("unused")
val ReminderPicksSuite by testSuite("The shelf that holds the passages the user picked") {
    val morning = remembrance(id = 1L, text = "Whoever says good morning")
    val evening = remembrance(id = 2L, text = "Whoever says good evening")

    /**
     * A catalogue over a fake, on an unconfined scope.
     *
     * Unconfined is what lets an assertion read immediately after the call: the write and the read
     * that follows it both run on the calling thread, so "the shelf is there" and "the shelf is
     * gone" are observable without pumping a scheduler first.
     */
    fun catalogueWith(
        gateway: FakeCollectionsGateway,
        scope: CoroutineScope = CoroutineScope(TestDispatchers.unconfined),
    ): CollectionsCatalogue =
        CollectionsCatalogue(
            scope = scope,
            uiLocale = Locale.ENGLISH,
            gateway = gateway,
            covers = fakeCovers(),
        )

    test("there is no picks shelf until the user picks something") {
        val gateway =
            FakeCollectionsGateway(
                collections = listOf(officialCollection(id = 10L, name = "Morning Azkar")),
                remembrances = listOf(morning, evening),
            )
        val catalogue = catalogueWith(gateway)
        catalogue.loadData()

        assertThat(catalogue.reminderPicksCollectionId).isNull()
        assertThat(catalogue.isPicked(1L)).isFalse()
    }

    test("the first pick brings the shelf into being, and the shelf holds that passage") {
        val gateway =
            FakeCollectionsGateway(
                collections = listOf(officialCollection(id = 10L, name = "Morning Azkar")),
                remembrances = listOf(morning, evening),
            )
        val catalogue = catalogueWith(gateway)
        catalogue.loadData()

        catalogue.setReminderPick(1L, picked = true)

        val shelfId = catalogue.reminderPicksCollectionId
        assertThat(shelfId).isNotNull()
        val shelf = catalogue.allCollections.findById(shelfId!!)
        assertThat(shelf).isNotNull()
        assertThat(shelf!!.members.mapNotNull { it.id }).containsExactly(1L)
        assertThat(catalogue.isPicked(1L)).isTrue()
    }

    test("the shelf holds exactly the passages that were picked") {
        val gateway =
            FakeCollectionsGateway(
                collections = listOf(officialCollection(id = 10L, name = "Morning Azkar")),
                remembrances = listOf(morning, evening),
            )
        val catalogue = catalogueWith(gateway)
        catalogue.loadData()

        catalogue.setReminderPick(1L, picked = true)
        catalogue.setReminderPick(2L, picked = true)

        val shelf = catalogue.allCollections.findById(catalogue.reminderPicksCollectionId!!)
        assertThat(shelf!!.members.mapNotNull { it.id }).containsExactlyInAnyOrder(1L, 2L)
    }

    test("unpicking one of several leaves the shelf and the other pick alone") {
        val gateway =
            FakeCollectionsGateway(
                collections = listOf(officialCollection(id = 10L, name = "Morning Azkar")),
                remembrances = listOf(morning, evening),
            )
        val catalogue = catalogueWith(gateway)
        catalogue.loadData()

        catalogue.setReminderPick(1L, picked = true)
        catalogue.setReminderPick(2L, picked = true)
        catalogue.setReminderPick(1L, picked = false)

        val shelfId = catalogue.reminderPicksCollectionId
        assertThat(shelfId).isNotNull()
        val shelf = catalogue.allCollections.findById(shelfId!!)
        assertThat(shelf!!.members.mapNotNull { it.id }).containsExactly(2L)
        assertThat(catalogue.isPicked(1L)).isFalse()
        assertThat(catalogue.isPicked(2L)).isTrue()
    }

    test("the last pick takes the shelf away with it") {
        val gateway =
            FakeCollectionsGateway(
                collections = listOf(officialCollection(id = 10L, name = "Morning Azkar")),
                remembrances = listOf(morning, evening),
            )
        val catalogue = catalogueWith(gateway)
        catalogue.loadData()

        catalogue.setReminderPick(1L, picked = true)
        val shelfId = catalogue.reminderPicksCollectionId
        catalogue.setReminderPick(1L, picked = false)

        assertThat(catalogue.reminderPicksCollectionId).isNull()
        assertThat(catalogue.allCollections.findById(shelfId!!)).isNull()
        assertThat(catalogue.isPicked(1L)).isFalse()
    }

    test("a passage deleted from the library takes its pick with it") {
        val gateway =
            FakeCollectionsGateway(
                collections = listOf(officialCollection(id = 10L, name = "Morning Azkar")),
                remembrances = listOf(morning, evening),
            )
        val catalogue = catalogueWith(gateway)
        catalogue.loadData()

        catalogue.setReminderPick(1L, picked = true)
        val shelfId = catalogue.reminderPicksCollectionId

        catalogue.deleteRemembrance(1L)

        // The shelf is emptied by the database's cascade, and an empty shelf is not a shelf.
        assertThat(catalogue.reminderPicksCollectionId).isNull()
        assertThat(catalogue.allCollections.findById(shelfId!!)).isNull()
    }

    test("the shelf is the app's own, and is not the user's to move") {
        val gateway =
            FakeCollectionsGateway(
                collections = listOf(officialCollection(id = 10L, name = "Morning Azkar")),
                remembrances = listOf(morning, evening),
            )
        val catalogue = catalogueWith(gateway)
        catalogue.loadData()

        catalogue.setReminderPick(1L, picked = true)

        assertThat(catalogue.isSystemCollection(catalogue.reminderPicksCollectionId!!)).isTrue()
        assertThat(catalogue.isSystemCollection(10L)).isFalse()
    }
}

package com.azkar.ui.library

import androidx.datastore.preferences.core.stringPreferencesKey
import com.azkar.settings.withInMemorySettings
import com.azkar.ui.library.model.CollectionSort
import de.infix.testBalloon.framework.core.testSuite
import org.assertj.core.api.Assertions.assertThat

/**
 * The stored order of the library.
 *
 * The shelf's sort is a choice the user makes once and expects to keep, the same way the colour
 * preference is: a shelf that reopened on a different order every launch would make the "My order"
 * sort — the one the queue recites in — a thing that has to be re-chosen every morning.
 *
 * Nothing here touches the disk. What is being asked is how the order is *chosen and remembered*,
 * not whether a byte reached a file, and the in-memory store answers that question without the file
 * IO that would put a filesystem between the test and the thing it is testing. A round trip is a
 * second reader over the same settings rather than a copy of a file: the in-memory store has no
 * one-instance-per-file rule, so there is nothing to copy.
 *
 * TestBalloon generates the test entry point, but the IDE does not see that usage.
 */
@Suppress("unused")
val CollectionSortStoreSuite by testSuite("The stored order of the library") {
    test("a fresh install opens on the fullest shelves first") {
        withInMemorySettings { _, store, _ ->
            val sorts = CollectionSortStore(store)

            assertThat(sorts.read()).isEqualTo(CollectionSort.MOST_ITEMS)
        }
    }

    test("a stored order is what the next run opens with") {
        withInMemorySettings { _, store, _ ->
            val sorts = CollectionSortStore(store)
            sorts.persistAndAwait(CollectionSort.NAME)

            // The next launch is a second reader over the same settings.
            val nextLaunch = CollectionSortStore(store)

            assertThat(nextLaunch.read()).isEqualTo(CollectionSort.NAME)
        }
    }

    test("the order is written under the key the store reads back") {
        withInMemorySettings { _, store, dataStore ->
            CollectionSortStore(store).persistAndAwait(CollectionSort.NAME)

            // The write itself, not the reader's memory of it: a store that moved the value somewhere
            // else would still read back what it wrote, and the order would be lost between runs.
            assertThat(dataStore.stored[CollectionSortStore.key]).isEqualTo(CollectionSort.NAME.name)
        }
    }

    test("a name this build does not know falls back to the fullest first") {
        withInMemorySettings { _, store, _ ->
            val sorts = CollectionSortStore(store)
            // Seeded under the production key rather than through [CollectionSortStore.persist],
            // because no sort this build has can produce a name it does not know.
            sorts.putStoredName("BY_VIBES")

            // A known order beats no order: a hand-edited file or a renamed constant must not take
            // the launch down with it.
            assertThat(sorts.read()).isEqualTo(CollectionSort.MOST_ITEMS)
        }
    }

    test("the stored key is the name the file format already has") {
        // The pinning is the point of this one, and it is why the suite does not simply assert on
        // [CollectionSortStore.key]: renaming the constant would silently drop everyone's saved
        // order, so the name itself has to be written out here and compared.
        assertThat(CollectionSortStore.key)
            .isEqualTo(stringPreferencesKey("library_collection_sort"))
    }
}

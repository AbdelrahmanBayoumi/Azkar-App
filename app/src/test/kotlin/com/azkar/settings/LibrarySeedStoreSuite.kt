package com.azkar.settings

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import de.infix.testBalloon.framework.core.testSuite
import org.assertj.core.api.Assertions.assertThat

/**
 * None of the shipping path used to write down that the dataset had been seeded, so a second
 * [com.azkar.data.seed.CollectionSeed] run would restart the dataset. This suite pins the one place
 * that now holds those answers — whether this installation was seeded, and which row is the starred
 * shelf — to a store that holds nothing but the answers.
 *
 * The settings are in memory because the question being asked is "does the next run read the same
 * answer?", and the in-memory store answers it in full: the keys are written and read through the
 * same code a file would go through. Whether those keys survive a restart on disk is asked once, in
 * [SettingsStoreSuite].
 *
 * This suite used to carry its own copy of the file fixture, and that copy had the teardown the
 * shared one warns about: it cancelled the scope and *then* unlinked the directory, in that order,
 * which is a race against a reader that had not finished.
 *
 * TestBalloon generates the test entry point, but the IDE does not see that usage.
 */
@Suppress("unused")
val LibrarySeedStoreSuite by testSuite("The seed bookkeeping") {
    test("a fresh install has not been seeded yet") {
        withInMemorySettings { _, store, _ ->
            val seedStore = LibrarySeedStore(store)

            assertThat(seedStore.isSeeded()).isFalse()
        }
    }

    test("marking seeded is what the next launch reads back") {
        withInMemorySettings { scope, store, dataStore ->
            val seedStore = LibrarySeedStore(store)
            seedStore.markSeeded()

            val nextRun = LibrarySeedStore(SettingsStore(dataStore, scope))

            assertThat(nextRun.isSeeded()).isTrue()
        }
    }

    test("a fresh install has no favourites shelf id") {
        withInMemorySettings { _, store, _ ->
            val seedStore = LibrarySeedStore(store)

            assertThat(seedStore.favoritesCollectionId()).isNull()
        }
    }

    test("an uncategorized shelf id survives a restart") {
        withInMemorySettings { scope, store, dataStore ->
            val seedStore = LibrarySeedStore(store)
            seedStore.setUncategorizedCollectionId(73L)

            val nextRun = LibrarySeedStore(SettingsStore(dataStore, scope))

            assertThat(nextRun.uncategorizedCollectionId()).isEqualTo(73L)
        }
    }

    test("the favourites shelf id survives a restart") {
        withInMemorySettings { scope, store, dataStore ->
            val seedStore = LibrarySeedStore(store)
            seedStore.setFavoritesCollectionId(42L)
            seedStore.setUncategorizedCollectionId(73L)

            val nextRun = LibrarySeedStore(SettingsStore(dataStore, scope))

            assertThat(nextRun.favoritesCollectionId()).isEqualTo(42L)
        }
    }

    test("the reminder picks shelf id survives a restart") {
        withInMemorySettings { scope, store, dataStore ->
            val seedStore = LibrarySeedStore(store)
            seedStore.setReminderPicksCollectionId(91L)

            val nextRun = LibrarySeedStore(SettingsStore(dataStore, scope))

            assertThat(nextRun.reminderPicksCollectionId()).isEqualTo(91L)
        }
    }

    test("forgetting drops the gate and the shelf ids together") {
        withInMemorySettings { scope, store, dataStore ->
            val seedStore = LibrarySeedStore(store)
            seedStore.markSeeded()
            seedStore.setFavoritesCollectionId(42L)
            seedStore.setUncategorizedCollectionId(73L)
            seedStore.setReminderPicksCollectionId(91L)

            seedStore.forget()

            val nextRun = LibrarySeedStore(SettingsStore(dataStore, scope))
            assertThat(nextRun.isSeeded()).isFalse()
            assertThat(nextRun.favoritesCollectionId()).isNull()
            assertThat(nextRun.uncategorizedCollectionId()).isNull()
            assertThat(nextRun.reminderPicksCollectionId()).isNull()
        }
    }

    test("the stored key is the name the file format already has") {
        assertThat(LibrarySeedStore.seededKey).isEqualTo(booleanPreferencesKey("seed_completed"))
        assertThat(LibrarySeedStore.favoritesCollectionIdKey)
            .isEqualTo(longPreferencesKey("favorites_collection_id"))
        assertThat(LibrarySeedStore.uncategorizedCollectionIdKey)
            .isEqualTo(longPreferencesKey("uncategorized_collection_id"))
        assertThat(LibrarySeedStore.reminderPicksCollectionIdKey)
            .isEqualTo(longPreferencesKey("reminder_picks_collection_id"))
    }
}

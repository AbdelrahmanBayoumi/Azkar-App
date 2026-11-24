package com.azkar.ui.library.data

import com.azkar.domain.model.CollectionKind
import com.azkar.domain.model.LocalizedPair
import com.azkar.domain.model.RemembranceCollection
import com.azkar.domain.model.impl.RemembranceCollectionImpl
import com.azkar.ui.library.FixtureInstant
import com.azkar.ui.library.LaterFixtureInstant
import com.azkar.ui.library.customCollection
import com.azkar.ui.library.model.CollectionScope
import com.azkar.ui.library.model.CollectionSort
import com.azkar.ui.library.officialCollection
import com.azkar.ui.library.remembrance
import com.azkar.ui.library.tag
import de.infix.testBalloon.framework.core.testSuite
import org.assertj.core.api.Assertions.assertThat
import java.util.Locale

/**
 * Which collections the hub shows, in what order, and what its search box reaches.
 *
 * These are rules a screen cannot be trusted to hold on its own. Two of them in particular are
 * decisions rather than consequences: that a pinned collection stays at the top whatever sort is
 * chosen, and that a cross-collection hit is reported once per collection rather than once per
 * remembrance. Both are easy to "simplify" later into something that looks equivalent and is not.
 *
 * TestBalloon generates the test entry point, but the IDE does not see that usage.
 */
@Suppress("unused")
val CollectionFilteringSuite by testSuite("Narrowing, ordering and searching the shelf") {
    val morning = remembrance(id = 1L, text = "Whoever says good morning")
    val evening = remembrance(id = 2L, text = "Whoever says good evening")
    val travel = remembrance(id = 3L, text = "The traveller's supplication")

    val morningCollection =
        officialCollection(id = 10L, name = "Morning Azkar", members = listOf(morning))
    val eveningCollection =
        officialCollection(id = 11L, name = "Evening Azkar", members = listOf(evening))
    val pinnedEveningCollection =
        officialCollection(
            id = 11L,
            name = "Evening Azkar",
            members = listOf(evening),
            pinnedAt = FixtureInstant,
            lastOpenedAt = LaterFixtureInstant,
        )
    val carCollection =
        customCollection(id = 20L, name = "For the car", description = "Long drives.", members = listOf(travel))
    val pinnedCarCollection =
        customCollection(
            id = 20L,
            name = "For the car",
            description = "Long drives.",
            members = listOf(travel),
            pinnedAt = FixtureInstant,
        )

    test("the default shelf shows every collection") {
        val shelf =
            CollectionFiltering.shelf(
                collections = listOf(morningCollection, carCollection),
                scope = CollectionScope.ALL,
                query = "",
                sort = CollectionSort.NAME,
            )

        assertThat(shelf.map { it.name }).containsExactly("For the car", "Morning Azkar")
    }

    testSuite("choosing one collection from the sidebar") {
        val shelf =
            CollectionFiltering.shelf(
                collections = listOf(morningCollection, carCollection),
                scope = CollectionScope.ALL,
                query = "",
                sort = CollectionSort.NAME,
                categoryId = morningCollection.id,
            )

        test("the sidebar's chosen collection is the only card on show") {
            assertThat(shelf.map { it.name }).containsExactly("Morning Azkar")
        }

        test("a search that does not match the chosen collection empties the grid outright") {
            // The user picked a category and then typed something else. Leaving the neighbouring
            // collections in would answer a question they did not ask.
            val elsewhere =
                CollectionFiltering.shelf(
                    collections = listOf(morningCollection, carCollection),
                    scope = CollectionScope.ALL,
                    query = "For the car",
                    sort = CollectionSort.NAME,
                    categoryId = morningCollection.id,
                )

            assertThat(elsewhere).isEmpty()
        }

        test("a search that does match it still shows it") {
            val matching =
                CollectionFiltering.shelf(
                    collections = listOf(morningCollection, carCollection),
                    scope = CollectionScope.ALL,
                    query = "morning",
                    sort = CollectionSort.NAME,
                    categoryId = morningCollection.id,
                )

            assertThat(matching.map { it.name }).containsExactly("Morning Azkar")
        }

        test("the scope still has the last word, so a dead-end row shows nothing rather than a surprise") {
            // The sidebar lists a collection's own row; if the scope filter has hidden that
            // collection, clicking its row should not smuggle it back past the filter the user set.
            val pinnedOnly =
                CollectionFiltering.shelf(
                    collections = listOf(morningCollection, carCollection),
                    scope = CollectionScope.PINNED,
                    query = "",
                    sort = CollectionSort.NAME,
                    categoryId = morningCollection.id,
                )

            assertThat(pinnedOnly).isEmpty()
        }
    }

    testSuite("the progress scope") {
        // Three members, so a shelf can be part-way through rather than all-or-nothing: a
        // one-member collection is either untouched or finished, and neither is "in progress".
        val partlyRead =
            customCollection(
                id = 30L,
                name = "Half read",
                members = listOf(morning, evening, travel),
            )
        val untouched = customCollection(id = 31L, name = "Untouched", members = listOf(morning, evening))
        val finished = customCollection(id = 32L, name = "Finished", members = listOf(morning))
        val shelf = listOf(partlyRead, untouched, finished)

        test("only a collection that is neither untouched nor finished is in progress") {
            val inProgress =
                CollectionFiltering.shelf(
                    collections = shelf,
                    scope = CollectionScope.IN_PROGRESS,
                    query = "",
                    sort = CollectionSort.NAME,
                    recitedCounts = mapOf(30L to 1, 32L to 1),
                )

            assertThat(inProgress.map { it.name }).containsExactly("Half read")
        }

        test("a collection absent from the counts is treated as untouched, not as part-way") {
            val inProgress =
                CollectionFiltering.shelf(
                    collections = shelf,
                    scope = CollectionScope.IN_PROGRESS,
                    query = "",
                    sort = CollectionSort.NAME,
                    recitedCounts = mapOf(30L to 2, 32L to 1),
                )

            // 31L is missing on purpose: the map leaves untouched collections out to keep it small,
            // so a missing entry has to mean zero rather than "unknown".
            assertThat(inProgress.map { it.name }).containsExactly("Half read")
        }

        test("a scope asked for progress with no counts at all shows nothing") {
            val inProgress =
                CollectionFiltering.shelf(
                    collections = shelf,
                    scope = CollectionScope.IN_PROGRESS,
                    query = "",
                    sort = CollectionSort.NAME,
                )

            assertThat(inProgress).isEmpty()
        }

        test("the counts are members, not repetitions, so one repeated passage is not progress") {
            val singleRead =
                customCollection(id = 33L, name = "One passage said", members = listOf(morning, evening))
            val inProgress =
                CollectionFiltering.shelf(
                    collections = listOf(singleRead),
                    scope = CollectionScope.IN_PROGRESS,
                    query = "",
                    sort = CollectionSort.NAME,
                    // A count above the member count would mean repetitions were counted, which is
                    // exactly the confusion this scope must not have.
                    recitedCounts = mapOf(33L to 40),
                )

            assertThat(inProgress).isEmpty()
        }

        test("a pinned collection part-way through still leads, because pinning outranks progress") {
            val pinnedHalfRead =
                customCollection(
                    id = 34L,
                    name = "Aaa half read",
                    pinnedAt = FixtureInstant,
                    members = listOf(morning, evening),
                )
            val inProgress =
                CollectionFiltering.shelf(
                    collections = listOf(pinnedHalfRead, partlyRead),
                    scope = CollectionScope.IN_PROGRESS,
                    query = "",
                    sort = CollectionSort.NAME,
                    recitedCounts = mapOf(30L to 1, 34L to 1),
                )

            assertThat(inProgress.map { it.name }).containsExactly("Aaa half read", "Half read")
        }

        test("the search still has the last word over the scope") {
            val inProgress =
                CollectionFiltering.shelf(
                    collections = shelf,
                    scope = CollectionScope.IN_PROGRESS,
                    query = "finished",
                    sort = CollectionSort.NAME,
                    recitedCounts = mapOf(30L to 1, 32L to 1),
                )

            // The collection the query names is finished, so it is out of scope even though the
            // query matched it. Two narrowings, and the scope is applied first on purpose.
            assertThat(inProgress).isEmpty()
        }
    }

    test("the official scope keeps the collections the app ships") {
        val shelf =
            CollectionFiltering.shelf(
                collections = listOf(morningCollection, carCollection),
                scope = CollectionScope.OFFICIAL,
                query = "",
                sort = CollectionSort.NAME,
            )

        assertThat(shelf.map { it.name }).containsExactly("Morning Azkar")
    }

    test("the custom scope keeps only the collections the user made") {
        val shelf =
            CollectionFiltering.shelf(
                collections = listOf(morningCollection, carCollection),
                scope = CollectionScope.CUSTOM,
                query = "",
                sort = CollectionSort.NAME,
            )

        assertThat(shelf.map { it.name }).containsExactly("For the car")
    }

    test("the pinned scope keeps a pinned collection of either kind") {
        val shelf =
            CollectionFiltering.shelf(
                collections =
                    listOf(
                        morningCollection,
                        carCollection,
                        pinnedEveningCollection,
                        pinnedCarCollection,
                    ),
                scope = CollectionScope.PINNED,
                query = "",
                sort = CollectionSort.NAME,
            )

        // Pinning is a shelf position, not a kind: a pinned shipped collection has to survive the
        // same filter a pinned custom one does, or "pinned" would silently mean "pinned and mine".
        assertThat(shelf.map { it.name }).containsExactly("Evening Azkar", "For the car")
    }

    test("the starred scope keeps a starred collection of either kind, and a star is not a pin") {
        val starredEvening =
            officialCollection(
                id = 11L,
                name = "Evening Azkar",
                members = listOf(evening),
                isFavorite = true,
            )
        val starredCar =
            customCollection(
                id = 20L,
                name = "For the car",
                description = "Long drives.",
                members = listOf(travel),
                isFavorite = true,
            )
        val shelf =
            CollectionFiltering.shelf(
                collections =
                    listOf(
                        morningCollection,
                        carCollection,
                        pinnedEveningCollection,
                        pinnedCarCollection,
                        starredEvening,
                        starredCar,
                    ),
                scope = CollectionScope.FAVORITE,
                query = "",
                sort = CollectionSort.NAME,
            )

        // The starred rows are deliberately the *unpinned* twins of the pinned ones, because that is
        // the pair the shelf could plausibly confuse: a star says which collections are wanted and a
        // pin says where they sit, so each scope has to survive the other one's rows being present.
        // The pinned shortcut in the shelf is skipped for starred collections for the same reason —
        // the standing promise is pinned-first whatever the scope asked for.
        assertThat(shelf.map { it.name }).containsExactly("Evening Azkar", "For the car")
    }

    test("only the all scope leaves the shelf whole") {
        // Spelled out rather than counted, so that adding a scope fails this test rather than
        // quietly passing it: a new narrowing scope that nobody has declared narrowing yet is the
        // one this assertion exists to catch.
        assertThat(CollectionScope.entries.map { it.isNarrowing })
            .containsExactly(false, true, true, true, true, true, true)
    }

    test("the in-reminders scope keeps only the collections the queue draws from") {
        val shelf =
            CollectionFiltering.shelf(
                collections = listOf(morningCollection, carCollection),
                scope = CollectionScope.IN_REMINDERS,
                query = "",
                sort = CollectionSort.NAME,
                reminderCollectionIds = setOf(20L),
            )

        assertThat(shelf.map { it.name }).containsExactly("For the car")
    }

    test("the in-reminders scope is empty when nothing is in the queue") {
        val shelf =
            CollectionFiltering.shelf(
                collections = listOf(morningCollection, carCollection),
                scope = CollectionScope.IN_REMINDERS,
                query = "",
                sort = CollectionSort.NAME,
                reminderCollectionIds = emptySet(),
            )

        assertThat(shelf).isEmpty()
    }

    test("the custom sort is the order the user gave, and it is not the pinned one") {
        val ordered =
            listOf(
                customCollection(id = 20L, name = "For the car", position = 0),
                officialCollection(id = 10L, name = "Morning Azkar", position = 1),
            )
        val shelf =
            CollectionFiltering.shelf(
                collections = ordered,
                scope = CollectionScope.ALL,
                query = "",
                sort = CollectionSort.CUSTOM,
            )

        // The pinned shortcut is skipped for the custom sort: the order the user owns is the order
        // the shelf shows, and the queue recites in that same order.
        assertThat(shelf.map { it.name }).containsExactly("For the car", "Morning Azkar")
    }

    test("a collection nobody has moved sorts last under the custom sort") {
        val shelf =
            CollectionFiltering.shelf(
                collections =
                    listOf(
                        officialCollection(id = 10L, name = "Morning Azkar", position = 3),
                        customCollection(id = 20L, name = "For the car"),
                    ),
                scope = CollectionScope.ALL,
                query = "",
                sort = CollectionSort.CUSTOM,
            )

        // The one with no position has never been dragged, so it goes after the one that has —
        // otherwise a brand-new collection would jump to the front of the shelf the moment it was
        // created.
        assertThat(shelf.map { it.name }).containsExactly("Morning Azkar", "For the car")
    }

    testSuite("labelling a collection keeps only what carries the chosen label") {
        val morningTag = tag(1L, "morning")
        val eveningTag = tag(2L, "evening")
        val labelledMorning =
            officialCollection(id = 10L, name = "Morning Azkar", members = listOf(morning), tags = setOf(morningTag))
        val labelledEvening =
            officialCollection(id = 11L, name = "Evening Azkar", members = listOf(evening), tags = setOf(eveningTag))
        val labelledBoth =
            customCollection(
                id = 20L,
                name = "Both",
                members = listOf(morning, evening),
                tags = setOf(morningTag, eveningTag),
            )
        val unlabelled = customCollection(id = 21L, name = "Unlabelled", members = listOf(travel))

        fun shelfOf(
            vararg collections: RemembranceCollection,
            tagIds: Set<Long> = emptySet(),
        ) = CollectionFiltering.shelf(
            collections = collections.toList(),
            scope = CollectionScope.ALL,
            query = "",
            sort = CollectionSort.NAME,
            tagIds = tagIds,
        )

        test("no labels chosen leaves the shelf whole") {
            val shelf = shelfOf(labelledMorning, labelledEvening, labelledBoth, unlabelled)

            assertThat(shelf).hasSize(4)
        }

        test("a collection carrying any of the chosen labels is kept") {
            val shelf =
                shelfOf(labelledMorning, labelledEvening, labelledBoth, unlabelled, tagIds = setOf(morningTag.id!!))

            assertThat(shelf.map { it.name }).containsExactlyInAnyOrder("Morning Azkar", "Both")
        }

        test("requiring all of them is not the question being asked") {
            val shelf =
                shelfOf(
                    labelledMorning,
                    labelledEvening,
                    labelledBoth,
                    unlabelled,
                    tagIds = setOf(morningTag.id!!, eveningTag.id!!),
                )

            assertThat(shelf.map { it.name }).containsExactlyInAnyOrder("Morning Azkar", "Evening Azkar", "Both")
        }

        test("a collection carrying neither is dropped") {
            val shelf =
                shelfOf(labelledMorning, labelledEvening, labelledBoth, unlabelled, tagIds = setOf(morningTag.id!!))

            assertThat(shelf.map { it.name }).doesNotContain("Unlabelled")
            assertThat(shelf.map { it.name }).doesNotContain("Evening Azkar")
        }
    }

    testSuite("the progress sort") {
        // Deliberately different sizes, because the whole point of the order is that it compares
        // shares rather than counts. A big shelf read once must not outrank a small one nearly done.
        val small = customCollection(id = 40L, name = "Small", members = listOf(morning, evening, travel))
        val large =
            customCollection(
                id = 41L,
                name = "Large",
                members = List(20) { index -> remembrance(id = 100L + index) },
            )
        val untouched = customCollection(id = 42L, name = "Untouched", members = listOf(morning, evening))
        val shelf = listOf(large, untouched, small)

        test("the furthest along comes first, however big it is") {
            val sorted =
                CollectionFiltering.shelf(
                    collections = shelf,
                    scope = CollectionScope.ALL,
                    query = "",
                    sort = CollectionSort.PROGRESS,
                    recitedCounts = mapOf(40L to 2, 41L to 1),
                )

            // 2 of 3 beats 1 of 20. Counting passages instead would have put Large first.
            assertThat(sorted.map { it.name }).containsExactly("Small", "Large", "Untouched")
        }

        test("a shelf with nothing counted falls back to the name, not to the shelf order") {
            val sorted =
                CollectionFiltering.shelf(
                    collections = shelf,
                    scope = CollectionScope.ALL,
                    query = "",
                    sort = CollectionSort.PROGRESS,
                )

            assertThat(sorted.map { it.name }).containsExactly("Large", "Small", "Untouched")
        }

        test("two collections on the same share are ordered by name, whatever their size") {
            val aaa =
                customCollection(
                    id = 45L,
                    name = "Aaa",
                    members = listOf(morning, evening),
                )
            val bbb =
                customCollection(
                    id = 46L,
                    name = "Bbb",
                    members = List(4) { index -> remembrance(id = 200L + index) },
                )
            val sorted =
                CollectionFiltering.shelf(
                    collections = listOf(bbb, aaa),
                    scope = CollectionScope.ALL,
                    query = "",
                    sort = CollectionSort.PROGRESS,
                    // Half of two and half of four: the shares tie, the sizes do not.
                    recitedCounts = mapOf(45L to 1, 46L to 2),
                )

            assertThat(sorted.map { it.name }).containsExactly("Aaa", "Bbb")
        }

        test("a count left above a collection's member count does not outrank a finished one") {
            val done =
                customCollection(
                    id = 47L,
                    name = "Done",
                    members = List(3) { index -> remembrance(id = 300L + index) },
                )
            val stale = customCollection(id = 48L, name = "Stale", members = listOf(morning))
            val sorted =
                CollectionFiltering.shelf(
                    collections = listOf(stale, done),
                    scope = CollectionScope.ALL,
                    query = "",
                    sort = CollectionSort.PROGRESS,
                    // Five counted on a one-member collection, which is what deleting a passage
                    // after the fact leaves behind. Read raw it is 500% and would lead the shelf.
                    recitedCounts = mapOf(47L to 3, 48L to 5),
                )

            assertThat(sorted.map { it.name }).containsExactly("Done", "Stale")
        }

        test("a pinned collection still leads the progress order") {
            val pinnedUntouched =
                customCollection(
                    id = 44L,
                    name = "Aaa pinned, unread",
                    pinnedAt = FixtureInstant,
                    members = listOf(morning),
                )
            val sorted =
                CollectionFiltering.shelf(
                    collections = listOf(small, pinnedUntouched),
                    scope = CollectionScope.ALL,
                    query = "",
                    sort = CollectionSort.PROGRESS,
                    recitedCounts = mapOf(40L to 2),
                )

            assertThat(sorted.map { it.name }).containsExactly("Aaa pinned, unread", "Small")
        }

        test("the order the sort gives is the order the in-progress scope filters to") {
            val nearly = customCollection(id = 45L, name = "Nearly", members = listOf(morning, evening))
            val justStarted =
                customCollection(
                    id = 46L,
                    name = "Just started",
                    members = listOf(morning, evening, travel),
                )
            val counts = mapOf(45L to 1, 46L to 1)

            val sorted =
                CollectionFiltering.shelf(
                    collections = listOf(justStarted, nearly),
                    scope = CollectionScope.ALL,
                    query = "",
                    sort = CollectionSort.PROGRESS,
                    recitedCounts = counts,
                )
            val filtered =
                CollectionFiltering.shelf(
                    collections = listOf(justStarted, nearly),
                    scope = CollectionScope.IN_PROGRESS,
                    query = "",
                    sort = CollectionSort.PROGRESS,
                    recitedCounts = counts,
                )

            // A shelf filtered and then sorted must not reshuffle itself: one of two beats one of
            // three, so both routes have to agree.
            assertThat(sorted.map { it.name }).containsExactly("Nearly", "Just started")
            assertThat(filtered.map { it.name }).containsExactly("Nearly", "Just started")
        }
    }

    test("a collection query matches the name however it is capitalised") {
        val shelf =
            CollectionFiltering.shelf(
                collections = listOf(morningCollection, carCollection),
                scope = CollectionScope.ALL,
                query = "MORNING",
                sort = CollectionSort.NAME,
            )

        assertThat(shelf.map { it.name }).containsExactly("Morning Azkar")
    }

    test("a collection query also reaches the description") {
        val shelf =
            CollectionFiltering.shelf(
                collections = listOf(morningCollection, carCollection),
                scope = CollectionScope.ALL,
                query = "drives",
                sort = CollectionSort.NAME,
            )

        // The user should not have to remember whether they wrote "for the car" in the name or in
        // the description in order to find it again.
        assertThat(shelf.map { it.name }).containsExactly("For the car")
    }

    test("surrounding whitespace in a query does not hide the shelf") {
        val shelf =
            CollectionFiltering.shelf(
                collections = listOf(morningCollection, carCollection),
                scope = CollectionScope.ALL,
                query = "   ",
                sort = CollectionSort.NAME,
            )

        assertThat(shelf).hasSize(2)
    }

    test("a query is applied before the sort, so a hidden collection cannot reorder the rest") {
        val shelf =
            CollectionFiltering.shelf(
                collections =
                    listOf(
                        customCollection(id = 20L, name = "Zzz", members = listOf(morning, evening, travel)),
                        customCollection(id = 21L, name = "Aaa"),
                    ),
                scope = CollectionScope.ALL,
                query = "zzz",
                sort = CollectionSort.NAME,
            )

        assertThat(shelf.map { it.name }).containsExactly("Zzz")
    }

    test("sorting by name ignores case") {
        val shelf =
            CollectionFiltering.shelf(
                collections =
                    listOf(
                        customCollection(id = 20L, name = "banana"),
                        customCollection(id = 21L, name = "Apple"),
                        customCollection(id = 22L, name = "cherry"),
                    ),
                scope = CollectionScope.ALL,
                query = "",
                sort = CollectionSort.NAME,
            )

        assertThat(shelf.map { it.name }).containsExactly("Apple", "banana", "cherry")
    }

    test("sorting by size puts the fullest collection first and breaks ties by name") {
        val shelf =
            CollectionFiltering.shelf(
                collections =
                    listOf(
                        customCollection(id = 20L, name = "Small", members = listOf(morning)),
                        customCollection(id = 21L, name = "Tied second", members = listOf(evening, travel)),
                        customCollection(id = 22L, name = "Fullest", members = listOf(morning, evening, travel)),
                    ),
                scope = CollectionScope.ALL,
                query = "",
                sort = CollectionSort.MOST_ITEMS,
            )

        assertThat(shelf.map { it.name }).containsExactly("Fullest", "Tied second", "Small")
    }

    test("sorting by recency prefers the collection that was opened over the one that was edited") {
        val shelf =
            CollectionFiltering.shelf(
                collections =
                    listOf(
                        customCollection(id = 20L, name = "Edited only"),
                        customCollection(id = 21L, name = "Opened", lastOpenedAt = LaterFixtureInstant),
                    ),
                scope = CollectionScope.ALL,
                query = "",
                sort = CollectionSort.RECENTLY_UPDATED,
            )

        // Opening is the stronger signal: it is the thing the user actually did, where `updated_at`
        // on a shipped collection is only the day its seed row happened to be written.
        assertThat(shelf.map { it.name }).containsExactly("Opened", "Edited only")
    }

    test("a collection that has never been touched sorts last rather than first") {
        val shelf =
            CollectionFiltering.shelf(
                collections =
                    listOf(
                        customCollection(id = 20L, name = "Untouched", updatedAt = null),
                        customCollection(id = 21L, name = "Touched", lastOpenedAt = LaterFixtureInstant),
                    ),
                scope = CollectionScope.ALL,
                query = "",
                sort = CollectionSort.RECENTLY_UPDATED,
            )

        assertThat(shelf.map { it.name }).containsExactly("Touched", "Untouched")
    }

    test("a pinned collection stays above the rest even when the sort is by name") {
        val shelf =
            CollectionFiltering.shelf(
                collections =
                    listOf(
                        customCollection(id = 20L, name = "Aaa", pinnedAt = FixtureInstant),
                        customCollection(id = 21L, name = "Bbb"),
                        customCollection(id = 22L, name = "Ccc"),
                    ),
                scope = CollectionScope.ALL,
                query = "",
                sort = CollectionSort.NAME,
            )

        // The pin has to survive the sort or it is not a pin, it is a preference that happens to
        // line up with today's alphabetical order.
        assertThat(shelf.map { it.name }).containsExactly("Aaa", "Bbb", "Ccc")
    }

    test("a pinned collection stays above the rest when sorting by size") {
        val shelf =
            CollectionFiltering.shelf(
                collections =
                    listOf(
                        customCollection(id = 20L, name = "Fullest", members = listOf(morning, evening, travel)),
                        customCollection(
                            id = 21L,
                            name = "Empty but pinned",
                            pinnedAt = FixtureInstant,
                        ),
                    ),
                scope = CollectionScope.ALL,
                query = "",
                sort = CollectionSort.MOST_ITEMS,
            )

        assertThat(shelf.map { it.name }).containsExactly("Empty but pinned", "Fullest")
    }

    test("recency falls back to the last edit when a collection has never been opened") {
        assertThat(CollectionFiltering.recencyOf(morningCollection))
            .isEqualTo(FixtureInstant.toEpochMilli())
        assertThat(CollectionFiltering.recencyOf(pinnedEveningCollection))
            .isEqualTo(LaterFixtureInstant.toEpochMilli())
    }

    test("an untouched collection reports the floor, not the current time") {
        val neverSaved =
            RemembranceCollectionImpl(
                id = 30L,
                name = "Never",
                kind = CollectionKind.CUSTOM,
            )

        assertThat(CollectionFiltering.recencyOf(neverSaved)).isEqualTo(Long.MIN_VALUE)
    }

    test("searching for nothing returns no groups rather than every collection") {
        val groups =
            CollectionFiltering.groupSearchResults(
                collections = listOf(morningCollection),
                remembrances = listOf(morning),
                query = "   ",
            )

        assertThat(groups).isEmpty()
    }

    test("a hit is reported under the collection it was found in") {
        val groups =
            CollectionFiltering.groupSearchResults(
                collections = listOf(morningCollection, carCollection),
                remembrances = listOf(morning, evening, travel),
                query = "morning",
            )

        assertThat(groups).hasSize(1)
        assertThat(groups.single().collectionId).isEqualTo(10L)
        assertThat(groups.single().collectionName).isEqualTo("Morning Azkar")
        assertThat(groups.single().results.map { it.id }).containsExactly(1L)
    }

    test("the same remembrance in two collections is two groups rather than one hit") {
        val groups =
            CollectionFiltering.groupSearchResults(
                collections =
                    listOf(
                        morningCollection,
                        customCollection(id = 20L, name = "For the car", members = listOf(morning)),
                    ),
                remembrances = listOf(morning),
                query = "good morning",
            )

        // Each hit opens that collection's page, so collapsing them would leave one of the two
        // pages unreachable from the search results.
        assertThat(groups.map { it.collectionName }).containsExactly("Morning Azkar", "For the car")
    }

    test("groups come out in the order the hub is showing the shelf") {
        val groups =
            CollectionFiltering.groupSearchResults(
                collections = listOf(eveningCollection, morningCollection),
                remembrances = listOf(morning, evening),
                query = "good",
            )

        assertThat(groups.map { it.collectionName }).containsExactly("Evening Azkar", "Morning Azkar")
    }

    test("a query reaches a translation in a locale other than the one on screen") {
        val arabic = remembrance(id = 4L, text = "Morning supplication")

        assertThat(CollectionFiltering.matchesRemembrance(arabic, "ذكر")).isTrue()
    }

    test("a query reaches the source reference") {
        assertThat(CollectionFiltering.matchesRemembrance(travel, "sunan")).isTrue()
        assertThat(CollectionFiltering.matchesRemembrance(travel, "bukhari")).isFalse()
    }

    test("a query reaches the tags") {
        val tagged = remembrance(id = 5L, tags = setOf(tag(1L, "Nature")))

        assertThat(CollectionFiltering.matchesRemembrance(tagged, "nature")).isTrue()
        assertThat(CollectionFiltering.matchesRemembrance(tagged, "travel")).isFalse()
    }

    test("an empty query matches every remembrance, so a blank field is not a filter of nothing") {
        assertThat(CollectionFiltering.matchesRemembrance(travel, "")).isTrue()
    }

    test("Uncategorized holds only untagged remembrances with no other collection membership") {
        val alone = remembrance(id = 40L)
        val favorited = remembrance(id = 41L, favorite = true)
        val favoriteWithoutMirror = remembrance(id = 44L, favorite = true)
        val filedElsewhere = remembrance(id = 42L)
        val tagged = remembrance(id = 43L, tags = setOf(tag(9L, "morning")))
        val uncategorized = customCollection(id = 30L, name = "Uncategorized", members = listOf(alone))
        val favorites = customCollection(id = 31L, name = "Favorites", members = listOf(favorited))
        val other = customCollection(id = 32L, name = "Saved", members = listOf(filedElsewhere))

        val ids =
            CollectionFiltering.uncategorizedRemembranceIds(
                remembrances = listOf(alone, favorited, favoriteWithoutMirror, filedElsewhere, tagged),
                collections = listOf(uncategorized, favorites, other),
                uncategorizedCollectionId = 30L,
            )

        assertThat(ids).containsExactly(40L)
    }

    test("a member missing from the catalogue is skipped rather than reported without text") {
        val groups =
            CollectionFiltering.groupSearchResults(
                collections = listOf(morningCollection),
                // The morning collection's member is absent, so there is no text to match against.
                remembrances = listOf(evening),
                query = "morning",
            )

        assertThat(groups).isEmpty()
    }

    test("the picker offers the catalogue minus what the collection already holds") {
        val addable =
            CollectionFiltering.addableRemembrances(
                collection = morningCollection,
                catalogue = listOf(morning, evening, travel),
            )

        assertThat(addable.map { it.id }).containsExactly(2L, 3L)
    }

    test("only the user's own collections count towards the badge") {
        val counts =
            membershipCountsOf(
                listOf(
                    officialCollection(id = 10L, name = "Morning", members = listOf(morning)),
                    customCollection(id = 20L, name = "Mine", members = listOf(morning)),
                    customCollection(id = 21L, name = "Also mine", members = listOf(morning, evening)),
                ),
            )

        // A shipped collection holds everything carrying its tag, so counting it would put a badge
        // on nearly every tile and say nothing the user chose.
        assertThat(counts.customCollectionIds).containsExactlyInAnyOrder(20L, 21L)
        assertThat(counts.collectionsIn(1L)).isEqualTo(2)
        assertThat(counts.collectionsIn(2L)).isEqualTo(1)
        assertThat(counts.collectionsIn(99L)).isZero()
    }

    test("an empty shelf counts nothing") {
        assertThat(membershipCountsOf(emptyList()).collectionsIn(1L)).isZero()
        assertThat(MembershipCounts.EMPTY.collectionsIn(1L)).isZero()
    }
}

/**
 * Searching and sorting a shelf that has been translated.
 *
 * These are the same two rules as above — the query reaches the words on the card, and the name sort
 * puts them in order — applied to a shelf whose words differ by language. They need pinning because
 * the obvious implementation reads one string, and the translations are a map beside it: a query for
 * "أذكار" would find nothing, and a name sort would order the shelf by whatever language it happened
 * to be typed in. Both look correct in English and fail silently in Arabic.
 *
 * The rule is that a translated row is reachable in *every* language, not only in its own. Someone
 * reading the English interface still searches for the Arabic word they remember off a card, and
 * someone reading the Arabic one still pastes the English name a colleague sent them.
 *
 * TestBalloon generates the test entry point, but the IDE does not see that usage.
 */
@Suppress("unused")
val TranslatedShelfSuite by testSuite("Searching and sorting a shelf that has been translated") {
    val arabic = Locale.forLanguageTag("ar")
    val english = Locale.ENGLISH

    val morningReminder = remembrance(id = 1L, text = "Whoever says good morning")
    val eveningReminder = remembrance(id = 2L, text = "Whoever says good evening")

    val morning =
        officialCollection(
            id = 10L,
            name = "Morning Azkar",
            description = "Remembrance for the start of the day",
            members = listOf(morningReminder),
            translations =
                mapOf(
                    english to LocalizedPair("Morning Azkar", "Remembrance for the start of the day"),
                    arabic to LocalizedPair("أذكار الصباح", "أذكار لبداية اليوم"),
                ),
        )
    val evening =
        officialCollection(
            id = 11L,
            name = "Evening Azkar",
            description = "Remembrance for the end of the day",
            members = listOf(eveningReminder),
            translations =
                mapOf(
                    english to LocalizedPair("Evening Azkar", "Remembrance for the end of the day"),
                    arabic to LocalizedPair("أذكار المساء", "أذكار لنهاية اليوم"),
                ),
        )

    /** The shelf as it would be shown to a reader in [locale]. */
    fun searchedIn(
        locale: Locale,
        query: String,
        among: List<RemembranceCollection> = listOf(morning, evening),
    ) = CollectionFiltering.shelf(
        collections = among,
        scope = CollectionScope.ALL,
        query = query,
        sort = CollectionSort.NAME,
        uiLocale = locale,
    )

    test("the Arabic name is found while reading in English") {
        assertThat(searchedIn(english, "أذكار الصباح").map { it.name })
            .containsExactly("Morning Azkar")
    }

    test("the English name is found while reading in Arabic") {
        assertThat(searchedIn(arabic, "morning").map { it.name })
            .containsExactly("Morning Azkar")
    }

    test("a translated sentence is reached as well as a translated name") {
        assertThat(searchedIn(english, "لبداية").map { it.name })
            .containsExactly("Morning Azkar")
    }

    test("a word shared by both rows matches both, rather than the one that happens to be first") {
        // Both Arabic names open with أذكار. A matcher that stopped at the first hit would show one
        // shelf and look like it had found the right one.
        assertThat(searchedIn(arabic, "أذكار")).hasSize(2)
    }

    test("a query still finds nothing when neither language carries it") {
        assertThat(searchedIn(arabic, "أذكار الغروب")).isEmpty()
    }

    test("sorting by name orders the shelf the way the reader will see it") {
        // The order asserted below is the order Arabic sorts in *by code point*, which is what
        // String.CASE_INSENSITIVE_ORDER gives: ا ل س < ا ل ص < ا ل م because س is U+0633, ص U+0635
        // and م U+0645. It is not dictionary order, and an Arabic reader would say so. This is a
        // pre-existing property of every string list in the app rather than something the shelf
        // introduced, so it is pinned here as what it is rather than asserted as what it should be.

        val ordered =
            CollectionFiltering.shelf(
                collections = listOf(morning, evening),
                scope = CollectionScope.ALL,
                query = "",
                sort = CollectionSort.NAME,
                uiLocale = arabic,
            )

        // أذكار المساء sorts before أذكار الصباح alphabetically, which is the opposite of the order
        // the English names give. A sort that read the stored name would silently disagree with the
        // shelf the user is looking at.
        assertThat(ordered.map { it.name }).containsExactly("Morning Azkar", "Evening Azkar")
    }

    test("a shelf of translated and untranslated collections still sorts without a tie") {
        val mine = customCollection(id = 30L, name = "أذكار الفجر")

        val ordered =
            CollectionFiltering.shelf(
                collections = listOf(morning, mine),
                scope = CollectionScope.ALL,
                query = "",
                sort = CollectionSort.NAME,
                uiLocale = arabic,
            )

        // الفجر sorts *after* الصباح by code point (ف is U+0641, ص is U+0635), so the translated row
        // leads and the untranslated one follows — on words, not on the stored slugs, which would
        // have put "morning" (م) before "أذكار الفجر" for no reason a reader of either can see.
        assertThat(ordered.map { it.name }).containsExactly("Morning Azkar", "أذكار الفجر")
    }

    test("a search group is headed by the name the reader sees, not the stored one") {
        val groups =
            CollectionFiltering.groupSearchResults(
                collections = listOf(morning),
                remembrances = listOf(morningReminder),
                query = "ذكر",
                uiLocale = arabic,
            )

        // The query reaches the member's Arabic text (ذكر 1); the heading above the hits must then be
        // in the same language, or the result reads as a hit inside a collection the user cannot read.
        assertThat(groups.single().collectionName).isEqualTo("أذكار الصباح")
    }
}

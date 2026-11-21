package com.azkar.ui.library.data

import de.infix.testBalloon.framework.core.testSuite
import org.assertj.core.api.Assertions.assertThat
import java.time.Instant

/**
 * The rule deciding what the Home screen offers to pick back up.
 *
 * A rail that quietly reshuffles is worse than no rail, so the ordering is pinned down here rather
 * than left to a reader to infer. Each test builds its own list because testBalloon runs a suite's
 * tests concurrently and nothing mutable is shared here anyway.
 *
 * TestBalloon generates the test entry point, but the IDE does not see that usage.
 */
@Suppress("unused")
val HomeCollectionsSuite by testSuite("Choosing the Home rail") {
    test("a limit of zero or less offers nothing rather than everything") {
        assertThat(HomeCollections.pick(listOf(card(1L, "Morning")), 0)).isEmpty()
        assertThat(HomeCollections.pick(listOf(card(1L, "Morning")), -3)).isEmpty()
    }

    test("a collection with no members is left off the rail") {
        assertThat(HomeCollections.pick(listOf(card(1L, "Empty", members = 0)), 5)).isEmpty()
    }

    test("a collection the user has not started is only offered once they pin it") {
        val untouched = card(1L, "Morning Azkar", pinnedAt = null, recited = 0)

        assertThat(HomeCollections.pick(listOf(untouched), 5)).isEmpty()

        val pinned = card(1L, "Morning Azkar", pinnedAt = EARLY, recited = 0)
        assertThat(HomeCollections.pick(listOf(pinned), 5)).containsExactly(pinned)
    }

    test("a partly recited collection is offered without being pinned") {
        val started = card(1L, "Morning Azkar", members = 10, recited = 4, lastRecitedAt = EARLY)

        assertThat(HomeCollections.pick(listOf(started), 5)).containsExactly(started)
    }

    test("a finished collection is not offered, because starting over is the user's call") {
        val finished = card(1L, "Evening Azkar", members = 10, recited = 10, lastRecitedAt = LATE)

        assertThat(HomeCollections.pick(listOf(finished), 5)).isEmpty()
    }

    test("pinned cards come before resumable ones even when the resumable was recited more recently") {
        val pinned = card(1L, "Pinned", pinnedAt = EARLY, recited = 1, lastRecitedAt = EARLY)
        val resumable = card(2L, "Resumable", recited = 1, lastRecitedAt = LATE)

        assertThat(HomeCollections.pick(listOf(resumable, pinned), 5))
            .containsExactly(pinned, resumable)
    }

    test("the two halves are each ordered by recency rather than by one shared key") {
        val olderPinned = card(1L, "Older pin", pinnedAt = EARLY, recited = 1, lastRecitedAt = LATE)
        val newerPinned = card(2L, "Newer pin", pinnedAt = LATE, recited = 1, lastRecitedAt = EARLY)
        val olderResume = card(3L, "Older resume", recited = 1, lastRecitedAt = EARLY)
        val newerResume = card(4L, "Newer resume", recited = 1, lastRecitedAt = LATE)

        assertThat(HomeCollections.pick(listOf(olderResume, newerPinned, newerResume, olderPinned), 4))
            .containsExactly(newerPinned, olderPinned, newerResume, olderResume)
    }

    test("cards sharing a timestamp are ordered by name so the rail does not shuffle between runs") {
        val first = card(1L, "Alpha", pinnedAt = LATE, recited = 1, lastRecitedAt = LATE)
        val second = card(2L, "Beta", pinnedAt = LATE, recited = 1, lastRecitedAt = LATE)

        assertThat(HomeCollections.pick(listOf(second, first), 2)).containsExactly(first, second)
        assertThat(HomeCollections.pick(listOf(first, second), 2)).containsExactly(first, second)
    }

    test("a pinned card is never offered twice because it is also resumable") {
        val pinnedAndStarted = card(1L, "Morning", pinnedAt = EARLY, recited = 1, lastRecitedAt = EARLY)

        assertThat(HomeCollections.pick(listOf(pinnedAndStarted), 5)).containsExactly(pinnedAndStarted)
    }

    test("the limit truncates the tail, which is the resumable half first") {
        val pinned = card(1L, "Pinned", pinnedAt = LATE, recited = 1, lastRecitedAt = EARLY)
        val first = card(2L, "First", recited = 1, lastRecitedAt = LATE)
        val second = card(3L, "Second", recited = 1, lastRecitedAt = EARLY)

        assertThat(HomeCollections.pick(listOf(second, pinned, first), 2)).containsExactly(pinned, first)
    }

    test("completion is the share of members counted, and an empty collection reads as none") {
        assertThat(card(1L, "A", members = 10, recited = 3).completion).isEqualTo(0.3f)
        assertThat(card(1L, "A", members = 0, recited = 0).completion).isEqualTo(0f)
    }

    test("completion never reads above full, however many times a passage was recited") {
        val overCounted = card(1L, "A", members = 4, recited = 40, lastRecitedAt = EARLY)

        assertThat(overCounted.completion).isEqualTo(1f)
        assertThat(overCounted.isResumable).isFalse()
    }
}

private val EARLY = Instant.parse("2026-01-15T08:00:00Z")
private val LATE = Instant.parse("2026-02-20T19:30:00Z")

/**
 * A rail card with everything spelled out, so each test states only the field it is about.
 *
 * The defaults describe the card the rail has least to say about: members nobody has recited, and
 * nobody's hand on the pin. That matters more than it looks — `pick` sorts pinned and unpinned cards
 * into different halves, so a helper that pinned by default would quietly move half these tests into
 * the wrong group and leave them passing without proving anything.
 */
private fun card(
    id: Long,
    name: String,
    members: Int = 10,
    recited: Int = 0,
    pinnedAt: Instant? = null,
    lastRecitedAt: Instant? = null,
): HomeCollection =
    HomeCollection(
        id = id,
        name = name,
        description = null,
        iconKey = "mosque",
        memberCount = members,
        recitedCount = recited,
        pinnedAt = pinnedAt,
        lastRecitedAt = lastRecitedAt,
    )

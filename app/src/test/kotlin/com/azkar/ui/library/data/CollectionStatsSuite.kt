package com.azkar.ui.library.data

import com.azkar.domain.model.RecitationProgress
import com.azkar.domain.model.Remembrance
import com.azkar.domain.model.impl.RemembranceCollectionImpl
import com.azkar.ui.library.FixtureInstant
import com.azkar.ui.library.remembrance
import de.infix.testBalloon.framework.core.testSuite
import org.assertj.core.api.Assertions.assertThat
import java.time.Instant

// TestBalloon generates the test entry point, but the IDE does not see that usage.
@Suppress("unused")
val CollectionStatsSuite by testSuite("Collection statistics") {
    val first = remembrance(1L)
    val second = remembrance(2L)
    val third = remembrance(3L)

    fun collectionOf(vararg members: Remembrance) =
        RemembranceCollectionImpl(name = "Morning Azkar", members = members.toList())

    fun progress(
        remembranceId: Long,
        count: Int,
        recitedAt: Instant? = FixtureInstant,
    ) = RecitationProgress(remembranceId, count, recitedAt)

    test("an empty collection has no members and is not yet complete") {
        val stats = statsOf(collectionOf(), emptyList())

        assertThat(stats.totalMembers).isZero()
        assertThat(stats.completedMembers).isZero()
        assertThat(stats.completion).isZero()
        assertThat(stats.hasMembers).isFalse()
        assertThat(stats.isComplete).isFalse()
    }

    test("a collection nothing has been recited in reports no completion") {
        val stats = statsOf(collectionOf(first, second), emptyList())

        assertThat(stats.totalMembers).isEqualTo(2)
        assertThat(stats.completedMembers).isZero()
        assertThat(stats.completion).isZero()
        assertThat(stats.isComplete).isFalse()
    }

    test("a member counted at least once is completed, however small the count") {
        val stats = statsOf(collectionOf(first, second), listOf(progress(1L, 1)))

        assertThat(stats.completedMembers).isEqualTo(1)
        assertThat(stats.completion).isEqualTo(0.5f)
    }

    test("every member counted means the collection is complete") {
        val stats = statsOf(collectionOf(first, second), listOf(progress(1L, 3), progress(2L, 1)))

        assertThat(stats.completedMembers).isEqualTo(2)
        assertThat(stats.completion).isEqualTo(1f)
        assertThat(stats.isComplete).isTrue()
    }

    test("repeats are summed across members") {
        val stats = statsOf(collectionOf(first, second, third), listOf(progress(1L, 7), progress(3L, 2)))

        assertThat(stats.totalRepeats).isEqualTo(9)
    }

    test("progress recorded against another collection is not counted here") {
        val stats = statsOf(collectionOf(first), listOf(progress(2L, 4)))

        assertThat(stats.completedMembers).isZero()
        assertThat(stats.totalRepeats).isEqualTo(4)
    }

    test("the remaining estimate falls as members are counted") {
        val collection = collectionOf(first, second, third)

        val fresh = statsOf(collection, emptyList())
        val halfway = statsOf(collection, listOf(progress(1L, 1)))
        val done = statsOf(collection, listOf(progress(1L, 1), progress(2L, 1), progress(3L, 1)))

        assertThat(fresh.remainingRepeats).isEqualTo(3)
        assertThat(halfway.remainingRepeats).isEqualTo(2)
        assertThat(done.remainingRepeats).isZero()
        assertThat(done.estimatedRemaining.isZero).isTrue()
    }

    test("the estimate grows with what is left") {
        val stats = statsOf(collectionOf(first, second), emptyList())

        assertThat(stats.estimatedRemaining.seconds).isEqualTo(2 * REMEMBRANCE_SECONDS)
    }

    test("the next member is the first still to be recited") {
        val collection = collectionOf(first, second, third)

        assertThat(nextMember(collection, emptyList())?.id).isEqualTo(1L)
        assertThat(nextMember(collection, listOf(progress(1L, 1)))?.id).isEqualTo(2L)
        assertThat(nextMember(collection, listOf(progress(1L, 1), progress(2L, 1)))?.id).isEqualTo(3L)
    }

    test("a finished collection has no next member") {
        val collection = collectionOf(first)

        assertThat(nextMember(collection, listOf(progress(1L, 1)))).isNull()
    }

    test("an empty collection has no next member either") {
        assertThat(nextMember(collectionOf(), emptyList())).isNull()
    }

    test("the upcoming list follows the shelf's own order") {
        val collection = collectionOf(first, second, third)

        assertThat(upcomingMembers(collection, listOf(progress(1L, 1)), 10).map { it.id })
            .containsExactly(2L, 3L)
    }

    test("the upcoming list is capped at what was asked for") {
        val collection = collectionOf(first, second, third)

        assertThat(upcomingMembers(collection, emptyList(), 2).map { it.id }).containsExactly(1L, 2L)
    }

    test("asking for no upcoming members returns none") {
        assertThat(upcomingMembers(collectionOf(first, second), emptyList(), 0)).isEmpty()
    }

    test("a member with no id cannot be recorded, so it is never offered as next") {
        val collection = collectionOf(remembrance(id = null), first)

        assertThat(nextMember(collection, emptyList())?.id).isEqualTo(1L)
        assertThat(statsOf(collection, emptyList()).completedMembers).isZero()
        assertThat(upcomingMembers(collection, emptyList(), 10).map { it.id }).containsExactly(1L)
    }

    test("a progress row for a member that has since left the collection does not inflate the count") {
        val collection = collectionOf(first)

        val stats = statsOf(collection, listOf(progress(1L, 1), progress(2L, 50)))

        assertThat(stats.totalMembers).isEqualTo(1)
        assertThat(stats.completedMembers).isEqualTo(1)
        assertThat(stats.totalRepeats).isEqualTo(51)
    }
}

/** The per-remembrance seconds the estimate is built from, mirrored so the arithmetic is checkable. */
private const val REMEMBRANCE_SECONDS = 20L

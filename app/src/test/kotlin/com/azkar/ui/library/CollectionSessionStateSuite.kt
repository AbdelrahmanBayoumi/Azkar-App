package com.azkar.ui.library

import com.azkar.TestDispatchers
import com.azkar.domain.model.RecitationProgress
import com.azkar.ui.shell.NotificationPriority
import de.infix.testBalloon.framework.core.testSuite
import kotlinx.coroutines.CoroutineScope
import org.assertj.core.api.Assertions.assertThat
import java.util.Locale

/**
 * The session screen's rules, exercised against the fake gateway.
 *
 * The rules worth pinning down here are all about a position that is never stored: which passage is on
 * screen after a tap, what "skip" means at the end of a shelf, and that the counts roll back when the
 * write fails. A stored cursor would pass most of these by construction, which is exactly why they are
 * worth writing down.
 *
 * TestBalloon generates the test entry point, but the IDE does not see that usage.
 */
@Suppress("unused")
val CollectionSessionStateSuite by testSuite("The recitation session") {
    test("a session starts on the first member that has not been recited") {
        val session = sessionWith(members = listOf(1L, 2L, 3L), recited = setOf(2L))

        assertThat(session.current?.id).isEqualTo(1L)
        assertThat(session.position).isEqualTo(1)
        assertThat(session.isFinished).isFalse()
    }

    test("completing a passage advances to the next uncounted one") {
        val session = sessionWith(members = listOf(1L, 2L, 3L))

        session.complete()

        assertThat(session.current?.id).isEqualTo(2L)
        assertThat(session.position).isEqualTo(2)
    }

    test("completing counts only the passage on screen") {
        val gateway = gatewayWith(members = listOf(1L, 2L))
        val session = sessionWith(gateway = gateway, members = listOf(1L, 2L))

        session.complete()

        assertThat(gateway.recitationWrites).containsExactly(COLLECTION_ID to mapOf(1L to 1))
    }

    test("reciting again stays on the same passage and counts it twice over time") {
        val gateway = gatewayWith(members = listOf(1L, 2L))
        val session = sessionWith(gateway = gateway, members = listOf(1L, 2L))

        session.reciteAgain()
        session.reciteAgain()

        assertThat(session.current?.id).isEqualTo(1L)
        assertThat(gateway.recitationWrites)
            .containsExactly(COLLECTION_ID to mapOf(1L to 1), COLLECTION_ID to mapOf(1L to 1))
    }

    test("reciting again does not take the pin off, so the passage stays reachable") {
        val gateway = gatewayWith(members = listOf(1L, 2L))
        val session = sessionWith(gateway = gateway, members = listOf(1L, 2L))
        session.skip()

        assertThat(session.isPinned).isTrue()
        session.reciteAgain()

        assertThat(session.isPinned).isTrue()
        assertThat(session.current?.id).isEqualTo(2L)
    }

    test("resuming gives up the pin and returns to where the collection says is next") {
        val gateway = gatewayWith(members = listOf(1L, 2L, 3L))
        val session = sessionWith(gateway = gateway, members = listOf(1L, 2L, 3L))
        session.skip()

        session.resume()

        assertThat(session.isPinned).isFalse()
        assertThat(session.current?.id).isEqualTo(1L)
    }

    test("skipping moves to the next uncounted passage rather than the next in the shelf") {
        val gateway = gatewayWith(members = listOf(1L, 2L, 3L), recited = setOf(2L))
        val session = sessionWith(gateway = gateway, members = listOf(1L, 2L, 3L), recited = setOf(2L))

        session.skip()

        assertThat(session.current?.id).isEqualTo(3L)
    }

    test("skipping at the end of the shelf keeps the passage rather than wrapping to the start") {
        val gateway = gatewayWith(members = listOf(1L, 2L), recited = setOf(1L))
        val session = sessionWith(gateway = gateway, members = listOf(1L, 2L), recited = setOf(1L))
        assertThat(session.current?.id).isEqualTo(2L)

        session.skip()

        assertThat(session.current?.id).isEqualTo(2L)
        assertThat(gateway.recitationWrites).isEmpty()
    }

    test("a session over a fully recited collection reports itself finished") {
        val session =
            sessionWith(
                members = listOf(1L, 2L),
                recited = setOf(1L, 2L),
            )

        assertThat(session.isFinished).isTrue()
        assertThat(session.current).isNull()
        assertThat(session.position).isZero()
    }

    test("a collection with no members is finished rather than finished-and-empty twice over") {
        val session = sessionWith(members = emptyList())

        assertThat(session.hasMembers).isFalse()
        assertThat(session.isFinished).isFalse()
        assertThat(session.current).isNull()
    }

    test("a collection that no longer exists shows nothing and finishes") {
        val harness = harnessWith(members = listOf(1L))

        // Deleted from the shelf while the session was open, which is exactly what happens when the
        // same collection is removed on a screen underneath this one. The delete goes through the
        // catalogue rather than straight into the fake because that is the path the UI takes, and it
        // is the catalogue's quiet re-read that tells this session the collection has gone.
        harness.collections.deleteCollection(COLLECTION_ID)

        assertThat(harness.session.current).isNull()
        assertThat(harness.session.isFinished).isTrue()
    }

    test("the position counts the shelf's own order, not the order things were recited in") {
        val session =
            sessionWith(
                members = listOf(1L, 2L, 3L),
                recited = setOf(1L, 2L),
            )

        assertThat(session.position).isEqualTo(3)
        assertThat(session.totalMembers).isEqualTo(3)
    }

    test("upcoming lists what is ahead and leaves out the passage on screen") {
        val session = sessionWith(members = listOf(1L, 2L, 3L, 4L))

        assertThat(session.upcoming.map { it.id }).containsExactly(2L, 3L, 4L)
    }

    test("upcoming leaves out the pinned passage rather than the one at the head of the shelf") {
        val session = sessionWith(members = listOf(1L, 2L, 3L, 4L))
        session.skip()

        assertThat(session.current?.id).isEqualTo(2L)
        assertThat(session.upcoming.map { it.id }).doesNotContain(2L)
    }

    test("restarting clears every count so the session begins again") {
        val gateway = gatewayWith(members = listOf(1L, 2L), recited = setOf(1L))
        val session = sessionWith(gateway = gateway, members = listOf(1L, 2L), recited = setOf(1L))

        session.restart()

        assertThat(session.current?.id).isEqualTo(1L)
        assertThat(session.stats.totalRepeats).isZero()
    }

    test("a failed count rolls back and raises a failure notice") {
        val gateway = gatewayWith(members = listOf(1L, 2L))
        val harness = harnessWith(members = listOf(1L, 2L), gateway = gateway)
        val session = harness.session
        val collections = harness.collections
        gateway.failure = IllegalStateException("no")

        session.complete()

        assertThat(session.stats.totalRepeats).isZero()
        assertThat((collections.onNotice as? CollectionNotice.Message)?.key)
            .isEqualTo(CollectionNoticeKey.FAILED)
    }

    test("a completed session stays finished rather than falling back to the first member") {
        val gateway = gatewayWith(members = listOf(1L))
        val session = sessionWith(gateway = gateway, members = listOf(1L))

        session.complete()

        assertThat(session.isFinished).isTrue()
        assertThat(session.current).isNull()
    }

    test("the session names the passage the reader would open, and asks for it by id") {
        val session = sessionWith(members = listOf(1L, 2L))

        assertThat(session.readId()).isEqualTo(1L)
    }

    test("a finished session names no passage, because there is nothing left to read") {
        val session = sessionWith(members = listOf(1L))

        session.complete()

        assertThat(session.readId()).isNull()
    }

    test("the notice a failure raises is the high priority one, so it is not missed") {
        val gateway = gatewayWith(members = listOf(1L))
        val session = sessionWith(gateway = gateway, members = listOf(1L))
        gateway.failure = IllegalStateException("no")

        session.complete()

        assertThat(CollectionNoticeKey.FAILED.priority).isEqualTo(NotificationPriority.HIGH)
    }

    test("the first passage cannot be stepped back from, and the control says so") {
        val session = sessionWith(members = listOf(1L, 2L))

        assertThat(session.canStepBack).isFalse()

        session.previous()

        // Asking anyway is not an error, it is a no-op: the button is disabled rather than hidden, and
        // a keyboard shortcut or a stale press has to be safe to arrive at the first passage.
        assertThat(session.current?.id).isEqualTo(1L)
        assertThat(session.position).isEqualTo(1)
    }

    test("stepping back walks the shelf backwards and pins the passage it lands on") {
        val session = sessionWith(members = listOf(1L, 2L, 3L), recited = setOf(1L))

        assertThat(session.canStepBack).isTrue()

        session.previous()

        assertThat(session.current?.id).isEqualTo(1L)
        assertThat(session.position).isEqualTo(1)
        // Pinned, and the pin is what makes the stay reversible: a passage that has been counted is by
        // definition not the one progress would choose next, so without it the session would slide off
        // the passage the user just went back to.
        assertThat(session.isPinned).isTrue()
    }

    test("stepping back twice walks back twice and never past the first passage") {
        val session = sessionWith(members = listOf(1L, 2L, 3L), recited = setOf(1L, 2L))

        assertThat(session.position).isEqualTo(3)

        session.previous()
        session.previous()

        assertThat(session.current?.id).isEqualTo(1L)
        assertThat(session.canStepBack).isFalse()
    }

    test("stepping back does not take a count back off the passage") {
        val gateway = gatewayWith(members = listOf(1L, 2L), recited = setOf(1L))
        val session = sessionWith(gateway = gateway, members = listOf(1L, 2L), recited = setOf(1L))

        session.previous()

        // The count is a fact about what the user recited, and going back to look at a passage again is
        // not a retraction of it. Recording one here would be the same accident "Recite again" is careful
        // to avoid.
        assertThat(gateway.recitationWrites).isEmpty()
    }

    test("starring the collection from the session stars it on the shelf") {
        val harness = harnessWith(members = listOf(1L, 2L))

        assertThat(harness.session.collection?.isFavorite).isFalse()

        harness.session.setFavorite(true)

        assertThat(harness.session.collection?.isFavorite).isTrue()
        // Read through the shelf as well as the session, because a flag written on one screen and not
        // the other is the exact disagreement this class exists to prevent.
        assertThat(
            harness.collections.allCollections
                .first()
                .isFavorite,
        ).isTrue()
    }
}

private const val COLLECTION_ID = 10L

private fun gatewayWith(
    members: List<Long>,
    recited: Set<Long> = emptySet(),
): FakeCollectionsGateway {
    val remembrances = members.map { remembrance(it) }
    val collection =
        customCollection(
            id = COLLECTION_ID,
            name = "Morning Azkar",
            members = remembrances,
        )
    return FakeCollectionsGateway(
        collections = listOf(collection),
        remembrances = remembrances,
        progress =
            mapOf(
                COLLECTION_ID to recited.map { recitedAt(it) },
            ),
    )
}

private fun recitedAt(remembranceId: Long): RecitationProgress =
    RecitationProgress(remembranceId = remembranceId, repeatCount = 1, lastRecitedAt = FixtureInstant)

/**
 * The session and its shelf state, returned together so tests can change the world underneath it.
 */
private class SessionHarness(
    val session: CollectionSessionState,
    val collections: CollectionsCatalogue,
)

private fun sessionWith(
    members: List<Long>,
    recited: Set<Long> = emptySet(),
    gateway: FakeCollectionsGateway = gatewayWith(members, recited),
): CollectionSessionState = harnessWith(members, recited, gateway).session

private fun harnessWith(
    members: List<Long>,
    recited: Set<Long> = emptySet(),
    gateway: FakeCollectionsGateway = gatewayWith(members, recited),
): SessionHarness {
    val collections =
        CollectionsCatalogue(
            scope = CoroutineScope(TestDispatchers.unconfined),
            uiLocale = Locale.ENGLISH,
            gateway = gateway,
            covers = fakeCovers(),
        )
    collections.loadData()
    // The session carries the schedule store because its detail view does; nothing in this suite reads
    // a reminder, so the same gateway is handed over and no schedule is seeded.
    val session =
        CollectionSessionState(
            collections = collections,
            collectionId = COLLECTION_ID,
            reminders = CollectionReminderState(FakeReminderSources()),
        )
    session.loadProgress()
    return SessionHarness(session = session, collections = collections)
}

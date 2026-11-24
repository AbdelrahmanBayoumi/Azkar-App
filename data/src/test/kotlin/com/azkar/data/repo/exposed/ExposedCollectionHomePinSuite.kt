package com.azkar.data.repo.exposed

import com.azkar.data.seed.CollectionSeed
import com.azkar.data.table.CollectionTable
import com.azkar.domain.exception.CollectionNotFoundException
import com.azkar.domain.model.RemembranceCollection
import com.azkar.domain.model.impl.RemembranceCollectionImpl
import com.azkar.testUtils.ExposedRepositories
import com.azkar.testUtils.seededCollections
import de.infix.testBalloon.framework.core.testSuite
import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.update
import java.time.OffsetDateTime

/**
 * What putting a collection on the Home screen does, and what it must not do.
 *
 * The rail is the one place in the app where a shipped collection and a user's own are treated
 * identically, so the rule that pinning is allowed on either kind deserves a test of its own rather
 * than being folded into the collection repository's suite. The three things worth pinning down:
 *
 *  - a shipped collection can be pinned, because being on Home says nothing about what is in it;
 *  - the stamp orders the rail, most recently pinned first, because that is what "the ones I added
 *    most recently" means;
 *  - pinning does not move `updated_at`, because a user who has not edited a collection should not
 *    see it jump to the top of the shelf's recently-updated ordering.
 *
 * And one thing worth pinning down that lives in the seed: re-seeding must not quietly undo a pin.
 *
 * TestBalloon generates the test entry point, but the IDE does not see that usage.
 */
@Suppress("unused")
val ExposedCollectionHomePinSuite by testSuite("ExposedCollectionRepository: the Home rail") {
    seededCollections {
        testSuite("pinning a collection to Home") {
            test("a newly created collection is not on the Home screen") { repositories ->
                val collectionId = newCollection(repositories)

                assertThat(homeStamp(repositories, collectionId)).isNull()
                assertThat(repositories.collections.findById(collectionId)?.isOnHome).isFalse()
            }

            test("pinning stamps the collection and the read reports it") { repositories ->
                val collectionId = newCollection(repositories)

                repositories.collections.setHomePinned(collectionId, pinned = true)

                assertThat(homeStamp(repositories, collectionId)).isNotNull()
                assertThat(repositories.collections.findById(collectionId)?.isOnHome).isTrue()
            }

            test("taking it off clears the stamp rather than leaving a stale one") { repositories ->
                val collectionId = newCollection(repositories)
                repositories.collections.setHomePinned(collectionId, pinned = true)

                repositories.collections.setHomePinned(collectionId, pinned = false)

                assertThat(homeStamp(repositories, collectionId)).isNull()
                assertThat(repositories.collections.findById(collectionId)?.isOnHome).isFalse()
            }

            test("pinning a shipped collection is allowed, because the rail treats both kinds alike") { repositories ->
                val morningId = morningCollectionId(repositories)

                repositories.collections.setHomePinned(morningId, pinned = true)

                assertThat(repositories.collections.findById(morningId)?.isOnHome).isTrue()
                assertThat(repositories.collections.findHomePinned().map { it.name })
                    .containsExactly("Morning Azkar")
            }

            test("the rail returns most recently pinned first") { repositories ->
                val morningId = morningCollectionId(repositories)
                val eveningId = eveningCollectionId(repositories)
                val customId = newCollection(repositories)

                // Written raw with three explicitly different stamps, because three calls into
                // `now()` a few microseconds apart would order by luck rather than by the column.
                stampAs(repositories, morningId, "2026-01-01T08:00:00Z")
                stampAs(repositories, eveningId, "2026-02-01T08:00:00Z")
                stampAs(repositories, customId, "2026-03-01T08:00:00Z")

                assertThat(repositories.collections.findHomePinned().map { it.name })
                    .containsExactly("Morning copy", "Evening Azkar", "Morning Azkar")
            }

            test("the rail leaves out everything that was never pinned") { repositories ->
                val customId = newCollection(repositories)
                repositories.collections.setHomePinned(customId, pinned = true)

                assertThat(repositories.collections.findAll().size).isGreaterThan(1)
                assertThat(repositories.collections.findHomePinned().map { it.id }).containsExactly(customId)
            }

            test("pinning does not move updated_at, so the shelf order does not shift under it") { repositories ->
                val collectionId = newCollection(repositories)
                val before = repositories.collections.findById(collectionId)?.updatedAt

                repositories.collections.setHomePinned(collectionId, pinned = true)

                assertThat(repositories.collections.findById(collectionId)?.updatedAt).isEqualTo(before)
            }

            test("a collection that is not there fails rather than quietly reporting success") { repositories ->
                val missingId = 9_999L

                val failure = failureOf { repositories.collections.setHomePinned(missingId, pinned = true) }

                assertThat(failure)
                    .isInstanceOfSatisfying(CollectionNotFoundException::class.java) { it.collectionId }
                    .hasMessageContaining("9999")
            }
        }

        testSuite("re-seeding") {
            test("a pin the user set survives a re-seed") { repositories ->
                val morningId = morningCollectionId(repositories)
                repositories.collections.setHomePinned(morningId, pinned = true)
                val stamped = homeStamp(repositories, morningId)

                reseed(repositories)

                assertThat(homeStamp(repositories, morningId)).isEqualTo(stamped)
            }

            test("a collection still off the rail is not put on it by a re-seed") { repositories ->
                val morningId = morningCollectionId(repositories)

                reseed(repositories)

                assertThat(homeStamp(repositories, morningId)).isNull()
                assertThat(repositories.collections.findHomePinned()).isEmpty()
            }
        }
    }
}

/** Creates a custom collection and returns its generated id, failing the test if there is none. */
private suspend fun newCollection(repositories: ExposedRepositories): Long =
    requireNotNull(repositories.collections.save(RemembranceCollectionImpl(name = "Morning copy")).id)

/**
 * The failure [block] raised, or `null` when it did not raise one.
 *
 * AssertJ's `assertThatThrownBy` takes a non-suspending lambda, so it cannot be handed a repository
 * call directly; `runCatching` is the suspending equivalent.
 */
private suspend inline fun failureOf(crossinline block: suspend () -> Unit): Throwable? =
    runCatching { block() }.exceptionOrNull()

/**
 * The shipped collections are found by name now rather than by a catalogue key: the seed writes
 * them once and the user may rename either, so a name is the only handle a test can rely on when
 * it is also the thing being asserted about.
 */
private suspend fun morningCollectionId(repositories: ExposedRepositories): Long =
    repositories.collections
        .findAll()
        .single { it.name == "Morning Azkar" }
        .requireId()

private suspend fun eveningCollectionId(repositories: ExposedRepositories): Long =
    repositories.collections
        .findAll()
        .single { it.name == "Evening Azkar" }
        .requireId()

private fun RemembranceCollection.requireId(): Long = requireNotNull(id)

/** The stored stamp, read raw so the column itself is asserted rather than the derived flag. */
private fun homeStamp(
    repositories: ExposedRepositories,
    collectionId: Long,
): OffsetDateTime? =
    transaction(repositories.database) {
        CollectionTable
            .selectAll()
            .where { CollectionTable.id eq collectionId }
            .single()[CollectionTable.homePinnedAt]
    }

private fun reseed(repositories: ExposedRepositories) {
    transaction(repositories.database) { CollectionSeed.load() }
}

/**
 * Writes a chosen stamp over [collectionId], so an ordering assertion tests the column's ordering
 * rather than how close together three calls into `now()` happened to land.
 */
private fun stampAs(
    repositories: ExposedRepositories,
    collectionId: Long,
    instant: String,
) {
    transaction(repositories.database) {
        CollectionTable.update({ CollectionTable.id eq collectionId }) {
            it[CollectionTable.homePinnedAt] = OffsetDateTime.parse(instant)
        }
    }
}

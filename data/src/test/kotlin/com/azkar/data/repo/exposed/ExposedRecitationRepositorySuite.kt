package com.azkar.data.repo.exposed

import com.azkar.data.table.CollectionMemberTable
import com.azkar.data.table.RecitationProgressTable
import com.azkar.domain.model.impl.RemembranceCollectionImpl
import com.azkar.testUtils.ExposedRepositories
import com.azkar.testUtils.seededRemembrances
import de.infix.testBalloon.framework.core.testSuite
import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

// TestBalloon generates the test entry point, but the IDE does not see that usage.
@Suppress("unused")
val ExposedRecitationRepositorySuite by testSuite("ExposedRecitationRepository") {
    seededRemembrances {
        testSuite("recording progress") {
            test("a member with no row yet gets one on its first recitation") { repositories ->
                val collectionId = newCollection(repositories, "Morning Azkar")

                assertThat(repositories.recitations.findByCollection(collectionId)).isEmpty()
                val changed = repositories.recitations.bump(collectionId, mapOf(1L to 1))

                assertThat(changed).isEqualTo(1)
                val stored = repositories.recitations.findByCollection(collectionId).single()
                assertThat(stored.remembranceId).isEqualTo(1L)
                assertThat(stored.repeatCount).isEqualTo(1)
                assertThat(stored.lastRecitedAt).isNotNull()
            }

            test("a later recitation adds to the stored count rather than replacing it") { repositories ->
                val collectionId = newCollection(repositories, "Morning Azkar")

                repositories.recitations.bump(collectionId, mapOf(1L to 3))
                repositories.recitations.bump(collectionId, mapOf(1L to 4))

                assertThat(countOf(repositories, collectionId, 1L)).isEqualTo(7)
            }

            test("a delta of zero changes nothing and stores nothing") { repositories ->
                val collectionId = newCollection(repositories, "Morning Azkar")

                val changed = repositories.recitations.bump(collectionId, mapOf(1L to 0))

                assertThat(changed).isZero()
                assertThat(repositories.recitations.findByCollection(collectionId)).isEmpty()
            }

            test("a delta that would go below zero is clamped instead of stored negative") { repositories ->
                val collectionId = newCollection(repositories, "Morning Azkar")
                repositories.recitations.bump(collectionId, mapOf(1L to 2))

                val changed = repositories.recitations.bump(collectionId, mapOf(1L to -5))

                assertThat(changed).isEqualTo(1)
                assertThat(countOf(repositories, collectionId, 1L)).isZero()
            }

            test("a delta that cancels a positive one out still counts as a change") { repositories ->
                val collectionId = newCollection(repositories, "Morning Azkar")
                repositories.recitations.bump(collectionId, mapOf(1L to 2))

                assertThat(repositories.recitations.bump(collectionId, mapOf(1L to -2))).isEqualTo(1)
                assertThat(countOf(repositories, collectionId, 1L)).isZero()
            }

            test("one call may count several members at once") { repositories ->
                val collectionId = newCollection(repositories, "Morning Azkar")

                val changed = repositories.recitations.bump(collectionId, mapOf(1L to 1, 2L to 1, 3L to 5))

                assertThat(changed).isEqualTo(3)
                assertThat(
                    repositories.recitations.findByCollection(collectionId).map { it.remembranceId to it.repeatCount },
                ).containsExactlyInAnyOrder(1L to 1, 2L to 1, 3L to 5)
            }

            test("an empty batch is accepted and changes nothing") { repositories ->
                val collectionId = newCollection(repositories, "Morning Azkar")

                assertThat(repositories.recitations.bump(collectionId, emptyMap())).isZero()
                assertThat(repositories.recitations.findByCollection(collectionId)).isEmpty()
            }

            test("the same passage counts separately in two collections") { repositories ->
                val first = newCollection(repositories, "Morning Azkar")
                val second = newCollection(repositories, "Evening Azkar")

                repositories.recitations.bump(first, mapOf(1L to 3))
                repositories.recitations.bump(second, mapOf(1L to 1))

                assertThat(countOf(repositories, first, 1L)).isEqualTo(3)
                assertThat(countOf(repositories, second, 1L)).isEqualTo(1)
            }

            test("progress is found from the remembrance side across every collection") { repositories ->
                val first = newCollection(repositories, "Morning Azkar")
                val second = newCollection(repositories, "Evening Azkar")
                val unrelated = newCollection(repositories, "Unrelated")
                repositories.recitations.bump(first, mapOf(1L to 2))
                repositories.recitations.bump(second, mapOf(1L to 5))
                repositories.recitations.bump(unrelated, mapOf(2L to 1))

                assertThat(repositories.recitations.findByRemembrance(1L).map { it.repeatCount })
                    .containsExactlyInAnyOrder(2, 5)
                assertThat(repositories.recitations.findByRemembrance(99L)).isEmpty()
            }

            test("a collection's read returns its own rows and nobody else's") { repositories ->
                val first = newCollection(repositories, "Morning Azkar")
                val second = newCollection(repositories, "Evening Azkar")
                repositories.recitations.bump(first, mapOf(1L to 1))
                repositories.recitations.bump(second, mapOf(2L to 1))

                assertThat(repositories.recitations.findByCollection(first).map { it.remembranceId })
                    .containsExactly(1L)
                assertThat(repositories.recitations.findByCollection(second).map { it.remembranceId })
                    .containsExactly(2L)
            }
        }

        testSuite("setting and clearing") {
            test("setting a count for a member with no row creates the row") { repositories ->
                val collectionId = newCollection(repositories, "Morning Azkar")

                repositories.recitations.setCount(collectionId, 1L, 42)

                val stored = repositories.recitations.findByCollection(collectionId).single()
                assertThat(stored.remembranceId).isEqualTo(1L)
                assertThat(stored.repeatCount).isEqualTo(42)
            }

            test("setting a count twice replaces rather than adds") { repositories ->
                val collectionId = newCollection(repositories, "Morning Azkar")
                repositories.recitations.bump(collectionId, mapOf(1L to 9))

                repositories.recitations.setCount(collectionId, 1L, 2)

                assertThat(countOf(repositories, collectionId, 1L)).isEqualTo(2)
            }

            test("a negative count is clamped on the way in") { repositories ->
                val collectionId = newCollection(repositories, "Morning Azkar")

                repositories.recitations.setCount(collectionId, 1L, -3)

                assertThat(countOf(repositories, collectionId, 1L)).isZero()
            }

            test("resetting drops every row of that collection and none of another's") { repositories ->
                val first = newCollection(repositories, "Morning Azkar")
                val second = newCollection(repositories, "Evening Azkar")
                repositories.recitations.bump(first, mapOf(1L to 1, 2L to 1))
                repositories.recitations.bump(second, mapOf(1L to 1))

                assertThat(repositories.recitations.reset(first)).isEqualTo(2)

                assertThat(repositories.recitations.findByCollection(first)).isEmpty()
                assertThat(repositories.recitations.findByCollection(second)).hasSize(1)
            }

            test("resetting a collection that was never recited reports nothing dropped") { repositories ->
                val collectionId = newCollection(repositories, "Morning Azkar")

                assertThat(repositories.recitations.reset(collectionId)).isZero()
            }
        }

        testSuite("one figure per collection") {
            test("a collection with recited members reports how many are done") { repositories ->
                val collectionId = newCollection(repositories, "Morning Azkar")
                repositories.recitations.bump(collectionId, mapOf(1L to 1, 2L to 1))

                val summary = repositories.recitations.findSummaries(listOf(collectionId))

                assertThat(summary).containsOnlyKeys(collectionId)
                assertThat(summary.getValue(collectionId).recitedCount).isEqualTo(2)
                assertThat(summary.getValue(collectionId).lastRecitedAt).isNotNull()
            }

            test("a collection with nothing recited is absent rather than present with a zero") { repositories ->
                val collectionId = newCollection(repositories, "Morning Azkar")

                assertThat(repositories.recitations.findSummaries(listOf(collectionId))).isEmpty()
            }

            test("a member whose count has been undone is not counted as recited") { repositories ->
                val collectionId = newCollection(repositories, "Morning Azkar")
                repositories.recitations.bump(collectionId, mapOf(1L to 1, 2L to 1))
                repositories.recitations.bump(collectionId, mapOf(1L to -1))

                // The row survives holding a zero, so counting rows would report two of two done on a
                // collection with one member still to recite — and the shelf draws this number next to
                // every collection's size.
                assertThat(countOf(repositories, collectionId, 1L)).isZero()
                val summary = repositories.recitations.findSummaries(listOf(collectionId))
                assertThat(summary.getValue(collectionId).recitedCount).isEqualTo(1)
            }

            test("asking about nothing asks nothing") { repositories ->
                assertThat(repositories.recitations.findSummaries(emptyList())).isEmpty()
            }
        }

        testSuite("progress follows what it counts") {
            test("deleting a collection takes its progress with it") { repositories ->
                val collectionId = newCollection(repositories, "Morning Azkar")
                repositories.recitations.bump(collectionId, mapOf(1L to 3))

                repositories.collections.deleteById(collectionId)

                assertThat(progressRowCount(repositories)).isZero()
            }

            test("deleting a remembrance takes the rows that referred to it with it") { repositories ->
                val collectionId = newCollection(repositories, "Morning Azkar")
                repositories.recitations.bump(collectionId, mapOf(1L to 3))
                repositories.recitations.bump(collectionId, mapOf(2L to 1))

                transaction(repositories.database) {
                    CollectionMemberTable.deleteWhere { CollectionMemberTable.remembranceId eq 1L }
                    exec("DELETE FROM remembrance WHERE id = 1")
                }

                assertThat(progressRowCount(repositories)).isEqualTo(1)
                assertThat(countOf(repositories, collectionId, 2L)).isEqualTo(1)
            }

            test("a row against a collection that does not exist is refused rather than stored") { repositories ->
                val collectionId = newCollection(repositories, "Morning Azkar")
                transaction(repositories.database) { exec("DELETE FROM collection WHERE id = $collectionId") }

                val failure = failureOf { repositories.recitations.bump(collectionId, mapOf(1L to 1)) }

                assertThat(failure).isNotNull()
                assertThat(progressRowCount(repositories)).isZero()
            }
        }
    }
}

private suspend fun newCollection(
    repositories: ExposedRepositories,
    name: String,
): Long = requireNotNull(repositories.collections.save(RemembranceCollectionImpl(name = name)).id)

private suspend fun countOf(
    repositories: ExposedRepositories,
    collectionId: Long,
    remembranceId: Long,
): Int =
    repositories.recitations
        .findByCollection(collectionId)
        .firstOrNull { it.remembranceId == remembranceId }
        ?.repeatCount ?: 0

private fun progressRowCount(repositories: ExposedRepositories): Int =
    transaction(repositories.database) { RecitationProgressTable.selectAll().count().toInt() }

/**
 * AssertJ's `assertThatThrownBy` takes a non-suspending lambda, so a repository call cannot be
 * handed to it directly; the failure is captured here and inspected as a value instead.
 */
private suspend inline fun failureOf(crossinline block: suspend () -> Unit): Throwable? =
    runCatching { block() }.exceptionOrNull()

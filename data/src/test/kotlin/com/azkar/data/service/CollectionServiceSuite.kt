package com.azkar.data.service

import com.azkar.data.repo.exposed.ExposedCollectionRepository
import com.azkar.data.repo.exposed.ExposedTagRepository
import com.azkar.data.table.CollectionTagTable
import com.azkar.data.transaction.ExposedTransactionRunner
import com.azkar.domain.model.impl.TagImpl
import com.azkar.domain.service.CollectionService
import com.azkar.testUtils.TestDatabase
import de.infix.testBalloon.framework.core.testSuite
import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

/**
 * Creating a collection is the one write in the app that arrives as a single object carrying its
 * labels inline, and it is also the one write that goes out in two statements:
 * [ExposedCollectionRepository.save] writes the `collection` row and only
 * [ExposedCollectionRepository.setTags] writes the `collection_tag` rows behind it. That split is
 * invisible from the call site and therefore easy to break — it was broken, and a collection could
 * be created with labels that then went nowhere. These tests hold the seam shut: a collection
 * created with labels has to come back out of the database carrying those same labels, read back
 * through the same reader the shelf uses.
 *
 * The suite talks to a real H2 database rather than a repository double because the bug is a
 * SQL-shaped one. A double would have to restate `insert`'s column list to catch it, and a double
 * that drifts is the bug rather than the cure: `insert` grows a column, the double does not, and
 * the double keeps agreeing.
 *
 * TestBalloon generates the test entry point, but the IDE does not see that usage.
 */
@Suppress("unused")
val CollectionServiceSuite by testSuite("Collection service creation") {
    test("a collection created with labels comes back carrying them") {
        val service = serviceOver(emptyDatabase())

        val created =
            service.create(
                name = "Morning Azkar",
                tags = setOf(TagImpl(name = "morning"), TagImpl(name = "sleep")),
            )

        assertThat(created.tags.map { it.name }).containsExactly("morning", "sleep")
    }

    test("creating with labels writes one row per label") {
        val database = emptyDatabase()
        val service = serviceOver(database)

        val created = service.create(name = "Morning Azkar", tags = setOf(TagImpl(name = "morning")))

        transaction(database) {
            val rows =
                CollectionTagTable
                    .selectAll()
                    .filter { it[CollectionTagTable.collectionId].value == created.id }
            assertThat(rows).hasSize(1)
        }
    }

    test("a label written twice under different case is one label") {
        val service = serviceOver(emptyDatabase())

        val created =
            service.create(
                name = "Morning Azkar",
                tags = setOf(TagImpl(name = "Morning"), TagImpl(name = "morning")),
            )

        assertThat(created.tags.map { it.name }).containsExactly("morning")
    }

    test("a collection created without labels comes back with none rather than failing") {
        val service = serviceOver(emptyDatabase())

        assertThat(service.create(name = "Morning Azkar").tags).isEmpty()
    }

    test("labels chosen at creation survive a read through the repository the shelf uses") {
        val database = emptyDatabase()
        val service = serviceOver(database)
        val repository = repositoryOver(database)

        val created = service.create(name = "Morning Azkar", tags = setOf(TagImpl(name = "morning")))
        val reread = repository.findById(checkNotNull(created.id))

        assertThat(reread?.tags?.map { it.name }).containsExactly("morning")
    }

    test("labels chosen at creation are visible on the collection list the shelf renders") {
        val service = serviceOver(emptyDatabase())

        service.create(name = "Morning Azkar", tags = setOf(TagImpl(name = "morning")))

        assertThat(
            service
                .findAll()
                .single()
                .tags
                .map { it.name },
        ).containsExactly("morning")
    }
}

/**
 * A migrated database with every row cleared.
 *
 * [TestDatabase.bootstrapWithH2] loads the shipped dataset, which would leave a "the created
 * collection is the only one" assertion reading the seed shelf instead of this suite's own work.
 */
private fun emptyDatabase(): Database = TestDatabase.bootstrapWithH2().also(TestDatabase::reset)

/** The service under test over the same repository the shelf is wired to. */
private fun serviceOver(database: Database): CollectionService =
    CollectionService(repositoryOver(database), ExposedTransactionRunner(database))

private fun repositoryOver(database: Database): ExposedCollectionRepository {
    val transactions = ExposedTransactionRunner(database)
    return ExposedCollectionRepository(transactions, ExposedTagRepository(transactions))
}

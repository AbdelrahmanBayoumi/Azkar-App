package com.azkar.domain.service

import com.azkar.domain.exception.TagNameTakenException
import com.azkar.domain.model.Tag
import com.azkar.domain.model.TagSummary
import com.azkar.domain.model.TagUsage
import com.azkar.domain.model.impl.TagImpl
import com.azkar.domain.repo.TagRepository
import de.infix.testBalloon.framework.core.testSuite
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat

/**
 * The two rules the tag service owns that the database only enforces.
 *
 * Everything else in this service is a pass-through, and pass-throughs earn their tests in the
 * adapter that does the real work — `ExposedTagRepositorySuite` in the data module covers
 * the SQL. What is left is the part that has to hold no matter which database answers: two tags
 * cannot share a name, and the check has to happen *before* the write.
 *
 * A unique index catches the violation eventually, but only as a driver constraint exception, after
 * the transaction has already rolled back and carrying nothing that names the tag which collided.
 * Naming it is the difference between a dialog that can say which name is wrong and one that can
 * only report that the save failed.
 *
 * The fake below stands in for the database, so these tests are about ordering and nothing else. It
 * keeps just enough of a table to answer the lookups the service makes.
 *
 * TestBalloon generates the test entry point, but the IDE does not see that usage.
 */
@Suppress("unused")
val TagServiceSuite by testSuite("Tag names") {
    testSuite("create") {
        test("saves a tag the name is free for") {
            runTest {
                val repository = FakeTagRepository()
                val service = TagService(repository, PassthroughTransactions)

                val created = service.create("  Morning ")

                // Folding happens before the lookup, so the caller cannot smuggle a duplicate past
                // the check by way of the capitalisation and padding TagImpl normalises anyway.
                assertThat(created.name).isEqualTo("morning")
                assertThat(repository.saved.map { it.name }).containsExactly("morning")
            }
        }

        test("refuses a name a live tag already holds") {
            runTest {
                val repository = FakeTagRepository(existing = TagImpl(id = 1L, name = "morning"))
                val service = TagService(repository, PassthroughTransactions)

                assertThat(failureFrom { service.create("Morning") })
                    .isInstanceOf(TagNameTakenException::class.java)
                    .hasMessageContaining("morning")
                assertThat(repository.saved).describedAs("A refused name must not reach the database").isEmpty()
            }
        }
    }

    testSuite("rename") {
        test("saves the new name under the same id") {
            runTest {
                val repository = FakeTagRepository(existing = TagImpl(id = 1L, name = "morning"))
                val service = TagService(repository, PassthroughTransactions)

                service.rename(id = 1L, newName = "Dawn")

                assertThat(repository.saved.map { it.id }).containsExactly(1L)
                assertThat(repository.saved.map { it.name }).containsExactly("dawn")
            }
        }

        test("refuses a name another tag holds") {
            runTest {
                val repository =
                    FakeTagRepository(
                        existing = TagImpl(id = 1L, name = "morning"),
                        clash = TagImpl(id = 2L, name = "dawn"),
                    )
                val service = TagService(repository, PassthroughTransactions)

                assertThat(failureFrom { service.rename(id = 1L, newName = "Dawn") })
                    .isInstanceOf(TagNameTakenException::class.java)
                    .hasMessageContaining("dawn")
                assertThat(repository.saved).describedAs("A refused rename must not reach the database").isEmpty()
            }
        }

        test("allows a tag to keep the name it already has") {
            runTest {
                // Renaming to the name you were already called is what the dialog does when someone
                // opens it, reads it, and saves without typing. Refusing that would make the commonest
                // possible edit a failure, so it has to read as a clash with itself and be let past.
                val repository = FakeTagRepository(existing = TagImpl(id = 1L, name = "morning"))
                val service = TagService(repository, PassthroughTransactions)

                service.rename(id = 1L, newName = "Morning")

                assertThat(repository.saved).hasSize(1)
            }
        }

        test("refuses a blank name") {
            runTest {
                val repository = FakeTagRepository(existing = TagImpl(id = 1L, name = "morning"))
                val service = TagService(repository, PassthroughTransactions)

                // Caught by the model rather than by a guard of the service's own, which is where it
                // belongs: the one place deciding what a tag name may be is the one that builds it.
                assertThat(failureFrom { service.rename(id = 1L, newName = "   ") })
                    .isInstanceOf(IllegalArgumentException::class.java)
                assertThat(repository.saved).isEmpty()
            }
        }
    }

    testSuite("reads") {
        test("findAll() passes straight through, without opening a transaction") {
            runTest {
                val tags = listOf(TagImpl(id = 1L, name = "morning"))
                val repository = FakeTagRepository(all = tags)

                val found = TagService(repository, PassthroughTransactions).findAll()

                assertThat(found).isEqualTo(tags)
                assertThat(repository.transactionCount)
                    .describedAs("A read is not a write and has no reason to open a transaction")
                    .isZero()
            }
        }
    }
}

/**
 * The service's transactions, without any.
 *
 * The service checks a name and writes it inside one `run`, and these tests are about what happens
 * in that block rather than about the block itself. A runner that simply calls through keeps the
 * ordering assertions honest without standing up a database; the repository suite covers the case
 * where the transaction is real and a second caller would see the uncommitted write.
 */
private object PassthroughTransactions : TransactionRunner {
    override suspend fun <T> run(block: suspend () -> T): T = block()
}

/**
 * Runs [block] and hands back what it threw, or null when it threw nothing.
 *
 * AssertJ's `assertThatThrownBy` wants a plain lambda, so it cannot hold a suspend call, and a test
 * is only allowed to open one `runTest`, so wrapping the call in a second one is not an option
 * either. This is the remaining door: it suspends like the call it wraps, and it returns the
 * throwable rather than rethrowing it so the assertions read the way they otherwise would.
 */
private suspend fun failureFrom(block: suspend () -> Unit): Throwable? =
    try {
        block()
        null
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (thrown: Exception) {
        thrown
    }

/**
 * Enough of a tag table for the service to ask its questions against.
 *
 * [existing] answers the by-id lookups and [clash] the by-name one. Splitting them is what lets a
 * test say "this name is taken by *someone else*", a case [existing] alone cannot express: one row
 * cannot be both the tag being renamed and the tag that blocks it.
 *
 * [saved] records writes without checking them, so that "the database never saw it" is an assertion
 * a test can actually make about a refused rename.
 */
private class FakeTagRepository(
    private val existing: Tag? = null,
    private val clash: Tag? = null,
    private val all: List<Tag> = emptyList(),
) : TagRepository {
    val saved: MutableList<Tag> = mutableListOf()

    /** How many times a transaction was opened, so a read-only call is distinguishable from a write. */
    var transactionCount: Int = 0
        private set

    override suspend fun softDelete(id: Long?) = Unit

    override suspend fun restore(id: Long?) = Unit

    override suspend fun save(tag: Tag): Tag {
        transactionCount++
        saved += tag
        return tag
    }

    override suspend fun findById(id: Long?): Tag? = existing?.takeIf { it.id == id }

    override suspend fun findByNameIgnoreCase(name: String): Tag? =
        listOfNotNull(existing, clash).firstOrNull { it.name == name }

    /**
     * A translation match, answered from the same two rows the name match uses.
     *
     * Deliberately not a separate field: the suite asks "is this word already spoken for" rather than
     * "is this word a stored name", and a fake that could answer those two differently would let a
     * test pass against a repository that really cannot.
     */
    override suspend fun findByTranslationIgnoreCase(text: String): Tag? =
        listOfNotNull(existing, clash).firstOrNull { tag ->
            tag.translations.values.any { it == text }
        }

    override suspend fun findAll(): List<Tag> = all

    override suspend fun findByNameContainingIgnoreCase(fragment: String): List<Tag> =
        all.filter { it.name.contains(fragment, ignoreCase = true) }

    override suspend fun findSummaries(): List<TagSummary> = emptyList()

    override suspend fun findDeletedSummaries(): List<TagSummary> = emptyList()

    override suspend fun findUsage(id: Long?): TagUsage? = null
}

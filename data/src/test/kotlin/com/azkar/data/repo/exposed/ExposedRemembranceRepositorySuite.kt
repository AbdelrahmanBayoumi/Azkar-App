package com.azkar.data.repo.exposed

import com.azkar.domain.exception.RemembranceNotFoundException
import com.azkar.testUtils.seededRemembrances
import de.infix.testBalloon.framework.core.testSuite
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.util.Locale

/**
 * The query surface of the remembrance repository, exercised against a fixed three-row fixture
 * so every assertion can name the ids it expects instead of counting rows.
 *
 * TestBalloon generates the test entry point, but the IDE does not see that usage.
 */
@Suppress("unused")
val ExposedRemembranceRepositorySuite by testSuite(
    "ExposedRemembranceRepository – repository behavior against an in-memory H2 DB",
) {
    seededRemembrances {
        test("findAll(): returns all rows ordered by ID ascending") { repositories ->
            val all = repositories.remembrances.findAll()

            assertThat(all).describedAs("Expected exactly 3 remembrances in seed data").hasSize(3)
            assertThat(all.map { it.id })
                .describedAs("Remembrances should be ordered by ID ascending")
                .containsExactly(1L, 2L, 3L)
        }

        test("findFirst(): returns the lowest ID, and null when the library is empty") { repositories ->
            assertThat(repositories.remembrances.findFirst()?.id)
                .describedAs("findFirst should read one row and take the lowest ID, as findAll().firstOrNull() did")
                .isEqualTo(1L)

            // "One row" is the whole point of the method, so the assertion that matters is that
            // the aggregate it returns is the same one findAll would have produced for that id —
            // a truncated row would render a daily hadith with no text in it.
            assertThat(repositories.remembrances.findFirst())
                .describedAs("The first row should be fully aggregated, not a truncated projection")
                .isEqualTo(repositories.remembrances.findById(1L))

            repositories.remembrances.deleteById(1L)
            repositories.remembrances.deleteById(2L)
            repositories.remembrances.deleteById(3L)

            assertThat(repositories.remembrances.findFirst())
                .describedAs("An empty library has no first row, and should say so rather than fail")
                .isNull()
        }

        test("findById(): found for an existing ID, null for a missing ID") { repositories ->
            assertThat(repositories.remembrances.findById(2L))
                .describedAs("ID 2 exists in seed data and should be found")
                .isNotNull()
            assertThat(repositories.remembrances.findById(999L))
                .describedAs("Non-existent ID 999 should not be found")
                .isNull()
        }

        test(
            "save() followed by delete(): can re-save an existing row and delete it by entity and by ID",
        ) { repositories ->
            insertRemembranceNine(repositories.database)

            // Load via repo and re-save (the update path)
            val loaded = requireNotNull(repositories.remembrances.findById(9L))
            val saved = repositories.remembrances.save(loaded)

            assertThat(saved.id).describedAs("Saved entity should retain the ID=9").isEqualTo(9L)

            repositories.remembrances.delete(saved)
            assertThat(repositories.remembrances.findById(9L))
                .describedAs("Entity with ID=9 should be deleted")
                .isNull()

            // Deleting a row that is already gone must not throw nor recreate it.
            repositories.remembrances.deleteById(9L)
            assertThat(repositories.remembrances.findById(9L))
                .describedAs("deleteById on non-existing ID should have no effect and not recreate the row")
                .isNull()
        }

        test("findByTagNameIgnoreCase(): case-insensitive tag lookup returns correct IDs") { repositories ->
            val morning = repositories.remembrances.findByTagNameIgnoreCase("morning")
            assertThat(morning.map { it.id })
                .describedAs("Tag 'morning' should map to remembrances 1 and 3")
                .containsExactlyInAnyOrder(1L, 3L)

            val evening = repositories.remembrances.findByTagNameIgnoreCase("evening")
            assertThat(evening.map { it.id })
                .describedAs("Tag 'evening' should map to remembrance 2 only")
                .containsExactly(2L)
        }

        test("findFavorites(): returns favorites ordered by remembrance ID") { repositories ->
            assertThat(repositories.remembrances.findFavorites().map { it.id })
                .describedAs("Seed favorites are IDs 1 and 3, ordered by ID")
                .containsExactly(1L, 3L)
        }

        test("search(): uses LIKE matching and respects the requested Locale") { repositories ->
            val results = repositories.remembrances.search(Locale.forLanguageTag("ar"), "سبحان")

            assertThat(results.map { it.id })
                .describedAs("Arabic query should return remembrance with ID=1 only")
                .containsExactly(1L)
        }

        test("search(): blank or whitespace queries return an empty list") { repositories ->
            assertThat(repositories.remembrances.search(Locale.ENGLISH, "  "))
                .describedAs("Whitespace-only query must return empty results")
                .isEmpty()
            assertThat(repositories.remembrances.search(Locale.forLanguageTag("ar"), ""))
                .describedAs("Empty query must return empty results")
                .isEmpty()
        }

        testSuite("markFavorite() / unmarkFavorite()") {
            test("toggles favorites and preserves ordering") { repositories ->
                assertThat(repositories.remembrances.findFavorites().map { it.id })
                    .describedAs("Initial favorites should be [1, 3]")
                    .containsExactly(1L, 3L)

                repositories.remembrances.markFavorite(2L)
                assertThat(repositories.remembrances.findFavorites().map { it.id })
                    .describedAs("After marking ID=2, favorites should be [1, 2, 3]")
                    .containsExactly(1L, 2L, 3L)

                repositories.remembrances.unmarkFavorite(1L)
                assertThat(repositories.remembrances.findFavorites().map { it.id })
                    .describedAs("After unmarking ID=1, favorites should be [2, 3]")
                    .containsExactly(2L, 3L)
            }

            test("unknown IDs are rejected") { repositories ->
                // AssertJ's `ThrowingCallable` is not a suspend lambda, so execute the suspend call
                // inside the coroutine test harness and let the exception propagate to the assertion.
                assertThatThrownBy { runTest { repositories.remembrances.markFavorite(404L) } }
                    .isInstanceOf(RemembranceNotFoundException::class.java)
                assertThatThrownBy { runTest { repositories.remembrances.unmarkFavorite(404L) } }
                    .isInstanceOf(RemembranceNotFoundException::class.java)
            }
        }
    }
}

/** A row the fixture deliberately leaves out, so the update and delete paths have one to work on. */
private fun insertRemembranceNine(database: Database) {
    transaction(database) {
        exec("INSERT INTO remembrance (id, source, grade) VALUES (9,'Abu Daud','SAHIH')")
        exec("INSERT INTO remembrance_translation (remembrance_id, locale_code, text) VALUES (9,'ar','ذكر 9')")
        exec("INSERT INTO explanation_translation (remembrance_id, locale_code, text) VALUES (9,'ar','شرح 9')")
    }
}

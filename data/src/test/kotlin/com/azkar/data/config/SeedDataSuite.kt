package com.azkar.data.config

import com.azkar.data.seed.SeedDataLoader
import com.azkar.data.table.ExplanationTranslationTable
import com.azkar.data.table.RemembranceTable
import com.azkar.data.table.RemembranceTranslationTable
import com.azkar.data.table.TagTable
import com.azkar.testUtils.TestDatabase
import de.infix.testBalloon.framework.core.testSuite
import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.util.Locale

/**
 * The bundled dataset is loaded by a resource file rather than by a migration, so nothing else in
 * the suite would notice if it stopped loading. The counts here are deliberately coarse: the
 * point is that the rows landed at all, not that the dataset matches a snapshot.
 *
 * TestBalloon generates the test entry point, but the IDE does not see that usage.
 */
@Suppress("unused")
val SeedDataSuite by testSuite("Seed data") {
    // Migrated and seeded once for the whole suite and deliberately never reset — the seeded rows
    // are precisely what is under test, so every test in here reads the same database. The dataset
    // is loaded by the bootstrapper rather than by the migration, so a suite that wants it has to
    // ask for it the way the app does.
    val migrated =
        testFixture {
            TestDatabase.bootstrapWithH2().also { database ->
                transaction(database) { SeedDataLoader.load() }
            }
        }

    migrated asParameterForAll {
        test("seeds at least 250 remembrances, each with Arabic and English text") { database ->
            transaction(database) {
                assertThat(RemembranceTable.selectAll().count())
                    .describedAs("Total seeded remembrances")
                    .isGreaterThanOrEqualTo(250)
            }
            SEEDED_LOCALES.forEach { locale ->
                assertThat(countTranslations(database, locale))
                    .describedAs("Remembrances seeded with a $locale translation")
                    .isGreaterThanOrEqualTo(250)
                assertThat(countExplanations(database, locale))
                    .describedAs("Remembrances seeded with a $locale explanation")
                    .isGreaterThanOrEqualTo(250)
            }
        }

        test("stores citations as sources, occasion context as explanations, and no publisher tag") { database ->
            transaction(database) {
                assertThat(TagTable.selectAll().where { TagTable.name eq "muslim data" }.count()).isZero()
                val first = RemembranceTable.selectAll().first()
                assertThat(first[RemembranceTable.sourceText]).contains("Al-Bukhari")
                val explanation =
                    ExplanationTranslationTable
                        .selectAll()
                        .where { ExplanationTranslationTable.localeCode eq "en" }
                        .first()
                assertThat(explanation[ExplanationTranslationTable.text]).isEqualTo("When waking up")
                assertThat(RemembranceTable.selectAll().count()).isEqualTo(287)
            }
        }
    }

    // Deliberately outside the shared fixture: the fixture has already loaded the dataset by the
    // time a test starts, and this wants to watch the very call that does it.
    test("a first start reports what it loaded, and a second start loads nothing") {
        val database = TestDatabase.bootstrapWithH2()
        val counts = transaction(database) { SeedDataLoader.load() }

        // The dataset is a resource file, and a resource file can silently stop parsing. The
        // loader says what it inserted, so the question is whether that matches what landed —
        // asking the work is what makes the two impossible to disagree.
        assertThat(counts.remembrances)
            .describedAs("Remembrances the loader says it seeded")
            .isEqualTo(transaction(database) { RemembranceTable.selectAll().count().toInt() })
        assertThat(counts.tags)
            .describedAs("Tags the loader says it seeded")
            .isEqualTo(transaction(database) { TagTable.selectAll().count().toInt() })

        // A second start finds the table already populated and does nothing at all, so it has
        // nothing to report. A loader that answered with the size of the file rather than the
        // size of its work would say otherwise.
        val second = transaction(database) { SeedDataLoader.load() }
        assertThat(second.remembrances).describedAs("Remembrances seeded by the second start").isZero()
        assertThat(second.tags).describedAs("Tags seeded by the second start").isZero()
    }
}

/** The two locales the seed is expected to cover. */
private val SEEDED_LOCALES = listOf("ar", "en")

private fun countTranslations(
    database: Database,
    locale: String,
): Long =
    transaction(database) {
        RemembranceTranslationTable
            .selectAll()
            .where { RemembranceTranslationTable.localeCode eq locale.seedLanguageTag() }
            .count()
    }

private fun countExplanations(
    database: Database,
    locale: String,
): Long =
    transaction(database) {
        ExplanationTranslationTable
            .selectAll()
            .where { ExplanationTranslationTable.localeCode eq locale.seedLanguageTag() }
            .count()
    }

/** The seed stores locales as language tags, so `"ar"` has to become `"ar"`-as-a-tag. */
private fun String.seedLanguageTag(): String = Locale.forLanguageTag(this).toLanguageTag()

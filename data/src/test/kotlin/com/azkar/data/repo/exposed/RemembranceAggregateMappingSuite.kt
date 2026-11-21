package com.azkar.data.repo.exposed

import com.azkar.domain.model.HadithGrade
import com.azkar.domain.model.Remembrance
import com.azkar.domain.model.Tag
import com.azkar.domain.model.impl.RemembranceImpl
import com.azkar.domain.model.impl.TagImpl
import com.azkar.testUtils.exposedRepositories
import de.infix.testBalloon.framework.core.testSuite
import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.util.Locale

/**
 * Round-trips a whole remembrance aggregate through the Exposed tables.
 *
 * A remembrance is not one row but five: the core record, a translation and optional explanation per
 * locale, tag links, and an optional favorite. These tests walk that graph out to the database
 * and back to prove the reassembly holds — in particular that translations survive without their
 * optional explanations, while orphaned explanations are discarded.
 *
 * TestBalloon generates the test entry point, but the IDE does not see that usage.
 */
@Suppress("unused")
val RemembranceAggregateMappingSuite by testSuite("Remembrance aggregate mapping") {
    exposedRepositories {
        test("save() then findById(): the whole aggregate survives the round trip") { repositories ->
            val saved =
                repositories.remembrances.save(
                    remembrance(
                        source = "Abu Huraira",
                        favorite = true,
                        tags = setOf(tag("GOLD"), tag("SILVER")),
                    ),
                )

            assertThat(saved.id).isNotNull()
            assertThat(saved.source).isEqualTo("Abu Huraira")
            assertThat(saved.grade).isEqualTo(HadithGrade.SAHIH)
            assertThat(saved.isFavorite).isTrue()
            assertThat(saved.tags.map { it.name }).containsExactlyInAnyOrder("gold", "silver")
            assertEnglish(saved, "Seek knowledge")
        }

        test("save(): a tag already used by another remembrance is reused, not duplicated") { repositories ->
            // Spelled differently on the way in, which the fold makes irrelevant: both find the
            // one stored row rather than racing to create a second.
            repositories.remembrances.save(remembrance(source = "First", tags = setOf(tag("SHARED"))))
            repositories.remembrances.save(remembrance(source = "Second", tags = setOf(tag("shared"))))

            assertThat(repositories.tags.findAll().map { it.name }).containsExactly("shared")
            assertThat(repositories.remembrances.findByTagNameIgnoreCase("Shared")).hasSize(2)
        }

        test("save(): a translation without an explanation survives the round trip") { repositories ->
            val created =
                RemembranceImpl(
                    grade = HadithGrade.SAHIH,
                    translations =
                        mapOf(
                            ENGLISH to Remembrance.Translations(pair(""), pair("Seek knowledge")),
                            ARABIC to Remembrance.Translations(pair(""), pair("اطلبوا العلم")),
                        ),
                )

            val saved = repositories.remembrances.save(created)
            val english =
                requireNotNull(repositories.remembrances.findById(requireNotNull(saved.id)))
                    .translations
                    .getValue(ENGLISH)

            assertThat(english.translationPair.text).isEqualTo("Seek knowledge")
            assertThat(english.explanationPair.text).isEmpty()
            assertThat(english.explanationPair.id).isNull()
        }

        test("save(): re-saving with new text updates the row in place instead of adding one") { repositories ->
            val saved = repositories.remembrances.save(remembrance(source = "Abu Huraira"))
            val id = requireNotNull(saved.id)

            val updated =
                rebuild(
                    saved,
                    translations = mapOf(ENGLISH to Remembrance.Translations(pair(EXPLANATION), pair("Updated text"))),
                )
            repositories.remembrances.save(updated)

            assertEnglish(requireNotNull(repositories.remembrances.findById(id)), "Updated text")
        }

        test("save(): removing a locale from the aggregate deletes its rows") { repositories ->
            val saved = repositories.remembrances.save(remembrance(source = "Abu Huraira", extraLocale = true))
            val id = requireNotNull(saved.id)
            assertThat(saved.translations).hasSize(2)

            val withoutArabic = rebuild(saved, translations = mapOf(ENGLISH to ENGLISH_TRANSLATIONS))
            repositories.remembrances.save(withoutArabic)

            assertThat(requireNotNull(repositories.remembrances.findById(id)).translations)
                .containsOnlyKeys(ENGLISH)
        }

        test("save(): unmarking a favorite clears the flag and leaves the favourites rail") { repositories ->
            val saved = repositories.remembrances.save(remembrance(source = "Abu Huraira", favorite = true))
            val id = requireNotNull(saved.id)

            repositories.remembrances.save(rebuild(saved, favorite = false))

            assertThat(requireNotNull(repositories.remembrances.findById(id)).isFavorite).isFalse()
            assertThat(repositories.remembrances.findFavorites().map { it.id }).doesNotContain(id)
        }

        test("findById(): a missing optional explanation leaves its translation intact") { repositories ->
            val saved = repositories.remembrances.save(remembrance(source = "Abu Huraira", extraLocale = true))
            val id = requireNotNull(saved.id)

            // `save()` always writes a translation and an explanation together, so the two tables
            // can only drift apart behind its back — which is what a hand-edited or partially
            // migrated database looks like, and the case the mapper's report exists for.
            transaction(repositories.database) {
                exec("DELETE FROM explanation_translation WHERE remembrance_id = $id AND locale_code = 'ar'")
            }

            val dropped = mutableListOf<Pair<Long, List<String>>>()
            val own = repositories.reportingDropsTo { droppedId, locales -> dropped += droppedId to locales }
            val reloaded = requireNotNull(own.remembrances.findById(id))

            assertThat(reloaded.translations).containsOnlyKeys(ENGLISH, ARABIC)
            assertThat(
                reloaded.translations
                    .getValue(ARABIC)
                    .translationPair.text,
            ).isEqualTo("ذكر")
            assertThat(
                reloaded.translations
                    .getValue(ARABIC)
                    .explanationPair.text,
            ).isEmpty()
            assertThat(dropped)
                .describedAs("Nothing was discarded, so nothing was reported")
                .isEmpty()
        }

        test("findById(): an explanation without a translation is dropped and reported") { repositories ->
            val saved = repositories.remembrances.save(remembrance(source = "Abu Huraira"))
            val id = requireNotNull(saved.id)
            transaction(repositories.database) {
                exec(
                    "INSERT INTO explanation_translation (remembrance_id, locale_code, text) " +
                        "VALUES ($id,'fr','Explication')",
                )
            }

            val dropped = mutableListOf<Pair<Long, List<String>>>()
            val own = repositories.reportingDropsTo { droppedId, locales -> dropped += droppedId to locales }
            val reloaded = requireNotNull(own.remembrances.findById(id))

            assertThat(reloaded.translations).containsOnlyKeys(ENGLISH)
            assertThat(dropped)
                .describedAs("The report names the remembrance and the orphaned locale")
                .containsExactly(id to listOf("fr"))
        }
    }
}

// ------------------------------------------------------------------ fixtures

private fun remembrance(
    source: String,
    favorite: Boolean = false,
    tags: Set<Tag> = emptySet(),
    extraLocale: Boolean = false,
): Remembrance {
    val translations = mutableMapOf(ENGLISH to ENGLISH_TRANSLATIONS)
    if (extraLocale) {
        translations[ARABIC] = Remembrance.Translations(pair("شرح"), pair("ذكر"))
    }
    return RemembranceImpl(
        id = null,
        grade = HadithGrade.SAHIH,
        isFavorite = favorite,
        source = source,
        translations = translations,
        tags = tags,
    )
}

/** The same aggregate with a few fields swapped, standing in for what a caller would send back. */
private fun rebuild(
    remembrance: Remembrance,
    translations: Map<Locale, Remembrance.Translations> = remembrance.translations,
    favorite: Boolean = remembrance.isFavorite,
): Remembrance =
    RemembranceImpl(
        id = remembrance.id,
        grade = remembrance.grade,
        isFavorite = favorite,
        source = remembrance.source,
        createdAt = remembrance.createdAt,
        updatedAt = remembrance.updatedAt,
        translations = translations,
        tags = remembrance.tags,
    )

private fun tag(name: String): Tag = TagImpl(name = name)

private fun pair(text: String) = Remembrance.Translations.Pair(null, text)

private fun assertEnglish(
    remembrance: Remembrance,
    translation: String,
) {
    val resolved = requireNotNull(remembrance.translationsFor(ENGLISH))
    assertThat(resolved.translationPair.text).isEqualTo(translation)
    assertThat(resolved.explanationPair.text).isEqualTo(EXPLANATION)
}

private val ENGLISH: Locale = Locale.ENGLISH
private val ARABIC: Locale = Locale.forLanguageTag("ar")
private const val EXPLANATION = "An explanation of the hadith"
private val ENGLISH_TRANSLATIONS =
    Remembrance.Translations(
        Remembrance.Translations.Pair(null, EXPLANATION),
        Remembrance.Translations.Pair(null, "Seek knowledge"),
    )

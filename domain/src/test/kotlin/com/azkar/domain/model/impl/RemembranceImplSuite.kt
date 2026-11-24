package com.azkar.domain.model.impl

import com.azkar.domain.model.HadithGrade
import com.azkar.domain.model.Remembrance
import com.azkar.domain.model.Tag
import de.infix.testBalloon.framework.core.testSuite
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import java.time.Instant
import java.util.Locale

/**
 * The value object the whole domain is built on, so these tests are really about the
 * normalisation and the defensive copying its constructor promises — everything downstream
 * assumes a `RemembranceImpl` is already trimmed, already copied, and already validated.
 *
 * TestBalloon generates the test entry point, but the IDE does not see that usage.
 */
@Suppress("unused")
val RemembranceImplSuite by testSuite("A remembrance built through RemembranceImpl") {
    test("defaults every optional field when nothing is supplied") {
        val remembrance = RemembranceImpl()

        assertThat(remembrance.id).isNull()
        assertThat(remembrance.grade).isEqualTo(HadithGrade.UNSPECIFIED)
        assertThat(remembrance.isFavorite).isFalse()
        assertThat(remembrance.source).isNull()
        assertThat(remembrance.createdAt).isNull()
        assertThat(remembrance.updatedAt).isNull()
        assertThat(remembrance.translations).isEmpty()
        assertThat(remembrance.tags).isEmpty()
    }

    test("trims a source and keeps the trimmed form") {
        val remembrance = RemembranceImpl(source = "  Book of Dua  ")

        assertThat(remembrance.source).isEqualTo("Book of Dua")
    }

    test("treats a blank source as no source at all") {
        val remembrance = RemembranceImpl(source = "   ")

        assertThat(remembrance.source).isNull()
    }

    test("considers a differently cased source equal to the trimmed one") {
        val left = RemembranceImpl(source = "  Book of Dua  ")
        val right = RemembranceImpl(source = "Book of Dua")

        assertThat(left).isEqualTo(right)
    }

    testSuite("timestamps") {
        test("accepts an updatedAt that equals createdAt") {
            val instant = Instant.parse("2024-01-01T00:00:00Z")

            val remembrance = RemembranceImpl(createdAt = instant, updatedAt = instant)

            assertThat(remembrance.updatedAt).isEqualTo(instant)
        }

        test("rejects an updatedAt that is earlier than createdAt") {
            val created = Instant.parse("2024-01-02T00:00:00Z")
            val updated = Instant.parse("2024-01-01T00:00:00Z")

            assertThatThrownBy { RemembranceImpl(createdAt = created, updatedAt = updated) }
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessage("updatedAt cannot be before createdAt")
        }

        test("accepts an updatedAt when createdAt is unknown, since there is nothing to compare against") {
            val updated = Instant.parse("2024-01-01T00:00:00Z")

            assertThat(RemembranceImpl(createdAt = null, updatedAt = updated).updatedAt).isEqualTo(updated)
        }
    }

    testSuite("defensive copying") {
        test("copies the tag set so a later mutation of the caller's set cannot reach it") {
            val tags = mutableSetOf<Tag>(TagImpl(name = "morning"))
            val remembrance = RemembranceImpl(tags = tags)

            tags.add(TagImpl(name = "evening"))

            assertThat(remembrance.tags.map { it.name }).containsExactly("morning")
        }

        test("copies the translation map so a later mutation cannot reach it") {
            val translations = mutableMapOf(Locale.ENGLISH to translations("A dua"))
            val remembrance = RemembranceImpl(translations = translations)

            translations[Locale.FRENCH] = translations("Une dua")

            assertThat(remembrance.translations.keys).containsExactly(Locale.ENGLISH)
        }
    }

    test("resolves translations by locale and reports null for an unknown one") {
        val english = translations("A dua")
        val remembrance = RemembranceImpl(translations = mapOf(Locale.ENGLISH to english))

        assertThat(remembrance.translationsFor(Locale.ENGLISH)).isEqualTo(english)
        assertThat(remembrance.translationsFor(Locale.forLanguageTag("ar"))).isNull()
    }
}

private fun translations(text: String) =
    Remembrance.Translations(
        explanationPair = Remembrance.Translations.Pair(id = null, text = "Because $text"),
        translationPair = Remembrance.Translations.Pair(id = null, text = text),
    )

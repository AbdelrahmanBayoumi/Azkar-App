package com.azkar.ui.library.data

import com.azkar.domain.model.LocalizedPair
import com.azkar.ui.library.customCollection
import com.azkar.ui.library.officialCollection
import com.azkar.ui.library.remembrance
import com.azkar.ui.library.tag
import de.infix.testBalloon.framework.core.testSuite
import org.assertj.core.api.Assertions.assertThat
import java.util.Locale

/**
 * The icon that stands for a collection's labels, without a presenter instance.
 *
 * Ordering is for stability, not quality: nothing knows which of a user's labels is most
 * characteristic, only that the leader must not move underneath them as labels are added.
 *
 * TestBalloon generates the test entry point, but the IDE does not see that usage.
 */
@Suppress("unused")
val CollectionPresenterSuite by testSuite("The mark stands for the first label") {

    test("a collection with no labels falls back to the default mark") {
        assertThat(CollectionPresenter.iconKeyForTagNames(emptyList()))
            .isEqualTo(CollectionPresenter.DEFAULT_ICON_KEY)
    }

    test("a label the taxonomy does not know is drawn with the fallback mark") {
        assertThat(CollectionPresenter.iconKeyForTagNames(listOf("not a known category")))
            .isEqualTo(CollectionPresenter.CATEGORY_ICON_FALLBACK)
    }

    test("the first label alphabetically leads, however it is capitalised") {
        assertThat(CollectionPresenter.iconKeyForTagNames(listOf("prayer", "Morning")))
            .isEqualTo("sunrise")
    }

    test("the label names are matched case-insensitively against the taxonomy") {
        assertThat(CollectionPresenter.iconKeyForTagNames(listOf("MUSLIM DATA")))
            .isEqualTo("book")
    }
}

/**
 * The words on a card, read in the reader's language.
 *
 * A separate suite from the icon one above because they answer different questions. The icon tests
 * pin a rule about *which label leads*, which is a property of the taxonomy and holds whatever
 * language the app is in. These pin a rule about *what a card is called*, which is a property of the
 * translations and changes with them — so the assertions here are about two languages looking at one
 * shelf and disagreeing about its words.
 *
 * Every case is reachable without a resource environment: [CollectionPresenter.toTile] and
 * [CollectionPresenter.categories] resolve through the domain models rather than through string
 * resources, which is the whole point of resolving at the edge. A presenter that reached for
 * [org.jetbrains.compose.resources.getString] would not be testable here, so these two are also the
 * test that keeps it that way.
 *
 * TestBalloon generates the test entry point, but the IDE does not see that usage.
 */
@Suppress("unused")
val CollectionPresenterWordingSuite by testSuite("A card is called what its translations call it") {
    val english = Locale.ENGLISH
    val arabic = Locale.forLanguageTag("ar")

    /** A shipped shelf carrying both languages, as the seed writes it. */
    fun shelf(locale: Locale) =
        officialCollection(
            id = 1L,
            name = "Morning Azkar",
            description = "Remembrance for the start of the day",
            translations =
                mapOf(
                    english to LocalizedPair("Morning Azkar", "Remembrance for the start of the day"),
                    arabic to LocalizedPair("أذكار الصباح", "أذكار لبداية اليوم"),
                ),
        ).let { CollectionPresenter(locale).toTile(it) }

    test("each language reads the shelf in its own words") {
        assertThat(shelf(english).name).isEqualTo("Morning Azkar")
        assertThat(shelf(arabic).name).isEqualTo("أذكار الصباح")
    }

    test("the sentence under the name travels with the name") {
        assertThat(shelf(english).description).isEqualTo("Remembrance for the start of the day")
        assertThat(shelf(arabic).description).isEqualTo("أذكار لبداية اليوم")
    }

    test("a shelf nobody translated reads the same everywhere") {
        val untranslated = customCollection(id = 2L, name = "My shelf", description = "Mine")
        val inEnglish = CollectionPresenter(english).toTile(untranslated)
        val inArabic = CollectionPresenter(arabic).toTile(untranslated)

        // The user's own words are not a translation, and inventing one for another language would
        // put words in their mouth that they never typed and cannot edit from this screen.
        assertThat(inEnglish.name).isEqualTo("My shelf")
        assertThat(inArabic.name).isEqualTo("My shelf")
        assertThat(inArabic.description).isEqualTo("Mine")
    }

    test("a shelf translated only into Arabic shows nothing rather than an English sentence in Arabic") {
        val namedOnly =
            customCollection(
                id = 3L,
                name = "Quiet hours",
                description = "Remembrance said in the last hour",
                translations = mapOf(arabic to LocalizedPair("ساعة الهدوء")),
            )

        val tile = CollectionPresenter(arabic).toTile(namedOnly)

        // A translated name over an English sentence reads as a half-finished translation; an empty
        // line reads as a collection that simply does not describe itself in Arabic.
        assertThat(tile.name).isEqualTo("ساعة الهدوء")
        assertThat(tile.description).isEmpty()
    }

    test("a regional Arabic reader gets the Arabic a plain Arabic reader gets") {
        val regional =
            CollectionPresenter(Locale.forLanguageTag("ar-EG")).toTile(
                officialCollection(
                    id = 4L,
                    name = "Morning Azkar",
                    translations = mapOf(arabic to LocalizedPair("أذكار الصباح")),
                ),
            )

        // A name is not written per country, so `ar-EG` must not miss a row keyed `ar`.
        assertThat(regional.name).isEqualTo("أذكار الصباح")
    }

    test("a category chip is named in the reader's language too") {
        val remembrance =
            remembrance(
                id = 9L,
                tags =
                    setOf(
                        tag(
                            id = 1L,
                            name = "morning",
                            translations =
                                mapOf(
                                    arabic to "أذكار الصباح",
                                ),
                        ),
                    ),
            )

        assertThat(CollectionPresenter(english).categories(remembrance)).containsExactly("Morning")
        assertThat(CollectionPresenter(arabic).categories(remembrance)).containsExactly("أذكار الصباح")
    }

    test("an untranslated category keeps its capitalised stored name in every language") {
        val tagged = remembrance(id = 10L, tags = setOf(tag(id = 2L, name = "food & drink")))

        // Stored folded, drawn with its first letter lifted: `uq_tag__name` folds at the boundary,
        // so printing the column raw would put "food & drink" on a card. Only the first letter is
        // restored rather than the casing of every word — the tag is the user's own text, and
        // inventing capitals they did not type is as much an edit as translating them away would be.
        assertThat(CollectionPresenter(arabic).categories(tagged)).containsExactly("Food & drink")
    }

    test("the publisher label is filtered out whatever language it is written in") {
        val tagged =
            remembrance(
                id = 11L,
                tags =
                    setOf(
                        tag(id = 3L, name = "muslim data", translations = mapOf(arabic to "بيانات المسلمين")),
                        tag(id = 4L, name = "travel", translations = mapOf(arabic to "أذكار السفر")),
                    ),
            )

        val subjects = CollectionPresenter(arabic).subjectCategories(tagged)

        // It is on every shipped row, so a chip labelled with it selects exactly what "All" selects.
        assertThat(subjects).containsExactly("أذكار السفر")
    }
}

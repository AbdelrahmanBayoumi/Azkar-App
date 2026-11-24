package com.azkar.i18n

import de.infix.testBalloon.framework.core.testSuite
import org.assertj.core.api.Assertions.assertThat
import org.w3c.dom.Element
import org.w3c.dom.Node
import java.nio.file.Files
import java.nio.file.Path
import javax.xml.parsers.DocumentBuilderFactory

/**
 * The two language files agreeing with each other.
 *
 * Nothing else in the build can catch a key that exists in one language and not the other, or a
 * sentence whose `%1$d` is missing from the translation. The resource compiler resolves a key to a
 * *file*, so a key that is present in English and absent in Arabic is not an error anywhere — it is
 * an English sentence that quietly appears inside an Arabic screen, which is the exact symptom this
 * work was commissioned to remove. And a placeholder is not a key: `getString` binds arguments by
 * index, so an Arabic translation that drops `%1$d` renders a sentence with a hole where the number
 * was, with no diagnostic at all.
 *
 * So both files are parsed here as XML rather than as text, which is what lets the two kinds of
 * agreement be stated separately. A `<string>` must match exactly — same placeholders, same count.
 * A `<plurals>` must not, because Arabic has six quantities where English has two and legitimately
 * writes the number into fewer of them ("وسم واحد" carries it in the word). There the rule is that
 * English's placeholders all still appear, so no language loses the argument it was given.
 *
 * The files are found by walking up from the working directory rather than by a hard-coded relative
 * path, so the suite survives being run from the repository root as well as from the module.
 *
 * TestBalloon generates the test entry point, but the IDE does not see that usage.
 */
@Suppress("unused")
val TranslationParitySuite by testSuite("English and Arabic say the same set of things") {

    val english by lazy { Bundle.read(ENGLISH_FILE) }
    val arabic by lazy { Bundle.read(ARABIC_FILE) }

    testSuite("the keys") {
        test("Arabic has a sentence for every English sentence") {
            assertThat(arabic.strings.keys - english.strings.keys)
                .describedAs("A key Arabic does not carry falls back to English on an Arabic screen")
                .isEmpty()
        }

        test("English has a sentence for every Arabic sentence") {
            // The other direction is not an error — Compose resolves the default file when a
            // translation is missing, so English is where an unused key goes to sit unnoticed.
            // Caught here because an Arabic key with no English original is usually a typo or a
            // leftover, and either way it is a line nobody can ever see being maintained.
            assertThat(english.strings.keys - arabic.strings.keys)
                .describedAs("A key English does not carry can only be reached in Arabic")
                .isEmpty()
        }

        test("neither file says a key twice") {
            assertThat(english.duplicates).isEmpty()
            assertThat(arabic.duplicates).isEmpty()
        }

        test("every sentence has something in it") {
            assertThat(english.strings.filterValues { it.isBlank() }.keys).isEmpty()
            assertThat(arabic.strings.filterValues { it.isBlank() }.keys).isEmpty()
        }
    }

    testSuite("the sentences that take an argument") {
        test("a translation takes exactly the arguments its original takes") {
            val mismatched =
                english.strings.keys
                    .filter { it in arabic.strings }
                    .filter { english.placeholdersOfString(it) != arabic.placeholdersOfString(it) }

            assertThat(mismatched)
                .describedAs(
                    "A placeholder is bound by index, so a missing one leaves a hole and an extra one is not an " +
                        "error you will ever see",
                ).isEmpty()
        }

        test("every plural form English uses is still reachable in Arabic") {
            val missing =
                english.plurals.keys
                    .filter { it in arabic.plurals }
                    .filter { key ->
                        !arabic.placeholdersOfPlurals(key).containsAll(english.placeholdersOfPlurals(key))
                    }

            assertThat(missing)
                .describedAs(
                    "Arabic may write the number into fewer forms than English — \"وسم واحد\" already carries it — " +
                        "but never into more, and never none",
                ).isEmpty()
        }
    }

    testSuite("the plurals") {
        test("Arabic has a plural for every English plural") {
            assertThat(english.plurals.keys - arabic.plurals.keys).isEmpty()
            assertThat(arabic.plurals.keys - english.plurals.keys).isEmpty()
        }

        test("both languages answer the form that has no other answer") {
            // Compose resolves a quantity against this file and falls back to `other`, so a plural
            // without one is the one way a language can answer "no translation" without saying so.
            val missing =
                (english.plurals.keys + arabic.plurals.keys)
                    .filterNot { it in arabic.plurals }
                    .filter { key -> "other" !in english.plurals.getValue(key) }

            assertThat(missing).isEmpty()
        }
    }
}

/** Where the two files are, named after the languages rather than after the folder names. */
private const val ENGLISH_FILE = "values"
private const val ARABIC_FILE = "values-ar"

/**
 * One `strings.xml`, as the two things worth comparing: what it names, and what each name takes.
 *
 * A [plurals] is kept apart from a [strings] rather than folded into the same map because the two are
 * allowed to disagree about their shape — see the suite comment — and because their placeholders are
 * counted over items rather than over one value.
 */
private data class Bundle(
    val strings: Map<String, String>,
    val plurals: Map<String, List<String>>,
    val duplicates: List<String>,
) {
    fun placeholdersOfString(key: String): Set<String> = placeholdersIn(strings.getValue(key))

    fun placeholdersOfPlurals(key: String): Set<String> = plurals.getValue(key).flatMap(::placeholdersIn).toSet()

    companion object {
        fun read(directory: String): Bundle {
            val document =
                DocumentBuilderFactory
                    .newInstance()
                    .newDocumentBuilder()
                    .parse(locate(directory).toUri().toString())
            val root = document.documentElement

            val strings = mutableMapOf<String, String>()
            val plurals = mutableMapOf<String, List<String>>()
            val seen = mutableSetOf<String>()
            val duplicates = mutableListOf<String>()

            root.elements().forEach { element ->
                val key = element.getAttribute("name")
                if (!seen.add(key)) duplicates += key
                when (element.tagName) {
                    "string" -> strings[key] = element.textContent
                    "plurals" ->
                        plurals[key] =
                            element
                                .elements()
                                .filter { it.getAttribute("quantity") in ONE_AND_OTHER }
                                .map { it.textContent }
                }
            }
            return Bundle(strings, plurals, duplicates)
        }

        /**
         * The argument placeholders in [value], as the index-and-type pairs Compose binds by.
         *
         * `%1$d` and `%1$s` are kept apart from each other because they are: one formats a number
         * and the other formats text, and swapping them is a mistake that renders rather than fails.
         * A bare `%s` is read as the first argument, which is what Compose does with it.
         */
        private fun placeholdersIn(value: String): Set<String> =
            PLACEHOLDER
                .findAll(value)
                .map { it.groupValues[1].ifEmpty { "1" } + it.groupValues[2] }
                .toSet()

        private val PLACEHOLDER = Regex("""%(\d+)?\$?([a-zA-Z])""")

        /**
         * The quantities a comparison of placeholders should look at.
         *
         * The two every language must have, whether or not it needs them for its own grammar.
         * Arabic's `zero`/`two`/`few`/`many` are left out deliberately: a form that writes the
         * number into the word instead of a placeholder — "وسم واحد" — contributes no placeholder,
         * and counting it would make Arabic look like it had arguments English never passed.
         */
        private val ONE_AND_OTHER = setOf("one", "other")
    }
}

/**
 * The element children of a node, as an ordinary list.
 *
 * A DOM `NodeList` is indexed and length-bearing but is not `Iterable`, and every other XML shape in
 * this file is a Kotlin collection. This is the one adapter between the two, rather than the six
 * call sites it would otherwise have to sit at.
 */
private fun Node.elements(): List<Element> = (0 until childNodes.length).mapNotNull { childNodes.item(it) as? Element }

/**
 * The `strings.xml` for [directory], found by walking up until one is there.
 *
 * Walking rather than naming a relative path because Gradle runs a test with the module directory as
 * its working directory and a developer running one from an IDE may not. Both files are read lazily
 * for a reason the first version of this suite learned the hard way: TestBalloon builds a suite's
 * body during *discovery*, so a throw in here takes down the entire engine rather than failing one
 * test — and a missing file is a mistake worth one red test, not a whole red build.
 */
private fun locate(directory: String): Path {
    val tail = Path.of("src/main/composeResources/$directory/strings.xml")
    var candidate: Path? = Path.of(System.getProperty("user.dir")).toAbsolutePath()
    while (candidate != null) {
        val target = candidate.resolve(tail)
        if (Files.isRegularFile(target)) return target
        candidate = candidate.parent
    }
    error("No $tail at or above ${System.getProperty("user.dir")}")
}

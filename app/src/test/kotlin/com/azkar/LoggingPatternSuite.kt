package com.azkar

import ch.qos.logback.classic.Level
import com.azkar.testing.captureLogs
import com.azkar.testing.renderAsProduction
import de.infix.testBalloon.framework.core.testSuite
import org.assertj.core.api.Assertions.assertThat

/**
 * The shipped `logback.xml` pattern, checked against real events.
 *
 * A `ListAppender` can only prove a field reached the event; it says nothing about whether a human
 * would ever see it. The gap between those two is not hypothetical: `RemembranceMapper` declared a
 * `payload`, the field really was on the event, and the console still printed a bare sentence
 * because the pattern ended in `%msg%n`. So these tests *render* rather than inspect — they run the
 * production pattern over a captured event, which is the closest a test gets to a user's terminal.
 *
 * Only `:app` can run this: it is the module that packages `logback.xml`, so it is the only one
 * whose test classpath carries the file `renderAsProduction` reads the pattern out of.
 *
 * The capture hands out a logger of its own, and a logger created by name has no caller
 * information — so `%method` renders as `[?]` in the lines these tests assert on. Production loggers
 * use `KotlinLogging.logger {}`, which passes the callsite, and that is the only reason `%method`
 * is in the pattern at all.
 *
 * TestBalloon generates the test entry point, but the IDE does not see that usage.
 */
@Suppress("unused")
val LoggingPatternSuite by testSuite("The shipped log pattern") {
    test("a structured payload reaches the rendered line, not just the event") {
        val logs =
            captureLogs { logger ->
                logger.atWarn {
                    message = "Dropped locales missing counterpart"
                    payload =
                        mapOf(
                            "remembranceId" to 7L,
                            "droppedTranslations" to listOf("fr"),
                            "droppedExplanations" to emptyList<String>(),
                        )
                }
            }

        val rendered = renderAsProduction(logs.at(Level.WARN).single())

        // `%kvp` quotes each value, so a value containing a space stays one field. The point of
        // the assertions is the `key=value` pairs themselves, not their formatting.
        assertThat(rendered)
            .describedAs("A structured warning as the shipped console appender prints it")
            .contains("Dropped locales missing counterpart")
            .contains("""remembranceId="7"""")
            .contains("""droppedTranslations="[fr]"""")
            .contains("""droppedExplanations="[]"""")
    }

    test("a message with no payload still renders in full, and adds no stray field") {
        val logs =
            captureLogs { logger ->
                logger.warn { "Seed resource [/db/seed/library.json] holds no rows" }
            }

        val rendered = renderAsProduction(logs.at(Level.WARN).single())

        assertThat(rendered)
            .describedAs("A payload-free warning, which is the majority of what the app logs")
            .contains("Seed resource [/db/seed/library.json] holds no rows")
            // Nothing but a `key=value` pair can produce an `=`, and the date pattern uses `@`.
            // One here would mean `%kvp` was rendering something it should not have.
            .doesNotContain("=")
    }
}

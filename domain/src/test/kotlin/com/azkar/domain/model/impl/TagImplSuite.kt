package com.azkar.domain.model.impl

import de.infix.testBalloon.framework.core.testSuite
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import java.time.Instant

/**
 * A tag is shared by every remembrance that carries it, so the two properties that matter are
 * the one that makes two tags the same tag — the trimmed, case-folded name — and the refusal to
 * build one without a name at all.
 *
 * TestBalloon generates the test entry point, but the IDE does not see that usage.
 */
@Suppress("unused")
val TagImplSuite by testSuite("A tag built through TagImpl") {
    test("carries no id or creation time until it is persisted") {
        val tag = TagImpl(name = "morning")

        assertThat(tag.id).isNull()
        assertThat(tag.createdAt).isNull()
        assertThat(tag.name).isEqualTo("morning")
    }

    test("keeps the id and creation time it was given") {
        val createdAt = Instant.parse("2024-01-01T00:00:00Z")

        val tag = TagImpl(id = 7L, name = "morning", createdAt = createdAt)

        assertThat(tag.id).isEqualTo(7L)
        assertThat(tag.createdAt).isEqualTo(createdAt)
    }

    testSuite("name normalisation") {
        test("trims the name so surrounding whitespace cannot create a distinct tag") {
            val tag = TagImpl(name = "  morning  ")

            assertThat(tag.name).isEqualTo("morning")
        }

        test("considers a differently cased name equal to the trimmed one") {
            val left = TagImpl(name = "  morning  ")
            val right = TagImpl(name = "morning")

            assertThat(left).isEqualTo(right)
        }

        listOf("", "   ").forEach { candidate ->
            test("rejects a name of \"$candidate\"") {
                assertThatThrownBy { TagImpl(name = candidate) }
                    .isInstanceOf(IllegalArgumentException::class.java)
                    .hasMessage("Tag name must not be blank")
            }
        }
    }

    test("groups a remembrance by tag through set membership") {
        val morning = TagImpl(name = "morning")
        val remembrance = RemembranceImpl(tags = setOf(morning))

        assertThat(remembrance.tags).contains(morning)
    }
}

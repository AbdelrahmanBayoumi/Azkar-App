package com.azkar.data.seed

import de.infix.testBalloon.framework.core.testSuite
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy

// TestBalloon generates the test entry point, but the IDE does not see that usage.
@Suppress("unused")
val SeedDatasetSuite by testSuite("Reviewed seed library") {
    test("timed collections use explicit contexts and post-prayer excludes prayer-internal text") {
        val dataset = SeedDataset.bundled

        fun members(key: String) = dataset.collections.single { it.key == key }.members

        assertThat(members("morning")).containsExactlyElementsOf((75..97).map { "muslim-data:$it" })
        assertThat(members("evening")).containsExactlyElementsOf((98..118).map { "muslim-data:$it" })
        assertThat(members("prayer")).containsExactlyElementsOf((66..73).map { "muslim-data:$it" })
        assertThat(members("morning-evening")).hasSize(13).doesNotHaveDuplicates()
        assertThat(members("morning")).doesNotContain("muslim-data:1", "muslim-data:119")
    }

    test("shared wording keeps distinct references and occasion context") {
        val rows = SeedDataset.bundled.remembrances.associateBy { it.key }
        val morning = rows.getValue("muslim-data:75")
        val evening = rows.getValue("muslim-data:98")
        assertThat(morning.translations.getValue("ar").text).isEqualTo(evening.translations.getValue("ar").text)
        assertThat(morning.translations.getValue("en").explanation).isEqualTo("Remembrance In the morning")
        assertThat(evening.translations.getValue("en").explanation).isEqualTo("Remembrance In the evening")
        assertThat(rows).hasSize(287)
    }

    test("rejects duplicate keys and dangling collection members before seeding") {
        val dataset = SeedDataset.bundled
        assertThatThrownBy {
            dataset.copy(remembrances = dataset.remembrances + dataset.remembrances.first()).validate()
        }.isInstanceOf(IllegalArgumentException::class.java)
        assertThatThrownBy {
            dataset.copy(collections = listOf(dataset.collections.first().copy(members = listOf("missing")))).validate()
        }.isInstanceOf(IllegalArgumentException::class.java)
    }

    test("rejects missing wording and unsupported grades but allows an absent explanation") {
        val dataset = SeedDataset.bundled
        val first = dataset.remembrances.first()
        assertThatThrownBy {
            dataset.copy(remembrances = listOf(first.copy(translations = emptyMap()))).validate()
        }.isInstanceOf(IllegalArgumentException::class.java)
        assertThatThrownBy {
            dataset.copy(remembrances = listOf(first.copy(grade = "INFERRED"))).validate()
        }.isInstanceOf(IllegalArgumentException::class.java)
        val withoutExplanations =
            first.copy(
                translations = first.translations.mapValues { it.value.copy(explanation = null) },
            )
        dataset.copy(remembrances = listOf(withoutExplanations) + dataset.remembrances.drop(1)).validate()
    }
}

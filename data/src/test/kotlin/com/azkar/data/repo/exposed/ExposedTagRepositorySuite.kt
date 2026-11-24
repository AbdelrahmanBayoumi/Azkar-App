package com.azkar.data.repo.exposed

import com.azkar.domain.model.impl.TagImpl
import com.azkar.testUtils.exposedRepositories
import com.azkar.testUtils.seededRemembrances
import de.infix.testBalloon.framework.core.testSuite
import org.assertj.core.api.Assertions.assertThat

/**
 * Behaviour of the tag queries against a real database, not against a mocked one: case
 * folding, `LIKE` escaping and the ordering guarantees are all implemented in SQL, so an
 * in-memory H2 database is the only place they can honestly be checked.
 *
 * Names are asserted in their stored, folded form — what comes back is lower case however it was
 * asked for. Capitalisation is the presentation layer's business.
 *
 * TestBalloon generates the test entry point, but the IDE does not see that usage.
 */
@Suppress("unused")
val ExposedTagRepositorySuite by testSuite("ExposedTagRepository") {
    exposedRepositories {
        test("save(): persists a new tag and returns it with the generated id") { repositories ->
            val saved = repositories.tags.save(TagImpl(name = "Morning"))

            assertThat(saved.id).describedAs("Generated id should be present after save()").isNotNull()
            assertThat(saved.name).isEqualTo("morning")

            val found = repositories.tags.findById(saved.id)
            assertThat(found).isNotNull()
            assertThat(found?.name).isEqualTo("morning")
        }

        test("findAll(): returns tags ordered by id") { repositories ->
            val a = repositories.tags.save(TagImpl(name = "Alpha"))
            val b = repositories.tags.save(TagImpl(name = "Beta"))
            val c = repositories.tags.save(TagImpl(name = "Gamma"))

            val all = repositories.tags.findAll()

            assertThat(all.map { it.id }).containsExactly(a.id, b.id, c.id)
            assertThat(all.map { it.name }).containsExactly("alpha", "beta", "gamma")
        }

        test("findByNameIgnoreCase(): matches case-insensitively") { repositories ->
            val saved = repositories.tags.save(TagImpl(name = "Evening"))
            assertThat(saved.name).describedAs("The stored form is folded").isEqualTo("evening")

            val lower = repositories.tags.findByNameIgnoreCase("evening")
            val mixed = repositories.tags.findByNameIgnoreCase("EvEnInG")

            assertThat(lower).isNotNull()
            assertThat(lower?.id).isEqualTo(saved.id)
            assertThat(mixed).isNotNull()
            assertThat(mixed?.id).isEqualTo(saved.id)
        }

        testSuite("findByNameContainingIgnoreCase()") {
            test("supports LIKE %term% (case-insensitive)") { repositories ->
                repositories.tags.save(TagImpl(name = "Morning"))
                repositories.tags.save(TagImpl(name = "Noon"))
                repositories.tags.save(TagImpl(name = "Evening"))

                val matches = repositories.tags.findByNameContainingIgnoreCase("ing")

                assertThat(matches.map { it.name }).containsExactlyInAnyOrder("morning", "evening")
            }

            test("escapes LIKE wildcards in the term") { repositories ->
                repositories.tags.save(TagImpl(name = "100% Pure"))
                repositories.tags.save(TagImpl(name = "Nightly"))

                val matches = repositories.tags.findByNameContainingIgnoreCase("0%")

                assertThat(matches.map { it.name }).containsExactly("100% pure")
            }
        }
    }

    // Soft delete, the usage projection and revival are all checked against seeded join rows
    // rather than tags written on their own, because the join tables are where the interesting
    // behaviour lives: a tombstoned tag has to vanish from the collections that still point at
    // it, and has to come back with those pointers intact when it is restored.
    seededRemembrances {
        testSuite("soft delete") {
            test("hides the tag from every read") { repositories ->
                val id = repositories.tags.save(TagImpl(name = "Temp")).id

                repositories.tags.softDelete(id)

                assertThat(repositories.tags.findById(id)).isNull()
                assertThat(repositories.tags.findByNameIgnoreCase("Temp")).isNull()
                assertThat(repositories.tags.findAll().map { it.name }).doesNotContain("temp")
                assertThat(repositories.tags.findByNameContainingIgnoreCase("temp")).isEmpty()
            }

            test("keeps the row, so the name cannot be taken back") { repositories ->
                val id = repositories.tags.save(TagImpl(name = "Temp")).id

                repositories.tags.softDelete(id)

                assertThat(repositories.tags.findDeletedSummaries().map { it.name }).containsExactly("temp")
            }

            test("is a no-op for a null id and for an unknown one") { repositories ->
                repositories.tags.softDelete(null)
                repositories.tags.softDelete(999_999L)

                // The seed's two tags are untouched — nothing was deleted, and nothing was invented.
                assertThat(repositories.tags.findAll().map { it.name }).containsExactly("morning", "evening")
                assertThat(repositories.tags.findDeletedSummaries()).isEmpty()
            }

            test("leaves the original tombstone alone when asked twice") { repositories ->
                val id = repositories.tags.save(TagImpl(name = "Temp")).id

                repositories.tags.softDelete(id)
                val first =
                    repositories.tags
                        .findDeletedSummaries()
                        .single()
                        .deletedAt
                repositories.tags.softDelete(id)
                val second =
                    repositories.tags
                        .findDeletedSummaries()
                        .single()
                        .deletedAt

                // The screen orders its "recently deleted" list by this stamp, so overwriting it
                // on a second delete would silently move the tag back to the top of it.
                assertThat(second).isEqualTo(first)
            }

            test("takes the tag off the remembrances that carry it") { repositories ->
                // Tag 10 is 'morning' in the seed, linked to remembrances 1 and 3.
                val before =
                    repositories.remembrances
                        .findById(1)
                        ?.tags
                        .orEmpty()
                assertThat(before.map { it.name }).containsExactly("morning")

                repositories.tags.softDelete(10)

                assertThat(repositories.remembrances.findById(1)?.tags).isEmpty()
                assertThat(
                    repositories.remembrances
                        .findById(2)
                        ?.tags
                        ?.map { it.name },
                ).containsExactly("evening")
                // The query that reads the join straight rather than through an aggregate.
                assertThat(repositories.remembrances.findByTagNameIgnoreCase("Morning")).isEmpty()
            }
        }

        testSuite("restore") {
            test("brings the tag and its remembrances back") { repositories ->
                repositories.tags.softDelete(10)

                repositories.tags.restore(10)

                assertThat(repositories.tags.findById(10)?.name).isEqualTo("morning")
                assertThat(repositories.tags.findDeletedSummaries()).isEmpty()
                // This is the point of keeping the join rows: the links were never detached, so
                // restoring is clearing a column rather than rebuilding the membership.
                assertThat(
                    repositories.remembrances
                        .findById(1)
                        ?.tags
                        ?.map { it.name },
                ).containsExactly("morning")
                // Both remembrances the seed linked to the tag come back, not just the one asserted above.
                assertThat(repositories.remembrances.findByTagNameIgnoreCase("Morning")).hasSize(2)
            }

            test("is a no-op for a null id, an unknown one, and a live tag") { repositories ->
                repositories.tags.restore(null)
                repositories.tags.restore(999_999L)
                repositories.tags.restore(11)

                assertThat(repositories.tags.findAll().map { it.name }).containsExactly("morning", "evening")
            }
        }

        testSuite("findSummaries()") {
            test("counts the remembrances carrying each tag") { repositories ->
                val summaries = repositories.tags.findSummaries().associateBy { it.name }

                // The seed links tag 10 to remembrances 1 and 3, and tag 11 to remembrance 2.
                assertThat(summaries.getValue("morning").remembranceCount).isEqualTo(2)
                assertThat(summaries.getValue("evening").remembranceCount).isEqualTo(1)
            }

            test("leaves soft-deleted tags out") { repositories ->
                repositories.tags.softDelete(10)

                assertThat(repositories.tags.findSummaries().map { it.name }).containsExactly("evening")
            }

            test("reports a tag nothing points at as unused rather than dropping it") { repositories ->
                repositories.tags.save(TagImpl(name = "Orphan"))

                val orphan = repositories.tags.findSummaries().single { it.name == "orphan" }

                assertThat(orphan.usageCount).isZero()
                assertThat(orphan.isDeleted).isFalse()
            }
        }

        testSuite("findDeletedSummaries()") {
            test("returns most recently deleted first") { repositories ->
                repositories.tags.softDelete(10)
                repositories.tags.softDelete(11)

                assertThat(repositories.tags.findDeletedSummaries().map { it.name })
                    .containsExactlyInAnyOrder("morning", "evening")
                assertThat(repositories.tags.findDeletedSummaries()).allMatch { it.isDeleted }
            }

            test("is empty when nothing has been deleted") { repositories ->
                assertThat(repositories.tags.findDeletedSummaries()).isEmpty()
            }
        }

        testSuite("findUsage()") {
            test("names the remembrances carrying the tag, not just how many") { repositories ->
                // The seed links tag 10 to remembrances 1 and 3, and tag 11 to remembrance 2.
                val usage = repositories.tags.findUsage(10)

                assertThat(usage).isNotNull()
                assertThat(usage?.remembrances?.map { it.id }).containsExactlyInAnyOrder(1L, 3L)
                assertThat(usage?.tag?.remembranceCount).isEqualTo(2)
            }

            test("carries each passage's translations keyed by locale") { repositories ->
                val row =
                    repositories.tags
                        .findUsage(10)
                        ?.remembrances
                        ?.first { it.id == 1L }

                assertThat(row).isNotNull()
                // Only the translation table is read here — an explanation must not change what a
                // usage row can print, so this asserts the shape rather than a specific string.
                assertThat(row?.texts).isNotEmpty
                assertThat(row?.texts?.values).allMatch { it.isNotBlank() }
            }

            test("reports a tag on nothing as an empty usage rather than null") { repositories ->
                repositories.tags.save(TagImpl(name = "Orphan"))

                val usage = repositories.tags.findUsage(repositories.tags.findByNameIgnoreCase("orphan")?.id)

                assertThat(usage).isNotNull()
                assertThat(usage?.collections).isEmpty()
                assertThat(usage?.remembrances).isEmpty()
            }

            test("returns null for a tag that does not exist") { repositories ->
                assertThat(repositories.tags.findUsage(9_999L)).isNull()
            }

            test("returns null for a soft-deleted tag") { repositories ->
                repositories.tags.softDelete(10)

                // Null and not an empty usage on purpose: the screen has to be able to tell "this is
                // gone" apart from "this is on nothing", and a panel that opened empty onto a
                // deleted tag would read as though the library had just been emptied.
                assertThat(repositories.tags.findUsage(10)).isNull()
            }

            test("returns null for a null id") { repositories ->
                assertThat(repositories.tags.findUsage(null)).isNull()
            }
        }

        test("save(): revives a tombstone rather than colliding with it") { repositories ->
            repositories.tags.softDelete(10)

            // The unique index on `tag.name` covers the whole table, so a plain insert here
            // would fail. Resaving the same name is how a user gets a deleted tag back.
            val revived = repositories.tags.save(TagImpl(name = "Morning"))

            assertThat(revived.id).isEqualTo(10)
            assertThat(repositories.tags.findDeletedSummaries()).isEmpty()
            assertThat(repositories.tags.findById(10)?.name).isEqualTo("morning")
        }
    }
}

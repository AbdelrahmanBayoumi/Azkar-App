package com.azkar.ui.preview

import com.azkar.domain.model.CollectionKind
import com.azkar.domain.model.RecitationProgress

/** Preview library data shared by the shelf and collection session previews. */
internal fun previewGateway(
    includeEmptyCollection: Boolean = false,
    emptyLibrary: Boolean = false,
    progressByCollection: Map<Long, List<RecitationProgress>> = emptyMap(),
    failSnapshot: Boolean = false,
): PreviewCollectionsGateway =
    PreviewCollectionsGateway(
        collections = previewCollections(includeEmptyCollection, emptyLibrary),
        remembrances = previewLibraryRemembrances(),
        recitedCounts = mapOf(PREVIEW_COLLECTION_ID to 31, 12L to 3),
        progressByCollection = progressByCollection,
        failSnapshot = failSnapshot,
    )

private fun previewCollections(
    includeEmptyCollection: Boolean,
    emptyLibrary: Boolean,
) = if (emptyLibrary) {
    emptyList()
} else {
    listOf(
        previewCollection(
            id = PREVIEW_COLLECTION_ID,
            name = "Morning Azkar",
            iconKey = "sunrise",
            memberCount = 54,
        ),
        previewCollection(
            id = 12L,
            name = "My night route",
            description = "Remembrances for driving.",
            kind = CollectionKind.CUSTOM,
            iconKey = "compass",
            homePinnedAt = null,
            isFavorite = true,
        ),
    ) +
        if (includeEmptyCollection) {
            listOf(
                previewCollection(
                    id = 13L,
                    name = "An empty collection",
                    description = "A collection waiting for its first remembrance.",
                    kind = CollectionKind.CUSTOM,
                    iconKey = "folder",
                    memberCount = 0,
                    members = emptyList(),
                ),
            )
        } else {
            emptyList()
        }
}

private fun previewLibraryRemembrances() =
    listOf(
        previewRemembrance(101L),
        previewRemembrance(
            102L,
            arabic = "سُبْحَانَ اللَّهِ وَبِحَمْدِهِ",
            english = "Glory to Allah, and praise be to Him.",
            source = "Muslim 2691",
            favorite = true,
        ),
        previewRemembrance(
            103L,
            arabic = "اللَّهُمَّ إِنِّي أَسْأَلُكَ الْعَافِيَةَ",
            english = "O Allah, I ask You for wellbeing.",
            source = "Ibn Majah 3871",
        ),
        previewRemembrance(
            104L,
            arabic = "أَسْتَغْفِرُ اللَّهَ وَأَتُوبُ إِلَيْهِ",
            english = "I seek Allah's forgiveness and turn to Him in repentance.",
            source = "Abu Dawud 1516",
        ),
        previewRemembrance(
            105L,
            arabic = "رَبِّ زِدْنِي عِلْمًا",
            english = "My Lord, increase me in knowledge.",
            source = "Tirmidhi 3161",
        ),
    )

/** Progress for every member in [PREVIEW_COLLECTION_ID]'s default collection. */
internal fun previewCompletedCollectionProgress(): List<RecitationProgress> =
    (listOf(101L, 102L, 103L, 104L) + (900_000L..900_049L).toList()).map { id ->
        RecitationProgress(remembranceId = id, repeatCount = 1, lastRecitedAt = previewFixtureInstant())
    }

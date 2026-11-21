package com.azkar

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.azkar.ui.home.FavoritesState
import com.azkar.ui.home.HomeContent
import com.azkar.ui.home.HomeInputs
import com.azkar.ui.home.HomeNavigation
import com.azkar.ui.home.HomeRailState
import com.azkar.ui.home.NotificationSettingsState
import com.azkar.ui.home.PrayerTimesState
import com.azkar.ui.library.CollectionDetailScreen
import com.azkar.ui.library.CollectionSessionScreen
import com.azkar.ui.library.CollectionsCatalogue
import com.azkar.ui.library.RemembranceReaderScreen
import com.azkar.ui.library.rememberCollectionDetailState
import com.azkar.ui.library.rememberCollectionSessionState
import com.azkar.ui.library.rememberRemembranceReaderState
import com.azkar.ui.notifications.NotificationAction
import com.azkar.ui.notifications.NotificationContent
import com.azkar.ui.notifications.presentation.NotificationCard
import com.azkar.ui.notifications.presentation.OverlayCardSizing
import com.azkar.ui.notifications.presentation.PresentedNotice
import com.azkar.ui.preview.PREVIEW_COLLECTION_ID
import com.azkar.ui.preview.previewHadithState
import com.azkar.ui.preview.previewRailCards
import com.azkar.ui.preview.previewReminders
import com.azkar.ui.preview.rememberPreviewCatalogue
import com.azkar.ui.preview.rememberPreviewSettingsStore
import com.azkar.ui.theme.AzkarTheme
import com.azkar.ui.theme.LocalWindowSizeClass
import com.azkar.ui.theme.windowSizeClassFor
import java.time.LocalDateTime
import java.util.Locale

@Composable
internal fun HomeShot(onReady: () -> Unit = {}) {
    val prayerTimes = remember { PrayerTimesState(Locale.ENGLISH) }
    LaunchedEffect(prayerTimes) {
        prayerTimes.start()
        prayerTimes.stop()
        prayerTimes.tick(LocalDateTime.of(2026, 2, 20, 10, 0))
    }
    if (prayerTimes.snapshot.rows.isEmpty()) return
    SideEffect(onReady)
    HomeContent(
        inputs =
            HomeInputs(
                prayerTimes = prayerTimes,
                favorites = remember { FavoritesState(Locale.ENGLISH) },
                notifications = NotificationSettingsState(rememberPreviewSettingsStore(), rememberCoroutineScope()),
                rail = remember { HomeRailState().apply { replace(previewRailCards()) } },
                reminders = remember { previewReminders() },
                navigation = HomeNavigation({}, {}, {}),
            ),
        dateText = "Friday, 20 February",
        name = "Azkar",
        hadith = previewHadithState(),
    )
}

@Composable
internal fun LibraryShot(
    onReady: () -> Unit = {},
    content: @Composable (CollectionsCatalogue) -> Unit,
) {
    val catalogue = rememberPreviewCatalogue(Locale.ENGLISH)
    LaunchedEffect(catalogue) {
        catalogue.loadLabels()
        catalogue.loadData()
    }
    if (!catalogue.loading && catalogue.allCollections.isNotEmpty()) {
        SideEffect(onReady)
        content(catalogue)
    }
}

@Composable
internal fun PassageReminderShot() {
    NotificationCard(
        item =
            PresentedNotice(
                id = 0L,
                title = "Azkar reminder",
                message = "أَلَا بِذِكْرِ اللَّهِ تَطْمَئِنُّ الْقُلُوبُ",
                action = NotificationAction.Remembrance(0L),
                content =
                    NotificationContent.Remembrance(
                        text = "أَلَا بِذِكْرِ اللَّهِ تَطْمَئِنُّ الْقُلُوبُ",
                        position = 1,
                        total = 3,
                        isFavorite = true,
                    ),
            ),
        sizing = OverlayCardSizing.FIT,
        onOpen = {},
        onDismiss = {},
        onPauseChanged = {},
        onToggleFavorite = { _, _ -> true },
    )
}

@Composable
internal fun CollectionDetailShot(catalogue: CollectionsCatalogue) {
    requireScreenshotCollection(catalogue)
    val collectionId = PREVIEW_COLLECTION_ID
    val collectionReminders = remember(collectionId) { previewReminders(collectionId) }
    val detail = rememberCollectionDetailState(catalogue, collectionId, collectionReminders)
    CollectionDetailScreen(
        detail = detail,
        onBack = {},
        onStartSession = {},
        onOpenRemembrance = {},
        showToast = { _, _, _ -> },
    )
}

@Composable
internal fun CollectionSessionShot(catalogue: CollectionsCatalogue) {
    requireScreenshotCollection(catalogue)
    val collectionId = PREVIEW_COLLECTION_ID
    val collectionReminders = remember(collectionId) { previewReminders(collectionId) }
    CollectionSessionScreen(
        session = rememberCollectionSessionState(catalogue, collectionId, collectionReminders),
        onBack = {},
        onOpenRemembrance = {},
        showToast = { _, _, _ -> },
    )
}

@Composable
internal fun RemembranceShot(catalogue: CollectionsCatalogue) {
    val collection = catalogue.allCollections.single { it.id == PREVIEW_COLLECTION_ID }
    val firstMemberId =
        requireNotNull(collection.members.firstOrNull()?.id) {
            "The screenshot collection must contain a remembrance"
        }
    RemembranceReaderScreen(
        detail = rememberRemembranceReaderState(catalogue = catalogue, remembranceId = firstMemberId),
        onBack = {},
        showToast = { _, _, _ -> },
    )
}

private fun requireScreenshotCollection(catalogue: CollectionsCatalogue) {
    require(catalogue.allCollections.any { it.id == PREVIEW_COLLECTION_ID }) {
        "Missing screenshot collection $PREVIEW_COLLECTION_ID"
    }
}

@Composable
internal fun ScreenshotFrame(
    dark: Boolean,
    density: Float = 1f,
    content: @Composable () -> Unit,
) {
    AzkarTheme(darkTheme = dark, uiLocale = Locale.ENGLISH) {
        CompositionLocalProvider(
            LocalDensity provides Density(density),
            LocalWindowSizeClass provides windowSizeClassFor(1440.dp, 900.dp),
        ) {
            Box(Modifier.fillMaxSize().background(AzkarTheme.colors.background)) { content() }
        }
    }
}

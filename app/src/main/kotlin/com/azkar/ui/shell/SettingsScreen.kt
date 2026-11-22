package com.azkar.ui.shell

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.azkar.generated.resources.Res
import com.azkar.generated.resources.settingsSubtitle
import com.azkar.generated.resources.settingsTitle
import com.azkar.ui.home.PrayerKey
import com.azkar.ui.notifications.AudioPreferences
import com.azkar.ui.notifications.DesktopAudioPlayer
import com.azkar.ui.notifications.GeocodingClient
import com.azkar.ui.notifications.LocationDetector
import com.azkar.ui.notifications.NotificationPreferencesRepository
import com.azkar.ui.notifications.NotificationSettingsState
import com.azkar.ui.notifications.PrayerCalculationMethod
import com.azkar.ui.notifications.PrayerLocationSettings
import com.azkar.ui.notifications.QueueCadence
import com.azkar.ui.notifications.QueueMode
import com.azkar.ui.notifications.ReminderQueueConfig
import com.azkar.ui.notifications.StartupRegistration
import com.azkar.ui.notifications.presentation.NotificationPresentationSettings
import com.azkar.ui.preview.previewAzkarAppState
import com.azkar.ui.preview.rememberPreviewSettingsStore
import com.azkar.ui.theme.AzkarSpacing
import com.azkar.ui.theme.AzkarTheme
import com.azkar.ui.theme.AzkarVariantsPreview
import com.azkar.ui.theme.LocalUiLocale
import com.azkar.updates.UpdateManager
import io.ktor.client.HttpClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import java.time.DayOfWeek

// `ui/shell/SettingsScreen.kt` — the screen the gear opens: a rail that names every block on this page
// under the family it belongs to, and one long scroll holding them all in the same order.

/** How much of the page the rail's edge takes, as a hairline rather than a shadow. */
private val RAIL_RULE_WIDTH = 1.dp

/** The scroll container's own inset. Wider than the dashboard's, because the pane is narrower. */
private val PAGE_PADDING =
    PaddingValues(
        start = 40.dp,
        end = 40.dp,
        top = AzkarSpacing.screenPaddingTop,
        bottom = AzkarSpacing.screenPaddingBottom,
    )

/**
 * The app-level preferences.
 *
 * It is one scroll rather than a tab strip because a strip hides the shape of the page. The reminders
 * blocks together run several times the height of the language block, so "Notifications" read as a peer of
 * "Language" and gave no hint that choosing it was a longer scroll than the others — and a reader who
 * wanted only the prayer city had to open the last tab to find out. Stacked, the length *is* the
 * information: the reader sees that the reminder settings are the big ones before choosing anything.
 *
 * The rail is what replaces the strip's job. It names every block under the family it is filed in — the
 * four families partition the page, so a row's parent is always the answer to "what kind of setting is
 * this" — jumps to it, and marks the one the reader is inside, so the page still has a table of contents
 * without spending the top of the window on a few buttons that hide everything.
 *
 * State arrives rather than being held here. The theme and locale are already on [AzkarAppState] — they
 * have to be, since they outlive the window — and the notification and prayer values come from the same
 * [NotificationPreferencesRepository] the runtime reads, so this composable holds no field of its own
 * beyond the scroll position.
 */
@Composable
fun SettingsScreen(
    state: AzkarAppState,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    initialGroup: SettingsGroup = SettingsGroup.GENERAL,
    initialSection: SettingsSection? = null,
    sectionRequest: Int = 0,
) {
    val notificationState = rememberNotificationSettingsState()
    val notificationValues = rememberNotificationSettingsValues()
    val audioPlayer = koinInject<DesktopAudioPlayer>()
    val windowInfo = LocalWindowInfo.current
    LaunchedEffect(notificationState, windowInfo) {
        snapshotFlow { windowInfo.isWindowFocused }.collect { focused ->
            if (focused) notificationState.notifications.refreshStartOnLogin()
        }
    }

    SettingsScreenContent(
        inputs =
            SettingsScreenInputs(
                state = state,
                onClose = onClose,
                initialGroup = initialGroup,
                initialSection = initialSection,
                sectionRequest = sectionRequest,
                notificationState = notificationState,
                notificationValues = notificationValues,
                audioPlayer = audioPlayer,
                updateManager = koinInject(),
            ),
        modifier = modifier,
    )
}

@Composable
private fun SettingsScreenContent(
    inputs: SettingsScreenInputs,
    modifier: Modifier,
) {
    val onClose = inputs.onClose
    val initialGroup = inputs.initialGroup
    val requestedSection = inputs.initialSection
    val initialSection = requestedSection ?: initialGroup.sections.first()
    val sectionRequest = inputs.sectionRequest
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    var selectedSection by remember(initialSection, sectionRequest) { mutableStateOf(initialSection) }
    val selectedGroup = selectedSection.group
    val activeSection = activeSettingsSection(listState, selectedSection)
    val colors = AzkarTheme.colors
    LaunchedEffect(requestedSection, sectionRequest) {
        requestedSection?.let { listState.scrollToItem(sectionScrollIndexInSection(it)) }
    }

    Row(
        modifier = modifier.fillMaxSize().background(colors.background),
        verticalAlignment = Alignment.Top,
    ) {
        SettingsNavRail(
            active = activeSection,
            selectedGroup = selectedGroup,
            onSelect = { section ->
                val groupChanged = section.group != selectedGroup
                selectedSection = section
                if (groupChanged) {
                    scope.launch {
                        listState.scrollToItem(0)
                        listState.animateScrollToItem(sectionScrollIndexInSection(section))
                    }
                } else {
                    scope.launch { listState.animateScrollToItem(sectionScrollIndexInSection(section)) }
                }
            },
            onSelectGroup = { group ->
                if (group != selectedGroup) {
                    selectedSection = group.sections.first()
                    scope.launch { listState.scrollToItem(0) }
                }
            },
        )
        // The rail's trailing edge, drawn by the page rather than by the rail: the rail is the thing
        // being scrolled past, and a rule that belonged to it would scroll with it.
        Box(modifier = Modifier.width(RAIL_RULE_WIDTH).fillMaxHeight().background(colors.border))
        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f),
            contentPadding = PAGE_PADDING,
            verticalArrangement = Arrangement.spacedBy(SECTION_GAP),
        ) {
            item(key = "header") {
                SettingsPageHeader(onClose = onClose, modifier = Modifier.fillMaxWidth())
            }
            settingsSections(inputs, selectedGroup)
        }
    }
}

private data class SettingsScreenInputs(
    val state: AzkarAppState,
    val onClose: () -> Unit,
    val initialGroup: SettingsGroup,
    val initialSection: SettingsSection?,
    val sectionRequest: Int,
    val notificationState: NotificationSettingsState,
    val notificationValues: NotificationSettingsValues,
    val audioPlayer: DesktopAudioPlayer,
    val updateManager: UpdateManager,
)

/** Keeps the scroll declaration's body about the shell, with each settings block in its own item. */
private fun LazyListScope.settingsSections(
    inputs: SettingsScreenInputs,
    selectedGroup: SettingsGroup,
) {
    val state = inputs.state
    val notificationState = inputs.notificationState
    val notificationValues = inputs.notificationValues
    selectedGroup.sections.forEach { section ->
        item(key = section.name) {
            SettingsColumn {
                when (section) {
                    SettingsSection.LANGUAGE -> SettingsLanguageSection(state = state)
                    SettingsSection.STARTUP ->
                        SettingsStartupSection(
                            startOnLogin = notificationState.notifications.startOnLogin,
                            settings = notificationState,
                        )
                    SettingsSection.UPDATES -> UpdateSettingsSection(inputs.updateManager)
                    SettingsSection.THEME -> SettingsThemeSection(state = state)
                    SettingsSection.TEXT -> SettingsTypographySection(state = state)
                    SettingsSection.REMINDER_CARD ->
                        SettingsReminderCardSection(
                            presentation = notificationValues.presentation,
                            settings = notificationState,
                            enabled = notificationValues.queue.enabled,
                        )
                    SettingsSection.REMINDER_SCHEDULE ->
                        SettingsReminderScheduleSection(
                            queue = notificationValues.queue,
                            settings = notificationState,
                        )
                    SettingsSection.PRAYER_LOCATION ->
                        SettingsPrayerLocationSection(
                            location = notificationValues.prayerLocation,
                            settings = notificationState,
                        )
                    SettingsSection.PRAYER_CALCULATION -> {
                        SettingsPrayerCalculationSection(
                            location = notificationValues.prayerLocation,
                            settings = notificationState,
                        )
                    }
                    SettingsSection.PRAYER_PANEL -> DesktopPrayerPanelSetting()
                    SettingsSection.AUDIO ->
                        SettingsAudioSection(
                            audio = notificationValues.audio,
                            settings = notificationState,
                            player = inputs.audioPlayer,
                        )
                }
            }
        }
    }
}

/**
 * Every block's [SettingsSectionBlock], taking the whole pane beside the rail.
 *
 * These used to be capped at 900dp and held to the left of it, which was defensible for a settings row —
 * one short sentence beside one control does not want a 1200dp pane. But the cap was doing its work in
 * the wrong place: it kept the block itself narrow while leaving the pane's extra width as an empty
 * margin beside it, so the page read as a column of cards with the window's half missing rather than as
 * a page. Letting the block fill the pane it was given costs nothing at this size, because the text in
 * it is set beside its control and not run out to a measure, and the cards then line up with the page
 * header and the rail's rule instead of floating inside them.
 */
@Composable
private fun SettingsColumn(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Column(modifier = modifier.fillMaxWidth()) { content() }
}

/**
 * Which block the reader is inside, for the rail to mark.
 *
 * Derived from the scroll position rather than remembered: the scroll position is already state, and
 * keeping a second copy of "where am I" in sync with it is the sort of thing that survives a change to
 * the list order and then quietly highlights the wrong row.
 *
 * The last block is the awkward one. [LazyListState.firstVisibleItemIndex] names the block at the top of
 * the viewport, and the final block is short enough — with the page's bottom padding under it — that the
 * list runs out of scroll before it ever reaches the top. So reading the topmost item gave the same
 * answer for the whole of the last screenful, and "Prayer times" could not be highlighted however far
 * down the reader went: the rail said they were still in the block above. `canScrollForward` is what
 * distinguishes that case, because it is false exactly when there is nothing below left to reach.
 *
 * [OFFSET_FOR_HEADER] is why this is not just `entries[index]`. The first item is the page header, not a
 * block, so the arithmetic would be off by one for the whole list — and off-by-one here does not crash,
 * it just marks the wrong row, which is the failure that would have shipped.
 */
@Composable
private fun activeSettingsSection(
    listState: LazyListState,
    selectedSection: SettingsSection,
): SettingsSection {
    // layoutInfo changes on every scroll frame, but the highlighted section changes only at boundaries.
    val active by remember(listState, selectedSection) {
        derivedStateOf {
            if (!listState.canScrollForward && !listState.canScrollBackward) {
                selectedSection
            } else {
                val visible = listState.layoutInfo.visibleItemsInfo
                val index =
                    when {
                        visible.isEmpty() -> 0
                        !listState.canScrollBackward ->
                            visible.firstOrNull { it.index >= OFFSET_FOR_HEADER }?.index
                                ?: 0
                        listState.canScrollForward -> visible.first().index
                        else -> visible.last().index
                    }
                selectedSection.group.sections.getOrElse(
                    index - OFFSET_FOR_HEADER,
                ) { selectedSection.group.sections.first() }
            }
        }
    }
    return active
}

/** The number of items that are not blocks: the page header. */
private const val OFFSET_FOR_HEADER = 1

/** Where a block sits in the list, header included. */
private fun sectionScrollIndexInSection(section: SettingsSection): Int =
    section.group.sections.indexOf(section) + OFFSET_FOR_HEADER

/**
 * The page's identity and its way out.
 *
 * The close is a round dismiss with a key caption rather than a labelled button, because the rail has
 * already taken the job a "Back" button used to do here: it is the first thing on the page and it names
 * where you are. What is left is a way out, and a way out that reads as one is not owed a sentence.
 */
@Composable
private fun SettingsPageHeader(
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = AzkarTheme.colors
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(AzkarSpacing.xl),
        verticalAlignment = Alignment.Top,
    ) {
        Column(modifier = Modifier.weight(1f).padding(top = 4.dp)) {
            Text(
                text = stringResource(Res.string.settingsTitle),
                color = colors.textPrimary,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = stringResource(Res.string.settingsSubtitle),
                color = colors.textSecondary,
                fontSize = 13.sp,
            )
        }
        SettingsCloseButton(onClick = onClose)
    }
}

/**
 * The holder behind the notification and prayer blocks.
 *
 * Both of the platform capabilities are injected here rather than taken from a parameter: the settings
 * page is the only screen that needs them, and a screen that is handed a detector it does not own would
 * let a caller pass one that is not the one the runtime uses. They are still nullable on the holder so a
 * preview host with no network and no login manager still draws every row.
 */
@Composable
private fun rememberNotificationSettingsState(): NotificationSettingsState {
    val preferences = koinInject<NotificationPreferencesRepository>()
    val locationDetector = koinInject<LocationDetector>()
    val startupRegistration = koinInject<StartupRegistration>()
    val geocodingClient = koinInject<GeocodingClient>()
    val scope = rememberCoroutineScope()
    return remember(preferences, locationDetector, startupRegistration, geocodingClient) {
        NotificationSettingsState(
            scope = scope,
            preferences = preferences,
            locationDetector = locationDetector,
            startupRegistration = startupRegistration,
            geocodingClient = geocodingClient,
        )
    }
}

/**
 * Everything persisted on the moved blocks, snapshotted into one value.
 *
 * Three `collectAsState` calls would have left three locals to thread by hand into the blocks, and a single
 * immutable holder means a block only recomposes when the settings it actually reads change — the prayer
 * block does not recompose when the start-on-login flag is toggled far above it.
 *
 * [queue] joined them when the reminder schedule card moved here from the reminders screen. It is the one
 * value on this page the reminders screen also draws, which is the point of reading it from the repository
 * rather than from a local: the summary at the top of that screen and the card at the top of this one are
 * two readings of one record, and neither holds a copy.
 */
@Immutable
private data class NotificationSettingsValues(
    val queue: ReminderQueueConfig,
    val prayerLocation: PrayerLocationSettings,
    val presentation: NotificationPresentationSettings,
    val audio: AudioPreferences,
    val prayerFlags: Map<PrayerKey, Boolean>,
)

@Composable
private fun rememberNotificationSettingsValues(): NotificationSettingsValues {
    val preferences = koinInject<NotificationPreferencesRepository>()
    val queue by preferences.queue.collectAsState()
    val prayerLocation by preferences.prayerLocation.collectAsState()
    val presentation by preferences.presentation.collectAsState()
    val audio by preferences.audio.collectAsState()
    val prayerFlags by preferences.prayerFlags.collectAsState()
    return NotificationSettingsValues(queue, prayerLocation, presentation, audio, prayerFlags)
}

@AzkarVariantsPreview
@Composable
internal fun SettingsScreenPreview() {
    SettingsScreenPreviewContent(SettingsGroup.GENERAL)
}

@AzkarVariantsPreview
@Composable
internal fun SettingsScreenRemindersPreview() {
    SettingsScreenPreviewContent(SettingsGroup.REMINDERS)
}

@AzkarVariantsPreview
@Composable
internal fun SettingsScreenPrayerPreview() {
    SettingsScreenPreviewContent(SettingsGroup.PRAYER)
}

@Composable
private fun SettingsScreenPreviewContent(group: SettingsGroup) {
    val locale = LocalUiLocale.current
    val scope = rememberCoroutineScope()
    val store = rememberPreviewSettingsStore()
    val preferences = remember(store) { NotificationPreferencesRepository(store) }
    val httpClient = remember { HttpClient() }
    DisposableEffect(httpClient) {
        onDispose { httpClient.close() }
    }
    val updateManager = remember(store, scope, httpClient) { UpdateManager(store, httpClient, scope) }
    SeedSettingsPreview(preferences, scope)
    val notificationState = remember(scope, preferences) { NotificationSettingsState(scope, preferences) }
    val queue by preferences.queue.collectAsState()
    val prayerLocation by preferences.prayerLocation.collectAsState()
    val presentation by preferences.presentation.collectAsState()
    val prayerFlags by preferences.prayerFlags.collectAsState()
    SettingsScreenContent(
        inputs =
            SettingsScreenInputs(
                state = previewAzkarAppState(locale = locale),
                onClose = {},
                initialGroup = group,
                initialSection = null,
                sectionRequest = 0,
                notificationState = notificationState,
                notificationValues =
                    NotificationSettingsValues(
                        queue,
                        prayerLocation,
                        presentation,
                        preferences.audio.value,
                        prayerFlags,
                    ),
                audioPlayer = remember { DesktopAudioPlayer() },
                updateManager = updateManager,
            ),
        modifier = Modifier.fillMaxSize(),
    )
}

@Composable
private fun SeedSettingsPreview(
    preferences: NotificationPreferencesRepository,
    scope: CoroutineScope,
) {
    LaunchedEffect(preferences, scope) {
        preferences.updateQueue {
            it.copy(
                enabled = true,
                mode = QueueMode.MANUAL,
                cadence = QueueCadence.INTERVAL,
                collectionIds = listOf(1L, 2L),
                activeDays = setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY),
                intervalMinutes = 30,
                windowStartMinute = 9 * 60,
                windowEndMinute = 21 * 60,
            )
        }
        preferences.savePrayerLocation(
            PrayerLocationSettings(
                latitude = 31.95,
                longitude = 35.91,
                city = "Amman, Jordan",
                zoneId = "Asia/Amman",
                method = PrayerCalculationMethod.MUSLIM_WORLD_LEAGUE,
                autoDetect = false,
            ),
        )
        preferences.observe(scope)
    }
}

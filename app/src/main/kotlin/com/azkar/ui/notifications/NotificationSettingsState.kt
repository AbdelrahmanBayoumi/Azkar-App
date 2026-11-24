package com.azkar.ui.notifications

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.azkar.ui.notifications.presentation.OverlayCardSize
import com.azkar.ui.notifications.presentation.OverlayCardSizing
import com.azkar.ui.notifications.presentation.OverlayDurationMode
import com.azkar.ui.notifications.presentation.OverlayPlacement
import com.azkar.util.runCatchingCancellable
import dev.nucleusframework.autolaunch.AutoLaunchResult
import dev.nucleusframework.autolaunch.AutoLaunchState
import io.github.oshai.kotlinlogging.KLogger
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.ZoneId
import java.util.Locale
import kotlin.time.Duration.Companion.milliseconds

/**
 * Transient state for the notification and prayer settings that live on the settings screen.
 *
 * These used to sit on `ReminderManagerState` alongside the queue, because a single screen owned all
 * three sets of settings. They do not any more: prayer *times* are toggled from the home screen's
 * prayer card, and the location that calculator reads and the way a reminder is drawn over other
 * windows are both preferences rather than anything about a queue. So this class holds the four
 * location fields and the two presentations, and the queue's state holder no longer has to know what
 * a time zone is.
 *
 * Persisted values stay on [NotificationPreferencesRepository]'s `StateFlow`s and are read for drawing
 * with `collectAsState`; only the editable copies — the half-typed coordinates, the last failure —
 * live here. This class is the only writer, so every mutation goes through one `scope.launch` and one
 * repository call instead of being scattered across the body of a composable where a `LaunchedEffect`
 * could re-run it.
 *
 * Each of the two editors keeps its own notice type, so the prayer tab cannot render a startup
 * failure and the notifications tab cannot render a rejected coordinate. That is not cosmetic: the
 * two tabs are on different screens' worth of attention, and the old screen's single shared error
 * string put a login-manager complaint at the bottom of the prayer location card.
 */
@Stable
class NotificationSettingsState(
    private val scope: CoroutineScope,
    private val preferences: NotificationPreferencesRepository,
    /**
     * The IP lookup behind "Auto-detect", when the host can offer one.
     *
     * Nullable rather than required because it is a capability the surrounding platform supplies — a
     * network client — and a host that has none (a `@Preview`, a test of the presentation settings)
     * still has to be able to draw every other row on the screen. A missing one produces the same
     * notice a refused lookup does, so the button reports the truth instead of being hidden or
     * silently doing nothing.
     */
    private val locationDetector: LocationDetector? = null,
    /** The desktop login manager, when there is one. See [locationDetector] on nullability. */
    private val startupRegistration: StartupRegistration? = null,
    /** Place search and reverse geocoding behind the picker's search box. See [locationDetector]. */
    private val geocodingClient: GeocodingClient? = null,
    private val logger: KLogger = KotlinLogging.logger {},
) {
    /** The location editor's working copy of the four fields. */
    var locationDraft: PrayerLocationDraft by mutableStateOf(PrayerLocationDraft.of(preferences.prayerLocation.value))
        private set

    /** Writes for the prayer tab's location card. */
    val prayer = PrayerEditor()

    /** Writes for the notifications tab's overlay-appearance and startup cards. */
    val notifications = NotificationEditor()

    /** Writes the independent desktop audio preferences. */
    fun updateAudio(transform: (AudioPreferences) -> AudioPreferences) {
        scope.launch { preferences.updateAudio(transform) }
    }

    fun setAllPrayerSoundsEnabled(enabled: Boolean) {
        scope.launch { preferences.setAllPrayerSoundsEnabled(enabled) }
    }

    /**
     * Writes for the reminder schedule, the card at the top of the notifications tab.
     *
     * The same instance type the reminders screen uses, over the same repository singleton, so there
     * is one queue and two places to set it up. Both screens construct their own rather than sharing
     * one because each has its own composition scope to launch from; the editor holds nothing, so two
     * of them cannot disagree about what is stored.
     */
    val schedule = ReminderScheduleEditor(scope = scope, preferences = preferences)

    /**
     * Writes for the prayer tab.
     *
     * An inner class rather than four methods on [NotificationSettingsState] so that "these belong to
     * the prayer card" is a fact about the type rather than about the order somebody happened to
     * write the methods in. It holds no state of its own — every method writes through the same
     * [preferences] singleton — so switching tabs and dropping it loses nothing.
     */
    @Stable
    inner class PrayerEditor {
        /** The last location failure or confirmation, or null. See [LocationNotice]. */
        var notice: LocationNotice? by mutableStateOf(null)
            private set

        /** What has been typed into the picker's search box. */
        var searchQuery: String by mutableStateOf("")
            private set

        /** The places the last search matched, drawn as the suggestion list. */
        var suggestions: List<PrayerPlace> by mutableStateOf(emptyList())
            private set

        /** True while a search is in flight, so the field can say so. */
        var isSearching: Boolean by mutableStateOf(false)
            private set

        /**
         * The pending debounced search.
         *
         * Held so a keystroke can cancel the one before it: the search box fires on every character,
         * and firing a request per character would both hammer a free endpoint and let an early,
         * slower answer land after a later one and replace the list with stale results.
         */
        private var searchDebounce: Job? = null

        /**
         * Edits the four fields of the location editor.
         *
         * A transform rather than four setters so the draft is replaced as one value: half-typed
         * coordinates are read as a whole by [saveLocation], and letting them be three independent
         * writes would mean the validation below ran against a mix of the old and the new text.
         */
        fun editDraft(transform: (PrayerLocationDraft) -> PrayerLocationDraft) {
            locationDraft = transform(locationDraft)
        }

        /**
         * Validates the draft and writes it.
         *
         * The zone id is checked against [ZoneId] before anything is written, because
         * [PrayerTimeCalculator] reads it on every tick to build its times table — a bad id saved here
         * takes the whole runtime's prayer pass down, and it does so twenty seconds later, somewhere
         * with no link back to the field that was typed into. The coordinates get the same treatment
         * only because nothing downstream can reject them: they are stored as doubles, and a latitude
         * of 400 would be read happily by every calculator that sees it.
         */
        fun saveLocation() {
            val draft = locationDraft
            val latitude = draft.latitude.trim().toDoubleOrNull()
            val longitude = draft.longitude.trim().toDoubleOrNull()
            if (latitude == null || longitude == null) {
                notice = LocationNotice.InvalidCoordinates
                return
            }
            if (runCatching { ZoneId.of(draft.zoneId.trim()) }.isFailure) {
                notice = LocationNotice.InvalidTimeZone
                return
            }
            val candidate =
                preferences.prayerLocation.value.copy(
                    latitude = latitude,
                    longitude = longitude,
                    city = draft.city.trim(),
                    zoneId = draft.zoneId.trim(),
                    autoDetect = false,
                )
            scope.launch {
                preferences.savePrayerLocation(candidate)
                locationDraft = PrayerLocationDraft.of(preferences.prayerLocation.value)
                notice = LocationNotice.Saved
            }
        }

        /**
         * Switches the calculation method, and nothing else.
         *
         * A dropdown rather than a field because the five values are a closed set whose angles are the
         * whole difference between them — [PrayerCalculationMethod] exists precisely so the numbers do
         * not have to be typed in either.
         *
         * The method is written straight to the stored location rather than being held on the draft,
         * because it is not part of what "Save location" means: it is valid for every city, so holding
         * it behind a Save button that also writes coordinates would let a user pick a method, switch
         * tabs without saving, and lose the choice.
         */
        fun setMethod(method: PrayerCalculationMethod) {
            scope.launch {
                preferences.savePrayerLocation(preferences.prayerLocation.value.copy(method = method))
            }
        }

        /**
         * Fills the draft from IP geolocation and saves it.
         *
         * A network lookup inside a settings screen is the one thing here that can take seconds or fail
         * outright, so its three outcomes — found, refused, and no host capability at all — are all
         * first-class. The old inline `runCatching` collapsed the last two into one message that named
         * the failure in English inside a Kotlin file, which is how a Georgian error message ended up
         * next to an Arabic interface.
         *
         * The calculation method is carried across from what is stored rather than taken from the
         * detector's own default: a user who picked a method deliberately should not have it silently
         * reset by pressing the button that only meant to find their city.
         */
        fun detectLocation() {
            scope.launch {
                val detector =
                    locationDetector ?: run {
                        notice = LocationNotice.Unavailable(reason = null)
                        return@launch
                    }
                runCatchingCancellable { detector.detect() }
                    .onSuccess { detected ->
                        val updated = detected.copy(method = preferences.prayerLocation.value.method)
                        preferences.savePrayerLocation(updated)
                        locationDraft = PrayerLocationDraft.of(preferences.prayerLocation.value)
                        notice = LocationNotice.Detected(updated.city)
                    }.onFailure { failure ->
                        logger.error(failure) { "Unable to detect the prayer-time location" }
                        notice = LocationNotice.Unavailable(failure.message)
                    }
            }
        }

        /**
         * Records what the user is typing and, after a pause, searches for it.
         *
         * The pause is the whole point: a search fires on every character, so without the debounce a
         * request would leave for each of the eight letters of "New York" and seven of them would be
         * thrown away. An empty query cancels the pending search and clears the list rather than
         * searching for nothing.
         *
         * Named `update…` rather than `setSearchQuery` for the same reason as the reminder manager's:
         * a `fun setX(x: T)` beside a `var x: T` collides with the JVM setter the property generates.
         */
        fun updateSearchQuery(value: String) {
            searchQuery = value
            searchDebounce?.cancel()
            val query = value.trim()
            if (query.isEmpty()) {
                suggestions = emptyList()
                isSearching = false
                return
            }
            searchDebounce =
                scope.launch {
                    delay(SEARCH_DEBOUNCE_MILLIS.milliseconds)
                    runSearch(query)
                }
        }

        private suspend fun runSearch(query: String) {
            val client =
                geocodingClient ?: run {
                    isSearching = false
                    notice = LocationNotice.SearchFailed(reason = null)
                    return
                }
            isSearching = true
            runCatchingCancellable { client.searchPlaces(query, searchLanguage()) }
                .onSuccess { results ->
                    // A slower early answer must not overwrite a later query's list.
                    if (searchQuery.trim() != query) return@onSuccess
                    suggestions = results
                    notice = if (results.isEmpty()) LocationNotice.NoResults else null
                }.onFailure { failure ->
                    logger.warn(failure) { "Unable to search for places" }
                    suggestions = emptyList()
                    notice = LocationNotice.SearchFailed(failure.message)
                }
            isSearching = false
        }

        /**
         * Fills the location from a picked suggestion and writes it.
         *
         * A pick is a finished answer — the search box and the list are put back to rest, the four
         * fields are filled from the one place the suggestion carried, and the save is immediate
         * because the user has already chosen. The calculation method is left alone, as everywhere
         * else: choosing a city says nothing about which angles to use.
         */
        fun selectPlace(place: PrayerPlace) {
            searchDebounce?.cancel()
            searchQuery = place.label
            suggestions = emptyList()
            isSearching = false
            commit(
                latitude = place.latitude,
                longitude = place.longitude,
                city = place.label,
                zoneId = place.zoneId ?: preferences.prayerLocation.value.zoneId,
                saved = { LocationNotice.PlaceSelected(place.label) },
            )
        }

        /**
         * Writes a resolved location, then reports it.
         *
         * The one path every non-typed write goes through, so a picked suggestion, a dropped pin and
         * a detected city cannot disagree about validation or about re-seeding the draft. The zone is
         * still checked against [ZoneId] even though a provider gave it, because a provider is
         * exactly the kind of thing that can hand back a renamed or invented id.
         */
        private fun commit(
            latitude: Double,
            longitude: Double,
            city: String,
            zoneId: String,
            saved: () -> LocationNotice,
        ) {
            if (runCatching { ZoneId.of(zoneId) }.isFailure) {
                notice = LocationNotice.InvalidTimeZone
                return
            }
            val candidate =
                preferences.prayerLocation.value.copy(
                    latitude = latitude,
                    longitude = longitude,
                    city = city,
                    zoneId = zoneId,
                    autoDetect = false,
                )
            scope.launch {
                preferences.savePrayerLocation(candidate)
                locationDraft = PrayerLocationDraft.of(preferences.prayerLocation.value)
                notice = saved()
            }
        }

        /**
         * The language the geocoder should name places in.
         *
         * The process default, which the app's locale store sets when the interface language changes,
         * so an Arabic interface searches in Arabic. Blank (a JVM with no default locale) falls back
         * to English rather than sending an empty `language` parameter.
         */
        private fun searchLanguage(): String = Locale.getDefault().language.ifBlank { "en" }
    }

    /**
     * Writes for the notifications tab.
     *
     * Split from [PrayerEditor] for the same reason, and additionally because the startup registration
     * is the one write here that talks to the platform: keeping it in its own namespace keeps
     * `StartupNotice` reachable from exactly one place.
     */
    @Stable
    inner class NotificationEditor {
        /** The last login-manager failure, or null. See [StartupNotice]. */
        var notice: StartupNotice? by mutableStateOf(null)
            private set

        private var startupState: AutoLaunchState? by mutableStateOf(null)

        var isUpdatingStartup: Boolean by mutableStateOf(false)
            private set

        val startOnLogin: Boolean get() = startupState?.isStartupEnabled == true

        val canChangeStartup: Boolean
            get() = !isUpdatingStartup && startupState in setOf(AutoLaunchState.ENABLED, AutoLaunchState.DISABLED)

        private val unsupportedStartupNotice: StartupNotice
            get() =
                if (startupRegistration?.isDevelopmentBuild == true) {
                    StartupNotice.DevelopmentBuild
                } else {
                    StartupNotice.Unsupported
                }

        /** Re-read after returning from system settings; the OS owns this setting. */
        suspend fun refreshStartOnLogin() {
            if (isUpdatingStartup) return
            isUpdatingStartup = true
            try {
                val registration = startupRegistration
                if (registration == null) {
                    showStartupState(AutoLaunchState.UNSUPPORTED)
                } else {
                    registration
                        .state()
                        .onSuccess(::showStartupState)
                        .onFailure {
                            startupState = null
                            notice = StartupNotice.Unavailable(it.message)
                        }
                }
            } finally {
                isUpdatingStartup = false
            }
        }

        private fun showStartupState(state: AutoLaunchState) {
            startupState = state
            notice =
                when (state) {
                    AutoLaunchState.DISABLED_BY_USER -> StartupNotice.DisabledByUser
                    AutoLaunchState.DISABLED_BY_POLICY,
                    AutoLaunchState.ENABLED_BY_POLICY,
                    -> StartupNotice.ManagedByPolicy
                    AutoLaunchState.UNSUPPORTED -> unsupportedStartupNotice
                    else -> null
                }
        }

        fun setOverlayDuration(seconds: Int) {
            scope.launch {
                preferences.updatePresentation {
                    it.copy(
                        durationSeconds = seconds.coerceIn(MIN_DURATION, MAX_DURATION),
                    )
                }
            }
        }

        /**
         * Sets how long a card stays, but touches neither the stored seconds nor the reading model.
         *
         * A single write on purpose: when the mode returns to [OverlayDurationMode.FIXED], the card
         * should be shown for exactly the number that was last changed by hand, not for whatever the
         * reading estimate happened to produce for the reminder that was on screen when the reader
         * switched away from it.
         */
        fun setDurationMode(mode: OverlayDurationMode) {
            scope.launch { preferences.updatePresentation { it.copy(durationMode = mode) } }
        }

        fun setOverlayStackSize(count: Int) {
            scope.launch { preferences.updatePresentation { it.copy(maxVisible = count.coerceIn(1, 5)) } }
        }

        fun setPlacement(placement: OverlayPlacement) {
            scope.launch { preferences.updatePresentation { it.copy(placement = placement) } }
        }

        /**
         * Chooses the language the reminder text is written in, or blank for "the app's own".
         *
         * Stored as a tag rather than as a parsed [java.util.Locale] because the tag is what survives a
         * round trip through DataStore and what the settings model already models; the runtime is the
         * only place that turns it back into a language, through
         * [com.azkar.ui.notifications.presentation.NotificationPresentationSettings.notificationLocale].
         */
        fun setNotificationLanguage(languageTag: String) {
            scope.launch { preferences.updatePresentation { it.copy(notificationLanguageTag = languageTag) } }
        }

        /**
         * Jumps both dimensions to one of the three named sizes at once.
         *
         * One write rather than two on purpose. Width and height are stored as a pair, and a preset is
         * a *pair* of answers; writing them separately would let a second write be dropped if the
         * screen left composition in between, and the result would be a comfortable height on a
         * compact width with nothing on screen to say so.
         */
        fun setCardSize(preset: OverlayCardSize) {
            scope.launch {
                preferences.updatePresentation {
                    it.copy(cardWidthDp = preset.widthDp, cardHeightDp = preset.heightDp)
                }
            }
        }

        /**
         * Nudges one dimension, leaving the other where it was.
         *
         * Separate from [setCardSize] because a slider reports a continuous value on every frame of a
         * drag: the point of the pair is that a slider is fine adjustment *around* a preset, and a
         * whole-pair write would yank the untouched dimension back to its preset on the first pixel
         * of a drag.
         *
         * Clamped rather than rejected for the same reason [setOverlayDuration] clamps — a slider
         * stops at its own bounds, so anything outside the range arriving here means the two ended up
         * out of step, and dropping the value keeps the stored pair legal either way.
         */
        fun setCardWidthDp(widthDp: Int) {
            scope.launch {
                preferences.updatePresentation {
                    it.copy(cardWidthDp = widthDp.coerceIn(MIN_CARD_WIDTH_DP, MAX_CARD_WIDTH_DP))
                }
            }
        }

        fun setCardHeightDp(heightDp: Int) {
            scope.launch {
                preferences.updatePresentation {
                    it.copy(cardHeightDp = heightDp.coerceIn(MIN_CARD_HEIGHT_DP, MAX_CARD_HEIGHT_DP))
                }
            }
        }

        /**
         * Chooses whether a card follows its passage or the two dimensions above.
         *
         * A single write, and one that deliberately leaves both stored dimensions alone: the height the
         * reader set under a fixed slot is the height a fitted card falls back to wherever its content
         * cannot be measured, so switching modes must not throw it away. A reader who fits their cards,
         * misses the steadier slot and switches back finds the size they last chose rather than a
         * default they never picked.
         */
        fun setCardSizing(sizing: OverlayCardSizing) {
            scope.launch { preferences.updatePresentation { it.copy(cardSizing = sizing) } }
        }

        /**
         * Registers or unregisters the desktop launcher, then records the preference.
         *
         * The order matters and is the reason this is not a bare toggle: the stored preference is
         * only written once the platform call has actually succeeded. Writing it first — or ignoring
         * the result — is how a switch ends up reading "on" for a machine where the registration
         * failed and nothing happens at the next login.
         */
        fun setStartOnLogin(enabled: Boolean) {
            if (isUpdatingStartup) return
            isUpdatingStartup = true
            scope.launch {
                try {
                    val registration =
                        startupRegistration ?: run {
                            showStartupState(AutoLaunchState.UNSUPPORTED)
                            return@launch
                        }
                    registration
                        .setEnabled(enabled)
                        .onSuccess { state ->
                            showStartupState(state)
                            preferences.setStartOnLogin(state.isStartupEnabled)
                        }.onFailure { failure ->
                            registration.state().onSuccess { startupState = it }
                            logger.error(failure) { "Unable to register Azkar to start on login" }
                            notice =
                                when ((failure as? StartupRegistrationException)?.result) {
                                    AutoLaunchResult.BLOCKED_BY_USER -> StartupNotice.DisabledByUser
                                    AutoLaunchResult.BLOCKED_BY_POLICY -> StartupNotice.ManagedByPolicy
                                    AutoLaunchResult.UNSUPPORTED -> unsupportedStartupNotice
                                    else -> StartupNotice.Unavailable(failure.message)
                                }
                        }
                } finally {
                    isUpdatingStartup = false
                }
            }
        }

        fun openStartupSystemSettings() {
            scope.launch {
                if (startupRegistration?.openSystemSettings() != true) {
                    notice = StartupNotice.Unavailable(reason = null)
                }
            }
        }
    }

    private companion object {
        /**
         * The bounds the overlay duration is clamped to.
         *
         * Duplicated from [com.azkar.ui.notifications.presentation.NotificationPresentationSettings]'s own
         * `require`, because `require` throws and a stepper must not be able to hand the settings
         * something that throws. Anything outside the range is a bug in the stepper, not a user error.
         */
        const val MIN_DURATION = 3
        const val MAX_DURATION = 120

        /**
         * The bounds a card dimension is clamped to, for the same reason as [MIN_DURATION] above.
         *
         * These have to be the same numbers as
         * [com.azkar.ui.notifications.presentation.NotificationPresentationSettings]'s own `require`,
         * and they are duplicated rather than shared because that class keeps its bounds file-private
         * for the sake of its generated serializer. A slider that could hand the settings a width it
         * would refuse to construct would throw on the *read* side of the next decode, which is a
         * far worse place to find out.
         */
        const val MIN_CARD_WIDTH_DP = 300
        const val MAX_CARD_WIDTH_DP = 700
        const val MIN_CARD_HEIGHT_DP = 120
        const val MAX_CARD_HEIGHT_DP = 400

        /**
         * How long typing must pause before the place search fires.
         *
         * Long enough that a word is one request rather than one per letter, short enough that the
         * list appears to keep up with a person. The same order of magnitude as the library's own
         * search debounce, deliberately, so the two search boxes in the app feel the same.
         */
        const val SEARCH_DEBOUNCE_MILLIS = 300L
    }
}

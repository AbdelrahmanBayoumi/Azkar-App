package com.azkar.ui.notifications

import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.azkar.settings.SettingsStore
import com.azkar.ui.home.PrayerKey
import com.azkar.ui.notifications.presentation.NotificationPresentationSettings
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import java.io.IOException

internal enum class GnomePrayerDisplay {
    EXTENSION,
    PANEL,
    TRAY,
}

internal enum class PrayerMenuLayout {
    COMPACT,
    EXTENDED,
}

internal enum class PrayerMenuSurface(
    val defaultLayout: PrayerMenuLayout,
) {
    PANEL(PrayerMenuLayout.EXTENDED),
    TRAY(PrayerMenuLayout.COMPACT),
}

/** Owns the persisted settings shared by the manager UI and the process notification service. */
class NotificationPreferencesRepository(
    private val settings: SettingsStore,
    private val installAudioDefaults: suspend () -> Pair<String, String> = {
        BundledAudioDefaults.install(ioDispatcher = AudioIoDispatcher.default).let { it.notification to it.prayer }
    },
) {
    private val json = Json { ignoreUnknownKeys = true }
    private val mutex = Mutex()
    private val _queue = MutableStateFlow(ReminderQueueConfig())
    private val _queueProgress = MutableStateFlow(ReminderQueueProgress())
    private val _prayerLocation = MutableStateFlow(PrayerLocationSettings())
    private val windowsTaskbarPositionState = MutableStateFlow<Int?>(null)
    private val prayerPanelMenuLayoutState = MutableStateFlow(PrayerMenuSurface.PANEL.defaultLayout)
    private val prayerTrayMenuLayoutState = MutableStateFlow(PrayerMenuSurface.TRAY.defaultLayout)
    private val gnomePrayerDisplayState = MutableStateFlow(GnomePrayerDisplay.EXTENSION)
    private val _desktopPrayerEnabled = MutableStateFlow(DEFAULT_DESKTOP_PRAYER_ENABLED)
    private val _prayerProgress = MutableStateFlow(PrayerNotificationProgress())
    private val _prayerFlags = MutableStateFlow(PrayerKey.entries.associateWith { true })
    private val _startOnLogin = MutableStateFlow(false)
    private val _presentation = MutableStateFlow(NotificationPresentationSettings())
    private val _audio = MutableStateFlow(AudioPreferences())

    val queue: StateFlow<ReminderQueueConfig> = _queue.asStateFlow()
    val queueProgress: StateFlow<ReminderQueueProgress> = _queueProgress.asStateFlow()
    val prayerLocation: StateFlow<PrayerLocationSettings> = _prayerLocation.asStateFlow()
    internal val windowsTaskbarPosition = windowsTaskbarPositionState.asStateFlow()
    internal val prayerPanelMenuLayout = prayerPanelMenuLayoutState.asStateFlow()
    internal val prayerTrayMenuLayout = prayerTrayMenuLayoutState.asStateFlow()
    internal val gnomePrayerDisplay = gnomePrayerDisplayState.asStateFlow()
    val desktopPrayerEnabled: StateFlow<Boolean> = _desktopPrayerEnabled.asStateFlow()
    val prayerProgress: StateFlow<PrayerNotificationProgress> = _prayerProgress.asStateFlow()
    val prayerFlags: StateFlow<Map<PrayerKey, Boolean>> = _prayerFlags.asStateFlow()

    /** Last successful choice, retained for migrating old launch files; the UI reads the OS state. */
    val startOnLogin: StateFlow<Boolean> = _startOnLogin.asStateFlow()

    /**
     * How fired reminders are shown, as opposed to when they are due.
     *
     * Its own key rather than a field on [queue], so that changing a notification's duration cannot
     * rewrite the queue document and so that the manager screen's presentation controls and the
     * manager screen's schedule controls can be edited independently without either one clobbering
     * the other's pending write.
     */
    val presentation: StateFlow<NotificationPresentationSettings> = _presentation.asStateFlow()
    val audio: StateFlow<AudioPreferences> = _audio.asStateFlow()

    /** Begins following the DataStore once; safe to call from both the process service and a screen. */
    fun observe(scope: CoroutineScope) {
        scope.launch {
            settings.changes.collect { stored ->
                _queue.value = json.decodePreference(stored[QUEUE_KEY], ReminderQueueConfig())
                _queueProgress.value = json.decodePreference(stored[QUEUE_PROGRESS_KEY], ReminderQueueProgress())
                _prayerLocation.value = json.decodePreference(stored[PRAYER_LOCATION_KEY], PrayerLocationSettings())
                _desktopPrayerEnabled.value = stored[DESKTOP_PRAYER_KEY] ?: DEFAULT_DESKTOP_PRAYER_ENABLED
                gnomePrayerDisplayState.value = stored.gnomePrayerDisplay()
                prayerPanelMenuLayoutState.value = stored.prayerMenuLayout(PrayerMenuSurface.PANEL)
                prayerTrayMenuLayoutState.value = stored.prayerMenuLayout(PrayerMenuSurface.TRAY)
                windowsTaskbarPositionState.value = stored[WINDOWS_TASKBAR_POSITION_KEY]?.takeIf { it in 0..100 }
                _prayerProgress.value = json.decodePreference(stored[PRAYER_PROGRESS_KEY], PrayerNotificationProgress())
                _prayerFlags.value = PrayerKey.entries.associateWith { key -> stored[prayerKey(key)] ?: true }
                _startOnLogin.value = stored[START_ON_LOGIN_KEY] ?: false
                _presentation.value =
                    json.decodePreference(stored[PRESENTATION_KEY], NotificationPresentationSettings())
                loadAudioPreferences(stored[AUDIO_KEY])
            }
        }
    }

    suspend fun refresh() {
        val stored = settings.changes.first()
        _queue.value = json.decodePreference(stored[QUEUE_KEY], ReminderQueueConfig())
        _queueProgress.value = json.decodePreference(stored[QUEUE_PROGRESS_KEY], ReminderQueueProgress())
        _prayerLocation.value = json.decodePreference(stored[PRAYER_LOCATION_KEY], PrayerLocationSettings())
        _desktopPrayerEnabled.value = stored[DESKTOP_PRAYER_KEY] ?: DEFAULT_DESKTOP_PRAYER_ENABLED
        gnomePrayerDisplayState.value = stored.gnomePrayerDisplay()
        prayerPanelMenuLayoutState.value = stored.prayerMenuLayout(PrayerMenuSurface.PANEL)
        prayerTrayMenuLayoutState.value = stored.prayerMenuLayout(PrayerMenuSurface.TRAY)
        windowsTaskbarPositionState.value = stored[WINDOWS_TASKBAR_POSITION_KEY]?.takeIf { it in 0..100 }
        _prayerProgress.value = json.decodePreference(stored[PRAYER_PROGRESS_KEY], PrayerNotificationProgress())
        _prayerFlags.value = PrayerKey.entries.associateWith { key -> stored[prayerKey(key)] ?: true }
        _startOnLogin.value = stored[START_ON_LOGIN_KEY] ?: false
        _presentation.value = json.decodePreference(stored[PRESENTATION_KEY], NotificationPresentationSettings())
        loadAudioPreferences(stored[AUDIO_KEY])
    }

    suspend fun updateQueue(transform: (ReminderQueueConfig) -> ReminderQueueConfig) =
        mutex.withLock {
            val next = transform(_queue.value).validated()
            _queue.value = next
            settings.edit { this[QUEUE_KEY] = json.encodeToString(next) }
        }

    /** Seeds the shipped reminder sources only when this installation has no saved queue yet. */
    suspend fun initializeQueueDefaults(collectionIds: suspend () -> List<Long>) =
        mutex.withLock {
            if (settings.read(QUEUE_KEY) != null) return@withLock
            val next = ReminderQueueConfig(collectionIds = collectionIds().distinct().filter { it > 0 })
            _queue.value = next
            settings.edit { this[QUEUE_KEY] = json.encodeToString(next) }
        }

    /**
     * Adds a collection to the queue's sources, or takes it out.
     *
     * Reads the *stored* document rather than the in-memory copy, and does it under the same lock
     * as [updateQueue], because this is the one write that can arrive before the first read has
     * landed: a user can open the library and click "add to reminders" before the settings file has
     * been read even once. Transforming the in-memory copy then would write a queue that had lost
     * every source the user already had.
     */
    suspend fun setQueueCollectionEnabled(
        collectionId: Long,
        enabled: Boolean,
    ) = mutex.withLock {
        val stored = json.decodePreference(settings.read(QUEUE_KEY), ReminderQueueConfig())
        val next =
            stored
                .copy(
                    collectionIds =
                        if (enabled) {
                            (stored.collectionIds + collectionId).distinct()
                        } else {
                            stored.collectionIds - collectionId
                        },
                ).validated()
        _queue.value = next
        settings.edit { this[QUEUE_KEY] = json.encodeToString(next) }
    }

    suspend fun saveQueueProgress(progress: ReminderQueueProgress) =
        mutex.withLock {
            _queueProgress.value = progress
            settings.edit { this[QUEUE_PROGRESS_KEY] = json.encodeToString(progress) }
        }

    suspend fun savePrayerLocation(location: PrayerLocationSettings) =
        mutex.withLock {
            _prayerLocation.value = location
            settings.edit { this[PRAYER_LOCATION_KEY] = json.encodeToString(location) }
        }

    suspend fun savePrayerProgress(progress: PrayerNotificationProgress) =
        mutex.withLock {
            _prayerProgress.value = progress
            settings.edit { this[PRAYER_PROGRESS_KEY] = json.encodeToString(progress) }
        }

    suspend fun setPrayerEnabled(
        key: PrayerKey,
        enabled: Boolean,
    ) {
        _prayerFlags.value += (key to enabled)
        settings.edit { this[prayerKey(key)] = enabled }
    }

    internal suspend fun setWindowsTaskbarPosition(position: Int?) {
        settings.edit {
            if (position == null) {
                remove(WINDOWS_TASKBAR_POSITION_KEY)
            } else {
                this[WINDOWS_TASKBAR_POSITION_KEY] = position.coerceIn(0, 100)
            }
        }
    }

    internal suspend fun setPrayerMenuLayout(
        surface: PrayerMenuSurface,
        layout: PrayerMenuLayout,
    ) {
        settings.edit { this[surface.extendedMenuKey] = layout == PrayerMenuLayout.EXTENDED }
    }

    internal suspend fun setGnomePrayerDisplay(display: GnomePrayerDisplay) {
        settings.edit {
            this[DESKTOP_PRAYER_KEY] = display != GnomePrayerDisplay.TRAY
            if (display != GnomePrayerDisplay.TRAY) {
                this[GNOME_PRAYER_EXTENSION_KEY] = display == GnomePrayerDisplay.EXTENSION
            }
        }
    }

    suspend fun setDesktopPrayerEnabled(enabled: Boolean) {
        settings.edit { this[DESKTOP_PRAYER_KEY] = enabled }
    }

    suspend fun updatePresentation(transform: (NotificationPresentationSettings) -> NotificationPresentationSettings) =
        mutex.withLock {
            val next = transform(_presentation.value).validated()
            _presentation.value = next
            settings.edit { this[PRESENTATION_KEY] = json.encodeToString(next) }
        }

    suspend fun updateAudio(transform: (AudioPreferences) -> AudioPreferences) =
        mutex.withLock {
            val current = json.decodePreference(settings.read(AUDIO_KEY), _audio.value).validated()
            val next = transform(current).validated()
            _audio.value = next
            settings.edit { this[AUDIO_KEY] = json.encodeToString(next) }
        }

    suspend fun setPrayerSoundEnabled(
        key: PrayerKey,
        enabled: Boolean,
    ) = updateAudio { current ->
        val sounds = current.prayerSoundsEnabled + (key.id to enabled)
        current.copy(prayerEnabled = sounds.values.any { it }, prayerSoundsEnabled = sounds)
    }

    suspend fun setAllPrayerSoundsEnabled(enabled: Boolean) =
        updateAudio {
            it.copy(
                prayerEnabled = enabled,
                prayerSoundsEnabled = PrayerKey.entries.associate { key -> key.id to enabled },
            )
        }

    suspend fun setStartOnLogin(enabled: Boolean) =
        mutex.withLock {
            _startOnLogin.value = enabled
            settings.edit { this[START_ON_LOGIN_KEY] = enabled }
        }

    private suspend fun loadAudioPreferences(serialized: String?) {
        val stored = json.decodePreference(serialized, AudioPreferences())
        val defaults =
            if (stored.notificationFile.isBlank() || stored.prayerFile.isBlank()) {
                try {
                    installAudioDefaults()
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (failure: IOException) {
                    logAudioDefaultFailure(failure)
                    null
                } catch (failure: IllegalStateException) {
                    logAudioDefaultFailure(failure)
                    null
                } catch (failure: SecurityException) {
                    logAudioDefaultFailure(failure)
                    null
                }
            } else {
                null
            }
        mutex.withLock {
            val current = json.decodePreference(settings.read(AUDIO_KEY), AudioPreferences())
            val resolved =
                current
                    .copy(
                        notificationFile = current.notificationFile.ifBlank { defaults?.first.orEmpty() },
                        prayerFile = current.prayerFile.ifBlank { defaults?.second.orEmpty() },
                    ).validated()
            _audio.value = resolved
            if (resolved != current) {
                settings.edit { this[AUDIO_KEY] = json.encodeToString(resolved) }
            }
        }
    }

    private fun logAudioDefaultFailure(failure: Exception) {
        val audioDirectory = SettingsStore.appDataDirectory().resolve("audio")
        logger.error(failure) {
            "Unable to install bundled audio defaults into '$audioDirectory'; " +
                "audio paths will remain unset until configured"
        }
    }

    private companion object {
        const val DEFAULT_DESKTOP_PRAYER_ENABLED = true
        val logger = KotlinLogging.logger {}
        val QUEUE_KEY = stringPreferencesKey("notifications.queue.v1")
        val QUEUE_PROGRESS_KEY = stringPreferencesKey("notifications.queue.progress.v1")
        val PRAYER_LOCATION_KEY = stringPreferencesKey("prayer.location.v1")
        val WINDOWS_TASKBAR_POSITION_KEY = intPreferencesKey("prayer.desktop.windows.position")
        val PRAYER_PROGRESS_KEY = stringPreferencesKey("prayer.notification.progress.v1")
        val START_ON_LOGIN_KEY = booleanPreferencesKey("notifications.startOnLogin.enabled")
        val PRESENTATION_KEY = stringPreferencesKey("notifications.presentation.v1")
        val AUDIO_KEY = stringPreferencesKey("audio.preferences.v1")
    }
}

private val PRAYER_PANEL_EXTENDED_MENU_KEY = booleanPreferencesKey("prayer.desktop.extended.menu")
private val PRAYER_TRAY_EXTENDED_MENU_KEY = booleanPreferencesKey("prayer.desktop.tray.extended.menu")

private val PrayerMenuSurface.extendedMenuKey
    get() =
        when (this) {
            PrayerMenuSurface.PANEL -> PRAYER_PANEL_EXTENDED_MENU_KEY
            PrayerMenuSurface.TRAY -> PRAYER_TRAY_EXTENDED_MENU_KEY
        }
private val GNOME_PRAYER_EXTENSION_KEY = booleanPreferencesKey("prayer.desktop.gnome.extension.enabled")
private val DESKTOP_PRAYER_KEY = booleanPreferencesKey("prayer.desktop.enabled")

private fun Preferences.prayerMenuLayout(surface: PrayerMenuSurface): PrayerMenuLayout =
    when (this[surface.extendedMenuKey]) {
        true -> PrayerMenuLayout.EXTENDED
        false -> PrayerMenuLayout.COMPACT
        null -> surface.defaultLayout
    }

private fun Preferences.gnomePrayerDisplay(): GnomePrayerDisplay =
    when {
        this[DESKTOP_PRAYER_KEY] == false -> GnomePrayerDisplay.TRAY
        this[GNOME_PRAYER_EXTENSION_KEY] == false -> GnomePrayerDisplay.PANEL
        else -> GnomePrayerDisplay.EXTENSION
    }

private inline fun <reified T> Json.decodePreference(
    value: String?,
    fallback: T,
): T = value?.let { runCatching { decodeFromString<T>(it) }.getOrNull() } ?: fallback

private fun prayerKey(key: PrayerKey) = booleanPreferencesKey("prayer.${key.id}.enabled")

private fun ReminderQueueConfig.validated(): ReminderQueueConfig =
    copy(
        collectionIds = collectionIds.filter { it > 0 }.distinct(),
        windowStartMinute = windowStartMinute.coerceIn(0, 1439),
        windowEndMinute =
            windowEndMinute.coerceIn(0, 1439).let { end ->
                if (end == windowStartMinute) (end + 1) % 1440 else end
            },
        intervalMinutes = intervalMinutes.coerceIn(5, 1440),
        activeDays = activeDays.ifEmpty { ReminderQueueConfig.ALL_DAYS },
        defaultDropChance = defaultDropChance.coerceIn(0, 100),
        collectionDropChances = collectionDropChances.mapValues { it.value.coerceIn(0, 100) },
    )

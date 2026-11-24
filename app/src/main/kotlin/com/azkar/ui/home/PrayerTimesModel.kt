package com.azkar.ui.home

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.azkar.generated.resources.Res
import com.azkar.generated.resources.nextPrayerLabel
import com.azkar.ui.components.AzkarClock
import com.azkar.ui.notifications.PrayerLocationSettings
import com.azkar.ui.notifications.PrayerTimeCalculator
import org.jetbrains.compose.resources.getString
import java.time.Duration
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.Locale

/** The five fixed prayers, in the order `PrayerTimesController.bindPrayerRows` inserted them. */
enum class PrayerKey(
    val id: String,
) {
    FAJR("fajr"),
    DHUHR("dhuhr"),
    ASR("asr"),
    MAGHRIB("maghrib"),
    ISHA("isha"),
}

/**
 * One rendered prayer row, replacing the mutable `PrayerEntry` record plus the JavaFX
 * `PrayerRowComponent` it used to write into.
 */
@Immutable
data class PrayerRowState(
    val key: PrayerKey,
    val name: String,
    val timeLabel: String,
    val value: String,
    val isNext: Boolean,
    val notificationsEnabled: Boolean,
    val soundEnabled: Boolean = false,
)

/** `.next-prayer-time` text plus the raw remaining duration behind it. */
@Immutable
data class CountdownState(
    val text: String,
    val remaining: Duration,
)

/**
 * The prayer coming up next for a moment in time.
 *
 * Shared by the home table's `Next prayer` row and the tray countdown, so both always answer from
 * the same computation: one clock, one calculation, and a `remaining` measured inward from the
 * same `now`.
 */
@Immutable
data class NextPrayer(
    val key: PrayerKey,
    val name: String,
    val at: ZonedDateTime,
    val remaining: Duration,
)

/**
 * Stands in for the table until the localized prayer text has been resolved.
 *
 * The home screen shows a one-frame empty table on launch, which is what it already did while the
 * ticker was still in its `LaunchedEffect`.
 */
private val EMPTY_SNAPSHOT = PrayerTimesSnapshot(rows = emptyList(), countdown = CountdownState("", Duration.ZERO))

/**
 * Port of `com.azkar.components.home.controller.PrayerTimesController`.
 *
 * The JavaFX version wrote into `PrayerRowComponent` setters from a `Timeline`; the Compose port
 * keeps the exact same parsing/formatting rules but returns immutable snapshots that
 * [PrayerTimesState] republishes once per second.
 */
class PrayerTimesModel(
    private val uiLocale: Locale,
    private val location: PrayerLocationSettings,
) {
    /**
     * How a prayer time is written, once, for both halves of its row.
     *
     * The row used to print the same prayer twice in two conventions: `5:12 AM` as the subtitle and
     * `17:12` as the value. Whichever of the two the eye landed on first decided which one the reader
     * believed, and a reader who believed the 24-hour half had no reason to believe the other. There
     * is one clock in the app now ([AzkarClock]) and this row shows it once.
     */
    private val timeFormatter = AzkarClock.timeFormatter(uiLocale)

    /**
     * Empty until [load] has resolved the localized prayer names and times.
     *
     * [snapshot] runs on a one-second ticker, so it has to stay synchronous; resolving the text
     * once per locale and holding it in a field is what lets it be, and it also keeps the parse
     * from being repeated every tick.
     */
    private var prayers: List<ParsedPrayer> = emptyList()

    /**
     * The localized label shown for the next prayer, resolved by [load].
     */
    private var nextPrayerLabel: String = ""

    /**
     * Resolves localized prayer names. Times are calculated from the current location whenever a
     * snapshot is requested, so a location or calculation-method change takes effect immediately.
     *
     * Idempotent, so it is safe for the two [PrayerTimesState] entry points to both call it
     * without coordinating.
     */
    suspend fun load() {
        if (prayers.isNotEmpty()) return
        nextPrayerLabel = getString(Res.string.nextPrayerLabel)
        prayers =
            PrayerKey.entries.map { key ->
                ParsedPrayer(
                    key = key,
                    name = key.name(),
                )
            }
    }

    /** Resolves the next upcoming prayer and formats the whole table for [now]. */
    fun snapshot(now: ZonedDateTime): PrayerTimesSnapshot {
        if (prayers.isEmpty()) return EMPTY_SNAPSHOT
        val calculatedTimes = PrayerTimeCalculator.calculate(now.toLocalDate(), location)
        val next = computeNextPrayer(now, calculatedTimes)
        val rows =
            prayers.map { prayer ->
                val isNext = prayer.key == next.key
                val time = calculatedTimes.getValue(prayer.key)
                PrayerRowState(
                    key = prayer.key,
                    name = prayer.name,
                    timeLabel = if (isNext) nextPrayerLabel else "",
                    value = timeFormatter.format(time),
                    isNext = isNext,
                    // Match the fresh-install state in NotificationSettingsState while saved flags
                    // are being observed; rows must not flash off beneath an enabled master button.
                    notificationsEnabled = true,
                )
            }
        return PrayerTimesSnapshot(
            rows = rows,
            countdown = CountdownState(formatDuration(next.remaining), next.remaining),
            nextPrayer = next,
        )
    }

    private fun computeNextPrayer(
        now: ZonedDateTime,
        calculatedTimes: Map<PrayerKey, LocalTime>,
    ): NextPrayer {
        val today = now.toLocalDate()
        val nextToday =
            prayers
                .map { prayer -> today.atTime(calculatedTimes.getValue(prayer.key)).atZone(now.zone) to prayer }
                .filter { (at, _) -> !at.isBefore(now) }
                .minByOrNull { (at, _) -> at }
        val (at, prayer) =
            nextToday ?: run {
                val tomorrow = today.plusDays(1)
                val tomorrowTimes = PrayerTimeCalculator.calculate(tomorrow, location)
                prayers
                    .map { prayer -> tomorrow.atTime(tomorrowTimes.getValue(prayer.key)).atZone(now.zone) to prayer }
                    .minBy { (at, _) -> at }
            }
        return NextPrayer(
            key = prayer.key,
            name = prayer.name,
            at = at,
            remaining = Duration.between(now, at),
        )
    }

    private fun formatDuration(duration: Duration): String {
        val totalSeconds = maxOf(0L, duration.seconds)
        return String.format(
            uiLocale,
            "%02d:%02d:%02d",
            totalSeconds / 3600,
            (totalSeconds % 3600) / 60,
            totalSeconds % 60,
        )
    }

    private data class ParsedPrayer(
        val key: PrayerKey,
        val name: String,
    )
}

/** Today's local schedule and the next upcoming occurrence. */
@Immutable
data class PrayerTimesSnapshot(
    val rows: List<PrayerRowState>,
    val countdown: CountdownState,
    val nextPrayer: NextPrayer? = null,
)

/**
 * Observable wrapper around [PrayerTimesModel], replacing `HomeComponent`'s `Timeline`.
 *
 * The ticking itself lives in the composable's `LaunchedEffect`; this holder only stores the
 * latest snapshot and the persisted notification switches so a preference change re-renders
 * without restarting the ticker.
 */
@Stable
class PrayerTimesState(
    uiLocale: Locale,
    val location: PrayerLocationSettings = PrayerLocationSettings(),
    initialSnapshot: PrayerTimesSnapshot = EMPTY_SNAPSHOT,
) {
    private val model = PrayerTimesModel(uiLocale, location)
    private var loaded: Boolean = false

    var snapshot: PrayerTimesSnapshot by mutableStateOf(initialSnapshot)
        private set

    var notificationFlags: Map<PrayerKey, Boolean> by mutableStateOf(emptyMap())
        private set

    var running: Boolean by mutableStateOf(false)
        private set

    /**
     * Resolves the localized prayer text, then starts ticking.
     *
     * Suspends because the text comes from the resource store now, and the table cannot be built
     * without it — [EMPTY_SNAPSHOT] stands in until it arrives.
     */
    suspend fun start() {
        ensureLoaded()
        running = true
        tick()
    }

    /**
     * Loads the model once, for entry points that publish a snapshot before [start] runs.
     *
     * [applyAllFlags] and the ticker are launched independently, so whichever loses the race has
     * to be able to ask for the text itself rather than assume [start] got there first.
     */
    private suspend fun ensureLoaded() {
        if (loaded) return
        model.load()
        loaded = true
    }

    fun stop() {
        running = false
    }

    fun tick(now: ZonedDateTime = ZonedDateTime.now(ZoneId.of(location.zoneId))) {
        val fresh = model.snapshot(now)
        snapshot =
            if (notificationFlags.isEmpty()) {
                fresh
            } else {
                fresh.copy(
                    rows = fresh.rows.map { it.copy(notificationsEnabled = notificationFlags[it.key] == true) },
                )
            }
    }

    /**
     * Replaces the rendered switch states with [flags].
     *
     * The whole map goes in at once because it is always the complete set — a single-bell update
     * reaches here as a new complete map from the settings store, not as a patch, so a row can
     * never be left showing a switch the store has already moved past.
     */
    suspend fun applyAllFlags(flags: Map<PrayerKey, Boolean>) {
        ensureLoaded()
        notificationFlags = flags
        tick()
    }
}

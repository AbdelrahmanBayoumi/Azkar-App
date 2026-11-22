package com.azkar.ui.shell

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.vector.ImageVector
import com.azkar.generated.resources.Res
import com.azkar.generated.resources.desktopPrayerTomorrowTime
import com.azkar.generated.resources.trayNextPrayerCountdown
import com.azkar.ui.components.AzkarClock
import com.azkar.ui.components.AzkarIcons
import com.azkar.ui.home.PrayerKey
import com.azkar.ui.home.PrayerRowState
import com.azkar.ui.home.PrayerTimesModel
import com.azkar.ui.notifications.NotificationPreferencesRepository
import com.azkar.ui.notifications.PrayerLocationSettings
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.getString
import org.koin.compose.koinInject
import java.time.Duration
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.Locale
import kotlin.time.Duration.Companion.seconds

private val logger = KotlinLogging.logger {}

/** Display-ready prayer status shared by the tray and other desktop surfaces. */
@Immutable
internal data class DesktopPrayerStatus(
    val prayerName: String,
    val timeLabel: String,
    val remainingLabel: String,
    val countdownLabel: String,
    val today: List<PrayerRowState>,
    val nextIsTomorrow: Boolean,
) {
    val panelLabel: String
        get() = "$prayerName · $timeLabel · $remainingLabel"
}

@Composable
internal fun PrayerKey.desktopPrayerIcon(): ImageVector =
    when (this) {
        PrayerKey.FAJR -> AzkarIcons.Sunrise
        PrayerKey.DHUHR, PrayerKey.ASR -> AzkarIcons.Sun
        PrayerKey.MAGHRIB -> AzkarIcons.Sunset
        PrayerKey.ISHA -> AzkarIcons.Moon
    }

internal class DesktopPrayerStatusState(
    private val locale: Locale,
    private val location: PrayerLocationSettings,
) {
    private val model = PrayerTimesModel(locale, location)
    private val currentStatus = mutableStateOf<DesktopPrayerStatus?>(null)
    private var countdownFormat: String? = null
    private var tomorrowTimeFormat = ""
    private var lastKey: PrayerKey? = null

    val status: State<DesktopPrayerStatus?> = currentStatus

    suspend fun start() {
        if (countdownFormat != null) return
        model.load()
        tomorrowTimeFormat = getString(Res.string.desktopPrayerTomorrowTime)
        countdownFormat = getString(Res.string.trayNextPrayerCountdown)
        tick()
    }

    /** Read the wall clock on each tick so the display catches up after sleep or a clock change. */
    fun tick(now: ZonedDateTime = ZonedDateTime.now(ZoneId.of(location.zoneId))) {
        val format = countdownFormat ?: return
        val schedule = model.snapshot(now)
        val next = schedule.nextPrayer ?: return
        if (next.key != lastKey) {
            lastKey = next.key
            logger.info { "Next prayer is ${next.name} at ${next.at.toLocalDateTime()}" }
        }
        val remaining = formatRemaining(next.remaining)
        val prayerTime = AzkarClock.format(next.at.toLocalTime(), locale)
        val nextIsTomorrow = next.at.toLocalDate() != now.toLocalDate()
        val updatedStatus =
            DesktopPrayerStatus(
                prayerName = next.name,
                timeLabel = if (nextIsTomorrow) String.format(locale, tomorrowTimeFormat, prayerTime) else prayerTime,
                remainingLabel = remaining,
                countdownLabel = String.format(locale, format, next.name, remaining),
                today = schedule.rows,
                nextIsTomorrow = nextIsTomorrow,
            )
        if (updatedStatus != currentStatus.value) {
            currentStatus.value = updatedStatus
            logger.debug { "Desktop prayer countdown changed: ${updatedStatus.countdownLabel}" }
        }
    }
}

/** The desktop root owns the ticker so it can keep running while the main window is hidden. */
@Composable
internal fun rememberDesktopPrayerStatus(locale: Locale): State<DesktopPrayerStatus?> {
    val preferences = koinInject<NotificationPreferencesRepository>()
    val location by preferences.prayerLocation.collectAsState()
    val source = remember(locale, location) { DesktopPrayerStatusState(locale, location) }
    LaunchedEffect(source) {
        val startFailure = runCatching { source.start() }.exceptionOrNull()
        if (startFailure is CancellationException) throw startFailure
        if (startFailure != null) {
            logger.error(startFailure) { "Unable to start desktop prayer countdown" }
            return@LaunchedEffect
        }
        logger.info { "Desktop prayer countdown is running" }
        var previousTickFailed = false
        while (true) {
            delay(1.seconds)
            val tickFailure = runCatching { source.tick() }.exceptionOrNull()
            if (tickFailure is CancellationException) throw tickFailure
            if (tickFailure != null && !previousTickFailed) {
                logger.error(tickFailure) { "Desktop prayer countdown update failed. Retrying" }
            } else if (tickFailure == null && previousTickFailed) {
                logger.info { "Desktop prayer countdown recovered" }
            }
            previousTickFailed = tickFailure != null
        }
    }
    DisposableEffect(source) {
        onDispose { logger.debug { "Desktop prayer countdown stopped" } }
    }
    return source.status
}

/** Whole remaining minutes, with durations below a minute displayed as `<1m`. */
internal fun formatRemaining(duration: Duration): String {
    val totalSeconds = maxOf(0L, duration.seconds)
    if (totalSeconds < 60) return "<1m"

    val totalMinutes = totalSeconds / 60
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return when {
        hours > 0 && minutes > 0 -> String.format(Locale.ROOT, "%dh %dm", hours, minutes)
        hours > 0 -> String.format(Locale.ROOT, "%dh", hours)
        else -> String.format(Locale.ROOT, "%dm", totalMinutes)
    }
}

package com.azkar.ui.library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.azkar.domain.model.Remembrance
import com.azkar.generated.resources.Res
import com.azkar.generated.resources.collectionsActionCancel
import com.azkar.generated.resources.libraryReminderQueuePosition
import com.azkar.generated.resources.libraryReminderQueueSchedule
import com.azkar.generated.resources.libraryReminderQueueTitle
import com.azkar.generated.resources.libraryReminderQueueWindowValue
import com.azkar.generated.resources.libraryReminderStripEmpty
import com.azkar.generated.resources.reminderManagerQueueCadenceLabel
import com.azkar.generated.resources.reminderManagerQueueDaysLabel
import com.azkar.generated.resources.reminderManagerQueueIntervalLabel
import com.azkar.generated.resources.reminderManagerQueueModeLabel
import com.azkar.generated.resources.reminderManagerQueueWindowLabel
import com.azkar.ui.components.AzkarClock
import com.azkar.ui.components.AzkarDialogSurface
import com.azkar.ui.components.SecondaryButton
import com.azkar.ui.notifications.QueueCadence
import com.azkar.ui.notifications.QueueReading
import com.azkar.ui.notifications.ReminderQueueConfig
import com.azkar.ui.notifications.ReminderQueueProgress
import com.azkar.ui.notifications.activeDayLabel
import com.azkar.ui.notifications.cadenceLabel
import com.azkar.ui.notifications.intervalLabel
import com.azkar.ui.notifications.notificationLabel
import com.azkar.ui.notifications.queueModeLabel
import com.azkar.ui.notifications.upcomingPassages
import com.azkar.ui.theme.AzkarTheme
import org.jetbrains.compose.resources.stringResource
import java.time.DayOfWeek
import java.util.Locale

/**
 * A peek at the queue: the passages it will say next, and the schedule that decides when.
 *
 * Read-only, and on purpose. The strip under the banner names one passage; a user who wants to know
 * what the *rest* of the round holds, or when the next one is due, had nowhere to look — the reminders
 * page that used to answer it was removed when the management of the reminders moved into the
 * library. This is the answer, in the place the question is now asked.
 *
 * The passages come from the runtime's own progress rather than from a fresh shuffle, so the list
 * cannot disagree with what fires; see [upcomingPassages] for why the stored cycle is asked first and
 * the library's order is the fallback.
 */
@Composable
internal fun LibraryQueuePreviewDialog(
    config: ReminderQueueConfig,
    progress: ReminderQueueProgress,
    reading: QueueReading,
    uiLocale: Locale,
    onDismiss: () -> Unit,
) {
    val passages = upcomingPassages(progress, reading, UPCOMING_COUNT)
    Dialog(onDismissRequest = onDismiss) {
        AzkarDialogSurface(modifier = Modifier.widthIn(min = 420.dp, max = 560.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = stringResource(Res.string.libraryReminderQueueTitle),
                    color = AzkarTheme.colors.textPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                )
                if (passages.isEmpty()) {
                    Text(
                        text = stringResource(Res.string.libraryReminderStripEmpty),
                        color = AzkarTheme.colors.textSecondary,
                        fontSize = 12.sp,
                    )
                } else {
                    UpcomingPassages(passages = passages, uiLocale = uiLocale)
                }
                Spacer(Modifier.height(2.dp))
                ScheduleFacts(config = config, uiLocale = uiLocale)
                Row {
                    SecondaryButton(
                        text = stringResource(Res.string.collectionsActionCancel),
                        onClick = onDismiss,
                    )
                }
            }
        }
    }
}

/**
 * The passages the queue will say next, in the order it will say them.
 *
 * Numbered, because the first one is the one the strip already named and the rest are the round
 * behind it — a list with no order would be a list of equal possibilities, and this list is not one.
 */
@Composable
private fun UpcomingPassages(
    passages: List<Remembrance>,
    uiLocale: Locale,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        passages.forEachIndexed { index, passage ->
            Row(verticalAlignment = Alignment.Top) {
                Text(
                    text = stringResource(Res.string.libraryReminderQueuePosition, index + 1),
                    color = AzkarTheme.colors.textSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.width(QUEUE_POSITION_WIDTH),
                )
                Text(
                    text = notificationLabel(passage, uiLocale),
                    color = AzkarTheme.colors.textPrimary,
                    fontSize = 12.sp,
                    maxLines = QUEUE_PASSAGE_LINES,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/**
 * The schedule, as the settings page would show it.
 *
 * The window is left out when the queue is continuous, because in that mode there is no window to
 * show: the cadence says the queue never stops, and a start and an end beside it would contradict
 * that. The rest of the rows are the settings card's own facts, read rather than edited, so the
 * dialog cannot promise a schedule the settings do not hold.
 */
@Composable
private fun ScheduleFacts(
    config: ReminderQueueConfig,
    uiLocale: Locale,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = stringResource(Res.string.libraryReminderQueueSchedule),
            color = AzkarTheme.colors.textPrimary,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
        )
        ScheduleFact(
            label = stringResource(Res.string.reminderManagerQueueModeLabel),
            value = queueModeLabel(config.mode),
        )
        ScheduleFact(
            label = stringResource(Res.string.reminderManagerQueueCadenceLabel),
            value = cadenceLabel(config.cadence),
        )
        ScheduleFact(
            label = stringResource(Res.string.reminderManagerQueueIntervalLabel),
            value = intervalLabel(config.effectiveIntervalMinutes),
        )
        ScheduleFact(
            label = stringResource(Res.string.reminderManagerQueueDaysLabel),
            value = activeDaysLabel(config.activeDays),
        )
        if (config.cadence == QueueCadence.INTERVAL) {
            ScheduleFact(
                label = stringResource(Res.string.reminderManagerQueueWindowLabel),
                value = windowValue(config, uiLocale),
            )
        }
    }
}

/** One schedule row: the fact's name on the left, the fact on the right. */
@Composable
private fun ScheduleFact(
    label: String,
    value: String,
) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            color = AzkarTheme.colors.textSecondary,
            fontSize = 12.sp,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = value,
            color = AzkarTheme.colors.textPrimary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
        )
    }
}

/** The window's two ends, in the reader's own clock. */
@Composable
private fun windowValue(
    config: ReminderQueueConfig,
    uiLocale: Locale,
): String =
    stringResource(
        Res.string.libraryReminderQueueWindowValue,
        AzkarClock.format(config.windowStartMinute, uiLocale),
        AzkarClock.format(config.windowEndMinute, uiLocale),
    )

/** The days the queue runs on, in the order of the week rather than the order of the set. */
@Composable
private fun activeDaysLabel(activeDays: Set<DayOfWeek>): String =
    @Suppress("SimplifiableCallChain")
    DayOfWeek.entries
        .filter { day -> day in activeDays }
        .map { day -> activeDayLabel(day) }
        .joinToString(separator = " · ")

/** How many passages the look-ahead names. */
private const val UPCOMING_COUNT = 5

/** The width of the number beside each passage, so the passages start in one column. */
private val QUEUE_POSITION_WIDTH = 20.dp

/** How many lines a passage may take before it is cut short. */
private const val QUEUE_PASSAGE_LINES = 2

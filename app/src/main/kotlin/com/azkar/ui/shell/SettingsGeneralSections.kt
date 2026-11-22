package com.azkar.ui.shell

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.azkar.generated.resources.Res
import com.azkar.generated.resources.settingsLanguageBody
import com.azkar.generated.resources.settingsLanguageHeader
import com.azkar.generated.resources.settingsNotificationStartupFailed
import com.azkar.generated.resources.settingsStartupDevelopmentBuild
import com.azkar.generated.resources.settingsStartupDisabledByUser
import com.azkar.generated.resources.settingsStartupHint
import com.azkar.generated.resources.settingsStartupManagedByPolicy
import com.azkar.generated.resources.settingsStartupOpenSystemSettings
import com.azkar.generated.resources.settingsStartupTitle
import com.azkar.generated.resources.settingsStartupUnsupported
import com.azkar.i18n.displayNameIn
import com.azkar.i18n.isSameLanguageAs
import com.azkar.i18n.supportedLocales
import com.azkar.ui.components.AzkarToggleSwitch
import com.azkar.ui.library.AzkarDropdown
import com.azkar.ui.notifications.NotificationSettingsState
import com.azkar.ui.notifications.StartupNotice
import com.azkar.ui.preview.previewAzkarAppState
import com.azkar.ui.preview.rememberPreviewNotificationSettingsState
import com.azkar.ui.theme.AzkarSpacing
import com.azkar.ui.theme.AzkarTheme
import com.azkar.ui.theme.AzkarVariantsPreview
import org.jetbrains.compose.resources.stringResource

// `ui/shell/SettingsGeneralSections.kt` — the two blocks under General: the language the app speaks in,
// and whether it opens itself. Both used to sit among blocks about how the app looks, and start-on-login
// sat further still — inside the card headed "On screen & desktop notifications", under a heading about
// reminders, for a setting that has nothing to do with reminders at all. Neither is a visual preference,
// so neither is filed with the theme, and neither needs a reminder in order to be found.
//
// The whole of the language block, and of the startup row, is here; both were previously living in the
// files named for the *appearance* and *notifications* groups, which is how the page ended up grouped by
// which screen a setting had been written on rather than by what the setting is.

/**
 * Wide enough for the longest endonym in the list beside its menu arrow without the arrow crowding the
 * word, and narrow enough that a field sitting on a heading's line does not out-measure the page title.
 */
private val LANGUAGE_FIELD_WIDTH = 170.dp

/**
 * The language, as a dropdown on the block's own heading, with no card around it.
 *
 * This has been three shapes: a modal with a Save button, then a list of rows, then a card with one
 * row. Each version kept a container and the container kept costing the reader something. The modal
 * had an OK to dismiss and a modal to come back from, both for a setting people change once. The rows
 * drew the language as the only answer on the page that was not on the right-hand side, so it read as
 * content rather than as a setting. The card fixed that and then wrapped one word in 120dp of rounded
 * rectangle, under a block whose title had already said the word's job.
 *
 * What is left is the heading, the sentence under it, and the answer at the far end of the title's
 * line — which is the shape of a section that holds exactly one choice. Every other block on the page
 * needs a card because it holds rows; this one does not, and a reader who has to hunt for the control
 * inside a container pays for that container twice.
 *
 * It leads the page because it is the one setting here that changes the words everything else is written
 * in. Under the former appearance grouping it would have meant scrolling past two blocks about pixels to
 * reach the answer to "why is this in the language it is in".
 *
 * The answer is named by its endonym via [displayNameIn] against the *active* locale rather than
 * translated, which is the one piece of self-description a language list can give that a localised label
 * cannot: "English" written in Arabic is a claim about the option, and "English" written in English is a
 * claim about the reader.
 */
@Composable
internal fun SettingsLanguageSection(
    state: AzkarAppState,
    modifier: Modifier = Modifier,
) {
    val current = AzkarTheme.locale
    SettingsSectionBlock(
        title = stringResource(Res.string.settingsLanguageHeader),
        subtitle = stringResource(Res.string.settingsLanguageBody),
        modifier = modifier,
        headingAction = {
            AzkarDropdown(
                selected = supportedLocales.firstOrNull { it.isSameLanguageAs(state.locale) },
                options = supportedLocales,
                label = { candidate -> candidate.displayNameIn(current) },
                onSelect = state::applyLocale,
                fieldWidth = LANGUAGE_FIELD_WIDTH,
            )
        },
    )
}

/**
 * Whether Azkar is registered to open when the reader signs in.
 *
 * Its own block under General rather than a row of the reminders card, which is where it was. A reader
 * looking for "launch on startup" was being made to read past a card about how a reminder is delivered,
 * and — because it is a genuinely separate decision from anything about reminders — was left without
 * any hint that it applied when no reminder was ever due.
 *
 * One row in a card, which is thinner than most of the blocks around it and is not a problem: the card
 * is what tells the reader this is one decision, and a block whose only row is a switch is still easier
 * to find than that switch is as the third row of an unrelated group.
 */
@Composable
internal fun SettingsStartupSection(
    startOnLogin: Boolean,
    settings: NotificationSettingsState,
    modifier: Modifier = Modifier,
) {
    val notice = settings.notifications.notice
    SettingsSectionBlock(
        title = stringResource(Res.string.settingsStartupTitle),
        subtitle = stringResource(Res.string.settingsStartupHint),
        modifier = modifier,
        headingAction = {
            AzkarToggleSwitch(
                checked = startOnLogin,
                onCheckedChange = settings.notifications::setStartOnLogin,
                enabled = settings.notifications.canChangeStartup,
            )
        },
        content =
            if (notice == null) {
                null
            } else {
                {
                    Column {
                        StartupNoticeText(notice)
                        if (notice == StartupNotice.DisabledByUser) {
                            TextButton(onClick = settings.notifications::openStartupSystemSettings) {
                                Text(stringResource(Res.string.settingsStartupOpenSystemSettings))
                            }
                        }
                    }
                }
            },
    )
}

/**
 * The startup failure, in the validation colour the rest of the app uses for "this did not work".
 *
 * Accepts only [StartupNotice], keeping login-manager guidance separate from location failures.
 *
 * It sits under the switch rather than above it because the failure belongs to the action that just
 * happened, and a message about an action reads as its continuation, not as the row's description.
 */
@Composable
internal fun StartupNoticeText(
    notice: StartupNotice?,
    modifier: Modifier = Modifier,
) {
    val message =
        when (notice) {
            StartupNotice.DisabledByUser -> stringResource(Res.string.settingsStartupDisabledByUser)
            StartupNotice.ManagedByPolicy -> stringResource(Res.string.settingsStartupManagedByPolicy)
            StartupNotice.DevelopmentBuild -> stringResource(Res.string.settingsStartupDevelopmentBuild)
            StartupNotice.Unsupported -> stringResource(Res.string.settingsStartupUnsupported)
            is StartupNotice.Unavailable ->
                stringResource(Res.string.settingsNotificationStartupFailed) +
                    notice.reason?.let { ": $it" }.orEmpty()
            null -> return
        }
    Text(
        text = message,
        color = AzkarTheme.colors.dialogValidation,
        fontSize = 11.sp,
        fontWeight = FontWeight.Medium,
        modifier = modifier,
    )
}

@AzkarVariantsPreview
@Composable
internal fun SettingsGeneralSectionsPreview() {
    Column(
        modifier = Modifier.width(760.dp).padding(AzkarSpacing.xl),
        verticalArrangement = Arrangement.spacedBy(AzkarSpacing.section),
    ) {
        SettingsLanguageSection(state = previewAzkarAppState())
        SettingsStartupSection(
            startOnLogin = true,
            settings = rememberPreviewNotificationSettingsState(),
        )
    }
}

@AzkarVariantsPreview
@Composable
internal fun SettingsStartupNoticesPreview() {
    Column(
        modifier = Modifier.width(760.dp).padding(AzkarSpacing.xl),
        verticalArrangement = Arrangement.spacedBy(AzkarSpacing.md),
    ) {
        StartupNoticeText(StartupNotice.DisabledByUser)
        StartupNoticeText(StartupNotice.ManagedByPolicy)
        StartupNoticeText(StartupNotice.Unsupported)
        StartupNoticeText(StartupNotice.Unavailable(reason = "Login manager unavailable"))
    }
}

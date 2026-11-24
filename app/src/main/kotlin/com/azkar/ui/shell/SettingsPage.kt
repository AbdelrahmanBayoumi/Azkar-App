package com.azkar.ui.shell

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.azkar.generated.resources.Res
import com.azkar.generated.resources.settingsAudioTitle
import com.azkar.generated.resources.settingsClose
import com.azkar.generated.resources.settingsCloseKey
import com.azkar.generated.resources.settingsDesktopPrayerTitle
import com.azkar.generated.resources.settingsGroupGeneral
import com.azkar.generated.resources.settingsGroupPrayer
import com.azkar.generated.resources.settingsGroupReminders
import com.azkar.generated.resources.settingsLanguageHeader
import com.azkar.generated.resources.settingsNotificationsScheduleTitle
import com.azkar.generated.resources.settingsPrayerCalculationTitle
import com.azkar.generated.resources.settingsPrayerLocationTitle
import com.azkar.generated.resources.settingsReminderCardTitle
import com.azkar.generated.resources.settingsStartupTitle
import com.azkar.generated.resources.settingsThemeHeader
import com.azkar.generated.resources.settingsTypographyHeader
import com.azkar.generated.resources.settingsUpdatesTitle
import com.azkar.ui.components.ActionIcons
import com.azkar.ui.components.AzkarCard
import com.azkar.ui.components.pressableClick
import com.azkar.ui.theme.AzkarSpacing
import com.azkar.ui.theme.AzkarTheme
import com.azkar.ui.theme.CardSize
import org.jetbrains.compose.resources.stringResource

// `ui/shell/SettingsPage.kt` — the vocabulary the settings page is written in: the families a
// setting can belong to, the blocks those families hold, and the shapes every block is built from.
// Kept apart from the sections themselves so that adding a setting means writing a block, not
// learning a new layout language.
//
// The page is one long scroll rather than tabs, and that is what these pieces assume. The rail
// names every block under the family it belongs to, jumps to it, and highlights it as the reader
// scrolls, so nothing is more than one scroll's distance from anything else.

// The rail holds a 20-character label and an icon, so it needs far less than the content pane beside
// it, and a wider one would start competing with the cards it is meant to index.
val SETTINGS_RAIL_WIDTH = 268.dp

/**
 * The gap between two blocks.
 *
 * A block is a heading and what it holds, and two of them without a gap between read as one long card
 * with two headings. This is also the gap between blocks that share a scroll item, so the layout code
 * has to apply it itself where the list's own spacing cannot reach.
 */
val SECTION_GAP = AzkarSpacing.section + 6.dp

/** The gap between the header's rule and the first card under it. */
private val SECTION_HEADING_GAP = 18.dp

/** Tight, because the two lines are one heading rather than a title and a caption. */
private val HEADING_LINE_GAP = 2.dp

/*
 * The space inside a card that holds nothing but rows. Tighter than a card's own padding, so a stack of
 * rows reads as one block rather than as several cards that happen to be the same width.
 */
private val GROUP_CARD_PADDING = 6.dp

/** The diameter of the empty ring that marks an unchosen option, and the dot inside a chosen one. */
private val SELECTION_RING_SIZE = 18.dp

private val SELECTION_RING_STROKE = 2.dp

private val SELECTION_DOT_SIZE = 8.dp

/**
 * The families the page's blocks are filed under.
 *
 * The page used to have exactly one group row, standing above all five of its sections and naming
 * itself "General & Appearance" — while notifications and prayer times hung underneath it. A heading
 * that covers none of what sits below it is worse than no heading at all, because it teaches the reader
 * that the rail's groups do not mean anything; the next heading they meet will be filed the same way.
 * So the groups here are the ones that actually partition the page, and every block belongs to exactly
 * one of them.
 *
 * Declaration order is the reading order, and it is the *only* place that order is written down.
 * [SettingsSection] declares its own blocks grouped by this enum rather than the rail and the page each
 * walking their own list, because two lists that both claim to be "the order" is a disagreement waiting
 * for the next block added to the wrong one.
 */
enum class SettingsGroup {
    GENERAL,
    REMINDERS,
    PRAYER,
}

/**
 * Every addressable block of the settings page, in the order they appear.
 *
 * An enum rather than a list of titles because these are also the scroll positions: the rail writes
 * one and the page reads one to know which block is on screen, and a `String` key would let a renamed
 * translation silently stop matching. Declaration order is the reading order, so `entries` is the
 * whole navigation model.
 *
 * One entry per rail row, and a row per *block* — a heading and the rows that answer one question. The
 * granularity is what the page was getting wrong, so it is worth stating what the earlier, coarser cut
 * looked like and why each of these exists. "Notifications" was one block holding three answers: when a
 * reminder is due, what the card that carries it looks like, and whether it could also leave the app.
 * Card size and duration now sit in [SettingsSection.REMINDER_CARD], separate from the reminder
 * schedule. Start-on-login was in that card too, under a heading about reminders, for a setting about
 * the app launching — it is [STARTUP], under
 * [SettingsGroup.GENERAL]. Prayer times held a place *and* a calculation method under one subtitle
 * that described only the place, so the method read as a field of the address form; it is
 * [PRAYER_CALCULATION] now, and [PRAYER_LOCATION]'s subtitle says what its card actually holds.
 */
enum class SettingsSection(
    val group: SettingsGroup,
) {
    LANGUAGE(SettingsGroup.GENERAL),
    STARTUP(SettingsGroup.GENERAL),
    THEME(SettingsGroup.GENERAL),
    TEXT(SettingsGroup.GENERAL),
    UPDATES(SettingsGroup.GENERAL),
    REMINDER_SCHEDULE(SettingsGroup.REMINDERS),
    REMINDER_CARD(SettingsGroup.REMINDERS),
    PRAYER_CALCULATION(SettingsGroup.PRAYER),
    PRAYER_LOCATION(SettingsGroup.PRAYER),
    PRAYER_PANEL(SettingsGroup.PRAYER),
    AUDIO(SettingsGroup.PRAYER),
}

/** This block's name — the rail's row and the page's heading are the same word for the same thing. */
@Composable
fun settingsSectionLabel(section: SettingsSection): String =
    when (section) {
        SettingsSection.LANGUAGE -> stringResource(Res.string.settingsLanguageHeader)
        SettingsSection.STARTUP -> stringResource(Res.string.settingsStartupTitle)
        SettingsSection.UPDATES -> stringResource(Res.string.settingsUpdatesTitle)
        SettingsSection.THEME -> stringResource(Res.string.settingsThemeHeader)
        SettingsSection.TEXT -> stringResource(Res.string.settingsTypographyHeader)
        SettingsSection.REMINDER_CARD -> stringResource(Res.string.settingsReminderCardTitle)
        SettingsSection.REMINDER_SCHEDULE -> stringResource(Res.string.settingsNotificationsScheduleTitle)
        SettingsSection.PRAYER_LOCATION -> stringResource(Res.string.settingsPrayerLocationTitle)
        SettingsSection.PRAYER_CALCULATION -> stringResource(Res.string.settingsPrayerCalculationTitle)
        SettingsSection.PRAYER_PANEL -> stringResource(Res.string.settingsDesktopPrayerTitle)
        SettingsSection.AUDIO -> stringResource(Res.string.settingsAudioTitle)
    }

/** This family's name, as the rail's own row writes it. */
@Composable
fun settingsGroupLabel(group: SettingsGroup): String =
    when (group) {
        SettingsGroup.GENERAL -> stringResource(Res.string.settingsGroupGeneral)
        SettingsGroup.REMINDERS -> stringResource(Res.string.settingsGroupReminders)
        SettingsGroup.PRAYER -> stringResource(Res.string.settingsGroupPrayer)
    }

/** The blocks filed under one family, in reading order. */
val SettingsGroup.sections: List<SettingsSection>
    get() =
        SettingsSection.entries.filter {
            it.group == this && (it != SettingsSection.PRAYER_PANEL || prayerTrayLayoutAvailable)
        }

/**
 * One block: its name, a sentence about it, the rule under both, and everything it holds.
 *
 * The heading is the block's identity, which is why the title is lifted out of the cards. The cards
 * used to carry their own titles, so a card that needed saying what it was also had to repeat the
 * block it sat in, and the page read as a stack of same-sized boxes rather than as four things a
 * reader could name. Here the card is just a group of related rows and the heading above it does the
 * naming — which is also what lets the rail and the page agree on what a section is.
 *
 * The title and its sentence share a row with whatever [headingAction] supplies, so a control is
 * centred against the whole heading rather than against the title alone. Centring it against the title
 * alone is what an earlier version did, and it cost the heading its own line spacing: a 40dp control
 * is taller than an 11.sp title, so aligning their centres sank the title into the middle of the row
 * and left it hanging a good way above its own sentence.
 *
 * [headingAction] puts a control at the far end of the heading's line. It is
 * here for the one setting on this page that needs no card at all: the language. Wrapping it in a card
 * with a value row of its own meant a card whose only job was to hold a single word, and the word
 * itself said nothing the block's own title had not already said a line above it. Dropping the card
 * leaves the heading, the sentence about it, and the answer, and nothing between the question and the
 * answer but the reader's eye travelling sideways.
 *
 * [content] is optional because that same block has nothing left to put in one.
 */
@Composable
fun SettingsSectionBlock(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    headingAction: (@Composable () -> Unit)? = null,
    content: (@Composable () -> Unit)? = null,
) {
    val colors = AzkarTheme.colors
    Column(modifier = modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(HEADING_LINE_GAP),
            ) {
                Text(
                    text = title.uppercase(AzkarTheme.locale),
                    color = colors.textSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.1.sp,
                )
                Text(
                    text = subtitle,
                    color = colors.textSecondary,
                    fontSize = 12.sp,
                )
            }
            headingAction?.invoke()
        }
        if (content != null) {
            Spacer(Modifier.height(SECTION_HEADING_GAP))
            content()
        }
    }
}

/**
 * A card of related rows, with the padding turned in.
 *
 * Rows in this page carry more text than the switch rows elsewhere in the app — a heading, a value and
 * a sentence — so the default card padding, which is sized for a control and a label, leaves the text
 * in a column narrower than the card looks like it is. [GROUP_CARD_PADDING] is the row's own inset and
 * the rows supply the rest.
 */
@Composable
fun SettingsGroupCard(
    modifier: Modifier = Modifier,
    contentPadding: Dp = GROUP_CARD_PADDING,
    content: @Composable ColumnScope.() -> Unit,
) {
    AzkarCard(
        modifier = modifier.fillMaxWidth(),
        size = CardSize.MD,
        shape = RoundedCornerShape(16.dp),
        contentPadding = contentPadding,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(GROUP_CARD_PADDING), content = content)
    }
}

/** The rule between two rows of a [SettingsGroupCard]. */
@Composable
fun SettingsRowDivider(modifier: Modifier = Modifier) {
    HorizontalDivider(
        modifier = modifier.padding(horizontal = 16.dp),
        thickness = 1.dp,
        color = AzkarTheme.colors.border,
    )
}

/**
 * One row: a category, the setting's current answer, a sentence about it, and the control.
 *
 * [label] is the small category line and [value] the answer under it, which is how the reference
 * layout reads and why this is two fields rather than one. Several settings here have both a family
 * they belong to and a specific answer — "System notifications" is an answer about the desktop, and
 * "On screen for 8 seconds" is an answer about one of the ways a reminder is shown — and a single
 * line would have had to say both at once, in the same weight, which reads as a sentence nobody chose.
 *
 * [control] is last in the signature because it is the one argument every row has and the three before
 * it are the ones rows differ by. A row with nothing on the right is a caption, not a setting, so the
 * slot is optional only so a card can end in a note.
 */
@Composable
fun SettingsValueRow(
    value: String? = null,
    modifier: Modifier = Modifier,
    label: String? = null,
    description: String? = null,
    control: @Composable (() -> Unit)? = null,
    enabled: Boolean = true,
) {
    val colors = AzkarTheme.colors
    Row(
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AzkarSpacing.lg),
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            if (label != null) {
                Text(
                    text = label.uppercase(AzkarTheme.locale),
                    color = colors.textSecondary,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.9.sp,
                )
            }
            if (value != null) {
                Text(
                    text = value,
                    color = if (enabled) colors.textPrimary else colors.textSecondary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            if (description != null) {
                Text(
                    text = description,
                    color = colors.textSecondary,
                    fontSize = 12.sp,
                )
            }
        }
        if (control != null) {
            Box(modifier = Modifier.padding(start = AzkarSpacing.sm)) { control() }
        }
    }
}

/**
 * A row whose answer is typed rather than chosen, so the label sits above the box instead of beside it.
 *
 * Separate from [SettingsValueRow] because a text field wants the width and a toggle does not: put a
 * field in the right-hand slot and it is a 200dp box beside a short sentence, which is a box too small
 * to type a time zone into. Putting the label above it costs one line and buys the full card.
 */
@Composable
fun SettingsFieldRow(
    label: String,
    modifier: Modifier = Modifier,
    hint: String? = null,
    field: @Composable () -> Unit,
) {
    val colors = AzkarTheme.colors
    Column(
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(AzkarSpacing.sm),
    ) {
        Text(
            text = label,
            color = colors.textSecondary,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.9.sp,
        )
        field()
        if (hint != null) {
            Text(
                text = hint,
                color = colors.textSecondary,
                fontSize = 11.sp,
            )
        }
    }
}

/**
 * The ring that says which of a set of options is in force.
 *
 * A ring and not a check: a tick claims the row has been *done*, which is what the check in the
 * language list used to say, and a language is chosen rather than completed. A filled dot inside an
 * empty ring says the smaller thing accurately — this one, of these — and it is the shape the theme
 * tiles in the reference already use, so the page speaks one language about selection.
 */
@Composable
fun SettingsSelectionRing(
    selected: Boolean,
    modifier: Modifier = Modifier,
) {
    val colors = AzkarTheme.colors
    Box(
        modifier =
            modifier
                .size(SELECTION_RING_SIZE)
                .border(
                    width = SELECTION_RING_STROKE,
                    color = if (selected) colors.primary else colors.border,
                    shape = CircleShape,
                ),
        contentAlignment = Alignment.Center,
    ) {
        if (selected) {
            Box(
                modifier =
                    Modifier
                        .size(SELECTION_DOT_SIZE)
                        .background(colors.primary, CircleShape),
            )
        }
    }
}

/**
 * The way out of the page: a round dismiss and the key that does the same thing.
 *
 * The caption is on screen rather than in a tooltip because this is the only control on the page that
 * is not a setting, and a reader who wants to close without reaching for the mouse should not have to
 * find that out by hovering. The key label is deliberately untranslated — it names a key on the keyboard,
 * and keyboards are not localised.
 */
@Composable
fun SettingsCloseButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = AzkarTheme.colors
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            modifier =
                Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(colors.cardBackground)
                    .border(1.dp, colors.border, CircleShape)
                    .pressableClick(scale = 1f, onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = ActionIcons.dismiss,
                contentDescription = stringResource(Res.string.settingsClose),
                tint = colors.textPrimary,
                modifier = Modifier.size(18.dp),
            )
        }
        Text(
            text = stringResource(Res.string.settingsCloseKey),
            color = colors.textSecondary,
            fontSize = 9.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.8.sp,
        )
    }
}

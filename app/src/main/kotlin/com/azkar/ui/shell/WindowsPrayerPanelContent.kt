package com.azkar.ui.shell

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.OpenWith
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.use
import com.azkar.generated.resources.Res
import com.azkar.generated.resources.desktopPrayerCloseSchedule
import com.azkar.generated.resources.taskbarPositionAutomatic
import com.azkar.generated.resources.taskbarPositionManual
import com.azkar.generated.resources.taskbarPositionTip
import com.azkar.generated.resources.todayPrayerTimes
import com.azkar.generated.resources.trayMenuOpen
import com.azkar.i18n.isRightToLeft
import com.azkar.ui.components.ActionIcons
import com.azkar.ui.components.AzkarIcons
import com.azkar.ui.components.TooltipIconButton
import com.azkar.ui.home.PrayerRowState
import com.azkar.ui.theme.azkarTypography
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.ColorAlphaType
import org.jetbrains.skia.ColorType

@OptIn(ExperimentalComposeUiApi::class)
internal fun renderWindowsTaskbar(
    content: WindowsTaskbarContent,
    slot: WindowsTaskbarSlot,
    highlighted: Boolean,
): ByteArray =
    ImageComposeScene(
        width = slot.bounds.width,
        height = slot.bounds.height,
        density = Density(slot.dpi / 96f),
        layoutDirection = if (content.locale.isRightToLeft) LayoutDirection.Rtl else LayoutDirection.Ltr,
    ) { WindowsTaskbarSummary(content, slot.dark, highlighted, vertical = slot.layout?.vertical == true) }
        .use { scene ->
            scene.render().use { image ->
                Bitmap.makeFromImage(image).use { bitmap ->
                    // UpdateLayeredWindow expects tightly packed BGRA with premultiplied alpha.
                    val format =
                        bitmap.imageInfo
                            .withColorType(
                                ColorType.BGRA_8888,
                            ).withColorAlphaType(ColorAlphaType.PREMUL)
                    checkNotNull(bitmap.readPixels(format, format.minRowBytes))
                }
            }
        }

@Composable
private fun WindowsTaskbarSummary(
    content: WindowsTaskbarContent,
    dark: Boolean,
    highlighted: Boolean,
    vertical: Boolean,
) {
    val foreground = if (dark) Color(0xFFFAFAFA) else Color(0xFF1B1E24)
    val accent = if (dark) Color(0xFF97C3FF) else Color(0xFF194B9B)
    // Alpha zero would pass clicks through to Explorer, so idle uses the smallest nonzero alpha.
    val surface =
        when {
            highlighted -> if (dark) Color(0x14FFFFFF) else Color(0x10000000)
            dark -> Color(0x01000000)
            else -> Color(0x01FFFFFF)
        }
    val label =
        azkarTypography(
            content.locale,
            content.typography,
        ).labelMedium.copy(fontSize = 12.sp, lineHeight = 16.sp)
    if (vertical) {
        VerticalTaskbarSummary(content.remaining, label, foreground, accent, surface)
        return
    }
    Row(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(vertical = 2.dp)
                .background(surface, RoundedCornerShape(8.dp))
                .padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(AzkarIcons.Clock, contentDescription = null, modifier = Modifier.size(14.dp), tint = accent)
        val secondary = if (dark) Color(0xFFCED3DC) else Color(0xFF505967)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.Center) {
            Text(
                content.name,
                style = label.copy(fontSize = 13.sp, fontWeight = FontWeight.SemiBold),
                color = foreground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                content.time,
                style = label.copy(fontSize = 11.sp, lineHeight = 13.sp),
                color = secondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Text(
            content.remaining,
            modifier =
                Modifier
                    .background(accent.copy(alpha = 0.12f), RoundedCornerShape(6.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp),
            style = label.copy(fontSize = 11.sp, fontWeight = FontWeight.SemiBold),
            color = accent,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun VerticalTaskbarSummary(
    remaining: String,
    label: TextStyle,
    foreground: Color,
    accent: Color,
    surface: Color,
) {
    Column(
        modifier = Modifier.fillMaxSize().background(surface, RoundedCornerShape(8.dp)).padding(horizontal = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(AzkarIcons.Clock, contentDescription = null, modifier = Modifier.size(14.dp), tint = accent)
        Text(
            remaining,
            style = label.copy(fontSize = 10.sp),
            color = foreground,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
internal fun WindowsTaskbarPositionMenu(
    manual: Boolean,
    menu: PrayerTrayMenuContent,
    onDismiss: () -> Unit,
    onMove: () -> Unit,
    onAutomatic: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    var hoveredLabel by remember { mutableStateOf<String?>(null) }
    Column(Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier.fillMaxWidth().height(POSITION_TOOLTIP_HEIGHT.dp).padding(end = 14.dp, bottom = 4.dp),
            contentAlignment = Alignment.BottomEnd,
        ) {
            hoveredLabel?.let { label ->
                Surface(shape = RoundedCornerShape(6.dp), color = colors.inverseSurface) {
                    Text(
                        label,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        color = colors.inverseOnSurface,
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
            }
        }
        Surface(
            modifier = Modifier.fillMaxSize(),
            shape = RoundedCornerShape(14.dp),
            color = colors.surface,
            border = BorderStroke(1.dp, colors.outlineVariant),
        ) {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                menu.actions.filterNot { it == menu.quit }.forEach { entry ->
                    TaskbarMenuItem(entry, onDismiss)
                }
                HorizontalDivider()
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TaskbarMenuItem(menu.quit, onDismiss, Modifier.weight(1f))
                    Box(Modifier.padding(end = 14.dp)) {
                        TaskbarPositionControls(manual, onAutomatic, onMove) { label, hovered ->
                            hoveredLabel = hoveredTooltip(hoveredLabel, label, hovered)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TaskbarMenuItem(
    entry: TrayMenuEntry,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    DropdownMenuItem(
        modifier = modifier.heightIn(min = POSITION_ACTION_HEIGHT.dp),
        text = { Text(entry.label) },
        leadingIcon = { Icon(entry.icon, contentDescription = null, modifier = Modifier.size(18.dp)) },
        enabled = entry.isEnabled,
        onClick = {
            onDismiss()
            entry.onClick()
        },
    )
}

@Composable
private fun TaskbarPositionControls(
    manual: Boolean,
    onAutomatic: () -> Unit,
    onMove: () -> Unit,
    onHover: (String, Boolean) -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(20.dp)
    Row(Modifier.selectableGroup().clip(shape).border(1.dp, colors.outlineVariant, shape)) {
        val automaticLabel = stringResource(Res.string.taskbarPositionAutomatic)
        TaskbarPositionIcon(
            icon = Icons.Filled.AutoAwesome,
            label = automaticLabel,
            selected = !manual,
            onClick = onAutomatic,
            onHover = { onHover(automaticLabel, it) },
        )
        val manualLabel = stringResource(Res.string.taskbarPositionManual)
        val manualTip = "$manualLabel. " + stringResource(Res.string.taskbarPositionTip)
        TaskbarPositionIcon(
            icon = Icons.Filled.OpenWith,
            label = manualLabel,
            selected = manual,
            onClick = onMove,
            onHover = { onHover(manualTip, it) },
        )
    }
}

private fun hoveredTooltip(
    current: String?,
    label: String,
    hovered: Boolean,
): String? =
    when {
        hovered -> label
        current == label -> null
        else -> current
    }

@Composable
private fun TaskbarPositionIcon(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    onHover: (Boolean) -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val interactionSource = remember { MutableInteractionSource() }
    val hovered by interactionSource.collectIsHoveredAsState()
    val reportHover by rememberUpdatedState(onHover)
    LaunchedEffect(hovered) { reportHover(hovered) }
    Box(
        modifier =
            Modifier
                .size(width = 44.dp, height = 40.dp)
                .background(if (selected) colors.secondaryContainer else Color.Transparent)
                .selectable(
                    selected = selected,
                    role = Role.RadioButton,
                    onClick = onClick,
                    interactionSource = interactionSource,
                    indication = LocalIndication.current,
                ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            icon,
            contentDescription = label,
            modifier = Modifier.size(18.dp),
            tint = if (selected) colors.onSecondaryContainer else colors.onSurfaceVariant,
        )
    }
}

@Composable
internal fun WindowsPrayerSchedule(
    prayer: DesktopPrayerStatus,
    onDismiss: () -> Unit,
    onOpen: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    Surface(
        modifier = Modifier.fillMaxSize(),
        shape = RoundedCornerShape(14.dp),
        color = colors.surface,
        border = BorderStroke(1.dp, colors.outlineVariant),
    ) {
        Column {
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(start = 8.dp, top = 2.dp, bottom = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        AzkarIcons.Calendar,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = colors.primary,
                    )
                    Text(
                        stringResource(Res.string.todayPrayerTimes),
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.titleSmall,
                    )
                    TooltipIconButton(
                        icon = ActionIcons.dismiss,
                        tooltip = stringResource(Res.string.desktopPrayerCloseSchedule),
                        onClick = onDismiss,
                        buttonSize = 28.dp,
                        iconSize = 16.dp,
                    )
                    TooltipIconButton(
                        icon = AzkarIcons.Home,
                        tooltip = stringResource(Res.string.trayMenuOpen),
                        onClick = onOpen,
                        buttonSize = 28.dp,
                        iconSize = 16.dp,
                    )
                }
                prayer.today.forEach { row ->
                    PrayerPanelScheduleRow(row, next = row.isNext && !prayer.nextIsTomorrow)
                }
            }
        }
    }
}

@Composable
private fun PrayerPanelScheduleRow(
    row: PrayerRowState,
    next: Boolean,
) {
    val colors = MaterialTheme.colorScheme
    val weight = if (next) FontWeight.SemiBold else FontWeight.Normal
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(if (next) colors.primaryContainer else colors.surface, RoundedCornerShape(8.dp))
                .padding(horizontal = 8.dp, vertical = 5.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = row.key.desktopPrayerIcon(),
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = if (next) colors.onPrimaryContainer else colors.primary,
        )
        Text(
            row.name,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium,
            color = if (next) colors.onPrimaryContainer else colors.onSurface,
            fontWeight = weight,
        )
        Text(
            row.value,
            style = MaterialTheme.typography.bodyMedium,
            color = if (next) colors.onPrimaryContainer else colors.onSurfaceVariant,
            fontWeight = weight,
        )
    }
}

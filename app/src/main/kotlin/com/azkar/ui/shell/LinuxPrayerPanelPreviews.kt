package com.azkar.ui.shell

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.azkar.ui.components.AzkarIcons
import com.azkar.ui.notifications.DesktopPlatform
import com.azkar.ui.notifications.PrayerMenuLayout
import com.azkar.ui.theme.AzkarThemePreview

@Preview(widthDp = 240, heightDp = 144)
@Composable
internal fun LinuxPrayerPanelCompactPreview() = LinuxPrayerPanelMenuPreview(PrayerMenuLayout.COMPACT)

@Preview(widthDp = 240, heightDp = 390)
@Composable
internal fun LinuxPrayerPanelExtendedPreview() = LinuxPrayerPanelMenuPreview(PrayerMenuLayout.EXTENDED)

@Composable
private fun LinuxPrayerPanelMenuPreview(layout: PrayerMenuLayout) {
    AzkarThemePreview(darkTheme = true) {
        val prayer = previewPrayerStatus()
        val panel =
            GnomePrayerContent(
                label = prayer.panelLabel,
                tray = ReminderTrayContent("", AzkarIcons.Brand, {}, previewPrayerMenu(DesktopPlatform.LINUX)),
                menuTint = MaterialTheme.colorScheme.onSurface,
                panelVisible = true,
                panelLayout = layout,
            )
        // The host draws the real AppIndicator menu. Use its entries for this Compose preview.
        Surface(shape = RoundedCornerShape(12.dp)) {
            Column(Modifier.width(240.dp).padding(horizontal = 12.dp, vertical = 4.dp)) {
                panel.menuEntries().forEach { (_, entry) ->
                    if (entry == null) {
                        HorizontalDivider()
                    } else {
                        Row(
                            Modifier.fillMaxWidth().heightIn(min = 36.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                entry.label,
                                color =
                                    if (entry.isEnabled) {
                                        MaterialTheme.colorScheme.onSurface
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                                style = MaterialTheme.typography.bodyMedium,
                            )
                            Icon(entry.icon, contentDescription = null, Modifier.size(16.dp))
                        }
                    }
                }
            }
        }
    }
}

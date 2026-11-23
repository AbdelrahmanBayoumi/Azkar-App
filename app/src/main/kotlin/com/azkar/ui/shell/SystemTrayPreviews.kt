package com.azkar.ui.shell

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.azkar.generated.resources.Res
import com.azkar.generated.resources.trayNextPrayerCountdown
import com.azkar.ui.notifications.DesktopPlatform
import com.azkar.ui.notifications.PrayerMenuLayout
import com.azkar.ui.theme.AZKAR_PREVIEW_CAPTURE_TAG
import org.jetbrains.compose.resources.stringResource

@Preview(widthDp = 210, heightDp = 190)
@Composable
internal fun SystemTrayCompactPreview() = SystemTrayMenuPreview(PrayerMenuLayout.COMPACT)

@Preview(widthDp = 225, heightDp = 366)
@Composable
internal fun SystemTrayExtendedPreview() = SystemTrayMenuPreview(PrayerMenuLayout.EXTENDED)

@Composable
private fun SystemTrayMenuPreview(layout: PrayerMenuLayout) {
    val prayer = previewPrayerStatus()
    val fixture = previewPrayerMenu(DesktopPlatform.LINUX)
    val menu =
        fixture.copy(
            countdown =
                fixture.countdown.copy(
                    label = stringResource(Res.string.trayNextPrayerCountdown, prayer.today.last().name, "15m"),
                ),
            panelVisibility = null,
            trayLayout = layout,
        )
    // AppIndicator supplies the native menu chrome. Match the GNOME tray appearance in Compose.
    val sidePadding = if (layout == PrayerMenuLayout.COMPACT) 26.dp else 36.dp
    Surface(
        modifier = Modifier.fillMaxWidth().testTag(AZKAR_PREVIEW_CAPTURE_TAG),
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFF36363A),
        contentColor = Color.White,
    ) {
        Column(Modifier.padding(start = sidePadding, end = 20.dp)) {
            menu.entries.forEach { entry ->
                if (entry == null) {
                    HorizontalDivider(Modifier.padding(vertical = 10.dp), color = Color(0xFF4A4A4D))
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth().height(36.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = entry.label,
                            color = if (entry.isEnabled) Color(0xFFF5F5F5) else Color(0xFF94999F),
                            fontFamily = FontFamily.SansSerif,
                            fontSize = 14.sp,
                        )
                        Icon(entry.icon, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                    }
                }
            }
        }
    }
}

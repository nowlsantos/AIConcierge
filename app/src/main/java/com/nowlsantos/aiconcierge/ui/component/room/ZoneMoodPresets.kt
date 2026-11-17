package com.nowlsantos.aiconcierge.ui.component.room

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nowlsantos.aiconcierge.ui.component.widget.debouncedClick
import com.nowlsantos.aiconcierge.ui.theme.PeninsulaGold
import com.nowlsantos.aiconcierge.ui.theme.WhitePen
import com.nowlsantos.aiconcierge.ui.theme.sfTypography

/**
 * ZONE MOOD PRESETS
 * Interactive preset scene cards (Relax, Cinema, Daylight, Lounge) with icons and detail descriptions.
 */
@Composable
fun ZoneMoodPresets(
    selectedMood: String,
    onMoodSelect: (String) -> Unit
) {
    val moods = listOf(
        Triple("Relax", "Soft dim lights & quiet climate", Icons.Default.Bedtime),
        Triple("Cinema", "Darkened room & theater focus", Icons.Default.Movie),
        Triple("Daylight", "Full lights & open drapes", Icons.Default.WbSunny),
        Triple("Lounge", "Warm ambient glow & fresh air", Icons.Default.Celebration)
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "PRESET SCENES",
                style = sfTypography.titleMedium,
                color = PeninsulaGold,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Quick Ambience",
                style = sfTypography.labelMedium,
                color = WhitePen.copy(alpha = 0.5f)
            )
        }

        moods.forEach { (title, subtitle, icon) ->
            val isSelected = selectedMood.equals(title, ignoreCase = true)
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (isSelected) PeninsulaGold.copy(alpha = 0.2f) else WhitePen.copy(alpha = 0.05f),
                border = BorderStroke(
                    width = 1.dp,
                    color = if (isSelected) PeninsulaGold else WhitePen.copy(alpha = 0.12f)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(70.dp)
                    .debouncedClick(debounceTimeMs = 300L) { onMoodSelect(title) }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = if (isSelected) PeninsulaGold else WhitePen.copy(alpha = 0.7f),
                        modifier = Modifier.size(26.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = title,
                            style = sfTypography.titleSmall,
                            color = if (isSelected) PeninsulaGold else WhitePen,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold
                        )
                        Text(
                            text = subtitle,
                            style = sfTypography.labelMedium,
                            color = WhitePen.copy(alpha = 0.45f),
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

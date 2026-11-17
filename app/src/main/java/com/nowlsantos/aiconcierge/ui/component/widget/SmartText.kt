package com.nowlsantos.aiconcierge.ui.component.widget

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nowlsantos.aiconcierge.ui.theme.LightPen
import com.nowlsantos.aiconcierge.ui.theme.PeninsulaGold
import com.nowlsantos.aiconcierge.ui.theme.sfTypography

/**
 * ELITE FORMATTER: Detects lists and applies high-end typography
 */
@Composable
fun SmartText(text: String, color: Color = LightPen) {
    val lines = text.split("\n")
    
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        lines.forEach { line ->
            val trimmed = line.trim()
            //if (trimmed.isEmpty()) return@forEach

            // Detect Bullet (starts with -, *, or •)
            val isBullet = trimmed.startsWith("-") || trimmed.startsWith("*") || trimmed.startsWith("•")
            // Detect Numbered List (starts with digit followed by . or ))
            val isNumbered = trimmed.firstOrNull()?.isDigit() == true && (trimmed.getOrNull(1) == '.' || trimmed.getOrNull(1) == ')')

            if (isBullet || isNumbered) {
                Row(modifier = Modifier.padding(start = 12.dp)) {
                    Text(
                        text = if (isBullet) "•" else trimmed.takeWhile { it != '.' && it != ')' } + ".",
                        color = PeninsulaGold,
                        style = sfTypography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = if (isBullet) trimmed.substring(1).trim() else trimmed.substringAfter('.').substringAfter(')').trim(),
                        color = color,
                        style = sfTypography.labelLarge,
                        //lineHeight = 24.sp
                    )
                }
            } else {
                Text(
                    text = trimmed,
                    color = color,
                    style = sfTypography.titleMedium,
                    maxLines = Int.MAX_VALUE,
                    //lineHeight = 26.sp
                )
            }
        }
    }
}

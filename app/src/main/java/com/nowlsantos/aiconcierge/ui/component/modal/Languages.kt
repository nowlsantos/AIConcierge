package com.nowlsantos.aiconcierge.ui.component.modal

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nowlsantos.aiconcierge.ui.theme.PeninsulaGold
import com.nowlsantos.aiconcierge.ui.theme.WhitePen
import com.nowlsantos.aiconcierge.ui.theme.sfTypography

data class LanguageItem(
    val name: String,
    val flagEmoji: String
)

@Composable
fun LanguagesModal(
    activeLanguage: String,
    onLanguageSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val supportedLanguages = remember {
        listOf(
            LanguageItem("English (Default)", "🇬🇧"),
            LanguageItem("Simplified Chinese (简体中文)", "🇨🇳"),
            LanguageItem("Arabic (العربية)", "🇸🇦"),
            LanguageItem("French (Français)", "🇫🇷"),
            LanguageItem("Spanish (Español)", "🇪🇸"),
            LanguageItem("Japanese (日本語)", "🇯🇵")
        )
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Languages",
            style = sfTypography.headlineMedium,
            color = PeninsulaGold,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(bottom = 20.dp)
        )

        supportedLanguages.forEach { lang ->
            val isSelected = activeLanguage.equals(lang.name, ignoreCase = true) ||
                    (lang.name.startsWith("English") && activeLanguage.startsWith("English")) ||
                    (activeLanguage.contains("French", ignoreCase = true) && lang.name.contains("French", ignoreCase = true)) ||
                    (activeLanguage.contains("Spanish", ignoreCase = true) && lang.name.contains("Spanish", ignoreCase = true)) ||
                    (activeLanguage.contains("Chinese", ignoreCase = true) && lang.name.contains("Chinese", ignoreCase = true)) ||
                    (activeLanguage.contains("Arabic", ignoreCase = true) && lang.name.contains("Arabic", ignoreCase = true)) ||
                    (activeLanguage.contains("Japanese", ignoreCase = true) && lang.name.contains("Japanese", ignoreCase = true))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable {
                        onLanguageSelected(lang.name)
                        onDismiss()
                    }
                    .padding(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier.size(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        shape = CircleShape,
                        color = Color.Transparent,
                        border = BorderStroke(
                            width = 2.dp,
                            color = if (isSelected) PeninsulaGold else WhitePen.copy(alpha = 0.4f)
                        ),
                        modifier = Modifier.size(22.dp)
                    ) {}

                    if (isSelected) {
                        Surface(
                            shape = CircleShape,
                            color = PeninsulaGold,
                            modifier = Modifier.size(10.dp)
                        ) {}
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                Text(
                    text = lang.flagEmoji,
                    fontSize = 22.sp,
                    modifier = Modifier.padding(end = 12.dp)
                )

                Text(
                    text = lang.name,
                    style = sfTypography.titleMedium,
                    color = if (isSelected) PeninsulaGold else WhitePen,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                )
            }
        }
    }
}

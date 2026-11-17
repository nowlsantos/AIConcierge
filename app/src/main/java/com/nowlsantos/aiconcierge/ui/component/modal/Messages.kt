package com.nowlsantos.aiconcierge.ui.component.modal

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nowlsantos.aiconcierge.ui.theme.PeninsulaGold
import com.nowlsantos.aiconcierge.ui.theme.sfTypography

@Composable
fun MessagesModal() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Messages",
            style = sfTypography.headlineMedium,
            color = PeninsulaGold,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(bottom = 60.dp)
        )

        Text(
            text = "There are no messages in your inbox.",
            style = sfTypography.bodyLarge,
            color = PeninsulaGold.copy(alpha = 0.85f),
            fontSize = 18.sp
        )
    }
}

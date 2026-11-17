package com.nowlsantos.aiconcierge.ui.component.widget

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nowlsantos.aiconcierge.ui.theme.PeninsulaGold
import com.nowlsantos.aiconcierge.ui.theme.sfTypography

/**
 * SERVICE ACTION WIDGET
 * Action card for Spa, Shopping, Transport, Table reservations, Pen Academy, and PenChat.
 */
@Composable
fun ServiceActionWidget(
    title: String,
    subtitle: String?,
    status: String?,
    iconType: String = "SERVICE"
) {
    val icon = when {
        iconType.uppercase().contains("SPA") || iconType.uppercase().contains("MASSAGE") || iconType.uppercase().contains("FACIAL") || iconType.uppercase().contains("WELLNESS")
            -> Icons.Default.Spa
        iconType.uppercase().contains("SHOPPING") || iconType.uppercase().contains("SHOP") || iconType.uppercase().contains("BOUTIQUE") -> Icons.Default.ShoppingBag
        iconType.uppercase().contains("CAR") || iconType.uppercase().contains("TRANSPORT") -> Icons.Default.DirectionsCar
        iconType.uppercase().contains("HOUSEKEEPING") -> Icons.Default.CleaningServices
        iconType.uppercase().contains("MESSAGE") || iconType.uppercase().contains("NOTIFICATION") -> Icons.Default.Notifications
        iconType.uppercase().contains("RESTAURANT") || iconType.uppercase().contains("TABLE") -> Icons.Default.Restaurant
        iconType.uppercase().contains("ACADEMY") || iconType.uppercase().contains("TOUR") -> Icons.Default.Explore
        iconType.uppercase().contains("SCENE") -> Icons.Default.WbSunny
        iconType.uppercase().contains("PENCHAT") -> Icons.AutoMirrored.Filled.Chat
        iconType.uppercase().contains("DND") -> Icons.Default.DoNotDisturb
        else -> Icons.Default.Star
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
            modifier = Modifier.size(180.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(72.dp),
                    tint = PeninsulaGold
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = title.uppercase(),
            style = sfTypography.titleLarge,
            color = PeninsulaGold,
            letterSpacing = 2.sp
        )

        if (!subtitle.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = subtitle,
                style = sfTypography.bodyLarge,
                color = Color.White
            )
        }

        if (!status.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(16.dp))
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = PeninsulaGold.copy(alpha = 0.2f),
                border = BorderStroke(1.dp, PeninsulaGold.copy(alpha = 0.4f))
            ) {
                Text(
                    text = status.uppercase(),
                    style = sfTypography.labelMedium,
                    color = PeninsulaGold,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
        }
    }
}

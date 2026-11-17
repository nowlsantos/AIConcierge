package com.nowlsantos.aiconcierge.ui.component.widget

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.nowlsantos.aiconcierge.ui.theme.PeninsulaGold
import com.nowlsantos.aiconcierge.ui.theme.sfTypography

@Composable
fun RoomControlWidget(type: String, value: String?) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        val icon = when (type.uppercase()) {
            "CLIMATE" -> Icons.Default.Thermostat
            "LIGHTING" -> Icons.Default.Lightbulb
            "CURTAINS" -> Icons.Default.Window
            "FANS" -> Icons.Default.Air
            "TV" -> Icons.Default.Tv
            "RADIO" -> Icons.Default.Radio
            "MOVIES" -> Icons.Default.Movie
            else -> Icons.Default.Settings
        }
        
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
            modifier = Modifier.size(200.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    icon, 
                    contentDescription = null, 
                    modifier = Modifier.size(80.dp),
                    tint = PeninsulaGold
                )
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Text(
            text = type,
            style = sfTypography.labelLarge,
            color = MaterialTheme.colorScheme.primary
        )
        
        if (value != null) {
            Text(
                text = value,
                style = sfTypography.displayMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
        }
    }
}

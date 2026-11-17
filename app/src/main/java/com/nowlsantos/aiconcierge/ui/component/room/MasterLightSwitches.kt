package com.nowlsantos.aiconcierge.ui.component.room

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.nowlsantos.aiconcierge.ui.theme.GreyPen
import com.nowlsantos.aiconcierge.ui.theme.PeninsulaGold
import com.nowlsantos.aiconcierge.ui.theme.WhitePen
import com.nowlsantos.aiconcierge.ui.theme.sfTypography

/**
 * MASTER LIGHT STATE
 * Dedicated component state for Master Light and Night Light quick switches.
 */
data class MasterLightState(
    val masterLightOn: Boolean = false,
    val nightLightOn: Boolean = false
)

/**
 * REUSABLE SINGLE LIGHT SWITCH
 * Single circular toggle switch with icon, active gold background, and label text.
 */
@Composable
fun LightSwitch(
    title: String,
    isOn: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector = Icons.Default.PowerSettingsNew,
    buttonSize: Dp = 72.dp,
    iconSize: Dp = 38.dp
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        IconButton(
            onClick = onToggle,
            modifier = Modifier
                .size(buttonSize)
                .clip(CircleShape)
                .background(if (isOn) PeninsulaGold.copy(alpha = 0.25f) else WhitePen.copy(alpha = 0.08f))
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = if (isOn) PeninsulaGold else GreyPen,
                modifier = Modifier.size(iconSize)
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = title,
            style = sfTypography.titleSmall,
            color = if (isOn) PeninsulaGold else GreyPen,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}


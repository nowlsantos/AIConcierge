package com.nowlsantos.aiconcierge.ui.component.widget

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nowlsantos.aiconcierge.ui.theme.GoldPen
import com.nowlsantos.aiconcierge.ui.theme.PeninsulaCharcoal
import com.nowlsantos.aiconcierge.ui.theme.WhitePen
import com.nowlsantos.aiconcierge.ui.theme.sfTypography
import com.nowlsantos.aiconcierge.utils.getCardImageResource
import com.nowlsantos.common.DocumentNode

/**
 * MASTER CARD COMPONENT
 * The global luxury standard for all card-based interactions in AIConcierge.
 * Refactored to Surface for built-in Material touch ripples and clean declarative code.
 */
@Composable
fun MasterCard(
    node: DocumentNode,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxSize()
            .debouncedClick(
                debounceTimeMs = 200L,
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        shape = RoundedCornerShape(20.dp),
        color = PeninsulaCharcoal.copy(alpha = 0.80f),
        border = BorderStroke(1.dp, WhitePen.copy(alpha = 0.25f)),
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Retrieve image resource ID dynamically from node.imageAsset, node.iconName, or node.name
            val imageResId = getCardImageResource(node)

            Image(
                painter = painterResource(imageResId),
                contentDescription = node.name,
                modifier = Modifier.size(70.dp),
                contentScale = ContentScale.Fit
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = node.name,
                style = sfTypography.titleLarge,
                color = GoldPen,
                textAlign = TextAlign.Center,
                letterSpacing = 1.5.sp
            )
        }
    }
}

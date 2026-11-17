package com.nowlsantos.aiconcierge.ui.component.widget

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nowlsantos.aiconcierge.ui.theme.PeninsulaCharcoal
import com.nowlsantos.aiconcierge.ui.theme.WhitePen
import com.nowlsantos.aiconcierge.ui.theme.sfTypography
import com.nowlsantos.aiconcierge.utils.getCardImageResource
import com.nowlsantos.common.DocumentNode

/**
 * CHOICE LIST CARD COMPONENT
 * The global luxury standard for all choice-list card interactions in AIConcierge.
 * Refactored to Surface for built-in Material touch ripples and clean declarative code.
 */
@Composable
fun ChoiceListCard(
    node: DocumentNode,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxSize()
            .debouncedClick(debounceTimeMs = 250L, onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        color = PeninsulaCharcoal.copy(alpha = 0.80f),
        border = BorderStroke(1.dp, WhitePen.copy(alpha = 0.25f)),
        //onClick = onClick,
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.BottomCenter
        ) {
            // Retrieve image resource ID dynamically from node.imageAsset, node.iconName, or node.name
            val imageResId = getCardImageResource(node)

            Image(
                painter = painterResource(imageResId),
                contentDescription = node.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .background(PeninsulaCharcoal.copy(alpha = 0.8f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = node.name,
                    style = sfTypography.titleLarge,
                    color = WhitePen,
                    textAlign = TextAlign.Center,
                    letterSpacing = 1.sp
                )
            }
        }
    }
}

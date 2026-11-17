package com.nowlsantos.aiconcierge.ui.component.widget

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nowlsantos.aiconcierge.R
import com.nowlsantos.aiconcierge.ui.theme.GoldPen
import com.nowlsantos.aiconcierge.ui.theme.IndicatorRed
import com.nowlsantos.aiconcierge.ui.theme.sfTypography

@Composable
fun PrivacyWidget(
    isActive: Boolean = false,
    onClick: () -> Unit = {}
) {
    val activeColorAnimation by animateColorAsState(
        targetValue = if (isActive) IndicatorRed else GoldPen.copy(alpha = 0.85f),
        animationSpec = tween(300),
        label = "privacyColor"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.debouncedClick(debounceTimeMs = 250L, onClick = onClick)
    ) {
        Image(
            painter = painterResource(R.drawable.privacy_icon),
            contentDescription = "Privacy Icon",
            modifier = Modifier,
            contentScale = ContentScale.Fit,
            colorFilter = if (isActive) ColorFilter.tint(activeColorAnimation) else null
        )

        Text(
            text = if (!isActive) "Privacy" else "Privacy ON",
            textAlign = TextAlign.Center,
            color = activeColorAnimation,
            style = sfTypography.titleSmall,
            //fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
            letterSpacing = 1.sp
        )
    }
}

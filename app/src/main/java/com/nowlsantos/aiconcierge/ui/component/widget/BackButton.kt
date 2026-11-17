package com.nowlsantos.aiconcierge.ui.component.widget

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.nowlsantos.aiconcierge.ui.theme.GoldPen
import com.nowlsantos.aiconcierge.ui.theme.WhitePen

@Composable
fun BackButton(
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .size(50.dp)
            .debouncedClick(debounceTimeMs = 250L, onClick = onClick),
        shape = CircleShape,
        color = WhitePen.copy(alpha = 0.15f),
        contentColor = WhitePen,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = GoldPen,
                modifier = Modifier.size(40.dp)
            )
        }
    }
}

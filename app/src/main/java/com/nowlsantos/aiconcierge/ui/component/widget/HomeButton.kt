package com.nowlsantos.aiconcierge.ui.component.widget

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.nowlsantos.aiconcierge.ui.theme.WhitePen

@Composable
fun HomeButton(
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .size(70.dp)
            .padding(end = 8.dp, bottom = 8.dp)
            .debouncedClick(debounceTimeMs = 250L, onClick = onClick),
        shape = CircleShape,
        color = WhitePen.copy(alpha = 0.5f),
        contentColor = WhitePen,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = Icons.Default.Home,
                contentDescription = "Home",
                tint = WhitePen,
                modifier = Modifier.size(40.dp)
            )
        }
    }
}

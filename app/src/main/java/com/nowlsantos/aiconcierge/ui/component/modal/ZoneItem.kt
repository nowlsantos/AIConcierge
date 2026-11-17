package com.nowlsantos.aiconcierge.ui.component.modal

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nowlsantos.aiconcierge.ui.theme.WhitePen
import com.nowlsantos.aiconcierge.ui.theme.sfTypography
import com.nowlsantos.aiconcierge.utils.getMediaDrawable

@Composable
fun ZoneItem(
    id: String,
    label: String,
    onClick: () -> Unit
) {
    Column(modifier = Modifier
        .fillMaxHeight()
        .clickable { onClick() },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        val drawableRes = getMediaDrawable(id)
        Image(
            painter = painterResource(drawableRes),
            contentDescription = label,
            contentScale = ContentScale.Fit,
            modifier = Modifier.size(35.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = label,
            style = sfTypography.titleSmall,
            color = WhitePen,
            fontWeight = FontWeight.Medium
        )
    }
}

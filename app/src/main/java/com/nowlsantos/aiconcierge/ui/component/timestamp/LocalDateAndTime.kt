package com.nowlsantos.aiconcierge.ui.component.timestamp

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nowlsantos.aiconcierge.ui.theme.WhitePen
import com.nowlsantos.aiconcierge.ui.theme.sfTypography
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Calendar
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun LocalDateAndTime(
    modifier: Modifier = Modifier
) {
    var currentTime by remember { mutableStateOf(Calendar.getInstance().time) }

    LaunchedEffect(Unit) {
        while (true) {
            currentTime = Calendar.getInstance().time
            delay(1000.milliseconds)
        }
    }

    val locale = LocalConfiguration.current.locales[0]
    val timeFormatter = remember(locale) { SimpleDateFormat("h:mm a", locale) }
    val dateFormatter = remember(locale) { SimpleDateFormat("EEEE, MMMM dd, yyyy", locale) }

    /*Text(
        text = dateFormatter.format(currentTime),
        style = sfTypography.titleLarge,
        color = WhitePen.copy(alpha = 0.85f)
    )*/
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = timeFormatter.format(currentTime),
            style = sfTypography.headlineMedium,
            color = WhitePen,
            fontWeight = FontWeight.Normal
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = dateFormatter.format(currentTime),
            style = sfTypography.titleSmall,
            color = WhitePen.copy(alpha = 0.85f)
        )
    }
}

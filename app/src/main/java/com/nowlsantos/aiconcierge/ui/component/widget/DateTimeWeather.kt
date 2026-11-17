package com.nowlsantos.aiconcierge.ui.component.widget

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.nowlsantos.aiconcierge.ui.component.timestamp.LocalDateAndTime
import com.nowlsantos.aiconcierge.ui.component.weather.LocalWeatherWidget
import com.nowlsantos.aiconcierge.ui.theme.WhitePen

/**
 * PENINSULA DATE AND WEATHER WIDGET
 * Displays live Open-Meteo weather along with current date and time.
 */
@Composable
fun DateTimeWeather(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceAround
    ) {
        // Live Open-Meteo Weather Widget
        LocalWeatherWidget()

        HorizontalDivider(modifier = modifier.fillMaxWidth(0.9f), thickness = 2.dp)

        // Local Time and Date Display
        LocalDateAndTime()
    }
}

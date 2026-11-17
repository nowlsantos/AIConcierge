package com.nowlsantos.aiconcierge.ui.component.weather

import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.nowlsantos.aiconcierge.ui.theme.GoldPen
import com.nowlsantos.aiconcierge.ui.theme.WhitePen
import com.nowlsantos.aiconcierge.ui.theme.sfTypography
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * LOCAL WEATHER WIDGET
 * Displays current London weather with 15-minute in-memory caching.
 * Eliminates loading spinner flashes when returning to the Main Screen.
 */
@Composable
fun LocalWeatherWidget() {
    // 1. Initial State: Check if a non-expired weather response exists in WeatherCache
    val initialWeather = WeatherCache.cachedResponse ?: remember {
        OpenMeteoResponse(current = CurrentWeather(temperature = 26.0, weatherCode = 3))
    }

    var weatherState by remember { mutableStateOf(initialWeather) }
    var isLoading by remember { mutableStateOf(WeatherCache.cachedResponse == null) }

    LaunchedEffect(Unit) {
        val now = System.currentTimeMillis()
        val isExpired = (now - WeatherCache.lastFetchTimeMillis) > WeatherCache.CACHE_EXPIRATION_MS

        if (WeatherCache.cachedResponse == null || isExpired) {
            try {
                val liveData = withContext(Dispatchers.IO) {
                    WeatherApiRepository.fetchLondonWeather()
                }
                WeatherCache.cachedResponse = liveData
                WeatherCache.lastFetchTimeMillis = now
                weatherState = liveData
                isLoading = false
            } catch (_: Exception) {
                // Keep current/default weather on network offline or error
                isLoading = false
            }
        } else {
            // Instantly use RAM cached response with 0ms loading!
            isLoading = false
        }
    }

    Box(contentAlignment = Alignment.Center) {
        if (isLoading) {
            CircularProgressIndicator(
                color = GoldPen,
                strokeWidth = 2.dp,
                modifier = Modifier.size(24.dp)
            )
        } else {
            val current = weatherState.current
            val weatherDetails = getWeatherDetails(current.weatherCode)

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = weatherDetails.icon,
                    contentDescription = weatherDetails.description,
                    modifier = Modifier.size(80.dp),
                    tint = weatherDetails.iconColor
                )

                Spacer(modifier = Modifier.width(16.dp))

                Column {
                    Text(
                        text = "${current.temperature.toInt()}°C",
                        style = sfTypography.headlineLarge,
                        color = WhitePen,
                    )
                    Text(
                        text = weatherDetails.description,
                        style = sfTypography.titleMedium,
                        color = GoldPen.copy(alpha = 0.85f)
                    )
                }
            }
        }
    }
}

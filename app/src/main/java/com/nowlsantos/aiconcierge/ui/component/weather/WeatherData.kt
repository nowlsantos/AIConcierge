package com.nowlsantos.aiconcierge.ui.component.weather

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AcUnit
import androidx.compose.material.icons.outlined.Air
import androidx.compose.material.icons.outlined.Bedtime
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.CloudQueue
import androidx.compose.material.icons.outlined.Grain
import androidx.compose.material.icons.outlined.Thunderstorm
import androidx.compose.material.icons.outlined.Umbrella
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.nowlsantos.aiconcierge.ui.theme.*
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * IN-MEMORY WEATHER CACHE
 * Caches OpenMeteo weather response in RAM for 15 minutes.
 * Prevents redundant HTTP network requests and loading spinners when re-entering the Main Screen.
 */
object WeatherCache {
    var cachedResponse: OpenMeteoResponse? = null
    var lastFetchTimeMillis: Long = 0L
    const val CACHE_EXPIRATION_MS = 15 * 60 * 1000L // 15 Minutes Cache TTL
}

@Serializable
data class OpenMeteoResponse(
    val current: CurrentWeather,
    val daily: DailyForecast? = null
)

@Serializable
data class CurrentWeather(
    @SerialName("temperature_2m") val temperature: Double,
    @SerialName("relative_humidity_2m") val humidity: Int? = null,
    @SerialName("weather_code") val weatherCode: Int
)

@Serializable
data class DailyForecast(
    @SerialName("weather_code") val weatherCodes: List<Int> = emptyList(),
    @SerialName("temperature_2m_max") val maxTemps: List<Double> = emptyList(),
    @SerialName("temperature_2m_min") val minTemps: List<Double> = emptyList()
)

object WeatherApiRepository {
    private val client by lazy {
        HttpClient(OkHttp) {
            install(HttpTimeout) {
                requestTimeoutMillis = 10_000L
                connectTimeoutMillis = 5_000L
                socketTimeoutMillis = 10_000L
            }
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
        }
    }

    suspend fun fetchLondonWeather(): OpenMeteoResponse = withContext(Dispatchers.IO) {
        val url = "https://api.open-meteo.com/v1/forecast?latitude=51.5074&longitude=-0.1278&current=temperature_2m,relative_humidity_2m,weather_code"
        client.get(url).body()
    }
}

data class WeatherUiInfo(
    val description: String,
    val icon: ImageVector,
    val iconColor: Color
)

fun getWeatherDetails(code: Int): WeatherUiInfo {
    return when (code) {
        0 -> WeatherUiInfo("Clear Sky", Icons.Outlined.WbSunny, WbSunny)
        1 -> WeatherUiInfo("Mainly Clear", Icons.Outlined.Bedtime, Bedtime)
        2 -> WeatherUiInfo("Partly Cloudy", Icons.Outlined.CloudQueue, CloudQueue)
        3 -> WeatherUiInfo("Overcast", Icons.Outlined.Cloud, Cloud)
        45 -> WeatherUiInfo("Fog", Icons.Outlined.Air, Foggy)
        48 -> WeatherUiInfo("Depositing Rime Fog", Icons.Outlined.AcUnit, AcUnit)

        // Drizzle
        51 -> WeatherUiInfo("Drizzle: Light", Icons.Outlined.Grain, Grain)
        53 -> WeatherUiInfo("Drizzle: Moderate", Icons.Outlined.Grain, Grain)
        55 -> WeatherUiInfo("Drizzle: Dense", Icons.Outlined.Grain, Grain)
        56 -> WeatherUiInfo("Freezing Drizzle: Light", Icons.Outlined.AcUnit, AcUnit)
        57 -> WeatherUiInfo("Freezing Drizzle: Dense", Icons.Outlined.AcUnit, AcUnit)

        // Rain
        61 -> WeatherUiInfo("Rain: Slight", Icons.Outlined.WaterDrop, Rainy)
        63 -> WeatherUiInfo("Rain: Moderate", Icons.Outlined.WaterDrop, Rainy)
        65 -> WeatherUiInfo("Rain: Heavy", Icons.Outlined.Umbrella, Umbrella)
        66 -> WeatherUiInfo("Freezing Rain: Light", Icons.Outlined.AcUnit, AcUnit)
        67 -> WeatherUiInfo("Freezing Rain: Heavy", Icons.Outlined.AcUnit, AcUnit)

        // Snow
        71 -> WeatherUiInfo("Snow fall: Slight", Icons.Outlined.AcUnit, Snowing)
        73 -> WeatherUiInfo("Snow fall: Moderate", Icons.Outlined.AcUnit, Snowing)
        75 -> WeatherUiInfo("Snow fall: Heavy", Icons.Outlined.AcUnit, Snowing)
        77 -> WeatherUiInfo("Snow grains", Icons.Outlined.Grain, Grain)

        // Rain Showers
        80 -> WeatherUiInfo("Rain showers: Slight", Icons.Outlined.WaterDrop, WaterDrop)
        81 -> WeatherUiInfo("Rain showers: Moderate", Icons.Outlined.WaterDrop, WaterDrop)
        82 -> WeatherUiInfo("Rain showers: Violent", Icons.Outlined.Thunderstorm, Thunderstorm)

        // Snow Showers
        85 -> WeatherUiInfo("Snow showers: Slight", Icons.Outlined.AcUnit, Snowing)
        86 -> WeatherUiInfo("Snow showers: Heavy", Icons.Outlined.AcUnit, Snowing)

        // Thunderstorms
        95 -> WeatherUiInfo("Thunderstorm: Slight or moderate", Icons.Outlined.Thunderstorm, Thunderstorm)
        96 -> WeatherUiInfo("Thunderstorm with slight hail", Icons.Outlined.Thunderstorm, Thunderstorm)
        99 -> WeatherUiInfo("Thunderstorm with heavy hail", Icons.Outlined.Thunderstorm, Thunderstorm)

        // Fallback catch-all
        else -> WeatherUiInfo("Overcast Clouds", Icons.Outlined.Cloud, Cloud)
    }
}

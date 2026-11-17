package com.nowlsantos.aiconcierge.di

import com.nowlsantos.aiconcierge.BuildConfig
import com.nowlsantos.aiconcierge.data.remote.GeminiTextClient
import com.nowlsantos.aiconcierge.data.remote.KtorGeminiTextClient
import com.nowlsantos.aiconcierge.domain.intent.ActionIntentProcessor
import com.nowlsantos.aiconcierge.domain.navigation.NodeNavigationEngine
import com.nowlsantos.aiconcierge.domain.sync.BrainConnectionWatchdog
import com.nowlsantos.aiconcierge.domain.tool.ConciergeToolHandler
import com.nowlsantos.aiconcierge.domain.tool.ToolCallDispatcher
import com.nowlsantos.aiconcierge.domain.tool.ToolExecutionResult
import com.nowlsantos.aiconcierge.hardware.capture.AudioCaptureEngine
import com.nowlsantos.aiconcierge.hardware.playback.AudioPlaybackEngine
import com.nowlsantos.aiconcierge.presentation.ConciergeViewModel
import com.nowlsantos.aiconcierge.presentation.coordinator.GeminiLiveActionCoordinator
import com.nowlsantos.aiconcierge.repository.ConciergeRepository
import com.nowlsantos.aiconcierge.speech.GeminiLiveManager
import com.nowlsantos.aiconcierge.speech.live.GeminiLiveClient
import com.nowlsantos.aiconcierge.speech.live.KtorGeminiLiveClient
import io.ktor.client.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.sse.*
import io.ktor.client.plugins.websocket.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.serialization.json.Json
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.core.qualifier.named
import org.koin.dsl.module

/**
 * SHARED JSON CONFIGURATION - Standard across all modules
 */
val networkJson = Json {
    ignoreUnknownKeys = true
    coerceInputValues = true
    isLenient = true
    encodeDefaults = true
}

private val LIVE_HTTP_CLIENT = named("liveHttpClient")

val appModule = module {
    single {
        HttpClient(OkHttp) {
            install(SSE)
            install(HttpTimeout) {
                requestTimeoutMillis = 15_000L
                connectTimeoutMillis = 1500L // Fast-fail 1.5s connect timeout for local offline fallback
                socketTimeoutMillis = 15_000L
            }
            install(ContentNegotiation) {
                json(networkJson)
            }
        }
    }

    single(LIVE_HTTP_CLIENT) {
        HttpClient(OkHttp) {
            install(WebSockets) {
                pingIntervalMillis = 20_000L
            }
            install(HttpTimeout) {
                connectTimeoutMillis = 15_000L
            }
        }
    }

    single<GeminiTextClient> {
        KtorGeminiTextClient(
            httpClient = get(),
            apiKey = BuildConfig.GEMINI_API_KEY
        )
    }

    single<GeminiLiveClient> {
        KtorGeminiLiveClient(
            httpClient = get(LIVE_HTTP_CLIENT),
            apiKey = BuildConfig.GEMINI_API_KEY
        )
    }

    single { AudioCaptureEngine(context = androidContext()) }

    single {
        AudioPlaybackEngine(
            scope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
            sampleRate = 24000
        )
    }

    single {
        val handlers = mapOf<String, ConciergeToolHandler>(
            "get_live_weather" to ConciergeToolHandler {
                ToolExecutionResult.Success("London weather: 18°C, Partly Cloudy with mild breeze.")
            }
        )
        ToolCallDispatcher(handlers = handlers)
    }

    single {
        GeminiLiveManager(
            scope = CoroutineScope(SupervisorJob() + Dispatchers.Main),
            audioCaptureEngine = get(),
            audioPlaybackEngine = get(),
            context = androidContext(),
            toolDispatcher = get(),
            liveClient = get()
        )
    }

    single {
        ConciergeRepository(
            geminiTextClient = get()
        )
    }

    single { NodeNavigationEngine() }
    single { ActionIntentProcessor(navigationEngine = get()) }
    single { BrainConnectionWatchdog(repository = get(), navigationEngine = get()) }
    single {
        GeminiLiveActionCoordinator(
            geminiLiveManager = get(),
            intentProcessor = get(),
            navigationEngine = get(),
            repository = get()
        )
    }

    viewModel {
        ConciergeViewModel(
            context = androidContext(),
            geminiLiveManager = get(),
            repository = get(),
            navigationEngine = get(),
            intentProcessor = get(),
            watchdog = get(),
            actionCoordinator = get()
        )
    }
}

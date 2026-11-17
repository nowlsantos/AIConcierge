package com.nowlsantos.aiconcierge.speech

import com.nowlsantos.aiconcierge.BuildConfig
import com.nowlsantos.aiconcierge.domain.tool.ToolCallDispatcher
import com.nowlsantos.aiconcierge.domain.tool.ToolExecutionResult
import com.nowlsantos.aiconcierge.hardware.capture.AudioCaptureEngine
import com.nowlsantos.aiconcierge.hardware.playback.AudioPlaybackEngine
import com.nowlsantos.aiconcierge.hardware.playback.PlaybackState
import com.nowlsantos.aiconcierge.speech.config.ArisSystemPrompt
import com.nowlsantos.aiconcierge.speech.config.GeminiToolDeclarations
import com.nowlsantos.aiconcierge.speech.live.GeminiLiveClient
import com.nowlsantos.aiconcierge.speech.live.GeminiLiveConnectionException
import com.nowlsantos.aiconcierge.speech.live.GeminiLiveModelConfig
import com.nowlsantos.aiconcierge.speech.live.KtorGeminiLiveClient
import com.nowlsantos.aiconcierge.speech.live.LiveFunctionResponse
import com.nowlsantos.aiconcierge.speech.live.LiveServerEvent
import com.nowlsantos.aiconcierge.speech.live.LiveSessionTransport
import com.nowlsantos.aiconcierge.speech.tool.GeminiToolDispatcher
import com.nowlsantos.aiconcierge.utils.Logger
import com.nowlsantos.common.DocumentNode
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.websocket.WebSockets
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

sealed interface GeminiLiveState {
    data object Idle : GeminiLiveState
    data object Connecting : GeminiLiveState
    data object Listening : GeminiLiveState
    data object Speaking : GeminiLiveState
    data class Error(val message: String) : GeminiLiveState
}

/**
 * GEMINI LIVE MANAGER
 * Direct Ktor WebSocket native audio-streaming voice session manager for Gemini Live (gemini-3.8-live).
 * Features ultra-fast watchdog recovery to guarantee Aris never gets stuck in Speaking mode.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class GeminiLiveManager(
    private val scope: CoroutineScope,
    private val audioCaptureEngine: AudioCaptureEngine,
    private val audioPlaybackEngine: AudioPlaybackEngine,
    private val context: android.content.Context,
    private val toolDispatcher: ToolCallDispatcher? = null,
    private val liveClient: GeminiLiveClient? = null
) {
    private val _state = MutableStateFlow<GeminiLiveState>(GeminiLiveState.Idle)
    val state: StateFlow<GeminiLiveState> = _state.asStateFlow()

    @Volatile
    private var session: LiveSessionTransport? = null
    private var connectJob: Job? = null
    private var protocolJob: Job? = null
    private var stateObserverJob: Job? = null
    private var silenceTimeoutJob: Job? = null
    private var unmuteJob: Job? = null
    private var toolExecutionJob: Job? = null
    private val toolExecutionMutex = Mutex()
    private var captureJob: Job? = null
    private val fallbackLiveClient: GeminiLiveClient by lazy {
        val wsClient = HttpClient(OkHttp) {
            install(WebSockets) {
                pingIntervalMillis = 20_000L
            }
            install(HttpTimeout) {
                connectTimeoutMillis = 15_000L
            }
        }
        KtorGeminiLiveClient(
            httpClient = wsClient,
            apiKey = BuildConfig.GEMINI_API_KEY
        )
    }

    @Volatile
    private var isStandbyPending = false

    @Volatile
    private var isServerTurnComplete = true

    @Volatile
    private var hasReceivedAudioForTurn = false

    @Volatile
    private var speakingEnteredAtMillis = 0L

    @Volatile
    private var isWelcomeGreetingActive = false

    private val audioStateLock = Any()

    /**
     * Dynamic Root Node Provider.
     */
    var getRootNode: (() -> DocumentNode?)? = null

    /**
     * Callback triggered as soon as Aris begins streaming speech audio for a turn.
     */
    var onAudioSpeechStarted: (() -> Unit)? = null

    val fallbackDispatcher = GeminiToolDispatcher()

    // Fallback lambdas
    var onSpokenTextReceived: ((String) -> Unit)?
        get() = fallbackDispatcher.onSpokenTextReceived
        set(value) { fallbackDispatcher.onSpokenTextReceived = value }
    var onNavigate: (suspend (String) -> String)?
        get() = fallbackDispatcher.onNavigate
        set(value) { fallbackDispatcher.onNavigate = value }
    var onScrollPage: (suspend (String) -> String)?
        get() = fallbackDispatcher.onScrollPage
        set(value) { fallbackDispatcher.onScrollPage = value }
    var onSetTheme: ((Boolean) -> Unit)?
        get() = fallbackDispatcher.onSetTheme
        set(value) { fallbackDispatcher.onSetTheme = value }
    var onRoomControl: (suspend (String, String, String) -> String)?
        get() = fallbackDispatcher.onRoomControl
        set(value) { fallbackDispatcher.onRoomControl = value }
    var onMediaControl: (suspend (String, String, String) -> String)?
        get() = fallbackDispatcher.onMediaControl
        set(value) { fallbackDispatcher.onMediaControl = value }
    var onGetServiceInfo: (suspend (String) -> String)?
        get() = fallbackDispatcher.onGetServiceInfo
        set(value) { fallbackDispatcher.onGetServiceInfo = value }
    var onSelectOption: (suspend (String, String) -> String)?
        get() = fallbackDispatcher.onSelectOption
        set(value) { fallbackDispatcher.onSelectOption = value }
    var onSubmitOrder: (suspend () -> String)?
        get() = fallbackDispatcher.onSubmitOrder
        set(value) { fallbackDispatcher.onSubmitOrder = value }
    var onConfirmOrder: (suspend () -> String)?
        get() = fallbackDispatcher.onConfirmOrder
        set(value) { fallbackDispatcher.onConfirmOrder = value }
    var onCancelOrder: (suspend (String) -> String)?
        get() = fallbackDispatcher.onCancelOrder
        set(value) { fallbackDispatcher.onCancelOrder = value }
    var onBookSpa: (suspend (String, Int, String) -> String)?
        get() = fallbackDispatcher.onBookSpa
        set(value) { fallbackDispatcher.onBookSpa = value }
    var onRequestHousekeeping: (suspend (String, Boolean) -> String)?
        get() = fallbackDispatcher.onRequestHousekeeping
        set(value) { fallbackDispatcher.onRequestHousekeeping = value }
    var onSetDoNotDisturb: (suspend (Boolean, String) -> String)?
        get() = fallbackDispatcher.onSetDoNotDisturb
        set(value) { fallbackDispatcher.onSetDoNotDisturb = value }
    var onScheduleValet: (suspend (String) -> String)?
        get() = fallbackDispatcher.onScheduleValet
        set(value) { fallbackDispatcher.onScheduleValet = value }
    var onBookExperience: (suspend (String, Int, String) -> String)?
        get() = fallbackDispatcher.onBookExperience
        set(value) { fallbackDispatcher.onBookExperience = value }
    var onRequestTransportation: (suspend (String, String, String) -> String)?
        get() = fallbackDispatcher.onRequestTransportation
        set(value) { fallbackDispatcher.onRequestTransportation = value }
    var onReserveTable: (suspend (String, Int, String) -> String)?
        get() = fallbackDispatcher.onReserveTable
        set(value) { fallbackDispatcher.onReserveTable = value }
    var onSetMasterScene: (suspend (String) -> String)?
        get() = fallbackDispatcher.onSetMasterScene
        set(value) { fallbackDispatcher.onSetMasterScene = value }
    var onSendPenChat: (suspend (String, String) -> String)?
        get() = fallbackDispatcher.onSendPenChat
        set(value) { fallbackDispatcher.onSendPenChat = value }
    var onGetWeather: (suspend () -> String)?
        get() = fallbackDispatcher.onGetWeather
        set(value) { fallbackDispatcher.onGetWeather = value }
    var onCloseModal: (suspend () -> String)?
        get() = fallbackDispatcher.onCloseModal
        set(value) { fallbackDispatcher.onCloseModal = value }
    var onStandby: (() -> Unit)?
        get() = fallbackDispatcher.onStandby
        set(value) { fallbackDispatcher.onStandby = value }

    private var speakingWatchdogJob: Job? = null

    init {
        observePlaybackState()
        startSpeakingWatchdog()
    }

    private fun resolveLiveClient(): GeminiLiveClient = liveClient ?: fallbackLiveClient

    private fun startSpeakingWatchdog() {
        if (speakingWatchdogJob?.isActive == true) return
        speakingWatchdogJob = scope.launch(Dispatchers.Default) {
            while (isActive) {
                delay(1000.milliseconds)
                synchronized(audioStateLock) {
                    if (session != null && _state.value == GeminiLiveState.Speaking) {
                        val isPlaybackIdle = audioPlaybackEngine.playbackState.value == PlaybackState.Idle
                        val isQueueEmpty = audioPlaybackEngine.audioQueue.isEmpty
                        val isToolRunning = toolExecutionJob?.isActive == true
                        val elapsedSpeakingMs = System.currentTimeMillis() - speakingEnteredAtMillis

                        // Only recover if the server turn is already marked complete, OR if we have been stuck
                        // waiting for audio for > 8 seconds with no tool call in progress.
                        if (isPlaybackIdle && isQueueEmpty && !isToolRunning &&
                            (isServerTurnComplete || elapsedSpeakingMs > 8000L)
                        ) {
                            Logger.w("SPEAKING WATCHDOG: Recovering from hung Speaking state (enforcing turn completion).", "ARIS_GEMINI")
                            isServerTurnComplete = true
                            if (isStandbyPending) {
                                isStandbyPending = false
                                stopSession()
                            } else {
                                scheduleMicUnmute()
                            }
                        }
                    }
                }
            }
        }
    }

    fun requestStandbyAfterPlayback() {
        isStandbyPending = true
    }

    private fun observePlaybackState() {
        stateObserverJob = scope.launch(Dispatchers.Default) {
            audioPlaybackEngine.playbackState.collect { playbackState ->
                synchronized(audioStateLock) {
                    when (playbackState) {
                        PlaybackState.Playing, PlaybackState.Draining -> {
                            unmuteJob?.cancel()
                            unmuteJob = null
                            silenceTimeoutJob?.cancel()
                            if (_state.value != GeminiLiveState.Speaking) {
                                speakingEnteredAtMillis = System.currentTimeMillis()
                            }
                            _state.value = GeminiLiveState.Speaking
                            audioCaptureEngine.pauseRecording()
                        }
                        PlaybackState.Idle -> {
                            if (isStandbyPending && (isServerTurnComplete || hasReceivedAudioForTurn)) {
                                isStandbyPending = false
                                stopSession()
                            } else if (session != null && (isServerTurnComplete || hasReceivedAudioForTurn)) {
                                // Resilient state transition: once playback drains after receiving turn audio or server turn completion, resume listening
                                isServerTurnComplete = true
                                scheduleMicUnmute()
                            }
                        }
                    }
                }
            }
        }
    }

    private fun scheduleMicUnmute() {
        unmuteJob?.cancel()
        unmuteJob = scope.launch(Dispatchers.Default) {
            // 450ms acoustic reverberation guard ensures room reflections completely dissipate before mic activates
            delay(450.milliseconds)
            synchronized(audioStateLock) {
                if (session == null) return@launch
                isWelcomeGreetingActive = false
                _state.value = GeminiLiveState.Listening
                hasReceivedAudioForTurn = false
                isServerTurnComplete = false
                audioCaptureEngine.resumeRecording()
                resetSilenceTimeout()
            }
        }
    }

    private fun resetSilenceTimeout() {
        silenceTimeoutJob?.cancel()
        silenceTimeoutJob = scope.launch(Dispatchers.IO) {
            delay(60.seconds)
            if (_state.value == GeminiLiveState.Listening) {
                withContext(Dispatchers.Main) {
                    stopSession()
                }
            }
        }
    }

    private fun buildDynamicDirectoryPrompt(): String {
        val root = getRootNode?.invoke() ?: return ""
        val names = mutableSetOf<String>()
        fun collectNames(node: DocumentNode) {
            if (node.name.isNotBlank()) {
                names.add(node.name)
            }
            node.children.forEach { collectNames(it) }
        }
        collectNames(root)
        return "\n\n[DYNAMIC HOTEL NAVIGATION DIRECTORY]\nList of valid target names: ${names.joinToString(", ")}"
    }

    private fun createModelConfig(): GeminiLiveModelConfig {
        val dynamicDirectory = buildDynamicDirectoryPrompt()
        val fullSystemInstruction = ArisSystemPrompt.SYSTEM_INSTRUCTION + dynamicDirectory

        return GeminiLiveModelConfig(
            modelName = GeminiLiveModelConfig.DEFAULT_MODEL_NAME,
            voiceName = "Puck",
            systemInstruction = fullSystemInstruction,
            tools = GeminiToolDeclarations.ALL_DECLARATIONS
        )
    }

    fun warmupSession() {
        synchronized(audioStateLock) {
            if (session != null || connectJob?.isActive == true || _state.value != GeminiLiveState.Idle) return
        }

        connectJob = scope.launch(Dispatchers.IO) {
            try {
                Logger.d("HOT STANDBY: Pre-warming Gemini Live WebSocket (${GeminiLiveModelConfig.DEFAULT_MODEL_NAME})...", "ARIS_GEMINI")
                val config = createModelConfig()
                val newSession = resolveLiveClient().connect(config)
                var accepted = false
                synchronized(audioStateLock) {
                    if (isActive) {
                        session = newSession
                        accepted = true
                    }
                }
                if (!accepted) {
                    newSession.close()
                    return@launch
                }

                audioPlaybackEngine.startConsumer()
                startCaptureAndProtocolJobs(newSession)
                Logger.d("HOT STANDBY: Gemini Live WebSocket pre-warmed successfully.", "ARIS_GEMINI")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Logger.w("HOT STANDBY: Pre-warm deferred: ${e.localizedMessage}", "ARIS_GEMINI")
                synchronized(audioStateLock) {
                    session = null
                }
            }
        }
    }

    fun startSession() {
        synchronized(audioStateLock) {
            if (_state.value == GeminiLiveState.Connecting || _state.value == GeminiLiveState.Speaking) return

            isStandbyPending = false
            isServerTurnComplete = false
            hasReceivedAudioForTurn = false
            isWelcomeGreetingActive = true
            speakingEnteredAtMillis = System.currentTimeMillis()

            val existingSession = session
            if (existingSession != null && existingSession.isConnected && protocolJob?.isActive == true) {
                Logger.d("HOT STANDBY: Instant-triggering welcome message on warm session...", "ARIS_GEMINI")
                _state.value = GeminiLiveState.Speaking
                audioCaptureEngine.pauseRecording()
                audioPlaybackEngine.startConsumer()
                startSpeakingWatchdog()

                scope.launch(Dispatchers.IO) {
                    try {
                        existingSession.sendText("Please greet the guest with your welcome message.", turnComplete = true)
                    } catch (e: Exception) {
                        Logger.w("Pre-warmed session send failed, reconnecting: ${e.localizedMessage}", "ARIS_GEMINI")
                        stopSession(reWarmup = false)
                        startSession()
                    }
                }
                return
            }

            _state.value = GeminiLiveState.Connecting
            startSpeakingWatchdog()
        }

        connectJob = scope.launch(Dispatchers.IO) {
            try {
                val config = createModelConfig()
                val newSession = resolveLiveClient().connect(config)
                var accepted = false
                synchronized(audioStateLock) {
                    if (isActive) {
                        session = newSession
                        accepted = true
                    }
                }
                if (!accepted) {
                    newSession.close()
                    return@launch
                }

                audioPlaybackEngine.startConsumer()
                startCaptureAndProtocolJobs(newSession)

                Logger.d("DEBUG: Connection established (${config.modelName}). Triggering welcome message...", "ARIS_GEMINI")
                synchronized(audioStateLock) {
                    speakingEnteredAtMillis = System.currentTimeMillis()
                    _state.value = GeminiLiveState.Speaking
                    isWelcomeGreetingActive = true
                }
                audioCaptureEngine.pauseRecording()
                newSession.sendText("Please greet the guest with your welcome message.", turnComplete = true)

            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Logger.e("CRITICAL: Connection failed (${e.javaClass.simpleName}): ${e.localizedMessage}", e, "ARIS_GEMINI")
                if (e is GeminiLiveConnectionException) {
                    Logger.w("HANDSHAKE FAILURE ADVICE: Verify GEMINI_API_KEY in local.properties and ensure the Generative Language API is enabled in Google Cloud Console.", "ARIS_GEMINI")
                }
                val errorMessage = e.localizedMessage ?: "Connection Failed: Channel closed by server"
                _state.value = GeminiLiveState.Error(errorMessage)
                stopSession(reWarmup = false)
            }
        }
    }

    private fun startCaptureAndProtocolJobs(activeSession: LiveSessionTransport) {
        captureJob?.cancel()
        captureJob = scope.launch(Dispatchers.IO) {
            try {
                audioCaptureEngine.startCaptureStream().collect { chunk ->
                    val currentSession = session
                    if (currentSession === activeSession && _state.value == GeminiLiveState.Listening) {
                        currentSession.sendAudio(chunk)
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Logger.e("Capture stream exception: ${e.localizedMessage}", e, "ARIS_GEMINI")
            }
        }

        protocolJob?.cancel()
        protocolJob = scope.launch(Dispatchers.Default) {
            try {
                Logger.d("PROTOCOL: Observer Started.", "ARIS_GEMINI")
                activeSession.events.collect { event ->
                    handleProtocolEvent(event)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Logger.e("PROTOCOL ERROR: ${e.localizedMessage}", e, "ARIS_GEMINI")
            } finally {
                Logger.w("PROTOCOL: Observer Finished.", "ARIS_GEMINI")
                synchronized(audioStateLock) {
                    if (session === activeSession) {
                        session = null
                        if (_state.value == GeminiLiveState.Listening || _state.value == GeminiLiveState.Connecting) {
                            _state.value = GeminiLiveState.Idle
                        }
                    }
                }
                withContext(NonCancellable + Dispatchers.IO) {
                    activeSession.close()
                }
            }
        }
    }

    fun sendContextUpdate(contextText: String) {
        val currentSession = session ?: return
        if (_state.value == GeminiLiveState.Speaking) return
        scope.launch(Dispatchers.IO) {
            try {
                val formatted = if (contextText.startsWith("[SYSTEM MEMORY SYNC")) {
                    contextText
                } else {
                    "[SYSTEM MEMORY SYNC - DO NOT SPEAK ALOUD] $contextText"
                }
                // turnComplete = false injects screen state silently without forcing a spoken turn or interrupting generation
                currentSession.sendText(formatted, turnComplete = false)
            } catch (e: Exception) {
                Logger.e("Context update failed", e, "ARIS_GEMINI")
            }
        }
    }

    private suspend fun handleProtocolEvent(event: LiveServerEvent) {
        when (event) {
            is LiveServerEvent.ServerContent -> {
                if (event.interrupted) {
                    Logger.d("MIC LOCK IMMUNITY: Dropping server-side interruption to ensure complete speech output.", "ARIS_GEMINI")
                    return
                }

                if (event.audioChunks.isNotEmpty()) {
                    synchronized(audioStateLock) {
                        isServerTurnComplete = false
                        hasReceivedAudioForTurn = true
                        speakingEnteredAtMillis = System.currentTimeMillis()
                        unmuteJob?.cancel()
                        audioCaptureEngine.pauseRecording()
                        _state.value = GeminiLiveState.Speaking
                    }
                    withContext(Dispatchers.Main) {
                        onAudioSpeechStarted?.invoke()
                    }
                }

                event.audioChunks.forEach { chunk ->
                    audioPlaybackEngine.audioQueue.send(chunk)
                }

                if (event.turnComplete) {
                    synchronized(audioStateLock) {
                        isServerTurnComplete = true
                        val isPlaybackIdle = audioPlaybackEngine.playbackState.value == PlaybackState.Idle
                        val isQueueEmpty = audioPlaybackEngine.audioQueue.isEmpty
                        if (isStandbyPending) {
                            if (isPlaybackIdle && isQueueEmpty) {
                                isStandbyPending = false
                                stopSession()
                            }
                        } else if (!hasReceivedAudioForTurn || (isPlaybackIdle && isQueueEmpty)) {
                            scheduleMicUnmute()
                        }
                    }
                    audioPlaybackEngine.audioQueue.send(null)
                }
            }
            is LiveServerEvent.ToolCall -> {
                synchronized(audioStateLock) {
                    audioCaptureEngine.pauseRecording()
                    hasReceivedAudioForTurn = false
                    isServerTurnComplete = false
                    speakingEnteredAtMillis = System.currentTimeMillis()
                    _state.value = GeminiLiveState.Speaking
                }
                executeToolCallSerialized(event)
            }
            is LiveServerEvent.GoAway -> {
                Logger.w("Gemini Live server sent GoAway (timeLeft=${event.timeLeft}). Closing session.", "ARIS_GEMINI")
                stopSession()
            }
            else -> {}
        }
    }

    private fun executeToolCallSerialized(toolCallEvent: LiveServerEvent.ToolCall) {
        toolExecutionJob = scope.launch(Dispatchers.IO) {
            toolExecutionMutex.withLock {
                val responses = mutableListOf<LiveFunctionResponse>()
                try {
                    for (toolCall in toolCallEvent.functionCalls) {
                        var resultData: String? = null
                        if (toolDispatcher != null) {
                            when (val res = toolDispatcher.dispatch(toolCall.name, toolCall.args)) {
                                is ToolExecutionResult.Success -> resultData = res.responsePayload
                                else -> {}
                            }
                        }
                        if (resultData == null) {
                            resultData = fallbackDispatcher.executeFallbackTool(toolCall.name, toolCall.args)
                        }
                        responses.add(
                            LiveFunctionResponse(
                                id = toolCall.id,
                                name = toolCall.name,
                                result = resultData
                            )
                        )
                    }
                    synchronized(audioStateLock) {
                        speakingEnteredAtMillis = System.currentTimeMillis()
                    }
                    session?.sendToolResponse(responses)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Logger.e("Tool execution failed", e, "ARIS_GEMINI")
                }
            }
        }
    }

    fun stopSession(reWarmup: Boolean = true) {
        val sessionToClose: LiveSessionTransport?
        synchronized(audioStateLock) {
            isStandbyPending = false
            isServerTurnComplete = true
            hasReceivedAudioForTurn = false
            isWelcomeGreetingActive = false
            connectJob?.cancel()
            connectJob = null
            unmuteJob?.cancel()
            unmuteJob = null
            silenceTimeoutJob?.cancel()
            silenceTimeoutJob = null
            toolExecutionJob?.cancel()
            toolExecutionJob = null
            captureJob?.cancel()
            captureJob = null
            audioPlaybackEngine.stopPlayback()
            audioCaptureEngine.stopCapture()
            protocolJob?.cancel()
            protocolJob = null
            sessionToClose = session
            session = null
            if (_state.value !is GeminiLiveState.Error) {
                _state.value = GeminiLiveState.Idle
            }
        }
        if (sessionToClose != null) {
            scope.launch(Dispatchers.IO) {
                sessionToClose.close()
            }
        }
        if (reWarmup && _state.value !is GeminiLiveState.Error) {
            scope.launch(Dispatchers.IO) {
                delay(1000.milliseconds)
                synchronized(audioStateLock) {
                    if (session != null || connectJob?.isActive == true || _state.value != GeminiLiveState.Idle) return@launch
                }
                warmupSession()
            }
        }
    }
}

package com.nowlsantos.aiconcierge.speech.live

import com.nowlsantos.aiconcierge.utils.Logger
import io.ktor.client.HttpClient
import io.ktor.client.plugins.websocket.DefaultClientWebSocketSession
import io.ktor.client.plugins.websocket.webSocketSession
import io.ktor.websocket.CloseReason
import io.ktor.websocket.Frame
import io.ktor.websocket.close
import io.ktor.websocket.readBytes
import io.ktor.websocket.readReason
import io.ktor.websocket.readText
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeout
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.time.Duration.Companion.seconds

class KtorGeminiLiveClient(
    private val httpClient: HttpClient,
    private val apiKey: String,
    private val ephemeralTokenProvider: (suspend () -> String)? = null
) : GeminiLiveClient {

    override suspend fun connect(config: GeminiLiveModelConfig): LiveSessionTransport {
        val url = resolveWebSocketUrl()
        val wsSession = try {
            httpClient.webSocketSession(urlString = url)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            throw GeminiLiveConnectionException(
                "WebSocket connection failed: ${e.localizedMessage ?: e.javaClass.simpleName}",
                e
            )
        }

        val transport = KtorLiveSessionTransport(wsSession)
        try {
            val setupPayload = GeminiLiveProtocol.buildSetupMessage(config)
            transport.sendRawText(setupPayload)
            withTimeout(SETUP_TIMEOUT) {
                transport.awaitSetupComplete()
            }
            return transport
        } catch (e: CancellationException) {
            transport.close()
            throw e
        } catch (e: Exception) {
            transport.close()
            throw GeminiLiveConnectionException(
                e.localizedMessage ?: "Gemini Live handshake failed",
                e
            )
        }
    }

    private suspend fun resolveWebSocketUrl(): String {
        val tokenProvider = ephemeralTokenProvider
        if (tokenProvider != null) {
            val token = tokenProvider().trim()
            if (token.isNotEmpty()) {
                return "$CONSTRAINED_WS_BASE?access_token=$token"
            }
        }

        val key = apiKey.trim()
        if (key.isEmpty()) {
            throw GeminiLiveConnectionException(
                "GEMINI_API_KEY is missing. Add GEMINI_API_KEY=... to local.properties."
            )
        }
        return "$STANDARD_WS_BASE?key=$key"
    }

    companion object {
        private val SETUP_TIMEOUT = 15.seconds
        private const val STANDARD_WS_BASE =
            "wss://generativelanguage.googleapis.com/ws/google.ai.generativelanguage.v1beta.GenerativeService.BidiGenerateContent"
        private const val CONSTRAINED_WS_BASE =
            "wss://generativelanguage.googleapis.com/ws/google.ai.generativelanguage.v1beta.GenerativeService.BidiGenerateContentConstrained"
    }
}

private class KtorLiveSessionTransport(
    private val wsSession: DefaultClientWebSocketSession
) : LiveSessionTransport {

    private val sessionScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val sendMutex = Mutex()
    private val setupCompleteDeferred = CompletableDeferred<Unit>()
    private val eventChannel = Channel<LiveServerEvent>(capacity = Channel.UNLIMITED)
    private val closed = AtomicBoolean(false)

    override val events: Flow<LiveServerEvent> = eventChannel.receiveAsFlow()

    override val isConnected: Boolean
        get() = wsSession.isActive && !closed.get()

    init {
        sessionScope.launch {
            var closeReasonText: String? = null
            try {
                for (frame in wsSession.incoming) {
                    val payloadText = when (frame) {
                        is Frame.Text -> frame.readText()
                        is Frame.Binary -> frame.readBytes().decodeToString()
                        is Frame.Close -> {
                            val reason = frame.readReason()
                            closeReasonText = formatCloseReason(reason)
                            break
                        }
                        else -> continue
                    }

                    val event = GeminiLiveProtocol.parseServerEvent(payloadText)
                    if (event is LiveServerEvent.SetupComplete) {
                        setupCompleteDeferred.complete(Unit)
                    } else if (event !is LiveServerEvent.Unknown) {
                        eventChannel.send(event)
                    }
                }

                if (!setupCompleteDeferred.isCompleted) {
                    val reason = runCatching { wsSession.closeReason.await() }.getOrNull()
                    val reasonMessage = closeReasonText ?: formatCloseReason(reason)
                    setupCompleteDeferred.completeExceptionally(
                        GeminiLiveConnectionException(
                            "Channel closed by server during setup ($reasonMessage)"
                        )
                    )
                }
                closed.set(true)
                eventChannel.close()
            } catch (e: CancellationException) {
                closed.set(true)
                if (!setupCompleteDeferred.isCompleted) {
                    setupCompleteDeferred.cancel(e)
                }
                eventChannel.close(e)
                throw e
            } catch (e: Exception) {
                closed.set(true)
                Logger.e("Gemini Live WebSocket receive error: ${e.localizedMessage}", e, "ARIS_GEMINI")
                if (!setupCompleteDeferred.isCompleted) {
                    setupCompleteDeferred.completeExceptionally(
                        GeminiLiveConnectionException(
                            "WebSocket error during setup: ${e.localizedMessage ?: e.javaClass.simpleName}",
                            e
                        )
                    )
                }
                eventChannel.close(e)
            }
        }
    }

    suspend fun awaitSetupComplete() {
        setupCompleteDeferred.await()
    }

    suspend fun sendRawText(payload: String) {
        sendMutex.withLock {
            if (!wsSession.isActive || closed.get()) {
                throw GeminiLiveConnectionException("Cannot send frame: WebSocket is closed")
            }
            wsSession.send(Frame.Text(payload))
        }
    }

    override suspend fun sendAudio(pcm16kChunk: ByteArray) {
        if (pcm16kChunk.isEmpty()) return
        sendRawText(GeminiLiveProtocol.buildRealtimeAudioMessage(pcm16kChunk))
    }

    override suspend fun sendAudioStreamEnd() {
        sendRawText(GeminiLiveProtocol.buildAudioStreamEndMessage())
    }

    override suspend fun sendText(text: String, turnComplete: Boolean) {
        sendRawText(GeminiLiveProtocol.buildClientTextMessage(text = text, turnComplete = turnComplete))
    }

    override suspend fun sendToolResponse(responses: List<LiveFunctionResponse>) {
        if (responses.isEmpty()) return
        sendRawText(GeminiLiveProtocol.buildToolResponseMessage(responses))
    }

    override suspend fun close() {
        if (closed.compareAndSet(false, true)) {
            runCatching { wsSession.close(CloseReason(CloseReason.Codes.NORMAL, "Session closed")) }
            eventChannel.close()
            sessionScope.cancel()
        }
    }

    private fun formatCloseReason(reason: CloseReason?): String {
        if (reason == null) return "no close reason"
        return "code=${reason.code}, message=${reason.message}"
    }
}

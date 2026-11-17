package com.nowlsantos.aiconcierge.speech

import android.content.Context
import com.nowlsantos.aiconcierge.hardware.capture.AudioCaptureEngine
import com.nowlsantos.aiconcierge.hardware.playback.AudioPlaybackEngine
import com.nowlsantos.aiconcierge.hardware.playback.PlaybackState
import com.nowlsantos.aiconcierge.speech.live.GeminiLiveClient
import com.nowlsantos.aiconcierge.speech.live.GeminiLiveModelConfig
import com.nowlsantos.aiconcierge.speech.live.LiveFunctionCall
import com.nowlsantos.aiconcierge.speech.live.LiveFunctionResponse
import com.nowlsantos.aiconcierge.speech.live.LiveServerEvent
import com.nowlsantos.aiconcierge.speech.live.LiveSessionTransport
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.JsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicReference

@OptIn(ExperimentalCoroutinesApi::class)
class GeminiLiveManagerTest {

    private class FakeTransport : LiveSessionTransport {
        val inbound = Channel<LiveServerEvent>(Channel.UNLIMITED)
        val sentTexts = CopyOnWriteArrayList<Pair<String, Boolean>>()
        val sentToolResponses = CopyOnWriteArrayList<List<LiveFunctionResponse>>()
        @Volatile
        var closed = false

        override val events: Flow<LiveServerEvent> = inbound.receiveAsFlow()
        override val isConnected: Boolean
            get() = !closed

        override suspend fun sendAudio(pcm16kChunk: ByteArray) {}
        override suspend fun sendAudioStreamEnd() {}

        override suspend fun sendText(text: String, turnComplete: Boolean) {
            sentTexts.add(text to turnComplete)
        }

        override suspend fun sendToolResponse(responses: List<LiveFunctionResponse>) {
            sentToolResponses.add(responses)
        }

        override suspend fun close() {
            closed = true
            inbound.close()
        }
    }

    private fun awaitCondition(timeoutMs: Long = 3000L, condition: () -> Boolean) {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            if (condition()) return
            Thread.sleep(15)
        }
        assertTrue("Condition was not met within ${timeoutMs}ms", condition())
    }

    @Test
    fun startSession_connectsWithGemini38Live_triggersGreeting_andDispatchesToolCall() = runBlocking {
        val managerScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        try {
            val context = mockk<Context>(relaxed = true)
            val captureEngine = mockk<AudioCaptureEngine>(relaxed = true)
            every { captureEngine.startCaptureStream() } returns emptyFlow()

            val playbackQueue = Channel<ByteArray?>(Channel.UNLIMITED)
            val playbackState = MutableStateFlow(PlaybackState.Idle)
            val playbackEngine = mockk<AudioPlaybackEngine>(relaxed = true)
            every { playbackEngine.audioQueue } returns playbackQueue
            every { playbackEngine.playbackState } returns playbackState

            val fakeTransport = FakeTransport()
            val capturedConfig = AtomicReference<GeminiLiveModelConfig?>(null)
            val fakeClient = GeminiLiveClient { config ->
                capturedConfig.set(config)
                fakeTransport
            }

            val manager = GeminiLiveManager(
                scope = managerScope,
                audioCaptureEngine = captureEngine,
                audioPlaybackEngine = playbackEngine,
                context = context,
                liveClient = fakeClient
            )

            val navigatedTarget = AtomicReference<String?>(null)
            manager.onNavigate = { target ->
                navigatedTarget.set(target)
                "Opened $target"
            }

            manager.startSession()
            awaitCondition {
                fakeTransport.sentTexts.any { it.second && it.first.contains("welcome message") }
            }
            assertEquals("gemini-3.8-live", capturedConfig.get()?.modelName)

            // Complete welcome greeting turn so manager transitions from Speaking -> Listening
            fakeTransport.inbound.send(
                LiveServerEvent.ServerContent(
                    audioChunks = emptyList(),
                    textParts = emptyList(),
                    turnComplete = true,
                    interrupted = false
                )
            )
            awaitCondition { manager.state.value == GeminiLiveState.Listening }

            manager.sendContextUpdate("Current screen: Home")
            awaitCondition {
                fakeTransport.sentTexts.any { !it.second && it.first.contains("SYSTEM MEMORY SYNC") }
            }

            fakeTransport.inbound.send(
                LiveServerEvent.ToolCall(
                    listOf(
                        LiveFunctionCall(
                            id = "call_99",
                            name = "navigate_to",
                            args = mapOf("target" to JsonPrimitive("Spa and Wellness"))
                        )
                    )
                )
            )
            awaitCondition { fakeTransport.sentToolResponses.isNotEmpty() }

            assertEquals("Spa and Wellness", navigatedTarget.get())
            val response = fakeTransport.sentToolResponses[0][0]
            assertEquals("call_99", response.id)
            assertEquals("navigate_to", response.name)
            assertEquals("Opened Spa and Wellness", response.result)

            manager.stopSession(reWarmup = false)
            awaitCondition { fakeTransport.closed }
        } finally {
            managerScope.cancel()
        }
    }
}

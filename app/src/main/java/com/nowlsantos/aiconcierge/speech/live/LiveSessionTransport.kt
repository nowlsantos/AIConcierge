package com.nowlsantos.aiconcierge.speech.live

import kotlinx.coroutines.flow.Flow

interface LiveSessionTransport {
    val events: Flow<LiveServerEvent>
    val isConnected: Boolean

    suspend fun sendAudio(pcm16kChunk: ByteArray)
    suspend fun sendAudioStreamEnd()
    suspend fun sendText(text: String, turnComplete: Boolean = true)
    suspend fun sendToolResponse(responses: List<LiveFunctionResponse>)
    suspend fun close()
}

fun interface GeminiLiveClient {
    suspend fun connect(config: GeminiLiveModelConfig): LiveSessionTransport
}

class GeminiLiveConnectionException(
    message: String,
    cause: Throwable? = null
) : Exception(message, cause)

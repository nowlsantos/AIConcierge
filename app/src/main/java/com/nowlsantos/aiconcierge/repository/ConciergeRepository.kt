package com.nowlsantos.aiconcierge.repository

import com.nowlsantos.aiconcierge.data.remote.GeminiTextClient
import com.nowlsantos.common.ConciergeStreamChunk
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * EXECUTIVE REPOSITORY - V144 (LEAK-PROOF)
 */
class ConciergeRepository(
    private val geminiTextClient: GeminiTextClient
) {
    fun checkConnection(): Result<String> {
        return Result.success("OK")
    }

    fun initialHandshake(): Result<ConciergeStreamChunk> {
        return Result.success(ConciergeStreamChunk(isFinished = true))
    }

    fun streamQuery(query: String): Flow<ConciergeStreamChunk> = flow {
        try {
            geminiTextClient.streamText(query).collect { textResponse ->
                if (textResponse.isNotEmpty()) {
                    emit(ConciergeStreamChunk(text = textResponse, isFinished = false))
                }
            }
            emit(ConciergeStreamChunk(isFinished = true))
        } catch (e: Exception) {
            emit(ConciergeStreamChunk(error = e.localizedMessage, isFinished = true))
        }
    }
}

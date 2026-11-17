package com.nowlsantos.aiconcierge.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.request.header
import io.ktor.client.request.preparePost
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.utils.io.readLine
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

interface GeminiTextClient {
    fun streamText(prompt: String): Flow<String>
}

class KtorGeminiTextClient(
    private val httpClient: HttpClient,
    private val apiKey: String,
    private val modelName: String = DEFAULT_MODEL_NAME
) : GeminiTextClient {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    override fun streamText(prompt: String): Flow<String> = flow {
        val key = apiKey.trim()
        require(key.isNotEmpty()) {
            "GEMINI_API_KEY is missing. Add GEMINI_API_KEY=... to local.properties."
        }

        val url = "https://generativelanguage.googleapis.com/v1beta/models/$modelName:streamGenerateContent?alt=sse&key=$key"
        val bodyJson = buildJsonObject {
            put("contents", buildJsonArray {
                add(buildJsonObject {
                    put("parts", buildJsonArray {
                        add(buildJsonObject {
                            put("text", prompt)
                        })
                    })
                })
            })
        }

        httpClient.preparePost(url) {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Accept, ContentType.Text.EventStream.toString())
            setBody(json.encodeToString(JsonObject.serializer(), bodyJson))
        }.execute { response ->
            if (!response.status.isSuccess()) {
                error("Gemini stream request failed with HTTP ${response.status.value}")
            }
            val channel = response.bodyAsChannel()
            while (!channel.isClosedForRead) {
                val line = channel.readLine() ?: break
                if (!line.startsWith("data:")) continue
                val dataPayload = line.removePrefix("data:").trim()
                if (dataPayload.isEmpty() || dataPayload == "[DONE]") continue

                val textChunk = extractCandidateText(dataPayload)
                if (!textChunk.isNullOrEmpty()) {
                    emit(textChunk)
                }
            }
        }
    }

    private fun extractCandidateText(rawJson: String): String? {
        val root = runCatching { json.parseToJsonElement(rawJson).jsonObject }.getOrNull() ?: return null
        val candidates = root["candidates"]?.jsonArray ?: return null
        val firstCandidate = candidates.firstOrNull()?.jsonObject ?: return null
        val parts = firstCandidate["content"]?.jsonObject?.get("parts")?.jsonArray ?: return null
        return parts.mapNotNull { it.jsonObject["text"]?.jsonPrimitive?.contentOrNull }
            .joinToString(separator = "")
            .takeIf { it.isNotEmpty() }
    }

    companion object {
        const val DEFAULT_MODEL_NAME = "gemini-2.5-flash"
    }
}

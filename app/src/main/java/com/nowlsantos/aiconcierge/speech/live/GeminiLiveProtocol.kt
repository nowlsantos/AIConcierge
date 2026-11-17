package com.nowlsantos.aiconcierge.speech.live

import com.nowlsantos.aiconcierge.speech.config.GeminiFunctionDeclaration
import com.nowlsantos.aiconcierge.speech.config.GeminiToolDeclarations
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import java.util.Base64

data class GeminiLiveModelConfig(
    val modelName: String = DEFAULT_MODEL_NAME,
    val voiceName: String = "Puck",
    val systemInstruction: String,
    val tools: List<GeminiFunctionDeclaration> = GeminiToolDeclarations.ALL_DECLARATIONS
) {
    companion object {
        const val DEFAULT_MODEL_NAME = "gemini-3.8-live"
    }
}

data class LiveFunctionCall(
    val id: String?,
    val name: String,
    val args: Map<String, JsonElement>
)

data class LiveFunctionResponse(
    val id: String?,
    val name: String,
    val result: String
)

sealed interface LiveServerEvent {
    data object SetupComplete : LiveServerEvent

    data class ServerContent(
        val audioChunks: List<ByteArray>,
        val textParts: List<String>,
        val turnComplete: Boolean,
        val interrupted: Boolean
    ) : LiveServerEvent

    data class ToolCall(
        val functionCalls: List<LiveFunctionCall>
    ) : LiveServerEvent

    data class ToolCallCancellation(
        val ids: List<String>
    ) : LiveServerEvent

    data class GoAway(
        val timeLeft: String?
    ) : LiveServerEvent

    data object Unknown : LiveServerEvent
}

object GeminiLiveProtocol {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    fun buildSetupMessage(config: GeminiLiveModelConfig): String {
        val qualifiedModel = if (config.modelName.startsWith("models/")) {
            config.modelName
        } else {
            "models/${config.modelName}"
        }

        val payload = buildJsonObject {
            put("setup", buildJsonObject {
                put("model", qualifiedModel)
                put("generationConfig", buildJsonObject {
                    put("responseModalities", buildJsonArray {
                        add(JsonPrimitive("AUDIO"))
                    })
                    put("speechConfig", buildJsonObject {
                        put("voiceConfig", buildJsonObject {
                            put("prebuiltVoiceConfig", buildJsonObject {
                                put("voiceName", config.voiceName)
                            })
                        })
                    })
                })
                if (config.systemInstruction.isNotBlank()) {
                    put("systemInstruction", buildJsonObject {
                        put("parts", buildJsonArray {
                            add(buildJsonObject {
                                put("text", config.systemInstruction)
                            })
                        })
                    })
                }
                if (config.tools.isNotEmpty()) {
                    put("tools", GeminiToolDeclarations.toToolsJsonArray(config.tools))
                }
            })
        }
        return json.encodeToString(JsonObject.serializer(), payload)
    }

    fun buildRealtimeAudioMessage(pcm16kChunk: ByteArray): String {
        val base64Audio = Base64.getEncoder().encodeToString(pcm16kChunk)
        val payload = buildJsonObject {
            put("realtimeInput", buildJsonObject {
                put("audio", buildJsonObject {
                    put("mimeType", "audio/pcm;rate=16000")
                    put("data", base64Audio)
                })
            })
        }
        return json.encodeToString(JsonObject.serializer(), payload)
    }

    fun buildAudioStreamEndMessage(): String {
        val payload = buildJsonObject {
            put("realtimeInput", buildJsonObject {
                put("audioStreamEnd", true)
            })
        }
        return json.encodeToString(JsonObject.serializer(), payload)
    }

    fun buildClientTextMessage(text: String, turnComplete: Boolean): String {
        val payload = buildJsonObject {
            put("clientContent", buildJsonObject {
                put("turns", buildJsonArray {
                    add(buildJsonObject {
                        put("role", "user")
                        put("parts", buildJsonArray {
                            add(buildJsonObject {
                                put("text", text)
                            })
                        })
                    })
                })
                put("turnComplete", turnComplete)
            })
        }
        return json.encodeToString(JsonObject.serializer(), payload)
    }

    fun buildToolResponseMessage(responses: List<LiveFunctionResponse>): String {
        val payload = buildJsonObject {
            put("toolResponse", buildJsonObject {
                put("functionResponses", buildJsonArray {
                    responses.forEach { response ->
                        add(buildJsonObject {
                            if (!response.id.isNullOrBlank()) {
                                put("id", response.id)
                            }
                            put("name", response.name)
                            put("response", buildJsonObject {
                                put("result", response.result)
                            })
                        })
                    }
                })
            })
        }
        return json.encodeToString(JsonObject.serializer(), payload)
    }

    fun parseServerEvent(rawJson: String): LiveServerEvent {
        val root = runCatching { json.parseToJsonElement(rawJson).jsonObject }.getOrNull()
            ?: return LiveServerEvent.Unknown

        if (root.containsKey("setupComplete")) {
            return LiveServerEvent.SetupComplete
        }

        root["toolCall"]?.jsonObject?.let { toolCallObj ->
            val calls = toolCallObj["functionCalls"]?.jsonArray?.mapNotNull { element ->
                val callObj = element.jsonObject
                val name = callObj["name"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null
                val id = callObj["id"]?.jsonPrimitive?.contentOrNull
                val args = callObj["args"]?.jsonObject ?: emptyMap()
                LiveFunctionCall(id = id, name = name, args = args)
            }.orEmpty()
            return LiveServerEvent.ToolCall(functionCalls = calls)
        }

        root["toolCallCancellation"]?.jsonObject?.let { cancelObj ->
            val ids = cancelObj["ids"]?.jsonArray?.mapNotNull {
                it.jsonPrimitive.contentOrNull
            }.orEmpty()
            return LiveServerEvent.ToolCallCancellation(ids = ids)
        }

        root["goAway"]?.jsonObject?.let { goAwayObj ->
            val timeLeft = goAwayObj["timeLeft"]?.jsonPrimitive?.contentOrNull
            return LiveServerEvent.GoAway(timeLeft = timeLeft)
        }

        root["serverContent"]?.jsonObject?.let { contentObj ->
            val interrupted = contentObj["interrupted"]?.jsonPrimitive?.booleanOrNull ?: false
            val turnComplete = contentObj["turnComplete"]?.jsonPrimitive?.booleanOrNull ?: false

            val audioChunks = mutableListOf<ByteArray>()
            val textParts = mutableListOf<String>()

            contentObj["modelTurn"]?.jsonObject
                ?.get("parts")?.jsonArray
                ?.forEach { partElement ->
                    val partObj = partElement.jsonObject
                    partObj["text"]?.jsonPrimitive?.contentOrNull
                        ?.takeIf { it.isNotBlank() }
                        ?.let { textParts.add(it) }

                    partObj["inlineData"]?.jsonObject?.let { inlineObj ->
                        val mimeType = inlineObj["mimeType"]?.jsonPrimitive?.contentOrNull.orEmpty()
                        val dataB64 = inlineObj["data"]?.jsonPrimitive?.contentOrNull.orEmpty()
                        if (mimeType.contains("audio", ignoreCase = true) && dataB64.isNotEmpty()) {
                            runCatching { Base64.getDecoder().decode(dataB64) }
                                .getOrNull()
                                ?.takeIf { it.isNotEmpty() }
                                ?.let { audioChunks.add(it) }
                        }
                    }
                }

            contentObj["outputTranscription"]?.jsonObject
                ?.get("text")?.jsonPrimitive?.contentOrNull
                ?.takeIf { it.isNotBlank() }
                ?.let { textParts.add(it) }

            return LiveServerEvent.ServerContent(
                audioChunks = audioChunks,
                textParts = textParts,
                turnComplete = turnComplete,
                interrupted = interrupted
            )
        }

        return LiveServerEvent.Unknown
    }
}

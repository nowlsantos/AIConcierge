package com.nowlsantos.aiconcierge.speech.live

import com.nowlsantos.aiconcierge.speech.config.GeminiToolDeclarations
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Base64

class GeminiLiveProtocolTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun buildSetupMessage_targetsGemini38Live_andOmitsLegacyThinkingFields() {
        val raw = GeminiLiveProtocol.buildSetupMessage(
            GeminiLiveModelConfig(
                systemInstruction = "You are Aris.",
                tools = GeminiToolDeclarations.ALL_DECLARATIONS
            )
        )

        val setup = json.parseToJsonElement(raw).jsonObject["setup"]!!.jsonObject
        assertEquals("models/gemini-3.8-live", setup["model"]!!.jsonPrimitive.content)

        val genConfig = setup["generationConfig"]!!.jsonObject
        assertFalse(genConfig.containsKey("thinkingConfig"))
        assertFalse(setup.containsKey("proactivity"))
        assertFalse(setup.containsKey("enable_affective_dialog"))
        assertEquals("AUDIO", genConfig["responseModalities"]!!.jsonArray[0].jsonPrimitive.content)

        val voiceName = genConfig["speechConfig"]!!.jsonObject["voiceConfig"]!!.jsonObject["prebuiltVoiceConfig"]!!
            .jsonObject["voiceName"]!!.jsonPrimitive.content
        assertEquals("Puck", voiceName)

        val declarations = setup["tools"]!!.jsonArray[0].jsonObject["functionDeclarations"]!!.jsonArray
        assertEquals(GeminiToolDeclarations.ALL_DECLARATIONS.size, declarations.size)
    }

    @Test
    fun buildRealtimeAudioMessage_usesRealtimeInputAudioField() {
        val pcm = byteArrayOf(1, 2, 3, 4, 5, 6)
        val raw = GeminiLiveProtocol.buildRealtimeAudioMessage(pcm)
        val realtimeInput = json.parseToJsonElement(raw).jsonObject["realtimeInput"]!!.jsonObject

        assertFalse(realtimeInput.containsKey("mediaChunks"))
        val audio = realtimeInput["audio"]!!.jsonObject
        assertEquals("audio/pcm;rate=16000", audio["mimeType"]!!.jsonPrimitive.content)
        assertEquals(Base64.getEncoder().encodeToString(pcm), audio["data"]!!.jsonPrimitive.content)
    }

    @Test
    fun buildClientTextMessage_respectsTurnCompleteFlag() {
        val silentUpdate = GeminiLiveProtocol.buildClientTextMessage("Screen changed", turnComplete = false)
        val silentObj = json.parseToJsonElement(silentUpdate).jsonObject["clientContent"]!!.jsonObject
        assertFalse(silentObj["turnComplete"]!!.jsonPrimitive.boolean)

        val greeting = GeminiLiveProtocol.buildClientTextMessage("Greet", turnComplete = true)
        val greetingObj = json.parseToJsonElement(greeting).jsonObject["clientContent"]!!.jsonObject
        assertTrue(greetingObj["turnComplete"]!!.jsonPrimitive.boolean)
    }

    @Test
    fun buildToolResponseMessage_preservesCallIdAndResult() {
        val raw = GeminiLiveProtocol.buildToolResponseMessage(
            listOf(LiveFunctionResponse(id = "call_42", name = "navigate_to", result = "Navigated to Spa"))
        )
        val responses = json.parseToJsonElement(raw).jsonObject["toolResponse"]!!.jsonObject["functionResponses"]!!.jsonArray
        assertEquals(1, responses.size)
        val first = responses[0].jsonObject
        assertEquals("call_42", first["id"]!!.jsonPrimitive.content)
        assertEquals("navigate_to", first["name"]!!.jsonPrimitive.content)
        assertEquals("Navigated to Spa", first["response"]!!.jsonObject["result"]!!.jsonPrimitive.content)
    }

    @Test
    fun parseServerEvent_handlesSetupComplete_Audio_ToolCall_andGoAway() {
        assertTrue(GeminiLiveProtocol.parseServerEvent("""{"setupComplete":{}}""") is LiveServerEvent.SetupComplete)

        val pcm = byteArrayOf(10, 20, 30, 40)
        val b64 = Base64.getEncoder().encodeToString(pcm)
        val audioEvent = GeminiLiveProtocol.parseServerEvent(
            """
            {
              "serverContent": {
                "modelTurn": {
                  "parts": [
                    {"inlineData": {"mimeType": "audio/pcm;rate=24000", "data": "$b64"}}
                  ]
                },
                "turnComplete": true
              }
            }
            """.trimIndent()
        ) as LiveServerEvent.ServerContent
        assertEquals(1, audioEvent.audioChunks.size)
        assertArrayEquals(pcm, audioEvent.audioChunks[0])
        assertTrue(audioEvent.turnComplete)
        assertFalse(audioEvent.interrupted)

        val toolEvent = GeminiLiveProtocol.parseServerEvent(
            """
            {
              "toolCall": {
                "functionCalls": [
                  {"id": "fc_1", "name": "navigate_to", "args": {"target": "Spa and Wellness"}}
                ]
              }
            }
            """.trimIndent()
        ) as LiveServerEvent.ToolCall
        assertEquals(1, toolEvent.functionCalls.size)
        assertEquals("fc_1", toolEvent.functionCalls[0].id)
        assertEquals("navigate_to", toolEvent.functionCalls[0].name)
        assertEquals("Spa and Wellness", toolEvent.functionCalls[0].args["target"]!!.jsonPrimitive.content)

        val goAwayEvent = GeminiLiveProtocol.parseServerEvent("""{"goAway":{"timeLeft":"10s"}}""") as LiveServerEvent.GoAway
        assertNotNull(goAwayEvent)
        assertEquals("10s", goAwayEvent.timeLeft)
    }
}

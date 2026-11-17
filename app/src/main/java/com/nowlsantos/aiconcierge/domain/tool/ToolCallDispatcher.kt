package com.nowlsantos.aiconcierge.domain.tool

import com.nowlsantos.aiconcierge.utils.Logger
import kotlinx.serialization.json.JsonElement

sealed interface ToolExecutionResult {
    data class Success(val responsePayload: String) : ToolExecutionResult
    data class Error(val errorMessage: String) : ToolExecutionResult
}

fun interface ConciergeToolHandler {
    suspend fun execute(args: Map<String, JsonElement>): ToolExecutionResult
}

/**
 * TOOL CALL DISPATCHER
 * Command / Registry pattern that maps Gemini Live tool call names to concrete handlers.
 */
class ToolCallDispatcher(
    private val handlers: Map<String, ConciergeToolHandler>
) {
    suspend fun dispatch(toolName: String, args: Map<String, JsonElement>): ToolExecutionResult {
        val handler = handlers[toolName]
        if (handler == null) {
            Logger.w("No handler registered for tool: $toolName", "TOOL_DISPATCHER")
            return ToolExecutionResult.Error("Unknown tool: $toolName")
        }

        return try {
            Logger.d("Dispatching tool: $toolName", "TOOL_DISPATCHER")
            handler.execute(args)
        } catch (e: Exception) {
            Logger.e("Error executing tool $toolName: ${e.localizedMessage}", e, "TOOL_DISPATCHER")
            ToolExecutionResult.Error("Execution failed: ${e.localizedMessage}")
        }
    }
}

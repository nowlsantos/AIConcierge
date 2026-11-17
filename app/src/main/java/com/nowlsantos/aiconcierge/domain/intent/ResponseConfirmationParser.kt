package com.nowlsantos.aiconcierge.domain.intent

import com.nowlsantos.aiconcierge.domain.navigation.NodeNavigationEngine
import com.nowlsantos.common.ActionIntent
import com.nowlsantos.common.ActionType
import com.nowlsantos.aiconcierge.utils.Logger
import com.nowlsantos.common.DocumentNode

/**
 * RESPONSE CONFIRMATION TEXT PARSER (PILLAR 1 FAIL-SAFE)
 * Pure, metadata-driven spoken text parser.
 * Extracts target destinations directly from Aris's verbal spoken response text
 * when Gemini Live omits the LiveServerToolCall packet over WebSockets.
 */
class ResponseConfirmationParser(
    private val navigationEngine: NodeNavigationEngine
) {

    fun parseConfirmationText(text: String, rootNode: DocumentNode?, currentScreenName: String? = null): ActionIntent? {
        if (text.isBlank() || rootNode == null) return null

        val lower = text.lowercase()

        // Universal spoken confirmation triggers used by Aris
        val triggers = listOf(
            "navigating you to",
            "navigating to",
            "displaying our",
            "displaying the",
            "displaying",
            "opening our",
            "opening the",
            "opening",
            "showing you",
            "showing the",
            "showing",
            "taking you to",
            "switching to",
            "tuning to",
            "returning to the",
            "returning to",
            "returned to",
            "here is the",
            "here are the",
            "welcome to"
        )

        for (trigger in triggers) {
            val index = lower.indexOf(trigger)
            if (index != -1) {
                val targetRaw = lower.substring(index + trigger.length).trim().trimEnd('.', '!', ',')
                if (targetRaw.isNotEmpty()) {
                    val path = navigationEngine.findPathToNode(targetRaw, rootNode)
                    if (!path.isNullOrEmpty()) {
                        val destinationNode = path.last()
                        // Don't trigger duplicate fail-safe navigation if user is ALREADY on that screen!
                        if (currentScreenName != null && navigationEngine.isSameViewType(currentScreenName, destinationNode.name, rootNode)) {
                            return null
                        }
                        Logger.d("PILLAR 1 FAIL-SAFE: Matched target '$targetRaw' -> node '${destinationNode.name}'", "ACTION_INTENT")
                        return ActionIntent(ActionType.NAVIGATE, targetSection = destinationNode.name)
                    }
                }
            }
        }
        return null
    }
}

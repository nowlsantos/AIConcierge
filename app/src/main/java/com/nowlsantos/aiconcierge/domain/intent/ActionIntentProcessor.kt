package com.nowlsantos.aiconcierge.domain.intent

import com.nowlsantos.aiconcierge.domain.navigation.NodeNavigationEngine
import com.nowlsantos.aiconcierge.presentation.*
import com.nowlsantos.aiconcierge.presentation.MediaCategory
import com.nowlsantos.aiconcierge.ui.component.modal.TopBarDialogType
import com.nowlsantos.aiconcierge.utils.Logger
import com.nowlsantos.common.*

data class ProcessedIntentResult(
    val newState: ConciergeUiState,
    val feedback: String
)

/**
 * IN-MEMORY STRUCTURED INTENT PROCESSOR
 * Processes structured action intents and derives updated UI state and Aris feedback.
 * Features Cross-Category Media Domain Routing for TV, Radio, and Movies.
 */
private val WHITESPACE_REGEX = Regex("\\s+")

class ActionIntentProcessor(
    private val navigationEngine: NodeNavigationEngine
) {

    fun processActionIntent(
        intent: ActionIntent,
        currentState: ConciergeUiState,
        rootNode: DocumentNode?
    ): ProcessedIntentResult {
        Logger.d("PROCESSING IN-MEMORY ACTION INTENT: $intent", "ACTION_INTENT")
        if (rootNode == null) {
            return ProcessedIntentResult(currentState, "Error: System root uninitialized")
        }

        return when (intent.actionType) {
            ActionType.NAVIGATE -> {
                val target = intent.targetSection ?: return ProcessedIntentResult(currentState, "Target missing")
                
                val targetClean = target.lowercase().trim()

                val isOrderSummary = targetClean == "summary" || targetClean == "review" || targetClean == "order summary" || targetClean == "checkout" || targetClean == "cart"
                if (isOrderSummary) {
                    val node = currentState.currentNode
                    val summaryNode = node?.copy(name = ActionDefaultText.ORDER_SUMMARY.value, uiTemplate = UiTemplate.ORDER_SUMMARY) 
                        ?: DocumentNode(name = ActionDefaultText.ORDER_SUMMARY.value, content = "", uiTemplate = UiTemplate.ORDER_SUMMARY)

                    val updatedState = currentState.copy(
                        currentNode = summaryNode,
                        navStack = currentState.navStack + summaryNode,
                        isOrderConfirmed = false,
                        isStreaming = false
                    )
                    return ProcessedIntentResult(updatedState, ArisFeedback.orderSubmitted())
                }

                // STANDBY / SLEEP INTERCEPTION SAFEGUARD:
                val isStandby = targetClean in setOf("standby", "stand by", "sleep", "pause", "rest", "goodbye", "good night", "goodnight", "dismiss") ||
                    targetClean.startsWith("standby") || targetClean.startsWith("stand by")
                if (isStandby) {
                    val updatedState = currentState.copy(
                        isSpeaking = false,
                        isListening = false,
                        isStreaming = false
                    )
                    return ProcessedIntentResult(updatedState, "Status: SUCCESS. Entering standby mode.")
                }

                // SELECTION VIEW ITEM INTERCEPTION SAFEGUARD:
                // If the user is currently on a SELECTION_VIEW and names a menu item/option, intercept navigation
                // and treat it as selecting that option (select_option) instead of navigating away!
                val currentNode = currentState.currentNode
                if (currentNode?.uiTemplate == UiTemplate.SELECTION_VIEW && currentNode.children.isNotEmpty()) {
                    var matchedGroup: String? = null
                    var matchedOption: String? = null

                    currentNode.children.forEach { group ->
                        group.children.find { opt ->
                            val oName = opt.name.lowercase()
                            oName == targetClean || oName.contains(targetClean) || targetClean.contains(oName)
                        }?.let { matchedOpt ->
                            matchedGroup = group.name
                            matchedOption = matchedOpt.name
                        }
                    }

                    if (matchedGroup != null && matchedOption != null) {
                        Logger.d("SELECTION INTERCEPTION SAFEGUARD: Intercepted navigation intent '$target' on SELECTION_VIEW as select_option($matchedGroup, $matchedOption).", "ACTION_INTENT")
                        val updatedSelections = currentState.selections + (matchedGroup to matchedOption)
                        val updatedState = currentState.copy(
                            selections = updatedSelections,
                            isStreaming = false
                        )
                        return ProcessedIntentResult(updatedState, ArisFeedback.selectionSuccess(matchedGroup, matchedOption))
                    }
                }

                // ROOM CONTROL INTERCEPTION SAFEGUARD:
                // If a room control command (lighting, climate, curtains, fan) is mistakenly routed through NAVIGATE,
                // intercept it and process it as a ROOM_CONTROL action instead of searching the directory tree!
                val roomControlKeywords = setOf("light", "lights", "lamp", "bulb", "dim", "illuminate", "climate", "temperature", "ac", "aircon", "curtain", "curtains", "blind", "blinds", "fan", "preset", "scene")
                val targetWords = targetClean.split(WHITESPACE_REGEX).toSet()
                val isRoomControlPhrase = roomControlKeywords.any { it in targetWords }
                if (isRoomControlPhrase) {
                    Logger.d("ROOM CONTROL INTERCEPTION: Intercepted room control phrase '$target' in NAVIGATE. Rerouting to ROOM_CONTROL.", "ACTION_INTENT")
                    val value = if (targetClean.contains("off")) "OFF" else "ON"
                    val category = when {
                        targetClean.contains("master") -> "Master Light"
                        targetClean.contains("night") -> "Night Light"
                        targetClean.contains("climate") || targetClean.contains("temp") -> "Climate"
                        targetClean.contains("curtain") || targetClean.contains("blind") -> "Curtains"
                        targetClean.contains("fan") -> "Fans"
                        else -> "Lighting"
                    }
                    val zones = listOf("BEDROOM", "LIVING ROOM", "SITTING ROOM")
                    val zone = zones.find { targetClean.contains(it.lowercase()) }
                        ?: currentState.roomStates.keys.reversed().firstNotNullOfOrNull { key ->
                            if (key.uppercase().startsWith("ROOM CONTROLS-")) key.substringAfter("ROOM CONTROLS-").trim().uppercase().takeIf { it in zones }
                            else null
                        } ?: "BEDROOM"
                    val baseRoomStates = currentState.roomStates.filterKeys { !it.startsWith("ROOM CONTROLS-") }
                    val activeZoneKey = "ROOM CONTROLS-$zone"
                    val updatedRoomStates = baseRoomStates + (activeZoneKey to "SHOW-${System.currentTimeMillis()}") + ("${category.uppercase()}-$zone" to value)
                    val updatedState = currentState.copy(
                        roomStates = updatedRoomStates,
                        isStreaming = false
                    )
                    return ProcessedIntentResult(updatedState, ArisFeedback.roomUpdate(category, zone, value))
                }

                val topBarDialog = when {
                    targetClean.contains("language") -> TopBarDialogType.LANGUAGE
                    (targetClean.contains("order") && (targetClean.contains("my") || targetClean.contains("status") || targetClean.contains("active"))) || targetClean == "my orders" -> TopBarDialogType.MY_ORDERS
                    targetClean.contains("message") || targetClean.contains("penchat") || targetClean.contains("inbox") -> TopBarDialogType.MESSAGES
                    else -> TopBarDialogType.NONE
                }

                if (topBarDialog != TopBarDialogType.NONE) {
                    val updatedState = currentState.copy(
                        activeTopBarDialog = topBarDialog,
                        isStreaming = false
                    )
                    return ProcessedIntentResult(updatedState, ArisFeedback.serviceInfo(target, "Opening modal."))
                }

                // CROSS-MEDIA GENRE ROUTING (Resolves exact MediaHub domain for TV, Radio, Movies from ANY screen)
                val isRelaxingOrRadioGenre = targetClean.contains("relaxing") || targetClean.contains("top 40") || targetClean.contains("dance")
                val isTvSportsOrLocal = targetClean.contains("sports") || targetClean.contains("local")
                val isMovieGenre = targetClean.contains("action") || targetClean.contains("comedy") || targetClean.contains("drama") || targetClean.contains("sci-fi")

                val targetCategoryName = when {
                    isRelaxingOrRadioGenre || targetClean.contains("radio") -> "RADIO"
                    isTvSportsOrLocal || targetClean.contains("tv") || targetClean.contains("channel") -> "TV"
                    isMovieGenre || targetClean.contains("movie") || targetClean.contains("cinema") -> "MOVIES"
                    currentState.currentNode?.uiTemplate == UiTemplate.MEDIA_HUB -> currentState.currentNode.name.uppercase().trim()
                    else -> null
                }

                if (targetCategoryName != null) {
                    val mediaCategory = MediaCategory.from(targetCategoryName)
                    val filterCategories = mediaCategory.filterCategories
                    val mediaHubTargetNodeName = mediaCategory.nodeName

                    val matchedGenreChip = if (targetClean == "all" || targetClean == "everything") {
                        "All"
                    } else {
                        filterCategories.find { chip ->
                            val chipLower = chip.lowercase().trim()
                            chipLower == targetClean || targetClean.contains(chipLower) || (targetClean.length >= 3 && chipLower.contains(targetClean))
                        }
                    }

                    if (matchedGenreChip != null) {
                        val hubPath = navigationEngine.findPathToNode(mediaHubTargetNodeName, rootNode)
                        if (!hubPath.isNullOrEmpty()) {
                            val destinationNode = hubPath.last()
                            val updatedMediaState = currentState.mediaState + (targetCategoryName to "Filter: $matchedGenreChip")
                            val updatedState = currentState.copy(
                                currentNode = destinationNode,
                                navStack = hubPath,
                                mediaState = updatedMediaState,
                                selections = currentState.selections.filterKeys { k -> k.equals("Language", ignoreCase = true) },
                                isStreaming = false
                            )
                            return ProcessedIntentResult(updatedState, ArisFeedback.mediaPlaying("Filtering by $matchedGenreChip", targetCategoryName))
                        }
                    }
                }

                val isHomeTarget = targetClean in setOf(
                    "home", "homepage", "home page", "main menu", "main hub",
                    "home screen", "dashboard", "front page", "ribbon hub"
                )

                val path = if (isHomeTarget) {
                    listOf(rootNode)
                } else {
                    navigationEngine.findPathToNode(target, rootNode)
                }

                if (!path.isNullOrEmpty()) {
                    val rawDestination = path.last()
                    val parentNode = if (path.size >= 2) path[path.size - 2] else null

                    // MEDIA HUB SAFEGUARD: If target is a media channel/station (child of MEDIA_HUB), target MEDIA_HUB parent instead!
                    val isMediaChild = rawDestination.uiTemplate == UiTemplate.DETAIL_VIEW && parentNode?.uiTemplate == UiTemplate.MEDIA_HUB
                    val destinationNode = if (isMediaChild) parentNode else rawDestination

                    val mediaCategory = parentNode?.name?.uppercase() ?: destinationNode.name.uppercase()
                    val updatedMediaState = if (isMediaChild) {
                        currentState.mediaState + (mediaCategory to "Play: ${rawDestination.name}")
                    } else {
                        currentState.mediaState
                    }

                    val effectivePath = if (isMediaChild) path.dropLast(1) else path

                    val zones = listOf("BEDROOM", "LIVING ROOM", "SITTING ROOM")
                    val searchSource = listOfNotNull(
                        targetClean,
                        intent.targetSection?.lowercase(),
                        intent.category?.lowercase(),
                        intent.targetZone?.lowercase(),
                        intent.value?.lowercase()
                    ).joinToString(" ")

                    val matchedZone = zones.find { searchSource.contains(it.lowercase()) }

                    val updatedRoomStates = if (matchedZone != null) {
                        val baseRoomStates = currentState.roomStates.filterKeys { !it.startsWith("ROOM CONTROLS-") }
                        baseRoomStates + ("ROOM CONTROLS-$matchedZone" to "SHOW-${System.currentTimeMillis()}")
                    } else {
                        currentState.roomStates
                    }

                    val isNavigatingToNewNode = destinationNode.name != currentState.currentNode?.name
                    val isOrderSummary = destinationNode.uiTemplate == UiTemplate.ORDER_SUMMARY
                    val updatedSelections = if (isNavigatingToNewNode && !isOrderSummary) {
                        currentState.selections.filterKeys { k -> k.equals("Language", ignoreCase = true) }
                    } else {
                        currentState.selections
                    }

                    val updatedState = currentState.copy(
                        currentNode = destinationNode,
                        navStack = effectivePath,
                        roomStates = updatedRoomStates,
                        mediaState = updatedMediaState,
                        selections = updatedSelections,
                        isStreaming = false
                    )
                    
                    val feedbackMsg = if (isMediaChild) {
                        ArisFeedback.mediaPlaying(rawDestination.name, parentNode.name)
                    } else {
                        ArisFeedback.navSuccess(destinationNode.name, destinationNode.content)
                    }
                    ProcessedIntentResult(updatedState, feedbackMsg)
                } else {
                    if (currentState.currentNode?.uiTemplate == UiTemplate.SELECTION_VIEW) {
                        Logger.d("SELECTION_VIEW SAFEGUARD: Ignored unmatched navigation intent '$target' to prevent leaving configurator.", "ACTION_INTENT")
                        ProcessedIntentResult(currentState, "Status: SUCCESS. Continuing order configuration.")
                    } else {
                        ProcessedIntentResult(currentState, ArisFeedback.navNotFound(target))
                    }
                }
            }

            ActionType.ROOM_CONTROL -> {
                val category = intent.category ?: "ROOM CONTROLS"
                val zones = listOf("BEDROOM", "LIVING ROOM", "SITTING ROOM")
                val explicitTarget = intent.targetZone?.takeIf {
                    it.isNotBlank() && !it.equals("All", ignoreCase = true)
                }
                val activeTabZone = currentState.roomStates.keys.reversed().firstNotNullOfOrNull { key ->
                    if (key.uppercase().startsWith("ROOM CONTROLS-")) key.substringAfter("ROOM CONTROLS-").trim().uppercase().takeIf { it in zones }
                    else null
                }
                val target = explicitTarget ?: activeTabZone ?: "BEDROOM"
                val value = intent.value ?: "SHOW"

                val path = navigationEngine.findPathToNode(SectionNodes.ROOM_CONTROLS.value, rootNode)
                val destinationNode = path?.last() ?: currentState.currentNode
                val baseRoomStates = currentState.roomStates.filterKeys { !it.startsWith("ROOM CONTROLS-") }
                val activeZoneKey = "ROOM CONTROLS-${target.uppercase().trim()}"
                val updatedRoomStates = baseRoomStates + (activeZoneKey to "SHOW-${System.currentTimeMillis()}") + ("${category.uppercase()}-${target.uppercase()}" to value)

                val isNavigatingAway = destinationNode?.name != currentState.currentNode?.name
                val updatedSelections = if (isNavigatingAway) {
                    currentState.selections.filterKeys { k -> k.equals("Language", ignoreCase = true) }
                } else currentState.selections

                val updatedState = currentState.copy(
                    currentNode = destinationNode,
                    navStack = path ?: currentState.navStack,
                    roomStates = updatedRoomStates,
                    selections = updatedSelections,
                    isStreaming = false
                )
                ProcessedIntentResult(updatedState, ArisFeedback.roomUpdate(category, target, value))
            }

            ActionType.MEDIA_CONTROL -> {
                val rawCategory = intent.category ?: "TV"
                val rawAction = intent.value ?: "Play"
                val rawTarget = intent.targetSection ?: ""

                val filterOrChannelValue = rawTarget.ifBlank { rawAction }
                val filterClean = filterOrChannelValue.lowercase().trim()

                // CROSS-MEDIA GENRE ROUTING for MEDIA_CONTROL action intents
                val isRelaxingOrRadioGenre = filterClean.contains("relaxing") || filterClean.contains("top 40") || filterClean.contains("dance")
                val isTvSportsOrLocal = filterClean.contains("sports") || filterClean.contains("local")
                val isMovieGenre = filterClean.contains("action") || filterClean.contains("comedy") || filterClean.contains("drama") || filterClean.contains("sci-fi")

                val targetCategoryName = when {
                    isRelaxingOrRadioGenre || rawCategory.contains("radio", ignoreCase = true) -> "RADIO"
                    isTvSportsOrLocal || rawCategory.contains("tv", ignoreCase = true) -> "TV"
                    isMovieGenre || rawCategory.contains("movie", ignoreCase = true) -> "MOVIES"
                    currentState.currentNode?.uiTemplate == UiTemplate.MEDIA_HUB -> currentState.currentNode.name.uppercase().trim()
                    else -> rawCategory.uppercase()
                }

                val mediaCategory = MediaCategory.from(targetCategoryName)
                val filterCategories = mediaCategory.filterCategories
                val mediaHubTargetNodeName = mediaCategory.nodeName

                val matchedGenreChip = if (filterClean == "all" || filterClean == "everything") {
                    "All"
                } else {
                    filterCategories.find { chip ->
                        chip.lowercase().trim() == filterClean
                    }
                }

                val actionPrefix = if (matchedGenreChip != null || rawAction.equals("Filter", ignoreCase = true)) {
                    "Filter"
                } else {
                    rawAction
                }
                val finalValue = matchedGenreChip ?: filterOrChannelValue

                val categoryPath = navigationEngine.findPathToNode(mediaHubTargetNodeName, rootNode) ?: navigationEngine.findPathToNode(SectionNodes.MEDIA.value, rootNode)
                val valuePath = if (finalValue.isNotBlank() && matchedGenreChip == null) navigationEngine.findPathToNode(finalValue, rootNode) else null

                val valueNode = valuePath?.last()
                val categoryNode = categoryPath?.last()

                val destinationNode = categoryNode ?: valueNode ?: currentState.currentNode

                val updatedMediaState = if (finalValue.isNotBlank()) {
                    currentState.mediaState + (targetCategoryName to "$actionPrefix: $finalValue")
                } else {
                    currentState.mediaState
                }

                val isNavigatingAway = destinationNode?.name != currentState.currentNode?.name
                val updatedSelections = if (isNavigatingAway) {
                    currentState.selections.filterKeys { k -> k.equals("Language", ignoreCase = true) }
                } else currentState.selections

                val updatedState = currentState.copy(
                    currentNode = destinationNode,
                    navStack = categoryPath ?: currentState.navStack,
                    mediaState = updatedMediaState,
                    selections = updatedSelections,
                    isStreaming = false
                )
                ProcessedIntentResult(updatedState, ArisFeedback.mediaPlaying(finalValue.ifBlank { destinationNode?.name ?: targetCategoryName }, targetCategoryName))
            }

            ActionType.SERVICE_INFO -> {
                val topic = intent.targetSection ?: ""
                val path = navigationEngine.findPathToNode(topic, rootNode)

                if (!path.isNullOrEmpty()) {
                    val matchedNode = path.last()

                    val isNavigatingAway = matchedNode.name != currentState.currentNode?.name
                    val updatedSelections = if (isNavigatingAway) {
                        currentState.selections.filterKeys { k -> k.equals("Language", ignoreCase = true) }
                    } else currentState.selections

                    val updatedState = currentState.copy(
                        currentNode = matchedNode,
                        navStack = path,
                        selections = updatedSelections,
                        isStreaming = false
                    )
                    ProcessedIntentResult(updatedState, ArisFeedback.serviceInfo(topic, matchedNode.content))
                } else {
                    ProcessedIntentResult(currentState, ArisFeedback.navNotFound(topic))
                }
            }
        }
    }
}

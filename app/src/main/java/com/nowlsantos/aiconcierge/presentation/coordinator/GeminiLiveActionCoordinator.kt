package com.nowlsantos.aiconcierge.presentation.coordinator

import com.nowlsantos.aiconcierge.domain.intent.ActionIntentProcessor
import com.nowlsantos.aiconcierge.domain.navigation.NodeNavigationEngine
import com.nowlsantos.aiconcierge.presentation.ActionDefaultText
import com.nowlsantos.aiconcierge.presentation.ActionStatus
import com.nowlsantos.aiconcierge.presentation.ArisFeedback
import com.nowlsantos.aiconcierge.presentation.ConciergeUiEffect
import com.nowlsantos.aiconcierge.presentation.ConciergeUiState
import com.nowlsantos.aiconcierge.presentation.DevicePowerState
import com.nowlsantos.aiconcierge.presentation.OrderState
import com.nowlsantos.aiconcierge.presentation.ScrollEvent
import com.nowlsantos.aiconcierge.presentation.SectionNodes
import com.nowlsantos.aiconcierge.presentation.SystemStateKey
import com.nowlsantos.aiconcierge.repository.ConciergeRepository
import com.nowlsantos.aiconcierge.speech.GeminiLiveManager
import com.nowlsantos.aiconcierge.speech.GeminiLiveState
import com.nowlsantos.aiconcierge.ui.component.modal.TopBarDialogType
import com.nowlsantos.aiconcierge.utils.Logger
import com.nowlsantos.aiconcierge.utils.buildPlacedOrder
import com.nowlsantos.common.ActionIntent
import com.nowlsantos.common.ActionType
import com.nowlsantos.common.DocumentNode
import com.nowlsantos.common.UiTemplate
import kotlin.time.Duration.Companion.seconds

class GeminiLiveActionCoordinator(
    private val geminiLiveManager: GeminiLiveManager,
    private val intentProcessor: ActionIntentProcessor,
    private val navigationEngine: NodeNavigationEngine,
    @Suppress("unused") private val repository: ConciergeRepository? = null
) {

    private var lastNotifiedScreenName: String? = "Home"
    private var orderResetJob: kotlinx.coroutines.Job? = null

    /**
     * Touch-gated context update.
     * Sends context updates ONLY for manual finger touch taps to eliminate voice tool call echo loops.
     */
    fun notifyClientStateChanged(
        currentNode: DocumentNode?,
        stateSummaryDetail: String? = null,
        canScrollDown: Boolean = true,
        canScrollUp: Boolean = false
    ) {
        val nodeName = currentNode?.name ?: "Home"
        lastNotifiedScreenName = nodeName

        val whitelistDetails = if (currentNode?.uiTemplate == UiTemplate.SELECTION_VIEW && currentNode.children.isNotEmpty()) {
            val items = currentNode.children.flatMap { group -> group.children.map { it.name } }
            " Valid selectable items on this screen: [${items.joinToString(", ")}]. Use 'select_option' exclusively."
        } else ""

        val contextSummary = "Active Screen: '$nodeName' (Template: ${currentNode?.uiTemplate ?: "RIBBON_HUB"}).$whitelistDetails"
        geminiLiveManager.sendContextUpdate(contextSummary)
    }

    fun bindCallbacks(
        updateState: ((ConciergeUiState) -> ConciergeUiState) -> Unit,
        getState: () -> ConciergeUiState,
        getRootNode: () -> DocumentNode?,
        launchCoroutine: (suspend () -> Unit) -> kotlinx.coroutines.Job?,
        emitEffect: ((ConciergeUiEffect) -> Unit)? = null
    ) {
        // Bind dynamic root node provider to extract master_tree.json navigation targets dynamically
        geminiLiveManager.getRootNode = getRootNode

        fun processIntentImmediate(intent: ActionIntent): String {
            val root = getRootNode()
            val current = getState()
            val result = intentProcessor.processActionIntent(intent, current, root)

            // APPLY UI STATE UPDATE IMMEDIATELY (<1ms) FOR ZERO LATENCY!
            updateState { state ->
                val isSpeakingNow = geminiLiveManager.state.value == GeminiLiveState.Speaking
                val isListeningNow = geminiLiveManager.state.value == GeminiLiveState.Listening
                result.newState.copy(
                    isSpeaking = isSpeakingNow,
                    isListening = isListeningNow
                )
            }
            return result.feedback
        }

        // DISABLE SPOKEN TEXT PARSING COMPLETELY
        geminiLiveManager.onSpokenTextReceived = null

        geminiLiveManager.onNavigate = { target ->
            val targetClean = target.lowercase().trim()
            val isStandbyTrigger = targetClean in setOf(
                "standby", "stand by", "sleep", "pause", "rest", "goodbye", "good night", "goodnight", "dismiss", "that will be all", "that is all"
            ) || targetClean.startsWith("standby") || targetClean.startsWith("stand by") || targetClean.endsWith("standby")

            if (isStandbyTrigger) {
                Logger.d("[STANDBY_FLOW] Gemini navigation intent intercepted keyword '$target' as system standby/sleep command. Processing via GeminiLiveActionCoordinator.", "STANDBY_FLOW")
                geminiLiveManager.requestStandbyAfterPlayback()
                "Status: SUCCESS. Entering standby mode."
            } else {
                processIntentImmediate(ActionIntent(ActionType.NAVIGATE, targetSection = target))
            }
        }

        geminiLiveManager.onScrollPage = { direction ->
            val dir = direction.uppercase().trim()
            val state = getState()

            val isScrollAllowed = when (dir) {
                "DOWN", "BOTTOM" -> state.canScrollDown
                "UP", "TOP" -> state.canScrollUp
                else -> true
            }

            if (isScrollAllowed) {
                val event = ScrollEvent(direction = dir)
                updateState { it.copy(scrollEvent = event) }
                emitEffect?.invoke(ConciergeUiEffect.Scroll(direction = dir))
                "Status: SUCCESS. Scrolled $dir."
            } else {
                Logger.d("SCROLL REJECTED: Direction $dir disabled on active screen status [canScrollDown=${state.canScrollDown}, canScrollUp=${state.canScrollUp}]", "ACTION_INTENT")
                "Status: DISABLED. Cannot scroll $dir. All items in that direction are already visible or end of list reached."
            }
        }

        geminiLiveManager.onRoomControl = { category, target, value ->
            processIntentImmediate(ActionIntent(ActionType.ROOM_CONTROL, category = category, targetZone = target, value = value))
        }

        geminiLiveManager.onMediaControl = { category, action, value ->
            processIntentImmediate(ActionIntent(ActionType.MEDIA_CONTROL, category = category, targetSection = value, value = action))
        }

        geminiLiveManager.onGetServiceInfo = { topic ->
            val root = getRootNode()
            val current = getState().currentNode
            val path = root?.let { navigationEngine.findPathToNode(topic, it, current) }
            val matchedNode = path?.last()

            // Infallible Navigation Fail-Safe: If get_service_info is called for any browsable node, execute NAVIGATE!
            if (matchedNode != null) {
                Logger.d("get_service_info called for node '${matchedNode.name}'. Executing NAVIGATE for UI transition.", "ACTION_INTENT")
                processIntentImmediate(ActionIntent(ActionType.NAVIGATE, targetSection = topic))
            } else {
                processIntentImmediate(ActionIntent(ActionType.SERVICE_INFO, targetSection = topic))
            }
        }

        geminiLiveManager.onRequestHousekeeping = { items, urgent ->
            val requestedItems = items.ifBlank { ActionDefaultText.MAKEUP_ROOM.value }
            updateState { state ->
                val isSpeakingNow = geminiLiveManager.state.value == GeminiLiveState.Speaking
                val isListeningNow = geminiLiveManager.state.value == GeminiLiveState.Listening
                state.copy(
                    actionParams = mapOf(
                        SystemStateKey.SUBTITLE.key to requestedItems,
                        SystemStateKey.STATUS.key to if (urgent) ActionStatus.PRIORITY_DISPATCH.value else ActionStatus.CONFIRMED.value
                    ),
                    isSpeaking = isSpeakingNow,
                    isListening = isListeningNow,
                    isStreaming = false
                )
            }
            ArisFeedback.housekeepingDispatched(requestedItems)
        }

        geminiLiveManager.onSetDoNotDisturb = { enabled, untilTime ->
            updateState { state ->
                val isSpeakingNow = geminiLiveManager.state.value == GeminiLiveState.Speaking
                val isListeningNow = geminiLiveManager.state.value == GeminiLiveState.Listening
                state.copy(
                    roomStates = state.roomStates + (SystemStateKey.DND.key to if (enabled) DevicePowerState.ON.value else DevicePowerState.OFF.value),
                    isSpeaking = isSpeakingNow,
                    isListening = isListeningNow,
                    isStreaming = false
                )
            }
            ArisFeedback.dndUpdated(enabled, untilTime)
        }

        geminiLiveManager.onScheduleValet = { type ->
            val requestedType = type.ifBlank { ActionDefaultText.SHOE_SHINE.value }
            updateState { state ->
                val isSpeakingNow = geminiLiveManager.state.value == GeminiLiveState.Speaking
                val isListeningNow = geminiLiveManager.state.value == GeminiLiveState.Listening
                state.copy(
                    actionParams = mapOf(
                        SystemStateKey.SUBTITLE.key to "${ActionDefaultText.VALET_BOX_PICKUP.value}: $requestedType",
                        SystemStateKey.STATUS.key to ActionStatus.SCHEDULED.value
                    ),
                    isSpeaking = isSpeakingNow,
                    isListening = isListeningNow,
                    isStreaming = false
                )
            }
            ArisFeedback.valetScheduled(requestedType)
        }

        geminiLiveManager.onBookExperience = { exp, party, date ->
            val root = getRootNode()
            val current = getState().currentNode
            val path = root?.let { navigationEngine.findPathToNode(exp, it, current) } ?: root?.let { navigationEngine.findPathToNode(SectionNodes.PEN_ACADEMY.value, it, current) }
            val destinationNode = path?.last()

            updateState { state ->
                val currentName = state.currentNode?.name
                val isAlreadyOnScreen = navigationEngine.isSameViewType(currentName, destinationNode?.name, root)
                val isSpeakingNow = geminiLiveManager.state.value == GeminiLiveState.Speaking
                val isListeningNow = geminiLiveManager.state.value == GeminiLiveState.Listening
                val updatedSelections = if (!isAlreadyOnScreen) {
                    state.selections.filterKeys { k -> k.equals("Language", ignoreCase = true) }
                } else state.selections
                state.copy(
                    currentNode = if (isAlreadyOnScreen) state.currentNode else (destinationNode ?: state.currentNode),
                    navStack = if (isAlreadyOnScreen) state.navStack else (path ?: state.navStack),
                    selections = updatedSelections,
                    actionParams = mapOf(
                        SystemStateKey.SUBTITLE.key to "$party Guests • $date",
                        SystemStateKey.STATUS.key to ActionStatus.RESERVED.value
                    ),
                    isSpeaking = isSpeakingNow,
                    isListening = isListeningNow,
                    isStreaming = false
                )
            }
            ArisFeedback.experienceBooked(exp, party, date)
        }

        geminiLiveManager.onBookSpa = { treatment, party, dateTime ->
            val root = getRootNode()
            val current = getState().currentNode
            val path = root?.let { navigationEngine.findPathToNode(treatment, it, current) }
                ?: root?.let { navigationEngine.findPathToNode(SectionNodes.SPA_TREATMENTS.value, it, current) }
                ?: root?.let { navigationEngine.findPathToNode(SectionNodes.SPA_AND_WELLNESS.value, it, current) }

            val destinationNode = path?.last()

            updateState { state ->
                val currentName = state.currentNode?.name
                val isAlreadyOnScreen = navigationEngine.isSameViewType(currentName, destinationNode?.name, root)
                val isSpeakingNow = geminiLiveManager.state.value == GeminiLiveState.Speaking
                val isListeningNow = geminiLiveManager.state.value == GeminiLiveState.Listening
                val updatedSelections = if (!isAlreadyOnScreen) {
                    state.selections.filterKeys { k -> k.equals("Language", ignoreCase = true) }
                } else state.selections
                state.copy(
                    currentNode = if (isAlreadyOnScreen) state.currentNode else (destinationNode ?: state.currentNode),
                    navStack = if (isAlreadyOnScreen) state.navStack else (path ?: state.navStack),
                    selections = updatedSelections,
                    actionParams = mapOf(
                        SystemStateKey.SUBTITLE.key to "$party Guests • $dateTime",
                        SystemStateKey.STATUS.key to ActionStatus.SPA_RESERVED.value
                    ),
                    isSpeaking = isSpeakingNow,
                    isListening = isListeningNow,
                    isStreaming = false
                )
            }
            ArisFeedback.spaBooked(treatment, party, dateTime)
        }

        geminiLiveManager.onRequestTransportation = { dest, time, vehicle ->
            updateState { currentState ->
                val rootNode = currentState.navStack.firstOrNull() ?: currentState.currentNode
                val path = rootNode?.let { navigationEngine.findPathToNode(SectionNodes.TRANSPORTATION.value, it, currentState.currentNode) }
                val destinationNode = path?.last()
                val currentName = currentState.currentNode?.name
                val isAlreadyOnScreen = navigationEngine.isSameViewType(currentName, destinationNode?.name, rootNode)
                val isSpeakingNow = geminiLiveManager.state.value == GeminiLiveState.Speaking
                val isListeningNow = geminiLiveManager.state.value == GeminiLiveState.Listening
                val updatedSelections = if (!isAlreadyOnScreen) {
                    currentState.selections.filterKeys { k -> k.equals("Language", ignoreCase = true) }
                } else currentState.selections

                currentState.copy(
                    currentNode = if (isAlreadyOnScreen) currentState.currentNode else (destinationNode ?: currentState.currentNode),
                    navStack = if (isAlreadyOnScreen) currentState.navStack else (path ?: currentState.navStack),
                    selections = updatedSelections,
                    actionParams = mapOf(
                        SystemStateKey.SUBTITLE.key to "To $dest at $time",
                        SystemStateKey.STATUS.key to "$vehicle ${ActionStatus.CONFIRMED.value}"
                    ),
                    isSpeaking = isSpeakingNow,
                    isListening = isListeningNow,
                    isStreaming = false
                )
            }
            ArisFeedback.transportBooked(dest, time, vehicle)
        }

        geminiLiveManager.onReserveTable = { rest, party, time ->
            updateState { currentState ->
                val rootNode = currentState.navStack.firstOrNull() ?: currentState.currentNode
                val path = rootNode?.let { navigationEngine.findPathToNode(rest, it, currentState.currentNode) } ?: rootNode?.let { navigationEngine.findPathToNode(SectionNodes.RESTAURANTS.value, it, currentState.currentNode) }
                val destinationNode = path?.last()
                val currentName = currentState.currentNode?.name
                val isAlreadyOnScreen = navigationEngine.isSameViewType(currentName, destinationNode?.name, rootNode)
                val isSpeakingNow = geminiLiveManager.state.value == GeminiLiveState.Speaking
                val isListeningNow = geminiLiveManager.state.value == GeminiLiveState.Listening
                val updatedSelections = if (!isAlreadyOnScreen) {
                    currentState.selections.filterKeys { k -> k.equals("Language", ignoreCase = true) }
                } else currentState.selections

                currentState.copy(
                    currentNode = if (isAlreadyOnScreen) currentState.currentNode else (destinationNode ?: currentState.currentNode),
                    navStack = if (isAlreadyOnScreen) currentState.navStack else (path ?: currentState.navStack),
                    selections = updatedSelections,
                    actionParams = mapOf(
                        SystemStateKey.SUBTITLE.key to "$party Guests at $time",
                        SystemStateKey.STATUS.key to ActionStatus.CONFIRMED.value
                    ),
                    isSpeaking = isSpeakingNow,
                    isListening = isListeningNow,
                    isStreaming = false
                )
            }
            ArisFeedback.tableReserved(rest, party, time)
        }

        geminiLiveManager.onSetMasterScene = { scene ->
            val sceneClean = scene.lowercase().trim()
            val isStandbyTrigger = sceneClean in setOf(
                "standby", "stand by", "sleep", "pause", "rest", "goodbye", "good night", "goodnight", "dismiss"
            ) || sceneClean.contains("standby") || sceneClean.contains("stand by")

            if (isStandbyTrigger) {
                Logger.d("onSetMasterScene intercepted standby/sleep scene '$scene'. Requesting smooth standby.", "STANDBY_FLOW")
                geminiLiveManager.requestStandbyAfterPlayback()
            }

            updateState { state ->
                val activeZone = state.roomStates.keys.reversed().firstNotNullOfOrNull { key ->
                    val parts = key.split("-")
                    if (parts.size >= 2) {
                        val z = parts[1].trim().uppercase()
                        if (z in listOf("BEDROOM", "LIVING ROOM", "SITTING ROOM")) z else null
                    } else null
                } ?: "BEDROOM"

                val isSpeakingNow = geminiLiveManager.state.value == GeminiLiveState.Speaking
                val isListeningNow = geminiLiveManager.state.value == GeminiLiveState.Listening

                state.copy(
                    roomStates = state.roomStates + ("PRESET MOOD-$activeZone" to scene) + ("SCENE-$activeZone" to scene) + (SystemStateKey.SCENE.key to scene),
                    isSpeaking = isSpeakingNow,
                    isListening = isListeningNow,
                    isStreaming = false
                )
            }

            if (isStandbyTrigger) {
                "Status: SUCCESS. Entering standby mode."
            } else {
                ArisFeedback.sceneActivated(scene)
            }
        }

        geminiLiveManager.onStandby = {
            Logger.d("System standby requested via standby_system tool. Queueing smooth session stop AFTER farewell audio drains...", "ACTION_INTENT")
            geminiLiveManager.requestStandbyAfterPlayback()
        }

        geminiLiveManager.onSendPenChat = { recipient, _ ->
            ArisFeedback.penChatSent(recipient)
        }

        geminiLiveManager.onGetWeather = {
            ArisFeedback.weatherReport("18°C, Partly Cloudy in London")
        }

        geminiLiveManager.onCloseModal = {
            updateState { state ->
                val isSpeakingNow = geminiLiveManager.state.value == GeminiLiveState.Speaking
                val isListeningNow = geminiLiveManager.state.value == GeminiLiveState.Listening
                state.copy(
                    activeTopBarDialog = TopBarDialogType.NONE,
                    isSpeaking = isSpeakingNow,
                    isListening = isListeningNow,
                    isStreaming = false
                )
            }
            ArisFeedback.serviceInfo("Modal", "Closed modal")
        }

        geminiLiveManager.onSelectOption = { group, option ->
            val isLanguageGroup = group.equals("Language", ignoreCase = true) || group.equals("Languages", ignoreCase = true)
            if (isLanguageGroup) {
                updateState { state ->
                    val isSpeakingNow = geminiLiveManager.state.value == GeminiLiveState.Speaking
                    val isListeningNow = geminiLiveManager.state.value == GeminiLiveState.Listening
                    state.copy(
                        selections = state.selections + ("Language" to option),
                        activeTopBarDialog = TopBarDialogType.NONE,
                        isSpeaking = isSpeakingNow,
                        isListening = isListeningNow,
                        isStreaming = false
                    )
                }
                "Status: SUCCESS. Language set to '$option' and modal closed."
            } else {
                val current = getState().currentNode

                val groupNode = if (current?.uiTemplate == UiTemplate.SELECTION_VIEW) {
                    current.children.find { g ->
                        val gName = g.name.lowercase()
                        val qGroup = group.lowercase()
                        gName == qGroup || gName.contains(qGroup) || qGroup.contains(gName) ||
                        g.children.any { c -> c.name.lowercase().contains(qGroup) || qGroup.contains(c.name.lowercase()) }
                    }
                } else null

                val targetGroup = groupNode?.name ?: group

                updateState { state ->
                    val existingVal = state.selections.entries.find { (k, _) ->
                        k.equals(targetGroup, ignoreCase = true) || k.lowercase().contains(targetGroup.lowercase()) || targetGroup.lowercase().contains(k.lowercase())
                    }?.value

                    val mergedVal = mergeSelectionValue(groupNode, existingVal, option)
                    val updatedSelections = state.selections + (targetGroup to mergedVal)
                    val isSpeakingNow = geminiLiveManager.state.value == GeminiLiveState.Speaking
                    val isListeningNow = geminiLiveManager.state.value == GeminiLiveState.Listening
                    state.copy(
                        selections = updatedSelections,
                        isSpeaking = isSpeakingNow,
                        isListening = isListeningNow,
                        isStreaming = false
                    )
                }
                ArisFeedback.selectionSuccess(targetGroup, option)
            }
        }

        geminiLiveManager.onSubmitOrder = {
            updateState { state ->
                val currentNode = state.currentNode
                currentNode?.let { node ->
                    val summary = node.copy(name = ActionDefaultText.ORDER_SUMMARY.value, uiTemplate = UiTemplate.ORDER_SUMMARY)
                    val isSpeakingNow = geminiLiveManager.state.value == GeminiLiveState.Speaking
                    val isListeningNow = geminiLiveManager.state.value == GeminiLiveState.Listening
                    state.copy(
                        currentNode = summary,
                        navStack = state.navStack + summary,
                        isOrderConfirmed = false,
                        isSpeaking = isSpeakingNow,
                        isListening = isListeningNow,
                        isStreaming = false
                    )
                } ?: state
            }
            "Status: SUCCESS. Order submitted and summary displayed."
        }

        geminiLiveManager.onConfirmOrder = {
            updateState { state ->
                val currentNode = state.currentNode
                val selections = state.selections
                val newOrder = buildPlacedOrder(currentNode, selections)

                val isSpeakingNow = geminiLiveManager.state.value == GeminiLiveState.Speaking
                val isListeningNow = geminiLiveManager.state.value == GeminiLiveState.Listening
                state.copy(
                    isOrderConfirmed = true,
                    pastOrders = state.pastOrders + newOrder,
                    isSpeaking = isSpeakingNow,
                    isListening = isListeningNow,
                    isStreaming = false
                )
            }
            orderResetJob?.cancel()
            orderResetJob = launchCoroutine {
                kotlinx.coroutines.delay(5.seconds)
                updateState { state ->
                    if (state.isOrderConfirmed) {
                        getState().navStack.firstOrNull()?.let { root ->
                            state.copy(
                                currentNode = root,
                                navStack = listOf(root),
                                selections = state.selections.filterKeys { k -> k.equals("Language", ignoreCase = true) },
                                isOrderConfirmed = false
                            )
                        } ?: state.copy(
                            selections = state.selections.filterKeys { k -> k.equals("Language", ignoreCase = true) },
                            isOrderConfirmed = false
                        )
                    } else state
                }
            }
            ArisFeedback.orderConfirmed()
        }

        geminiLiveManager.onCancelOrder = { orderName ->
            updateState { state ->
                val updatedOrders = state.pastOrders.map { order ->
                    if (orderName.isBlank() || order.orderName.contains(orderName, ignoreCase = true)) {
                        order.copy(state = OrderState.CANCELLED, statusText = "Cancelled by Guest")
                    } else order
                }
                state.copy(pastOrders = updatedOrders)
            }
            "Status: SUCCESS. Order successfully cancelled."
        }

        geminiLiveManager.onSetTheme = { isDark ->
            updateState { state ->
                val isSpeakingNow = geminiLiveManager.state.value == GeminiLiveState.Speaking
                val isListeningNow = geminiLiveManager.state.value == GeminiLiveState.Listening
                state.copy(
                    isDarkMode = isDark,
                    isSpeaking = isSpeakingNow,
                    isListening = isListeningNow,
                    isStreaming = false
                )
            }
        }
    }

    fun unbindCallbacks() {
        orderResetJob?.cancel()
        orderResetJob = null
        geminiLiveManager.getRootNode = null
        geminiLiveManager.onSpokenTextReceived = null
        geminiLiveManager.onNavigate = null
        geminiLiveManager.onScrollPage = null
        geminiLiveManager.onRoomControl = null
        geminiLiveManager.onMediaControl = null
        geminiLiveManager.onGetServiceInfo = null
        geminiLiveManager.onRequestHousekeeping = null
        geminiLiveManager.onSetDoNotDisturb = null
        geminiLiveManager.onScheduleValet = null
        geminiLiveManager.onBookExperience = null
        geminiLiveManager.onBookSpa = null
        geminiLiveManager.onRequestTransportation = null
        geminiLiveManager.onReserveTable = null
        geminiLiveManager.onSetMasterScene = null
        geminiLiveManager.onStandby = null
        geminiLiveManager.onSendPenChat = null
        geminiLiveManager.onGetWeather = null
        geminiLiveManager.onCloseModal = null
        geminiLiveManager.onSelectOption = null
        geminiLiveManager.onSubmitOrder = null
        geminiLiveManager.onConfirmOrder = null
        geminiLiveManager.onCancelOrder = null
        geminiLiveManager.onSetTheme = null
    }
}

private fun mergeSelectionValue(
    groupNode: DocumentNode?,
    existingValue: String?,
    newOption: String
): String {
    val controlType = groupNode?.control?.lowercase() ?: "radio"
    when (controlType) {
        "counter" -> {
            val countMap = mutableMapOf<String, Int>()
            val displayNameMap = mutableMapOf<String, String>()

            if (!existingValue.isNullOrBlank()) {
                existingValue.split(",").forEach { part ->
                    val trimmed = part.trim()
                    if (trimmed.isNotBlank()) {
                        if (trimmed.contains(":")) {
                            val kv = trimmed.split(":")
                            if (kv.size >= 2) {
                                val name = kv[0].trim()
                                val qty = kv[1].trim().toIntOrNull() ?: 1
                                if (qty > 0) {
                                    val key = name.lowercase()
                                    countMap[key] = qty
                                    displayNameMap[key] = name
                                }
                            }
                        } else {
                            val digits = Regex("\\d+").find(trimmed)?.value?.toIntOrNull() ?: 1
                            val nameOnly = trimmed.replace(Regex("\\d+|x|X"), "").trim()
                            if (nameOnly.isNotBlank()) {
                                val key = nameOnly.lowercase()
                                countMap[key] = digits
                                displayNameMap[key] = nameOnly
                            }
                        }
                    }
                }
            }

            val trimmedNew = newOption.trim()
            var reqQty = 1
            var reqName = trimmedNew

            if (trimmedNew.contains(":")) {
                val kv = trimmedNew.split(":")
                if (kv.size >= 2) {
                    reqName = kv[0].trim()
                    reqQty = kv[1].trim().toIntOrNull() ?: 1
                }
            } else {
                val digits = Regex("\\d+").find(trimmedNew)?.value?.toIntOrNull()
                if (digits != null) reqQty = digits
                val nameOnly = trimmedNew.replace(Regex("\\d+|x|X"), "").trim()
                if (nameOnly.isNotBlank()) reqName = nameOnly
            }

            val rLower = reqName.lowercase().trim()
            val exactChild = groupNode?.children?.find { it.name.lowercase().trim() == rLower }
            val matchedChild = exactChild ?: groupNode?.children?.find { child ->
                val cLower = child.name.lowercase().trim()
                cLower.contains(rLower) || rLower.contains(cLower)
            }
            val resolvedName = matchedChild?.name ?: reqName
            val resolvedKey = resolvedName.lowercase()

            val currentQty = countMap[resolvedKey] ?: 0
            val updatedQty = if (currentQty > 0 && !trimmedNew.contains(":")) currentQty + reqQty else reqQty

            countMap[resolvedKey] = updatedQty
            displayNameMap[resolvedKey] = resolvedName

            return countMap.entries
                .filter { it.value > 0 }
                .joinToString(", ") { (k, qty) ->
                    val disp = displayNameMap[k] ?: k
                    "$disp: $qty"
                }
        }

        "checkbox" -> {
            val existingSet = existingValue?.split(",")?.map { it.trim() }?.filter { it.isNotBlank() }?.toSet() ?: emptySet()
            val rLower = newOption.lowercase().trim()
            val exactChild = groupNode?.children?.find { it.name.lowercase().trim() == rLower }
            val matchedChild = exactChild ?: groupNode?.children?.find { child ->
                val cLower = child.name.lowercase().trim()
                cLower.contains(rLower) || rLower.contains(cLower)
            }
            val resolvedName = matchedChild?.name ?: newOption.trim()
            val maxLimit = groupNode?.maxSelections
            if (maxLimit != null && existingSet.size >= maxLimit && !existingSet.any { it.equals(resolvedName, ignoreCase = true) }) {
                return existingSet.joinToString(", ")
            }
            val updatedSet = existingSet + resolvedName
            return updatedSet.joinToString(", ")
        }

        else -> {
            val rLower = newOption.lowercase().trim()
            val exactChild = groupNode?.children?.find { it.name.lowercase().trim() == rLower }
            val matchedChild = exactChild ?: groupNode?.children?.find { child ->
                val cLower = child.name.lowercase().trim()
                cLower.contains(rLower) || rLower.contains(cLower)
            }
            return matchedChild?.name ?: newOption.trim()
        }
    }
}

package com.nowlsantos.aiconcierge.presentation

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nowlsantos.aiconcierge.R
import com.nowlsantos.aiconcierge.di.networkJson
import com.nowlsantos.aiconcierge.domain.intent.ActionIntentProcessor
import com.nowlsantos.aiconcierge.domain.navigation.NodeNavigationEngine
import com.nowlsantos.aiconcierge.domain.sync.BrainConnectionWatchdog
import com.nowlsantos.aiconcierge.domain.sync.SyncResult
import com.nowlsantos.aiconcierge.presentation.coordinator.GeminiLiveActionCoordinator
import com.nowlsantos.aiconcierge.repository.ConciergeRepository
import com.nowlsantos.aiconcierge.speech.GeminiLiveManager
import com.nowlsantos.aiconcierge.speech.GeminiLiveState
import com.nowlsantos.aiconcierge.ui.component.modal.TopBarDialogType
import com.nowlsantos.aiconcierge.utils.Logger
import com.nowlsantos.aiconcierge.utils.buildPlacedOrder
import com.nowlsantos.common.ActionIntent
import com.nowlsantos.common.ConciergeStreamChunk
import com.nowlsantos.common.DocumentNode
import com.nowlsantos.common.UiTemplate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.time.Duration.Companion.seconds

sealed interface ConciergeUiEffect {
    data class Scroll(val direction: String) : ConciergeUiEffect
}

class ConciergeViewModel(
    context: Context,
    private val geminiLiveManager: GeminiLiveManager,
    private val repository: ConciergeRepository,
    private val navigationEngine: NodeNavigationEngine = NodeNavigationEngine(),
    private val intentProcessor: ActionIntentProcessor = ActionIntentProcessor(navigationEngine),
    private val watchdog: BrainConnectionWatchdog = BrainConnectionWatchdog(repository, navigationEngine),
    private val actionCoordinator: GeminiLiveActionCoordinator = GeminiLiveActionCoordinator(
        geminiLiveManager = geminiLiveManager,
        intentProcessor = intentProcessor,
        navigationEngine = navigationEngine,
        repository = repository
    )
) : ViewModel() {

    private val appContext = context.applicationContext

    private val _uiState = MutableStateFlow(ConciergeUiState(isLoading = true))
    val uiState: StateFlow<ConciergeUiState> = _uiState.asStateFlow()

    private val _uiEffect = MutableSharedFlow<ConciergeUiEffect>(extraBufferCapacity = 16)
    val uiEffect = _uiEffect.asSharedFlow()

    private var streamJob: Job? = null
    private var watchdogJob: Job? = null
    private var orderResetJob: Job? = null

    @Volatile
    private var isSyncInProgress = false

    @Volatile
    private var isStartupLoading = true

    private var lastSubmittedQuery = ""
    private var lastSubmitTimestamp = 0L

    // Synchronously pre-load master_tree.json from assets so rootTree is NEVER null at startup
    private var rootTree: DocumentNode? = try {
        val jsonText = context.applicationContext.assets.open("master_tree.json").bufferedReader().use { it.readText() }
        networkJson.decodeFromString<DocumentNode>(jsonText)
    } catch (e: Exception) {
        Logger.e("Failed to pre-load rootTree synchronously: ${e.localizedMessage}", e)
        null
    }

    init {
        isStartupLoading = false
        rootTree?.let { root ->
            val devPath = listOf(root)
            _uiState.update { state ->
                state.copy(
                    currentNode = root,
                    navStack = devPath,
                    responseText = root.content,
                    isLoading = false
                )
            }
        } ?: run {
            _uiState.update { it.copy(isLoading = false) }
        }

        actionCoordinator.bindCallbacks(
            updateState = { transform -> _uiState.update(transform) },
            getState = { _uiState.value },
            getRootNode = { getRootNode() },
            launchCoroutine = { block -> viewModelScope.launch { block() } },
            emitEffect = { effect -> _uiEffect.tryEmit(effect) }
        )
        observeGeminiState()
        startWatchdog()
    }

    override fun onCleared() {
        super.onCleared()
        watchdogJob?.cancel()
        streamJob?.cancel()
        orderResetJob?.cancel()
        actionCoordinator.unbindCallbacks()
        geminiLiveManager.stopSession()
    }

    private fun getRootNode(): DocumentNode? {
        val root = rootTree ?: _uiState.value.navStack.firstOrNull() ?: _uiState.value.currentNode
        if (root == null || root.level != 1) {
            return rootTree ?: try {
                val jsonText = appContext.assets.open("master_tree.json").bufferedReader().use { it.readText() }
                val localRoot = networkJson.decodeFromString<DocumentNode>(jsonText)
                rootTree = localRoot
                localRoot
            } catch (e: Exception) {
                root
            }
        }
        return root
    }

    fun onScrollStatusChanged(node: DocumentNode?, canDown: Boolean, canUp: Boolean) {
        _uiState.update { it.copy(canScrollDown = canDown, canScrollUp = canUp) }
        // Automated composable layout mounting is NOT a manual touch tap. Pass isTouch = false to prevent WebSocket context injection mid-turn.
        actionCoordinator.notifyClientStateChanged(node, canScrollDown = canDown, canScrollUp = canUp)
    }

    fun processActionIntent(intent: ActionIntent): String {
        var feedbackResult = ""
        _uiState.update { current ->
            val result = intentProcessor.processActionIntent(intent, current, getRootNode())
            feedbackResult = result.feedback
            result.newState
        }
        return feedbackResult
    }

    private fun observeGeminiState() {
        viewModelScope.launch {
            geminiLiveManager.state.collect { state ->
                when (state) {
                    is GeminiLiveState.Connecting -> {
                        _uiState.update { it.copy(debugInfo = R.string.connecting_aris, isStreaming = true) }
                    }
                    is GeminiLiveState.Listening -> {
                        _uiState.update { it.copy(isListening = true, isSpeaking = false, isStreaming = false, debugInfo = R.string.ready) }
                    }
                    is GeminiLiveState.Speaking -> {
                        _uiState.update { it.copy(isSpeaking = true, isListening = false, isStreaming = false, debugInfo = R.string.aris_speaking) }
                    }
                    is GeminiLiveState.Error -> {
                        _uiState.update { it.copy(error = state.message, isStreaming = false) }
                    }
                    is GeminiLiveState.Idle -> {
                        _uiState.update { it.copy(isListening = false, isSpeaking = false, isStreaming = false) }
                    }
                }
            }
        }
    }

    private fun startWatchdog() {
        watchdogJob?.cancel()
        watchdogJob = viewModelScope.launch(Dispatchers.IO) {
            while (isActive) {
                if (_uiState.value.currentNode == null && !isSyncInProgress) {
                    performInitialSync()
                } else {
                    watchdog.checkConnection()
                        .onSuccess {
                            _uiState.update {
                                it.copy(
                                    isServerConnected = true,
                                    isLoading = isStartupLoading,
                                    debugInfo = R.string.link_established
                                )
                            }
                        }
                        .onFailure {
                            _uiState.update {
                                it.copy(
                                    isServerConnected = false,
                                    isLoading = false,
                                    debugInfo = R.string.ready
                                )
                            }
                        }
                }
                delay(15.seconds)
            }
        }
    }

    private suspend fun performInitialSync() {
        if (isSyncInProgress) return
        isSyncInProgress = true
        _uiState.update { it.copy(isLoading = true, debugInfo = R.string.connecting_aris) }

        val devStartTarget = "Home"
        when (val result = watchdog.performInitialSync(appContext, devStartTarget)) {
            is SyncResult.Success -> {
                rootTree = result.rootNode
                _uiState.update { state ->
                    state.copy(
                        isLoading = isStartupLoading,
                        isServerConnected = result.isServerConnected,
                        currentNode = result.startNode,
                        navStack = result.devPath,
                        responseText = result.responseText,
                        debugInfo = R.string.ready
                    )
                }
                geminiLiveManager.warmupSession()
            }
            is SyncResult.Failure -> {
                _uiState.update { state ->
                    state.copy(
                        isLoading = isStartupLoading,
                        isServerConnected = false,
                        debugInfo = R.string.retry_in_5s
                    )
                }
            }
        }
        isSyncInProgress = false
    }

    private fun executeQuery(query: String) {
        val currentTime = System.currentTimeMillis()
        if (query == lastSubmittedQuery && (currentTime - lastSubmitTimestamp) < 2000) return

        lastSubmittedQuery = query
        lastSubmitTimestamp = currentTime

        _uiState.update { it.copy(isListening = false, isStreaming = true, transcript = "", predictedTranscript = "") }

        streamJob?.cancel()
        streamJob = viewModelScope.launch(Dispatchers.IO) {
            withContext(Dispatchers.Main) {
                _uiState.update { it.copy(transcript = "", responseText = "", isStreaming = true, error = null, debugInfo = R.string.searching_brain) }
            }
            repository.streamQuery(query).collect { chunk ->
                withContext(Dispatchers.Main) { handleStreamChunk(chunk) }
            }
        }
    }

    private fun handleStreamChunk(chunk: ConciergeStreamChunk) {
        if (chunk.error != null) {
            _uiState.update { it.copy(error = chunk.error, isStreaming = false) }
            return
        }
        chunk.node?.let {
            if (it.name != _uiState.value.currentNode?.name) {
                val root = getRootNode()
                val path = root?.let { r -> navigationEngine.findPathToNode(it.name, r) } ?: (_uiState.value.navStack + it)
                val isOrderSummary = it.uiTemplate == UiTemplate.ORDER_SUMMARY
                _uiState.update { state ->
                    state.copy(
                        currentNode = it,
                        navStack = path,
                        selections = if (!isOrderSummary) state.selections.filterKeys { k -> k.equals("Language", ignoreCase = true) } else state.selections
                    )
                }
            }
        }
        _uiState.update { state ->
            state.copy(
                responseText = state.responseText + chunk.text,
                isStreaming = !chunk.isFinished,
                actionParams = chunk.actionParams ?: state.actionParams,
                debugInfo = if (chunk.isFinished) R.string.ready else R.string.thinking
            )
        }
    }

    fun onEvent(event: ConciergeUiEvent) {
        Logger.d("UI EVENT: $event")
        when (event) {
            is ConciergeUiEvent.OnMicClicked -> toggleListening()
            is ConciergeUiEvent.OnThemeToggled -> _uiState.update { it.copy(isDarkMode = !it.isDarkMode) }
            is ConciergeUiEvent.OnBackClicked -> navigateBackLocally()
            is ConciergeUiEvent.OnHomeClicked -> navigateHomeLocally()
            is ConciergeUiEvent.OnNodeSelected -> {
                orderResetJob?.cancel()
                val rootNode = getRootNode()
                val path = rootNode?.let { navigationEngine.findPathToNode(event.node.name, it) } ?: (_uiState.value.navStack + event.node)
                val isNavigatingToNewNode = event.node.name != _uiState.value.currentNode?.name
                val isOrderSummary = event.node.uiTemplate == UiTemplate.ORDER_SUMMARY
                _uiState.update { it.copy(
                    currentNode = event.node,
                    navStack = path,
                    selections = if (isNavigatingToNewNode && !isOrderSummary) {
                        it.selections.filterKeys { k -> k.equals("Language", ignoreCase = true) }
                    } else it.selections,
                    isOrderConfirmed = false
                ) }
                actionCoordinator.notifyClientStateChanged(_uiState.value.currentNode)
            }
            is ConciergeUiEvent.OnSelectionChanged -> {
                _uiState.update { it.copy(selections = it.selections + (event.group to event.option)) }
            }
            is ConciergeUiEvent.OnLanguageSelected -> {
                _uiState.update { it.copy(selections = it.selections + ("Language" to event.language), activeTopBarDialog = TopBarDialogType.NONE) }
                geminiLiveManager.sendContextUpdate("Guest selected language '${event.language}' from tablet screen. Please switch your spoken language to '${event.language}' and confirm warmly to the guest now in '${event.language}'.")
            }
            is ConciergeUiEvent.OnTopBarDialogChanged -> {
                _uiState.update { it.copy(activeTopBarDialog = event.dialogType) }
            }
            is ConciergeUiEvent.OnActiveRoomZoneChanged -> {
                _uiState.update { it.copy(activeRoomZone = event.zone) }
            }
            is ConciergeUiEvent.OnRoomControlChanged -> {
                val catUpper = event.category.uppercase().trim()
                val targetUpper = event.target.uppercase().trim()
                val key = "$catUpper-$targetUpper"
                _uiState.update { current ->
                    var newRoomStates = current.roomStates + (key to event.value)
                    if (catUpper.contains("CURTAIN") || catUpper.contains("CLIMATE") || catUpper.contains("LIGHT") || catUpper.contains("FAN")) {
                        newRoomStates = newRoomStates.filterKeys { k ->
                            !k.startsWith("PRESET MOOD-$targetUpper") && !k.startsWith("MOOD-$targetUpper") && !k.startsWith("PRESET-$targetUpper")
                        }
                    }
                    current.copy(roomStates = newRoomStates)
                }
            }
            is ConciergeUiEvent.OnSubmitOrder -> {
                orderResetJob?.cancel()
                _uiState.value.currentNode?.let { node ->
                    val summary = node.copy(name = ActionDefaultText.ORDER_SUMMARY.value, uiTemplate = UiTemplate.ORDER_SUMMARY)
                    _uiState.update { it.copy(
                        currentNode = summary,
                        navStack = it.navStack + summary,
                        isOrderConfirmed = false
                    ) }
                }
            }
            is ConciergeUiEvent.OnConfirmOrder -> {
                val currentNode = _uiState.value.currentNode
                val selections = _uiState.value.selections
                val newOrder = buildPlacedOrder(currentNode, selections)

                // Keep selections while displaying confirmation so OrderSummaryView renders accurate total
                _uiState.update { it.copy(
                    isOrderConfirmed = true,
                    pastOrders = it.pastOrders + newOrder
                ) }
                orderResetJob?.cancel()
                orderResetJob = viewModelScope.launch {
                    delay(5.seconds)
                    if (_uiState.value.isOrderConfirmed) {
                        _uiState.value.navStack.firstOrNull()?.let { root ->
                            _uiState.update { it.copy(
                                currentNode = root,
                                navStack = listOf(root),
                                selections = it.selections.filterKeys { k -> k.equals("Language", ignoreCase = true) },
                                isOrderConfirmed = false
                            ) }
                        }
                    }
                }
            }
            is ConciergeUiEvent.OnCancelOrder -> {
                _uiState.update { state ->
                    val updatedOrders = state.pastOrders.map { order ->
                        if (order.id == event.orderId) {
                            order.copy(state = OrderState.CANCELLED, statusText = "Cancelled by Guest")
                        } else order
                    }
                    state.copy(pastOrders = updatedOrders)
                }
            }
            is ConciergeUiEvent.OnQuerySubmitted -> executeQuery(event.query)
            is ConciergeUiEvent.OnErrorDismissed -> _uiState.update { it.copy(error = null) }
        }
    }

    private fun toggleListening() {
        when (geminiLiveManager.state.value) {
            is GeminiLiveState.Listening,
            is GeminiLiveState.Connecting,
            is GeminiLiveState.Speaking -> geminiLiveManager.stopSession()
            is GeminiLiveState.Idle,
            is GeminiLiveState.Error -> geminiLiveManager.startSession()
        }
    }

    private fun navigateBackLocally() {
        orderResetJob?.cancel()
        if (_uiState.value.navStack.size > 1) {
            val updatedStack = _uiState.value.navStack.dropLast(1)
            val previousNode = updatedStack.last()
            val isReturningToConfigurator = previousNode.uiTemplate == UiTemplate.SELECTION_VIEW
            _uiState.update { it.copy(
                currentNode = previousNode,
                navStack = updatedStack,
                selections = if (isReturningToConfigurator) it.selections else it.selections.filterKeys { k -> k.equals("Language", ignoreCase = true) },
                isOrderConfirmed = false
            ) }
            actionCoordinator.notifyClientStateChanged(_uiState.value.currentNode)
            speakLocal(ArisFeedback.navSuccess(previousNode.name, previousNode.content))
        }
    }

    private fun navigateHomeLocally() {
        orderResetJob?.cancel()
        _uiState.value.navStack.firstOrNull()?.let { root ->
            _uiState.update { it.copy(
                currentNode = root,
                navStack = listOf(root),
                selections = it.selections.filterKeys { k -> k.equals("Language", ignoreCase = true) },
                isOrderConfirmed = false
            ) }
            actionCoordinator.notifyClientStateChanged(_uiState.value.currentNode)
            speakLocal(ArisFeedback.returningHome())
        }
    }

    private fun speakLocal(text: String) {
        _uiState.update { it.copy(responseText = text) }
    }

    internal fun findPathToNode(targetName: String, root: DocumentNode): List<DocumentNode>? {
        return navigationEngine.findPathToNode(targetName, root)
    }
}

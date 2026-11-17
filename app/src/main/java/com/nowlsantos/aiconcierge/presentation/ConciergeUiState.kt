package com.nowlsantos.aiconcierge.presentation

import com.nowlsantos.aiconcierge.ui.component.modal.TopBarDialogType
import com.nowlsantos.common.DocumentNode

data class ScrollEvent(
    val id: Long = System.nanoTime(),
    val direction: String
)

enum class OrderState {
    CREATION,
    CONFIRMED,
    COMPLETED,
    CANCELLED
}

data class PlacedOrder(
    val id: String = java.util.UUID.randomUUID().toString(),
    val orderName: String,
    val items: List<Pair<String, Double>>,
    val total: Double,
    val state: OrderState = OrderState.CONFIRMED,
    val statusText: String = "Dispatched to Kitchen",
    val timestamp: Long = System.currentTimeMillis()
)

data class ConciergeUiState(
    val isLoading: Boolean = false,
    val transcript: String = "",
    val predictedTranscript: String = "",
    val responseText: String = "",
    val currentNode: DocumentNode? = null,
    val navStack: List<DocumentNode> = emptyList(),
    val isListening: Boolean = false,
    val isStreaming: Boolean = false,
    val isSpeaking: Boolean = false,
    val isWaitingForSpeech: Boolean = false,
    val voiceVolume: Float = 0f,
    val isDarkMode: Boolean = true,
    val isServerConnected: Boolean = false,
    val error: String? = null,
    val debugInfo: Int? = null,
    val actionParams: Map<String, String>? = null,
    val selections: Map<String, String> = emptyMap(),
    val roomStates: Map<String, String> = emptyMap(),
    val mediaState: Map<String, String> = emptyMap(),
    val isOrderConfirmed: Boolean = false,
    val pastOrders: List<PlacedOrder> = emptyList(),
    val scrollEvent: ScrollEvent? = null,
    val canScrollDown: Boolean = true,
    val canScrollUp: Boolean = false,
    val activeRoomZone: String = "BEDROOM",
    val activeTopBarDialog: TopBarDialogType = TopBarDialogType.NONE
)

package com.nowlsantos.aiconcierge.presentation

import com.nowlsantos.aiconcierge.ui.component.modal.TopBarDialogType
import com.nowlsantos.common.DocumentNode

sealed interface ConciergeUiEvent {
    data object OnMicClicked : ConciergeUiEvent
    data object OnThemeToggled : ConciergeUiEvent
    data object OnBackClicked : ConciergeUiEvent
    data object OnHomeClicked : ConciergeUiEvent
    data class OnNodeSelected(val node: DocumentNode) : ConciergeUiEvent
    data class OnSelectionChanged(val group: String, val option: String) : ConciergeUiEvent
    data class OnLanguageSelected(val language: String) : ConciergeUiEvent
    data class OnRoomControlChanged(val category: String, val target: String, val value: String) : ConciergeUiEvent
    data class OnActiveRoomZoneChanged(val zone: String) : ConciergeUiEvent
    data class OnTopBarDialogChanged(val dialogType: TopBarDialogType) : ConciergeUiEvent
    data object OnSubmitOrder : ConciergeUiEvent
    data object OnConfirmOrder : ConciergeUiEvent
    data class OnCancelOrder(val orderId: String) : ConciergeUiEvent
    data class OnQuerySubmitted(val query: String) : ConciergeUiEvent
    data object OnErrorDismissed : ConciergeUiEvent
}

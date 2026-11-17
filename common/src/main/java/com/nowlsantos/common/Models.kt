package com.nowlsantos.common

import kotlinx.serialization.Serializable

object UiTemplate {
    const val RIBBON_HUB = "RIBBON_HUB"
    const val CATEGORY_GRID = "CATEGORY_GRID"
    const val CHOICE_LIST = "CHOICE_LIST"
    const val DETAIL_VIEW = "DETAIL_VIEW"
    const val SELECTION_VIEW = "SELECTION_VIEW"
    const val ORDER_SUMMARY = "ORDER_SUMMARY"
    const val PLACEHOLDER = "PLACEHOLDER"
    const val ROOM_CONTROLS = "ROOM_CONTROLS"
    const val MEDIA_HUB = "MEDIA_HUB"
}

@Serializable
enum class ActionType {
    NAVIGATE,
    ROOM_CONTROL,
    MEDIA_CONTROL,
    SERVICE_INFO
}

@Serializable
data class ActionIntent(
    val actionType: ActionType,
    val targetSection: String? = null,
    val category: String? = null,
    val targetZone: String? = null,
    val value: String? = null
)

@Serializable
data class DocumentNode(
    val name: String,
    val content: String,
    val level: Int = 1,
    val uiTemplate: String = "RIBBON_HUB",
    val imageAsset: String? = null,
    val iconName: String? = null,
    val displayType: String = "GRID",
    val keywords: String = "",
    val price: Double? = null,
    val control: String? = null,
    val maxSelections: Int? = null,
    val children: List<DocumentNode> = emptyList()
)

@Serializable
data class ConciergeStreamChunk(
    val text: String = "",
    val isFinished: Boolean = false,
    val node: DocumentNode? = null,
    val matchedSources: List<String> = emptyList(),
    val actionParams: Map<String, String>? = null,
    val status: String? = null,
    val error: String? = null
)

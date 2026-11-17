package com.nowlsantos.aiconcierge.ui.component

import androidx.compose.runtime.Composable
import com.nowlsantos.aiconcierge.utils.Logger
import com.nowlsantos.common.DocumentNode

@Composable
fun RibbonHub(
    nodes: List<DocumentNode>,
    roomStates: Map<String, String> = emptyMap(),
    actionParams: Map<String, String>? = null,
    onNodeClick: (DocumentNode) -> Unit
) {
    Logger.d("RibbonHub rendering ${nodes.size} nodes: ${nodes.map { it.name }}", "RIBBON_HUB")

    PeninsulaDashboardHub(
        nodes = nodes,
        roomStates = roomStates,
        actionParams = actionParams,
        onNodeClick = onNodeClick
    )
}

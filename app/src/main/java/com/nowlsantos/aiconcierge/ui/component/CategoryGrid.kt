package com.nowlsantos.aiconcierge.ui.component

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.nowlsantos.aiconcierge.ui.component.widget.MasterCard
import com.nowlsantos.aiconcierge.ui.theme.WhitePen
import com.nowlsantos.aiconcierge.ui.theme.sfTypography
import com.nowlsantos.aiconcierge.utils.Logger
import com.nowlsantos.common.DocumentNode

@Composable
fun CategoryGrid(
    title: String,
    nodes: List<DocumentNode>,
    onNodeClick: (DocumentNode) -> Unit
) {
    Logger.d("CategoryGrid '$title' rendering ${nodes.size} nodes: ${nodes.map { it.name }}", "CATEGORY_GRID")
    val dpSize = DpSize(280.dp, 160.dp)

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // TOP TITLE
            Spacer(modifier = Modifier.weight(0.5f))
            
            Text(
                text = title.uppercase(),
                style = sfTypography.headlineMedium,
                color = WhitePen,
                fontWeight = FontWeight.Normal,
                modifier = Modifier.padding(bottom = 32.dp)
            )
            
            // DYNAMIC GRID (Rows of 4)
            val rows = nodes.chunked(4)
            rows.forEachIndexed { index, rowNodes ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(20.dp, Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    rowNodes.forEach { node ->
                        Box(
                            modifier = Modifier
                                .width(dpSize.width)
                                .height(dpSize.height)
                        ) {
                            MasterCard(
                                node = node,
                                onClick = { onNodeClick(node) }
                            )
                        }
                    }
                }
                if (index < rows.size - 1) {
                    Spacer(modifier = Modifier.height(20.dp))
                }
            }
            
            Spacer(modifier = Modifier.weight(1f))
        }
    }
}

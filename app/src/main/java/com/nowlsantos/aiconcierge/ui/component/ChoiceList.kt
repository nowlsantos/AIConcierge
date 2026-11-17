package com.nowlsantos.aiconcierge.ui.component

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.nowlsantos.aiconcierge.presentation.ScrollEvent
import com.nowlsantos.aiconcierge.ui.component.widget.ChoiceListCard
import com.nowlsantos.aiconcierge.ui.theme.WhitePen
import com.nowlsantos.aiconcierge.ui.theme.sfTypography
import com.nowlsantos.aiconcierge.utils.Logger
import com.nowlsantos.common.DocumentNode

/**
 * CHOICE_LIST
 * Generic 60fps scrolling grid layout template for displaying selectable options or sub-categories.
 */
@Composable
fun ChoiceList(
    title: String,
    nodes: List<DocumentNode>,
    scrollEvent: ScrollEvent? = null,
    onScrollStatusChanged: (canScrollDown: Boolean, canScrollUp: Boolean) -> Unit = { _, _ -> },
    onNodeClick: (DocumentNode) -> Unit
) {
    Logger.d("ChoiceList '$title' rendering ${nodes.size} nodes", "CHOICE_LIST")
    val dpSize = DpSize(280.dp, 160.dp)
    val lazyListState = key(title) { rememberLazyListState() }

    // Reset scroll position to TOP whenever a new choice list page is displayed
    LaunchedEffect(title, nodes) {
        lazyListState.scrollToItem(0)
    }

    // Real-time Scroll Ability Detection
    val canScrollDown by remember { derivedStateOf { lazyListState.canScrollForward } }
    val canScrollUp by remember { derivedStateOf { lazyListState.canScrollBackward } }

    LaunchedEffect(canScrollDown, canScrollUp) {
        onScrollStatusChanged(canScrollDown, canScrollUp)
    }

    val rows = remember(nodes) { nodes.chunked(4) }

    // Voice-Controlled Smooth Scroll Animations
    LaunchedEffect(scrollEvent) {
        scrollEvent?.let { event ->
            when (event.direction) {
                "DOWN" -> {
                    val currentIndex = lazyListState.firstVisibleItemIndex
                    val targetIndex = (currentIndex + 2).coerceAtMost((rows.size - 1).coerceAtLeast(0))
                    lazyListState.animateScrollToItem(targetIndex)
                }
                "UP" -> {
                    val currentIndex = lazyListState.firstVisibleItemIndex
                    val targetIndex = (currentIndex - 2).coerceAtLeast(0)
                    lazyListState.animateScrollToItem(targetIndex)
                }
                "TOP" -> lazyListState.animateScrollToItem(0)
                "BOTTOM" -> lazyListState.animateScrollToItem((rows.size - 1).coerceAtLeast(0))
            }
        }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .padding(bottom = 50.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // TOP TITLE
            Text(
                text = title.uppercase(),
                style = sfTypography.titleLarge,
                color = WhitePen,
                fontWeight = FontWeight.Normal,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            // DYNAMIC 60FPS LAZY SCROLLING GRID (Rows of 4)
            LazyColumn(
                state = lazyListState,
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                itemsIndexed(rows) { _, rowNodes ->
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
                                ChoiceListCard(
                                    node = node,
                                    onClick = { onNodeClick(node) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

package com.nowlsantos.aiconcierge.ui.component

import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nowlsantos.aiconcierge.presentation.ScrollEvent
import com.nowlsantos.aiconcierge.ui.component.widget.RoomControlWidget
import com.nowlsantos.aiconcierge.ui.component.widget.ServiceActionWidget
import com.nowlsantos.aiconcierge.ui.component.widget.SmartText
import com.nowlsantos.aiconcierge.ui.theme.PeninsulaGold
import com.nowlsantos.aiconcierge.ui.theme.sfTypography
import com.nowlsantos.aiconcierge.utils.LoadImage
import com.nowlsantos.aiconcierge.utils.Logger
import com.nowlsantos.common.DocumentNode

@Composable
fun DetailView(
    node: DocumentNode, 
    actionParams: Map<String, String>? = null,
    roomStates: Map<String, String> = emptyMap(),
    mediaState: Map<String, String> = emptyMap(),
    navStack: List<DocumentNode> = emptyList(),
    scrollEvent: ScrollEvent? = null,
    onScrollStatusChanged: (canScrollDown: Boolean, canScrollUp: Boolean) -> Unit = { _, _ -> }
) {
    Logger.d("DetailView rendering node: '${node.name}' (imageAsset: ${node.imageAsset})", "DETAIL_VIEW")

    val scrollState = key(node.name) { rememberScrollState() }

    // Reset scroll position to TOP whenever a new detail page/node is displayed
    LaunchedEffect(node.name) {
        scrollState.scrollTo(0)
    }

    // Real-time Scroll Ability Detection
    val canScrollDown by remember { derivedStateOf { scrollState.canScrollForward } }
    val canScrollUp by remember { derivedStateOf { scrollState.canScrollBackward } }

    LaunchedEffect(canScrollDown, canScrollUp) {
        onScrollStatusChanged(canScrollDown, canScrollUp)
    }

    // Voice-Controlled Smooth Scroll Animations
    LaunchedEffect(scrollEvent) {
        scrollEvent?.let { event ->
            when (event.direction) {
                "DOWN" -> scrollState.animateScrollBy(500f)
                "UP" -> scrollState.animateScrollBy(-500f)
                "TOP" -> scrollState.animateScrollTo(0)
                "BOTTOM" -> scrollState.animateScrollTo(scrollState.maxValue)
            }
        }
    }

    Box(modifier = Modifier
        .fillMaxSize()
        .padding(vertical = 24.dp)
    ) {
        Row(modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Left Column: Item Title & Description
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(scrollState)
            ) {
                Text(
                    text = node.name,
                    style = sfTypography.headlineMedium,
                    color = PeninsulaGold,
                    letterSpacing = 1.sp,
                )
                Spacer(modifier = Modifier.height(24.dp))
                SmartText(text = node.content)
            }

            // Right Column: High-Definition Hero Image Asset OR Action Widget
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(20.dp))
            ) {
                if (!node.imageAsset.isNullOrBlank()) {
                    // Render High-Definition Hero Asset Image
                    LoadImage(
                        modifier = Modifier.fillMaxSize(),
                        assetName = node.imageAsset,
                        description = node.name,
                        contentScale = ContentScale.Crop
                    )
                } else {
                    // Fallback to Action Widgets
                    val parentNode = navStack.getOrNull(navStack.size - 2) ?: node
                    val resolvedIcon = node.iconName ?: parentNode.iconName

                    // This will render the image on the remote control
                    when (resolvedIcon) {
                        "controls" -> {
                            val category = parentNode.name.uppercase()
                            val geminiValue = roomStates["$category-All"]
                            RoomControlWidget(
                                type = actionParams?.get("type") ?: category,
                                value = geminiValue ?: actionParams?.get("value") ?: node.name
                            )
                        }
                        "media" -> {
                            val category = parentNode.name.uppercase()
                            val currentPlay = mediaState[category] ?: node.name
                            RoomControlWidget(
                                type = category.uppercase(),
                                value = currentPlay
                            )
                        }
                        // This will render the image asset as a fallback
                        else -> {
                            ServiceActionWidget(
                                title = node.name,
                                subtitle = actionParams?.get("subtitle") ?: parentNode.name,
                                status = actionParams?.get("status") ?: "AVAILABLE",
                                iconType = "SERVICE"
                            )
                        }
                    }
                }
            }
        }
    }
}

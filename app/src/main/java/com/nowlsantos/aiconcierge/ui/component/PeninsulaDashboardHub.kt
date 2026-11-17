package com.nowlsantos.aiconcierge.ui.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.asComposePaint
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.nowlsantos.aiconcierge.R
import com.nowlsantos.aiconcierge.ui.component.widget.AnalogClock
import com.nowlsantos.aiconcierge.ui.component.widget.DateTimeWeather
import com.nowlsantos.aiconcierge.ui.component.widget.MakeupRoomWidget
import com.nowlsantos.aiconcierge.ui.component.widget.MasterCard
import com.nowlsantos.aiconcierge.ui.component.widget.PeninsulaGallery
import com.nowlsantos.aiconcierge.ui.component.widget.PrivacyWidget
import com.nowlsantos.aiconcierge.ui.component.widget.ValetWidget
import com.nowlsantos.aiconcierge.ui.theme.BlackPen
import com.nowlsantos.aiconcierge.ui.theme.GreyPen
import com.nowlsantos.aiconcierge.ui.theme.PeninsulaCharcoal
import com.nowlsantos.aiconcierge.ui.theme.PeninsulaGreyCard
import com.nowlsantos.aiconcierge.ui.theme.WhitePen
import com.nowlsantos.common.DocumentNode

@Composable
fun PeninsulaDashboardHub(
    nodes: List<DocumentNode>,
    roomStates: Map<String, String> = emptyMap(),
    actionParams: Map<String, String>? = null,
    onNodeClick: (DocumentNode) -> Unit
) {
    val remotePrivacyActive = roomStates["dnd"] == "ON"
    val remoteMakeupActive = actionParams?.get("subtitle")?.contains("Makeup", ignoreCase = true) == true
    val remoteValetActive = actionParams?.get("subtitle")?.contains("Valet", ignoreCase = true) == true

    var localPrivacyToggle by remember(remotePrivacyActive) { mutableStateOf<Boolean?>(null) }
    var localMakeupToggle by remember(remoteMakeupActive) { mutableStateOf<Boolean?>(null) }
    var localValetToggle by remember(remoteValetActive) { mutableStateOf<Boolean?>(null) }

    val isPrivacyActive = localPrivacyToggle ?: remotePrivacyActive
    val isMakeupActive = localMakeupToggle ?: remoteMakeupActive
    val isValetActive = localValetToggle ?: remoteValetActive

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
        //contentAlignment = Alignment.TopCenter
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.90f)
                .clip(RoundedCornerShape(20.dp))
                .background(GreyPen.copy(alpha = 0.2f))
                .border(
                    width = 1.dp,
                    color = WhitePen.copy(alpha = 0.25f),
                    shape = RoundedCornerShape(20.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Row(modifier = Modifier.padding(18.dp)) {
                // ==================== LEFT COLUMN ====================
                Column(
                    modifier = Modifier
                        .weight(0.8f)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    findOrSyntheticNode(nodes, "Guest Services")?.let { DashboardCardItem(it) { onNodeClick(it) } }
                    findOrSyntheticNode(nodes, "Dining")?.let { DashboardCardItem(it) { onNodeClick(it) } }
                    findOrSyntheticNode(nodes, "Spa and Wellness")?.let { DashboardCardItem(it) { onNodeClick(it) } }
                    findOrSyntheticNode(nodes, "Shopping")?.let { DashboardCardItem(it) { onNodeClick(it) } }
                }

                // ==================== CENTER COLUMN ====================
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .padding(horizontal = 18.dp)
                        .clip(RoundedCornerShape(20.dp)),
                        //.background(PeninsulaGreyCard.copy(alpha = 0.3f)),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    //verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        DateTimeWeather()
                    }

                    Box(modifier = Modifier
                        .padding(vertical = 12.dp)
                        .weight(2.25f)
                        .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        PeninsulaGallery()
                        //AnalogClock(modifier = Modifier.size(300.dp))
                    }
                    // Valet, Makeup Room, Privacy
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(modifier = Modifier.weight(0.8f),
                            contentAlignment = Alignment.Center
                        ) {
                            ValetWidget(
                                isActive = isValetActive,
                                onClick = { localValetToggle = !isValetActive }
                            )
                        }

                        VerticalDivider(modifier = Modifier.fillMaxHeight(0.8f), thickness = 2.dp)

                        Box(modifier = Modifier.weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            MakeupRoomWidget(
                                isActive = isMakeupActive,
                                onClick = { localMakeupToggle = !isMakeupActive }
                            )
                        }

                        VerticalDivider(modifier = Modifier.fillMaxHeight(0.8f), thickness = 2.dp)

                        Box(modifier = Modifier.weight(0.8f),
                            contentAlignment = Alignment.Center
                        ) {
                            PrivacyWidget(
                                isActive = isPrivacyActive,
                                onClick = { localPrivacyToggle = !isPrivacyActive }
                            )
                        }
                    }
                }

                // ==================== RIGHT COLUMN ====================
                Column(
                    modifier = Modifier
                        .weight(0.8f)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    findOrSyntheticNode(nodes, "Media")?.let { DashboardCardItem(it) { onNodeClick(it) } }
                    findOrSyntheticNode(nodes, "Room Controls")?.let { DashboardCardItem(it) { onNodeClick(it) } }
                    findOrSyntheticNode(nodes, "Housekeeping")?.let { DashboardCardItem(it) { onNodeClick(it) } }
                    findOrSyntheticNode(nodes, "Room Connect")?.let { DashboardCardItem(it) { onNodeClick(it) } }
                }
            }
        }
    }
}

@Composable
private fun ColumnScope.DashboardCardItem(
    node: DocumentNode,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .weight(1f)
            .roundedShadow(
                cornerRadius = 16.dp,
                color = BlackPen,
                alpha = 0.7f,
                shadowBlurRadius = 12.dp,
                offsetY = 8.dp
            )
    ) {
        MasterCard(node, onClick)
    }
}

/**
 * Finds a node in the root children or sub-children, or creates a synthetic DocumentNode if needed.
 */
private fun findOrSyntheticNode(
    nodes: List<DocumentNode>,
    targetName: String,
    displayName: String? = null
): DocumentNode? {
    val cleanTarget = targetName.lowercase().trim()
    val match = nodes.firstOrNull { it.name.lowercase().contains(cleanTarget) }
        ?: nodes.flatMap { it.children }.firstOrNull { it.name.lowercase().contains(cleanTarget) }

    if (match != null) {
        return if (displayName != null) match.copy(name = displayName) else match
    }

    // Fallback synthetic node if specific service sub-item isn't at root level
    return DocumentNode(
        name = displayName ?: targetName,
        level = 2,
        uiTemplate = "DETAIL_VIEW",
        content = "Request $targetName for your residence.",
        iconName = targetName.lowercase()
    )
}

/**
 * Creates a drop shadow specifically for RoundedCornerShape elements (Deprecation-free, optimized with remember cache).
 */
@Composable
fun Modifier.roundedShadow(
    cornerRadius: Dp = 16.dp,
    color: Color = BlackPen,
    alpha: Float = 0.2f,
    shadowBlurRadius: Dp = 8.dp,
    offsetY: Dp = 4.dp,
    offsetX: Dp = 0.dp
): Modifier {
    val shadowColor = remember(color, alpha) { color.copy(alpha = alpha).toArgb() }
    val transparentColor = remember(color) { color.copy(alpha = 0f).toArgb() }
    val paint = remember(color) {
        android.graphics.Paint().apply {
            this.color = transparentColor
        }
    }

    return this.drawBehind {
        paint.color = transparentColor
        paint.setShadowLayer(
            shadowBlurRadius.toPx(),
            offsetX.toPx(),
            offsetY.toPx(),
            shadowColor
        )

        drawIntoCanvas { canvas ->
            canvas.drawRoundRect(
                left = 0f,
                top = 0f,
                right = size.width,
                bottom = size.height,
                radiusX = cornerRadius.toPx(),
                radiusY = cornerRadius.toPx(),
                paint = paint.asComposePaint()
            )
        }
    }
}

/**
 * Creates a perfectly circular drop shadow (Optimized with remember cache).
 */
/*
@Composable
private fun Modifier.circleShadow(
    color: Color = BlackPen,
    alpha: Float = 0.2f,
    shadowBlurRadius: Dp = 8.dp,
    offsetY: Dp = 4.dp,
    offsetX: Dp = 0.dp
): Modifier {
    val shadowColor = remember(color, alpha) { color.copy(alpha = alpha).toArgb() }
    val transparentColor = remember(color) { color.copy(alpha = 0f).toArgb() }
    val paint = remember(color) {
        android.graphics.Paint().apply {
            this.color = transparentColor
        }
    }

    return this.drawBehind {
        paint.color = transparentColor
        paint.setShadowLayer(
            shadowBlurRadius.toPx(),
            offsetX.toPx(),
            offsetY.toPx(),
            shadowColor
        )

        drawIntoCanvas { canvas ->
            val radius = minOf(size.width, size.height) / 2f
            canvas.drawCircle(
                center = Offset(size.width / 2f, size.height / 2f),
                radius = radius,
                paint = paint.asComposePaint()
            )
        }
    }
}*/

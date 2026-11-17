package com.nowlsantos.aiconcierge.ui.component.selection

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nowlsantos.aiconcierge.R
import com.nowlsantos.aiconcierge.presentation.ScrollEvent
import com.nowlsantos.aiconcierge.ui.component.widget.SmartText
import com.nowlsantos.aiconcierge.ui.theme.BlackPen
import com.nowlsantos.aiconcierge.ui.theme.PeninsulaGold
import com.nowlsantos.aiconcierge.ui.theme.sfTypography
import com.nowlsantos.aiconcierge.utils.Logger
import com.nowlsantos.aiconcierge.utils.getCardImageResource
import com.nowlsantos.common.DocumentNode

@Composable
fun SelectionView(
    node: DocumentNode,
    currentSelections: Map<String, String>,
    onSelectionChange: (String, String) -> Unit,
    onOrderClick: () -> Unit,
    department: String = "Dining",
    scrollEvent: ScrollEvent? = null,
    onScrollStatusChanged: (canScrollDown: Boolean, canScrollUp: Boolean) -> Unit = { _, _ -> }
) {
    Logger.d("SelectionView rendering node: '${node.name}' with ${node.children.size} groups", "SELECTION_VIEW")

    val scrollState = key(node.name) { rememberScrollState() }

    // Reset scroll position to TOP whenever a new page/node is displayed
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

    val buttonText = when {
        department.contains("Dining") -> stringResource(R.string.place_order)
        department.contains("Media") -> stringResource(R.string.play_now)
        department.contains("Controls") -> stringResource(R.string.apply_settings)
        else -> stringResource(R.string.proceed)
    }

    Box(modifier = Modifier
        .fillMaxSize()
        .padding(vertical = 24.dp)
    ) {
        Row(modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // LEFT SIDE: Content and Configurator
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(horizontal = 12.dp)
                    .verticalScroll(scrollState),
            ) {
                Text(
                    text = node.name,
                    style = sfTypography.headlineMedium,
                    color = PeninsulaGold,
                )

                if (node.content.isNotBlank()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    SmartText(text = node.content)
                }

                val parentPrice = node.price
                if (parentPrice != null && parentPrice > 0) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color.White.copy(alpha = 0.05f),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.3f))
                    ) {
                        Text(
                            text = "€%.2f".format(parentPrice),
                            style = sfTypography.titleMedium,
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Render Child Groups (Level 6 Nodes) dynamically by control type
                node.children.forEach { group ->
                    val gTrim = group.name.trim()
                    val selectionVal = currentSelections[gTrim]
                        ?: currentSelections.entries.find { (k, _) ->
                            k.trim().equals(gTrim, ignoreCase = true)
                        }?.value

                    val controlType = group.control?.lowercase() ?: "radio"
                    when (controlType) {
                        "checkbox" -> CheckboxSelectionGroupCard(
                            group = group,
                            selectionValue = selectionVal,
                            onSelectionChange = { onSelectionChange(group.name, it) }
                        )
                        "counter" -> CounterSelectionGroupCard(
                            group = group,
                            selectionValue = selectionVal,
                            onSelectionChange = { onSelectionChange(group.name, it) }
                        )
                        else -> RadioSelectionGroupCard(
                            group = group,
                            selectedOption = selectionVal,
                            onOptionClick = { onSelectionChange(group.name, it) }
                        )
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                }

                // DYNAMIC BUTTON with Flexible Validation
                if (node.children.isNotEmpty() || node.displayType == "ACTION") {
                    val isAllSelected = node.children.isEmpty() || node.children.all { group ->
                        val sel = currentSelections.entries.find { it.key.equals(group.name, ignoreCase = true) }?.value
                        !sel.isNullOrBlank()
                    }

                    Button(
                        onClick = onOrderClick,
                        enabled = isAllSelected,
                        modifier = Modifier.fillMaxWidth().height(60.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PeninsulaGold,
                            disabledContainerColor = Color.White.copy(alpha = 0.1f)
                        )
                    ) {
                        Text(
                            text = if (isAllSelected) buttonText else stringResource(R.string.complete_selection),
                            style = sfTypography.titleMedium,
                            color = if (isAllSelected) BlackPen else Color.White.copy(alpha = 0.3f)
                        )
                    }
                }
            }

            // RIGHT SIDE: Hero Image / Visual
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(20.dp)),
            ) {
                val imageResId = getCardImageResource(node)
                Image(
                    painter = painterResource(imageResId),
                    contentDescription = node.name,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
        }
    }
}

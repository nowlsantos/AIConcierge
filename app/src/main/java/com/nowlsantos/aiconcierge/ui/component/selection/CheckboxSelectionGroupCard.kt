package com.nowlsantos.aiconcierge.ui.component.selection

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nowlsantos.aiconcierge.ui.theme.BlackPen
import com.nowlsantos.aiconcierge.ui.theme.PeninsulaGold
import com.nowlsantos.aiconcierge.ui.theme.sfTypography
import com.nowlsantos.common.DocumentNode

/**
 * 2. CHECKBOX CONTROL: Multi-choice selection with maxSelections limit
 */
@Composable
fun CheckboxSelectionGroupCard(
    group: DocumentNode,
    selectionValue: String?,
    onSelectionChange: (String) -> Unit
) {
    val checkedList = remember(selectionValue) {
        selectionValue?.split(",")?.map { it.trim() }?.filter { it.isNotBlank() } ?: emptyList()
    }

    val maxSelections = group.maxSelections
    val checkedCount = checkedList.size
    val remaining = if (maxSelections != null) (maxSelections - checkedCount).coerceAtLeast(0) else null
    val isSatisfied = checkedList.isNotEmpty()

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = Color.White.copy(alpha = 0.05f),
        border = BorderStroke(
            width = if (isSatisfied) 1.dp else 2.dp,
            color = if (isSatisfied) Color.White.copy(alpha = 0.1f) else PeninsulaGold.copy(alpha = 0.3f)
        )
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Text(
                text = group.name,
                style = sfTypography.titleLarge,
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.5.sp
            )

            val subtitleText = when {
                maxSelections != null -> "Max $maxSelections selections ($remaining remaining)"
                else -> "Multiple selections allowed"
            }

            Text(
                text = subtitleText,
                style = sfTypography.labelLarge,
                color = Color.White.copy(alpha = 0.5f),
                modifier = Modifier.padding(top = 4.dp)
            )

            Spacer(modifier = Modifier.height(20.dp))

            group.children.forEach { option ->
                val optTrim = option.name.trim()
                val isChecked = checkedList.any { it.equals(optTrim, ignoreCase = true) }
                val canToggleOn = maxSelections == null || checkedCount < maxSelections

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            val newSelections = if (isChecked) {
                                checkedList.filterNot { it.equals(optTrim, ignoreCase = true) }
                            } else {
                                if (canToggleOn) {
                                    checkedList + optTrim
                                } else {
                                    checkedList
                                }
                            }
                            onSelectionChange(newSelections.joinToString(", "))
                        }
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .border(
                                width = 2.dp,
                                color = if (isChecked) PeninsulaGold else Color.White.copy(alpha = 0.3f),
                                shape = RoundedCornerShape(4.dp)
                            )
                            .background(
                                color = if (isChecked) PeninsulaGold else Color.Transparent,
                                shape = RoundedCornerShape(4.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isChecked) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = BlackPen,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = option.name,
                            style = sfTypography.titleMedium,
                            color = if (isChecked) Color.White else Color.White.copy(alpha = 0.7f),
                            fontWeight = if (isChecked) FontWeight.Bold else FontWeight.Normal
                        )
                        if (option.content.isNotEmpty()) {
                            Text(
                                text = option.content,
                                style = sfTypography.labelLarge,
                                color = Color.White.copy(alpha = 0.4f),
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }

                    val optionPrice = option.price
                    if (optionPrice != null && optionPrice > 0) {
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "+€%.2f".format(optionPrice),
                            style = sfTypography.titleMedium,
                            color = PeninsulaGold,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

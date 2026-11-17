package com.nowlsantos.aiconcierge.ui.component.selection

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nowlsantos.aiconcierge.ui.theme.PeninsulaGold
import com.nowlsantos.aiconcierge.ui.theme.sfTypography
import com.nowlsantos.common.DocumentNode

/**
 * 3. COUNTER CONTROL: Quantity increment/decrement buttons per item
 */
@Composable
fun CounterSelectionGroupCard(
    group: DocumentNode,
    selectionValue: String?,
    onSelectionChange: (String) -> Unit
) {
    val countMap = remember(selectionValue) {
        val map = mutableMapOf<String, Int>()
        if (!selectionValue.isNullOrBlank()) {
            selectionValue.split(",").forEach { part ->
                val trimmed = part.trim()
                if (trimmed.isNotBlank()) {
                    if (trimmed.contains(":")) {
                        val kv = trimmed.split(":")
                        if (kv.size >= 2) {
                            val k = kv[0].trim().lowercase()
                            val v = kv[1].trim().toIntOrNull() ?: 1
                            if (v > 0) map[k] = v
                        }
                    } else {
                        val digits = Regex("\\d+").find(trimmed)?.value?.toIntOrNull() ?: 1
                        val nameOnly = trimmed.replace(Regex("\\d+|x|X"), "").trim().lowercase()
                        if (nameOnly.isNotBlank()) {
                            map[nameOnly] = digits
                        } else {
                            map[trimmed.lowercase()] = digits
                        }
                    }
                }
            }
        }
        map
    }

    val maxLimit = group.maxSelections ?: 10
    val isSatisfied = countMap.values.sum() > 0

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

            if (group.content.isNotEmpty()) {
                Text(
                    text = group.content,
                    style = sfTypography.labelLarge,
                    color = Color.White.copy(alpha = 0.5f),
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            group.children.forEach { option ->
                val optNameLower = option.name.lowercase().trim()
                val currentQty = countMap[optNameLower] ?: 0

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = Color.White.copy(alpha = 0.05f)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = option.name,
                                style = sfTypography.titleMedium,
                                color = if (currentQty > 0) Color.White else Color.White.copy(alpha = 0.7f),
                                fontWeight = if (currentQty > 0) FontWeight.Bold else FontWeight.Normal
                            )
                            if (option.content.isNotEmpty()) {
                                Text(
                                    text = option.content,
                                    style = sfTypography.labelLarge,
                                    color = Color.White.copy(alpha = 0.4f),
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            }
                            val optionPrice = option.price
                            if (optionPrice != null && optionPrice > 0) {
                                Text(
                                    text = "+€%.2f / portion".format(optionPrice),
                                    style = sfTypography.labelMedium,
                                    color = PeninsulaGold,
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            }
                        }

                        // COUNTER BUTTONS (- 0 +)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Minus Button
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(if (currentQty > 0) Color.White.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.08f))
                                    .clickable(enabled = currentQty > 0) {
                                        val newQty = currentQty - 1
                                        val newMap = countMap.toMutableMap()
                                        if (newQty <= 0) newMap.remove(optNameLower) else newMap[optNameLower] = newQty
                                        val formatted = newMap.entries
                                            .filter { it.value > 0 }
                                            .joinToString(", ") { (k, v) ->
                                                val childName = group.children.find { it.name.lowercase().trim() == k }?.name ?: k
                                                "$childName: $v"
                                            }
                                        onSelectionChange(formatted)
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "−",
                                    style = sfTypography.titleMedium,
                                    color = if (currentQty > 0) Color.White else Color.White.copy(alpha = 0.3f),
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            // Qty Text
                            Text(
                                text = "$currentQty",
                                style = sfTypography.titleMedium,
                                color = if (currentQty > 0) PeninsulaGold else Color.White.copy(alpha = 0.6f),
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp)
                            )

                            // Plus Button
                            val canIncrement = currentQty < maxLimit
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(if (canIncrement) Color.White.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.08f))
                                    .clickable(enabled = canIncrement) {
                                        val newQty = currentQty + 1
                                        val newMap = countMap.toMutableMap()
                                        newMap[optNameLower] = newQty
                                        val formatted = newMap.entries
                                            .filter { it.value > 0 }
                                            .joinToString(", ") { (k, v) ->
                                                val childName = group.children.find { it.name.lowercase().trim() == k }?.name ?: k
                                                "$childName: $v"
                                            }
                                        onSelectionChange(formatted)
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "+",
                                    style = sfTypography.titleMedium,
                                    color = if (canIncrement) Color.White else Color.White.copy(alpha = 0.3f),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

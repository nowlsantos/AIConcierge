package com.nowlsantos.aiconcierge.ui.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nowlsantos.aiconcierge.ui.theme.BlackPen
import com.nowlsantos.aiconcierge.ui.theme.GoldPen
import com.nowlsantos.aiconcierge.ui.theme.sfTypography
import com.nowlsantos.aiconcierge.utils.Logger
import com.nowlsantos.common.DocumentNode

/**
 * ORDER SUMMARY VIEW
 * Renders the itemized order summary, total calculation, and kitchen dispatch confirmation.
 */
@Composable
fun OrderSummaryView(
    node: DocumentNode,
    selections: Map<String, String>,
    isConfirmed: Boolean = false,
    onConfirm: () -> Unit,
    onHomeClick: () -> Unit = {}
) {
    Logger.d("OrderSummaryView rendering node '${node.name}' (isConfirmed=$isConfirmed)", "ORDER_SUMMARY_VIEW")

    val selectedItems = mutableListOf<Pair<String, Double>>()
    val baseSetPrice = node.price ?: 0.0
    var total = baseSetPrice

    node.children.forEach { group ->
        val groupName = group.name
        val selectionEntry = selections.entries.find { (key, _) ->
            val k = key.lowercase()
            val gName = groupName.lowercase()
            gName == k || gName.contains(k) || k.contains(gName) ||
            group.children.any { c -> c.name.lowercase().contains(k) || k.contains(c.name.lowercase()) }
        } ?: selections.entries.find { (_, value) ->
            group.children.any { c ->
                c.name.lowercase().contains(value.lowercase()) || value.lowercase().contains(c.name.lowercase())
            }
        }

        val selectionValue = selectionEntry?.value
        if (!selectionValue.isNullOrBlank()) {
            val controlType = group.control?.lowercase() ?: "radio"
            when (controlType) {
                "checkbox" -> {
                    val selectedNames = selectionValue.split(",").map { it.trim() }.filter { it.isNotBlank() }
                    selectedNames.forEach { optName ->
                        val resolvedOptName = resolveBestOption(group, optName)
                        val option = group.children.find { it.name.equals(resolvedOptName, ignoreCase = true) }
                        val displayName = option?.name ?: resolvedOptName
                        val price = option?.price ?: 0.0
                        if (selectedItems.none { it.first.equals(displayName, ignoreCase = true) }) {
                            selectedItems.add(displayName to price)
                            total += price
                        }
                    }
                }
                "counter" -> {
                    selectionValue.split(",").map { it.trim() }.filter { it.isNotBlank() }.forEach { part ->
                        val kv = part.split(":")
                        val optName = if (kv.size == 2) kv[0].trim() else part.trim()
                        val qty = if (kv.size == 2) kv[1].trim().toIntOrNull() ?: 1 else 1

                        val resolvedOptName = resolveBestOption(group, optName)
                        val option = group.children.find { it.name.equals(resolvedOptName, ignoreCase = true) }
                        val unitPrice = option?.price ?: 0.0
                        val totalPrice = unitPrice * qty
                        val resolvedName = option?.name ?: resolvedOptName
                        val displayName = if (qty > 1) "$resolvedName (x$qty)" else resolvedName
                        if (selectedItems.none { it.first.equals(displayName, ignoreCase = true) }) {
                            selectedItems.add(displayName to totalPrice)
                            total += totalPrice
                        }
                    }
                }
                else -> {
                    val resolvedOptName = resolveBestOption(group, selectionValue)
                    val option = group.children.find { it.name.equals(resolvedOptName, ignoreCase = true) }
                    val displayName = option?.name ?: resolvedOptName
                    val price = option?.price ?: 0.0
                    if (selectedItems.none { it.first.equals(displayName, ignoreCase = true) }) {
                        selectedItems.add(displayName to price)
                        total += price
                    }
                }
            }
        }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
            //.background(GreyPen.copy(alpha = 0.2f)),
        contentAlignment = Alignment.TopCenter
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.5f)
                .padding(24.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.White.copy(alpha = 0.05f)
            ),
            border = BorderStroke(1.dp, GoldPen.copy(alpha = 0.25f))
        ) {
            if (isConfirmed) {
                // ORDER DISPATCHED CONFIRMATION CARD
                Column(
                    modifier = Modifier
                        .padding(40.dp)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .background(GoldPen, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Confirmed",
                            tint = BlackPen,
                            modifier = Modifier.size(40.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        text = "ORDER DISPATCHED TO KITCHEN",
                        style = sfTypography.headlineSmall,
                        color = GoldPen,
                        letterSpacing = 2.sp,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Your order has been transmitted directly to our executive chef. Estimated delivery time to your residence is 20 minutes.",
                        style = sfTypography.bodyLarge,
                        color = Color.White.copy(alpha = 0.85f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    HorizontalDivider(color = Color.White.copy(alpha = 0.15f))

                    Spacer(modifier = Modifier.height(24.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "ORDER TOTAL",
                            style = sfTypography.titleMedium,
                            color = Color.White.copy(alpha = 0.6f)
                        )
                        Text(
                            text = "€${"%.2f".format(total)}",
                            style = sfTypography.titleLarge,
                            color = GoldPen,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(36.dp))

                    Button(
                        onClick = onHomeClick,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = GoldPen)
                    ) {
                        Text(
                            text = "RETURN TO HOME",
                            style = sfTypography.titleMedium,
                            color = BlackPen,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            } else {
                // ORDER RECAP / REVIEW CARD
                val scrollState = androidx.compose.runtime.key(node.name) { rememberScrollState() }
                androidx.compose.runtime.LaunchedEffect(node.name) {
                    scrollState.scrollTo(0)
                }

                Column(
                    modifier = Modifier
                        .padding(40.dp)
                        .verticalScroll(scrollState),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "ORDER SUMMARY",
                        style = sfTypography.headlineSmall,
                        color = GoldPen,
                        letterSpacing = 4.sp,
                        textAlign = TextAlign.Center
                    )
                    
                    Text(
                        text = node.name.uppercase(),
                        modifier = Modifier.padding(top = 8.dp),
                        style = sfTypography.labelLarge,
                        color = Color.White.copy(alpha = 0.5f),
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    // Base Set Row (only shown if set package price exists)
                    if (baseSetPrice > 0) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "${node.name} Package",
                                style = sfTypography.titleMedium,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "€${"%.2f".format(baseSetPrice)}",
                                style = sfTypography.titleMedium,
                                color = GoldPen,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        HorizontalDivider(color = Color.White.copy(alpha = 0.2f))
                    }

                    // Itemized Selections
                    selectedItems.forEach { (name, extraPrice) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 10.dp, horizontal = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "• $name",
                                style = sfTypography.bodyLarge,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                            Text(
                                text = if (extraPrice > 0) "+€${"%.2f".format(extraPrice)}" else "INCLUDED",
                                style = sfTypography.bodyMedium,
                                color = if (extraPrice > 0) GoldPen else Color.White.copy(alpha = 0.5f),
                                fontWeight = FontWeight.Normal
                            )
                        }
                        HorizontalDivider(color = Color.White.copy(alpha = 0.05f))
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "TOTAL",
                            style = sfTypography.titleMedium,
                            color = GoldPen,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "€${"%.2f".format(total)}",
                            style = sfTypography.headlineMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(48.dp))

                    Button(
                        onClick = onConfirm,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(60.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = GoldPen)
                    ) {
                        Text(
                            text = "DISPATCH TO KITCHEN",
                            style = sfTypography.titleMedium,
                            color = BlackPen,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

private fun resolveBestOption(group: DocumentNode, rawOption: String): String {
    val q = rawOption.lowercase().trim()
    group.children.find { it.name.lowercase() == q }?.let { return it.name }
    group.children.find { it.name.lowercase().contains(q) || q.contains(it.name.lowercase()) }?.let { return it.name }
    val qTokens = q.split(Regex("[^a-z0-9]")).filter { it.isNotBlank() }
    var bestMatch: DocumentNode? = null
    var maxOverlap = 0
    group.children.forEach { child ->
        val cTokens = child.name.lowercase().split(Regex("[^a-z0-9]")).filter { it.isNotBlank() }
        val overlap = qTokens.count { qt -> cTokens.any { ct -> ct == qt || ct.contains(qt) || qt.contains(ct) } }
        if (overlap > maxOverlap) {
            maxOverlap = overlap
            bestMatch = child
        }
    }
    return bestMatch?.name ?: rawOption
}

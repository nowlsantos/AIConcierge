package com.nowlsantos.aiconcierge.utils

import com.nowlsantos.aiconcierge.presentation.PlacedOrder
import com.nowlsantos.common.DocumentNode

fun buildPlacedOrder(node: DocumentNode?, selections: Map<String, String>): PlacedOrder {
    val basePrice = node?.price ?: 0.0
    var total = basePrice
    val items = mutableListOf<Pair<String, Double>>()

    node?.children?.forEach { group ->
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
                        if (items.none { it.first.equals(displayName, ignoreCase = true) }) {
                            items.add(displayName to price)
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
                        if (items.none { it.first.equals(displayName, ignoreCase = true) }) {
                            items.add(displayName to totalPrice)
                            total += totalPrice
                        }
                    }
                }

                else -> {
                    val resolvedOptName = resolveBestOption(group, selectionValue)
                    val option = group.children.find { it.name.equals(resolvedOptName, ignoreCase = true) }
                    val displayName = option?.name ?: resolvedOptName
                    val price = option?.price ?: 0.0
                    if (items.none { it.first.equals(displayName, ignoreCase = true) }) {
                        items.add(displayName to price)
                        total += price
                    }
                }
            }
        }
    }

    val orderName = node?.name ?: "In-Room Dining Order"
    return PlacedOrder(orderName = orderName, items = items, total = total)
}

fun resolveBestOption(group: DocumentNode, rawOption: String): String {
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

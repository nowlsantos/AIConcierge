package com.nowlsantos.aiconcierge.utils

import android.annotation.SuppressLint
import android.content.Context
import androidx.annotation.DrawableRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.nowlsantos.aiconcierge.R
import com.nowlsantos.common.DocumentNode
import android.util.LruCache

private val drawableCache = LruCache<String, Int>(512)

/**
 * PURE DYNAMIC RESOURCE RESOLVER
 * Automatically resolves any PNG in res/drawable/ matching the candidate string name.
 * Converts "Dining Icon" -> "dining_icon", "In-Room Dining Ordering" -> "in_room_dining_ordering", etc.
 */
@SuppressLint("DiscouragedApi", "LocalContextResourcesRead")
@DrawableRes
fun resolveDrawableByName(context: Context, name: String?): Int {
    if (name.isNullOrBlank()) return R.drawable.services_icon

    val cacheKey = "resolve-$name"
    drawableCache.get(cacheKey)?.let { return it }

    // 1. Convert string to snake_case: "Room Controls" -> "room_controls"
    val cleanName = name.lowercase().trim().replace(" ", "_").replace("-", "_")
    var resId = context.resources.getIdentifier(cleanName, "drawable", context.packageName)
    if (resId != 0) {
        drawableCache.put(cacheKey, resId)
        return resId
    }

    // 2. Try with "_icon" suffix: "dining" -> "dining_icon"
    if (!cleanName.endsWith("_icon")) {
        resId = context.resources.getIdentifier("${cleanName}_icon", "drawable", context.packageName)
        if (resId != 0) {
            drawableCache.put(cacheKey, resId)
            return resId
        }
    }

    // Default fallback image if no PNG file exists in res/drawable/
    drawableCache.put(cacheKey, R.drawable.services_icon)
    return R.drawable.services_icon
}

/**
 * PURE DYNAMIC CARD ASSET RESOLVER (COMPOSABLE)
 * Priority resolution: node.imageAsset -> node.iconName -> node.name
 */
@Composable
@DrawableRes
fun getCardImageResource(node: DocumentNode): Int {
    val context = LocalContext.current

    // Candidates in priority order: imageAsset, iconName, name
    val candidates = listOfNotNull(node.iconName, node.imageAsset, node.name)

    for (candidate in candidates) {
        val resId = resolveDrawableByName(context, candidate)
        if (resId != R.drawable.services_icon) {
            return resId
        }
    }

    return R.drawable.services_icon
}

/**
 * HIGH-SPEED DYNAMIC MEDIA DRAWABLE RESOLVER (COMPOSABLE)
 * Uses high-performance ConcurrentHashMap caching to avoid Android getIdentifier reflection overhead during fast scrolling.
 */
@SuppressLint("DiscouragedApi", "LocalContextResourcesRead")
@Composable
@DrawableRes
fun getMediaDrawable(imageKey: String?, categoryName: String = ""): Int {
    if (imageKey.isNullOrBlank()) return if (categoryName.equals("Radio", ignoreCase = true)) R.drawable.radio_default else R.drawable.media_icon

    val cacheKey = "media-$imageKey-$categoryName"
    drawableCache.get(cacheKey)?.let { return it }

    val context = LocalContext.current

    // 1. Try dynamic Android lookup in res/drawable/ for imageKey (e.g. "bbc_news", "cnn")
    val cleanKey = imageKey.lowercase().trim().replace(" ", "_").replace("-", "_")
    var resId = context.resources.getIdentifier(cleanKey, "drawable", context.packageName)
    if (resId == 0 && !cleanKey.endsWith("_icon")) {
        resId = context.resources.getIdentifier("${cleanKey}_icon", "drawable", context.packageName)
    }

    if (resId == 0) {
        resId = when {
            categoryName.equals("Radio", ignoreCase = true) || cleanKey.contains("radio") -> R.drawable.media_radio_icon
            categoryName.equals("Movies", ignoreCase = true) || cleanKey.contains("movie") -> R.drawable.media_movie_icon
            categoryName.equals("TV", ignoreCase = true) || cleanKey.contains("tv") -> R.drawable.media_tv_icon
            else -> R.drawable.media_icon
        }
    }

    drawableCache.put(cacheKey, resId)
    return resId
}

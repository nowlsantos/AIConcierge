package com.nowlsantos.aiconcierge.utils

import android.annotation.SuppressLint
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.nowlsantos.aiconcierge.R

/**
 * PRODUCTION IMAGE & ICON LOADER UTILITY
 * Supports String asset names ("dining_hero"), Resource IDs (R.drawable.*), and URLs.
 */

// 1. Dynamic Asset Name Loader (From DocumentNode.imageAsset)
@SuppressLint("DiscouragedApi", "LocalContextResourcesRead")
@Composable
fun LoadImage(
    modifier: Modifier = Modifier,
    assetName: String?,
    description: String = "",
    contentScale: ContentScale = ContentScale.Crop
) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val resourceId = remember(assetName, configuration) {
        if (!assetName.isNullOrBlank()) {
            val id = context.resources.getIdentifier(assetName, "drawable", context.packageName)
            if (id != 0) id else R.drawable.ic_launcher_foreground
        } else {
            R.drawable.ic_launcher_foreground
        }
    }

    LoadImage(
        imageID = resourceId,
        description = description,
        modifier = modifier,
        contentScale = contentScale
    )
}

// 2. Resource ID / Coil Loader
@Composable
fun LoadImage(
    modifier: Modifier = Modifier,
    imageID: Int?,
    description: String = "",
    contentScale: ContentScale = ContentScale.Crop
) {
    AsyncImage(
        model = ImageRequest.Builder(LocalContext.current)
            .data(imageID)
            .crossfade(true)
            .error(R.drawable.ic_launcher_foreground)
            .placeholder(R.drawable.ic_launcher_foreground)
            .build(),
        contentScale = contentScale,
        contentDescription = description,
        modifier = modifier.fillMaxSize()
    )
}

// 3. Icon Loader with Optional Tinting (From DocumentNode.iconName)
@SuppressLint("DiscouragedApi", "LocalContextResourcesRead")
@Composable
fun LoadIconImage(
    modifier: Modifier = Modifier,
    iconName: String?,
    description: String = "",
    tint: Color? = null
) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val resourceId = remember(iconName, configuration) {
        if (!iconName.isNullOrBlank()) {
            val id = context.resources.getIdentifier(iconName, "drawable", context.packageName)
            if (id != 0) id else R.drawable.ic_launcher_foreground
        } else {
            R.drawable.ic_launcher_foreground
        }
    }

    Image(
        painter = painterResource(resourceId),
        contentDescription = description,
        modifier = modifier,
        colorFilter = tint?.let { ColorFilter.tint(it) }
    )
}

// 4. Overload for Int Resource ID Icon
@Composable
fun LoadIconImage(
    modifier: Modifier = Modifier,
    imageID: Int,
    description: String = "",
    tint: Color? = null
) {
    Image(
        painter = painterResource(imageID),
        contentDescription = description,
        modifier = modifier,
        colorFilter = tint?.let { ColorFilter.tint(it) }
    )
}

package com.nowlsantos.aiconcierge.ui.component.widget

import androidx.compose.animation.*
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nowlsantos.aiconcierge.ui.theme.DarkPen
import com.nowlsantos.aiconcierge.ui.theme.GoldPen
import com.nowlsantos.aiconcierge.ui.theme.WhitePen
import com.nowlsantos.aiconcierge.ui.theme.sfTypography
import com.nowlsantos.aiconcierge.utils.LoadImage
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.seconds

data class GalleryItem(
    val assetName: String,
    val title: String
)

/**
 * PENINSULA LUXURY PICTURE GALLERY
 * Animated, auto-advancing 5.0s slideshow with smooth 1,000ms crossfade transitions
 * and interactive gold page indicator dots for direct photo jumping.
 */
@Composable
fun PeninsulaGallery(
    modifier: Modifier = Modifier,
    autoAdvanceIntervalSec: Int = 5
) {
    val items = remember {
        listOf(
            GalleryItem("dine_the_lobby", "Restaurants and Bars"),
            GalleryItem("gs_peninsula_academy", "The Peninsula Academy"),
            GalleryItem("dine_boutique_cafe", "Peninsula Boutique & Café"),
            GalleryItem("spa_swimming_pool", "The Swimming Pool"),
            GalleryItem("spa_about_us", "Spa and Wellness"),
            GalleryItem("shop_asprey", "Shops at the Pen"),
            GalleryItem("gs_cinema", "The Cinema"),
            GalleryItem("spa_fitness_centre", "The Fitness Centre")
        )
    }

    var currentIndex by remember { mutableIntStateOf(0) }

    // Auto-slideshow timer (Advances photo every 5 seconds)
    LaunchedEffect(currentIndex, autoAdvanceIntervalSec) {
        delay(autoAdvanceIntervalSec.seconds)
        currentIndex = (currentIndex + 1) % items.size
    }

    val currentItem = items[currentIndex]

    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(vertical = 12.dp)
            //.padding(horizontal = 24.dp)
            .clip(RoundedCornerShape(16.dp)),
        contentAlignment = Alignment.Center
    ) {
        // 1. Crossfade Image Transition
        AnimatedContent(
            targetState = currentItem,
            transitionSpec = {
                fadeIn(animationSpec = tween(1500, easing = FastOutSlowInEasing)).togetherWith(
                    fadeOut(animationSpec = tween(1500, easing = FastOutSlowInEasing))
                )
            },
            label = "galleryTransition",
            modifier = Modifier.fillMaxSize()
        ) { item ->
            Box(modifier = Modifier.fillMaxSize()) {
                LoadImage(
                    assetName = item.assetName,
                    description = item.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )

                // Title Overlay Banner
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .align(Alignment.Center)
                        .background(DarkPen.copy(alpha = 0.8f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = item.title,
                        style = sfTypography.titleMedium,
                        color = WhitePen,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        // 2. Interactive Gold Dot Page Indicators (Bottom Overlay)
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 12.dp)
                .padding(horizontal = 12.dp, vertical = 6.dp)
                .background(DarkPen.copy(alpha = 0.5f), shape = RoundedCornerShape(12.dp)),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.indices.forEach { index ->
                val isSelected = index == currentIndex
                Box(
                    modifier = Modifier
                        .size(if (isSelected) 10.dp else 7.dp)
                        .clip(CircleShape)
                        .background(if (isSelected) GoldPen else WhitePen.copy(alpha = 0.4f))
                        //.clickable { currentIndex = index }
                )
            }
        }
    }
}

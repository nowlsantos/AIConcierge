package com.nowlsantos.aiconcierge.ui.component.media

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nowlsantos.aiconcierge.presentation.MediaCategory
import com.nowlsantos.aiconcierge.presentation.ScrollEvent
import com.nowlsantos.aiconcierge.ui.theme.BlackPen
import com.nowlsantos.aiconcierge.ui.theme.GoldPen
import com.nowlsantos.aiconcierge.ui.theme.PeninsulaCharcoal
import com.nowlsantos.aiconcierge.ui.theme.WhitePen
import com.nowlsantos.aiconcierge.ui.theme.sfTypography
import com.nowlsantos.aiconcierge.utils.Logger
import com.nowlsantos.common.DocumentNode

/**
 * MEDIA HUB VIEW
 * Custom split-pane dashboard for TV, Radio, and Movies with category-specific Remote Controllers.
 * Left Pane: Remote Controller Panel | Right Pane: 60fps Scrolling Channel/Station Grid.
 * STRICT MANDATES: 
 * 1. Media filtering for TV, Radio, and Movies checks item.content ONLY.
 * 2. Genre filter actions highlight the Header Genre Chip ONLY (channel cards in grid are not highlighted).
 * 3. Zero reference to item.keywords or item.name during media filtering.
 */
@Composable
fun MediaHubView(
    node: DocumentNode,
    mediaState: Map<String, String> = emptyMap(),
    scrollEvent: ScrollEvent? = null,
    onScrollStatusChanged: (canScrollDown: Boolean, canScrollUp: Boolean) -> Unit = { _, _ -> },
    onMediaControlClick: (category: String, action: String, item: String) -> Unit = { _, _, _ -> }
) {
    Logger.d("MediaHubView rendering section '${node.name}' with ${node.children.size} items", "MEDIA_HUB")

    val mediaCategory = remember(node.name) { MediaCategory.from(node.name) }
    val sectionTitle = mediaCategory.sectionTitle
    val filterCategories = mediaCategory.filterCategories

    var userSelectedFilter by remember(node.name) { mutableStateOf<String?>(null) }
    var userSelectedItem by remember(node.name) { mutableStateOf<DocumentNode?>(null) }

    // Clear local user touch overrides whenever a voice command updates mediaState
    LaunchedEffect(mediaState) {
        userSelectedFilter = null
        userSelectedItem = null
    }

    // Synchronously derive active genre filter from mediaState on frame 1
    val activeMediaVal = mediaState[node.name.uppercase().trim()]

    val rawValue = if (!activeMediaVal.isNullOrBlank()) {
        activeMediaVal.replace("Play:", "", ignoreCase = true)
            .replace("Play", "", ignoreCase = true)
            .replace("Filter:", "", ignoreCase = true)
            .replace("Filter", "", ignoreCase = true).trim()
    } else ""

    val isFilterChipMatch = filterCategories.any { chip ->
        !chip.equals("All", ignoreCase = true) && chip.equals(rawValue, ignoreCase = true)
    }

    val isFilterAction = (activeMediaVal?.startsWith("Filter", ignoreCase = true) == true) || isFilterChipMatch

    val activeGenreFromState: String? = if (!activeMediaVal.isNullOrBlank() && rawValue.isNotBlank() && isFilterAction) {
        filterCategories.find { chip -> chip.equals(rawValue, ignoreCase = true) } ?: rawValue
    } else null

    val selectedFilter = userSelectedFilter ?: activeGenreFromState ?: "All"

    // Synchronously derive selected channel/station/movie from mediaState on frame 1 ONLY when NOT a filter action
    val activeSelectedItem: DocumentNode? = if (!activeMediaVal.isNullOrBlank() && !isFilterAction) {
        val cleanActive = rawValue.lowercase()
        node.children.find { child ->
            val childName = child.name.lowercase()
            childName == cleanActive || (cleanActive.length >= 3 && childName.contains(cleanActive))
        }
    } else null

    // STRICT MANDATE: On filter action, highlight the Header Genre Chip ONLY!
    val activeItem = if (isFilterAction) null else (userSelectedItem ?: activeSelectedItem)

    val lazyGridState = key(node.name, selectedFilter) { rememberLazyGridState() }

    // Reset scroll position to TOP whenever a new media category page or genre filter is selected
    LaunchedEffect(node.name, selectedFilter) {
        lazyGridState.scrollToItem(0)
    }

    // STRICT MANDATE: Media filtering for TV, Radio, and Movies checks item.content ONLY!
    val filteredNodes = remember(selectedFilter, node.children) {
        if (selectedFilter.equals("All", ignoreCase = true) || (filterCategories.isEmpty() && selectedFilter.isBlank())) {
            node.children
        } else {
            val query = selectedFilter.lowercase().replace(Regex("[^a-z0-9&]"), "").trim()
            node.children.filter { item ->
                val cleanedContent = item.content.lowercase().replace(Regex("[^a-z0-9&,\\s]"), "")
                val contentTokens = cleanedContent.split(",", " ").map { it.trim() }.filter { it.isNotBlank() }

                contentTokens.any { token -> token == query || token.contains(query) || query.contains(token) }
            }
        }
    }

    // Real-time Scroll Ability Detection
    val canScrollDown by remember { derivedStateOf { lazyGridState.canScrollForward } }
    val canScrollUp by remember { derivedStateOf { lazyGridState.canScrollBackward } }

    LaunchedEffect(canScrollDown, canScrollUp) {
        onScrollStatusChanged(canScrollDown, canScrollUp)
    }

    val density = LocalDensity.current
    val fourRowsPx = with(density) { 576.dp.toPx() }

    // Voice-Controlled Smooth Silky Scroll Animations (800ms FastOutSlowInEasing Glide)
    LaunchedEffect(scrollEvent) {
        scrollEvent?.let { event ->
            when (event.direction) {
                "DOWN" -> {
                    lazyGridState.animateScrollBy(
                        value = fourRowsPx,
                        animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing)
                    )
                }
                "UP" -> {
                    lazyGridState.animateScrollBy(
                        value = -fourRowsPx,
                        animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing)
                    )
                }
                "TOP" -> lazyGridState.animateScrollToItem(0)
                "BOTTOM" -> lazyGridState.animateScrollToItem((filteredNodes.size - 1).coerceAtLeast(0))
            }
        }
    }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // HEADER & GENRE FILTER CHIPS
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = sectionTitle,
                    style = sfTypography.headlineSmall,
                    color = WhitePen,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )

                if (filterCategories.isNotEmpty()) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        filterCategories.forEach { filter ->
                            val isSelected = selectedFilter.equals(filter, ignoreCase = true)
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    userSelectedFilter = filter
                                    userSelectedItem = null // Clear card selection on chip tap
                                    onMediaControlClick(node.name.uppercase().trim(), "Filter", filter)
                                },
                                label = {
                                    Text(
                                        text = filter,
                                        style = sfTypography.titleSmall,
                                        color = if (isSelected) BlackPen else WhitePen.copy(alpha = 0.85f),
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    containerColor = PeninsulaCharcoal.copy(alpha = 0.6f),
                                    selectedContainerColor = GoldPen,
                                    labelColor = WhitePen,
                                    selectedLabelColor = BlackPen
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    enabled = true,
                                    selected = isSelected,
                                    borderColor = GoldPen.copy(alpha = 0.4f),
                                    selectedBorderColor = GoldPen
                                ),
                                shape = RoundedCornerShape(12.dp)
                            )
                        }
                    }
                }
            }

            // SPLIT PANE DASHBOARD (Left: Remote Controller | Right: Channel/Station Grid)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // LEFT PANE: REMOTE CONTROLLER PANEL (35% Width)
                Box(
                    modifier = Modifier
                        .weight(0.5f)
                        .fillMaxHeight()
                ) {
                    when (node.name.uppercase().trim()) {
                        "TV" -> TvRemoteController(
                            activeItem = activeItem,
                            nodeName = node.name,
                            onMediaControlClick = onMediaControlClick
                        )
                        "RADIO" -> RadioRemoteController(
                            activeItem = activeItem,
                            nodeName = node.name,
                            onMediaControlClick = onMediaControlClick
                        )
                        "MOVIES" -> MovieRemoteController(
                            activeItem = activeItem,
                            nodeName = node.name,
                            onMediaControlClick = onMediaControlClick
                        )
                        else -> TvRemoteController(
                            activeItem = activeItem,
                            nodeName = node.name,
                            onMediaControlClick = onMediaControlClick
                        )
                    }
                }

                // RIGHT PANE: SCROLLING CHANNEL/STATION/MOVIE GRID (65% Width)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                ) {
                    if (filteredNodes.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No items available in this category.",
                                style = sfTypography.bodyLarge,
                                color = WhitePen.copy(alpha = 0.6f),
                                textAlign = TextAlign.Center
                            )
                        }
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(4),
                            modifier = Modifier.fillMaxSize(),
                            state = lazyGridState,
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp),
                            contentPadding = PaddingValues(bottom = 24.dp)
                        ) {
                            items(filteredNodes, key = { it.name }) { item ->
                                val isSelected = activeItem?.name.equals(item.name, ignoreCase = true)
                                MediaChannelCard(
                                    node = item,
                                    isSelected = isSelected,
                                    onClick = {
                                        userSelectedItem = item
                                        val cat = node.name.uppercase().trim()
                                        onMediaControlClick(cat, "Play", item.name)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

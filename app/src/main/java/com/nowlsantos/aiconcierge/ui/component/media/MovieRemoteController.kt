package com.nowlsantos.aiconcierge.ui.component.media

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nowlsantos.aiconcierge.ui.theme.Bedtime
import com.nowlsantos.aiconcierge.ui.theme.BlackPen
import com.nowlsantos.aiconcierge.ui.theme.PeninsulaGold
import com.nowlsantos.aiconcierge.ui.theme.WhitePen
import com.nowlsantos.aiconcierge.ui.theme.sfTypography
import com.nowlsantos.aiconcierge.utils.LoadImage
import com.nowlsantos.aiconcierge.utils.getMediaDrawable
import com.nowlsantos.common.DocumentNode

/**
 * MOVIE REMOTE CONTROLLER
 * Tailored for 4K Ultra HD films with video scrubber bar, seek 10s, audio/subtitle selection, and Play on TV.
 */
@Composable
fun MovieRemoteController(
    activeItem: DocumentNode?,
    nodeName: String,
    onMediaControlClick: (category: String, action: String, item: String) -> Unit
) {
    var isPlaying by remember { mutableStateOf(true) }
    var currentProgress by remember { mutableFloatStateOf(0.35f) }

    Card(
        //modifier = Modifier.width(360.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = WhitePen.copy(alpha = 0.05f)),
        border = BorderStroke(1.dp, WhitePen.copy(alpha = 0.1f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            horizontalAlignment = Alignment.Start
        ) {
            // Movie Header Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (activeItem != null) "4K ULTRA HD CINEMA" else "MOVIE CONTROLLER",
                    style = sfTypography.labelMedium,
                    color = WhitePen.copy(alpha = 0.6f),
                    letterSpacing = 1.sp
                )
                if (activeItem != null) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = Bedtime
                    ) {
                        Text(
                            text = "HDR",
                            style = sfTypography.labelSmall,
                            color = WhitePen,
                            fontWeight = FontWeight.Bold,
                            fontSize = 9.sp,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Text(
                text = activeItem?.name ?: "Select a Movie",
                style = sfTypography.titleMedium,
                color = PeninsulaGold,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
            )

            // HERO LOGO / MOVIE POSTER ART CARD
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(WhitePen.copy(alpha = 0.08f)),
                contentAlignment = Alignment.Center
            ) {
                val heroImageRes = if (activeItem != null) {
                    getMediaDrawable(activeItem.imageAsset ?: activeItem.iconName ?: activeItem.name, nodeName)
                } else {
                    getMediaDrawable(null, nodeName)
                }

                LoadImage(
                    modifier = Modifier.fillMaxSize().padding(10.dp),
                    imageID = heroImageRes,
                    description = activeItem?.name ?: nodeName,
                    contentScale = ContentScale.Fit
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // VIDEO SCRUBBER BAR (Current Progress / Total Duration)
            if (activeItem != null) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Slider(
                        value = currentProgress,
                        onValueChange = { currentProgress = it },
                        valueRange = 0f..1f,
                        colors = SliderDefaults.colors(
                            thumbColor = PeninsulaGold,
                            activeTrackColor = PeninsulaGold,
                            inactiveTrackColor = WhitePen.copy(alpha = 0.2f)
                        ),
                        modifier = Modifier.height(24.dp)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "00:38:15",
                            style = sfTypography.labelSmall,
                            color = WhitePen.copy(alpha = 0.6f),
                            fontSize = 10.sp
                        )
                        Text(
                            text = "01:43:00",
                            style = sfTypography.labelSmall,
                            color = WhitePen.copy(alpha = 0.6f),
                            fontSize = 10.sp
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            // MOVIE CONTROLS (Row 1: << 10s | Play/Pause | 10s >>)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(
                    enabled = activeItem != null,
                    onClick = { activeItem?.let { onMediaControlClick(nodeName, "Rewind10s", it.name) } },
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(WhitePen.copy(alpha = if (activeItem != null) 0.1f else 0.04f))
                ) {
                    Icon(Icons.Default.Replay10, "Rewind 10s", tint = if (activeItem != null) WhitePen else WhitePen.copy(alpha = 0.3f))
                }

                IconButton(
                    enabled = activeItem != null,
                    onClick = {
                        activeItem?.let {
                            isPlaying = !isPlaying
                            onMediaControlClick(nodeName, if (isPlaying) "Play" else "Pause", it.name)
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(WhitePen.copy(alpha = if (activeItem != null) 0.1f else 0.04f))
                ) {
                    Icon(if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, "Play/Pause", tint = if (activeItem != null) PeninsulaGold else PeninsulaGold.copy(alpha = 0.3f))
                }

                IconButton(
                    enabled = activeItem != null,
                    onClick = { activeItem?.let { onMediaControlClick(nodeName, "Forward10s", it.name) } },
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(WhitePen.copy(alpha = if (activeItem != null) 0.1f else 0.04f))
                ) {
                    Icon(Icons.Default.Forward10, "Forward 10s", tint = if (activeItem != null) WhitePen else WhitePen.copy(alpha = 0.3f))
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // SUBTITLE / AUDIO TRACK & FULLSCREEN CONTROLS
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(
                    enabled = activeItem != null,
                    onClick = { activeItem?.let { onMediaControlClick(nodeName, "Subtitles", it.name) } },
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(WhitePen.copy(alpha = if (activeItem != null) 0.1f else 0.04f))
                ) {
                    Icon(Icons.Default.Subtitles, "Subtitles", tint = if (activeItem != null) WhitePen else WhitePen.copy(alpha = 0.3f))
                }

                IconButton(
                    enabled = activeItem != null,
                    onClick = { activeItem?.let { onMediaControlClick(nodeName, "AudioLanguage", it.name) } },
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(WhitePen.copy(alpha = if (activeItem != null) 0.1f else 0.04f))
                ) {
                    Icon(Icons.Default.Language, "Audio Language", tint = if (activeItem != null) WhitePen else WhitePen.copy(alpha = 0.3f))
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // PRIMARY PLAY ON TV BUTTON
            Button(
                enabled = activeItem != null,
                onClick = { activeItem?.let { onMediaControlClick(nodeName, "Stream", it.name) } },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = PeninsulaGold,
                    disabledContainerColor = PeninsulaGold.copy(alpha = 0.3f)
                )
            ) {
                Icon(Icons.Default.Movie, contentDescription = null, tint = BlackPen, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Play Movie on TV",
                    style = sfTypography.titleSmall,
                    color = if (activeItem != null) BlackPen else BlackPen.copy(alpha = 0.5f),
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

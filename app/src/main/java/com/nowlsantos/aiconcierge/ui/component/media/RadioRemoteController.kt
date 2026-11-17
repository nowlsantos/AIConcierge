package com.nowlsantos.aiconcierge.ui.component.media

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
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
import com.nowlsantos.aiconcierge.ui.theme.BlackPen
import com.nowlsantos.aiconcierge.ui.theme.PeninsulaGold
import com.nowlsantos.aiconcierge.ui.theme.WhitePen
import com.nowlsantos.aiconcierge.ui.theme.sfTypography
import com.nowlsantos.aiconcierge.utils.LoadImage
import com.nowlsantos.aiconcierge.utils.getMediaDrawable
import com.nowlsantos.common.DocumentNode

/**
 * RADIO REMOTE CONTROLLER
 * Tailored for high-fidelity audio streams with Prev/Next Station, Equalizer presets, and Speaker streaming.
 */
@Composable
fun RadioRemoteController(
    activeItem: DocumentNode?,
    nodeName: String,
    onMediaControlClick: (category: String, action: String, item: String) -> Unit
) {
    var isPlaying by remember { mutableStateOf(true) }
    var isMuted by remember { mutableStateOf(false) }

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
            // Radio Header Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (activeItem != null) "HI-FI AUDIO STREAM" else "RADIO CONTROLLER",
                    style = sfTypography.labelMedium,
                    color = WhitePen.copy(alpha = 0.6f),
                    letterSpacing = 1.sp
                )
                if (activeItem != null) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = PeninsulaGold.copy(alpha = 0.2f),
                        border = BorderStroke(1.dp, PeninsulaGold)
                    ) {
                        Text(
                            text = "STEREO",
                            style = sfTypography.labelSmall,
                            color = PeninsulaGold,
                            fontWeight = FontWeight.Bold,
                            fontSize = 9.sp,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Text(
                text = activeItem?.name ?: "Select a Radio Station",
                style = sfTypography.titleMedium,
                color = PeninsulaGold,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
            )

            // HERO LOGO / STATION ART CARD
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
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

            Spacer(modifier = Modifier.height(16.dp))

            // STATION CONTROLS (Row 1: Prev | Play/Pause | Next)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(
                    enabled = activeItem != null,
                    onClick = { activeItem?.let { onMediaControlClick(nodeName, "Previous", it.name) } },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(WhitePen.copy(alpha = if (activeItem != null) 0.1f else 0.04f))
                ) {
                    Icon(Icons.Default.SkipPrevious, "Previous Station", tint = if (activeItem != null) WhitePen else WhitePen.copy(alpha = 0.3f))
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
                        .height(48.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(WhitePen.copy(alpha = if (activeItem != null) 0.1f else 0.04f))
                ) {
                    Icon(if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, "Play/Pause", tint = if (activeItem != null) PeninsulaGold else PeninsulaGold.copy(alpha = 0.3f))
                }

                IconButton(
                    enabled = activeItem != null,
                    onClick = { activeItem?.let { onMediaControlClick(nodeName, "Next", it.name) } },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(WhitePen.copy(alpha = if (activeItem != null) 0.1f else 0.04f))
                ) {
                    Icon(Icons.Default.SkipNext, "Next Station", tint = if (activeItem != null) WhitePen else WhitePen.copy(alpha = 0.3f))
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // VOLUME CONTROLS (Row 2: Vol - | Mute | Vol +)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(
                    enabled = activeItem != null,
                    onClick = { activeItem?.let { onMediaControlClick(nodeName, "VolumeDown", it.name) } },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(WhitePen.copy(alpha = if (activeItem != null) 0.1f else 0.04f))
                ) {
                    Icon(Icons.Default.Remove, "Volume Down", tint = if (activeItem != null) WhitePen else WhitePen.copy(alpha = 0.3f))
                }

                IconButton(
                    enabled = activeItem != null,
                    onClick = {
                        activeItem?.let {
                            isMuted = !isMuted
                            onMediaControlClick(nodeName, if (isMuted) "Mute" else "Unmute", it.name)
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(WhitePen.copy(alpha = if (activeItem != null) 0.1f else 0.04f))
                ) {
                    Icon(if (isMuted) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp, "Mute", tint = if (activeItem != null) WhitePen else WhitePen.copy(alpha = 0.3f))
                }

                IconButton(
                    enabled = activeItem != null,
                    onClick = { activeItem?.let { onMediaControlClick(nodeName, "VolumeUp", it.name) } },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(WhitePen.copy(alpha = if (activeItem != null) 0.1f else 0.04f))
                ) {
                    Icon(Icons.Default.Add, "Volume Up", tint = if (activeItem != null) WhitePen else WhitePen.copy(alpha = 0.3f))
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // PRIMARY STREAM BUTTON
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
                Icon(Icons.Default.GraphicEq, contentDescription = null, tint = BlackPen, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Stream to Room",
                    style = sfTypography.titleSmall,
                    color = if (activeItem != null) BlackPen else BlackPen.copy(alpha = 0.5f),
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

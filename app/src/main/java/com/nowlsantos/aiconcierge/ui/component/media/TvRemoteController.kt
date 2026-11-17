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
import com.nowlsantos.aiconcierge.ui.theme.RedPen
import com.nowlsantos.aiconcierge.ui.theme.WhitePen
import com.nowlsantos.aiconcierge.ui.theme.sfTypography
import com.nowlsantos.aiconcierge.utils.LoadImage
import com.nowlsantos.aiconcierge.utils.getMediaDrawable
import com.nowlsantos.common.DocumentNode

/**
 * TV REMOTE CONTROLLER
 * Tailored for live IPTV broadcast channels with CH +/- controls, Info/EPG, and Cast to TV.
 */
@Composable
fun TvRemoteController(
    activeItem: DocumentNode?,
    nodeName: String,
    onMediaControlClick: (category: String, action: String, item: String) -> Unit
) {
    var isMuted by remember { mutableStateOf(false) }
    val iconSize = 50.dp

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
            // Live TV Header Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (activeItem != null) "LIVE IPTV BROADCAST" else "TV CONTROLLER",
                    style = sfTypography.labelLarge,
                    color = WhitePen.copy(alpha = 0.6f),
                    letterSpacing = 1.sp
                )
                if (activeItem != null) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = RedPen
                    ) {
                        Text(
                            text = "LIVE",
                            style = sfTypography.labelSmall,
                            color = WhitePen,
                            fontWeight = FontWeight.Bold,
                            fontSize = 9.sp,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = activeItem?.name ?: "Select a TV Channel",
                style = sfTypography.titleMedium,
                color = PeninsulaGold,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
            )

            // HERO LOGO / BROADCAST PREVIEW CARD
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
                    modifier = Modifier.padding(10.dp).fillMaxWidth(),
                    imageID = heroImageRes,
                    description = activeItem?.name ?: nodeName,
                    contentScale = ContentScale.Fit
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // TV CHANNEL CONTROLS (Row 1: CH - | EPG Info | CH +)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(
                    enabled = activeItem != null,
                    onClick = { activeItem?.let { onMediaControlClick(nodeName, "ChannelDown", it.name) } },
                    modifier = Modifier
                        .weight(1f)
                        .height(iconSize)
                        .clip(RoundedCornerShape(8.dp))
                        .background(WhitePen.copy(alpha = if (activeItem != null) 0.1f else 0.04f))
                ) {
                    Text(
                        text = "CH -",
                        style = sfTypography.labelLarge,
                        color = if (activeItem != null) WhitePen else WhitePen.copy(alpha = 0.3f),
                        fontWeight = FontWeight.Bold
                    )
                }

                IconButton(
                    enabled = activeItem != null,
                    onClick = { activeItem?.let { onMediaControlClick(nodeName, "EPG", it.name) } },
                    modifier = Modifier
                        .weight(1f)
                        .height(iconSize)
                        .clip(RoundedCornerShape(8.dp))
                        .background(WhitePen.copy(alpha = if (activeItem != null) 0.1f else 0.04f))
                ) {
                    Icon(Icons.Default.Info, "TV Guide / EPG", tint = if (activeItem != null) PeninsulaGold else PeninsulaGold.copy(alpha = 0.3f))
                }

                IconButton(
                    enabled = activeItem != null,
                    onClick = { activeItem?.let { onMediaControlClick(nodeName, "ChannelUp", it.name) } },
                    modifier = Modifier
                        .weight(1f)
                        .height(iconSize)
                        .clip(RoundedCornerShape(8.dp))
                        .background(WhitePen.copy(alpha = if (activeItem != null) 0.1f else 0.04f))
                ) {
                    Text(
                        text = "CH +",
                        style = sfTypography.labelLarge,
                        color = if (activeItem != null) WhitePen else WhitePen.copy(alpha = 0.3f),
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // VOLUME CONTROLS (Row 2: Vol - | Mute | Vol +)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val iconSize = 60.dp

                IconButton(
                    enabled = activeItem != null,
                    onClick = { activeItem?.let { onMediaControlClick(nodeName, "VolumeDown", it.name) } },
                    modifier = Modifier
                        .weight(1f)
                        .height(iconSize)
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
                        .height(iconSize)
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
                        .height(iconSize)
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
                Icon(Icons.Default.Tv, contentDescription = null, tint = BlackPen, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Cast Channel to TV",
                    style = sfTypography.titleSmall,
                    color = if (activeItem != null) BlackPen else BlackPen.copy(alpha = 0.5f),
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

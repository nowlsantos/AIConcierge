package com.nowlsantos.aiconcierge.ui.component.widget

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun AudioWaveformBars(
    modifier: Modifier = Modifier,
    indicatorState: VoiceIndicatorState,
    voiceVolume: Float,
    activeColor: Color,
    isLeft: Boolean,
) {
    val barBaseHeights = if (isLeft) {
        listOf(10.dp, 18.dp, 28.dp, 16.dp, 24.dp, 12.dp)
    } else {
        listOf(12.dp, 24.dp, 16.dp, 28.dp, 18.dp, 10.dp)
    }

    val infiniteTransition = rememberInfiniteTransition(label = "waveform")

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = modifier.height(60.dp)
    ) {
        barBaseHeights.forEachIndexed { index, baseHeight ->
            val waveScale by infiniteTransition.animateFloat(
                initialValue = 0.3f,
                targetValue = 1.0f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 350 + (index * 80), easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "barScale_$index"
            )

            val animatedHeight: Dp = when (indicatorState) {
                VoiceIndicatorState.GUEST_SPEAKING -> {
                    val volumeFactor = (voiceVolume.coerceIn(0f, 12f) / 12f).coerceIn(0.2f, 1.0f)
                    baseHeight * (0.3f + (waveScale * volumeFactor * 0.9f))
                    //baseHeight * (0.3f + (waveScale * 0.7f))
                }
                VoiceIndicatorState.ARIS_SPEAKING -> {
                    baseHeight * (0.3f + (waveScale * 0.7f))
                }
                VoiceIndicatorState.STANDBY -> {
                    4.dp
                }
            }

            val barAlpha = when (indicatorState) {
                VoiceIndicatorState.STANDBY -> 0.3f
                else -> 0.85f
            }

            Box(
                modifier = Modifier
                    .width(3.5.dp)
                    .height(animatedHeight)
                    .clip(RoundedCornerShape(2.dp))
                    .background(activeColor.copy(alpha = barAlpha))
            )
        }
    }
}
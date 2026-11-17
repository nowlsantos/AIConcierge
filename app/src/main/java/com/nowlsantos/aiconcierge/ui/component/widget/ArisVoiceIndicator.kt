package com.nowlsantos.aiconcierge.ui.component.widget

import androidx.annotation.DrawableRes
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nowlsantos.aiconcierge.R
import com.nowlsantos.aiconcierge.ui.theme.IndicatorBlue
import com.nowlsantos.aiconcierge.ui.theme.IndicatorGreen
import com.nowlsantos.aiconcierge.ui.theme.IndicatorMuted
import com.nowlsantos.aiconcierge.ui.theme.sfTypography

enum class VoiceIndicatorState {
    STANDBY,
    GUEST_SPEAKING,
    ARIS_SPEAKING
}

@Composable
fun ArisVoiceIndicator(
    isListening: Boolean,
    isSpeaking: Boolean,
    isStreaming: Boolean,
    voiceVolume: Float,
    onClick: () -> Unit,
    @DrawableRes arisSpeakingIconRes: Int? = null
) {
    // Priority Rule: ARIS_SPEAKING takes precedence over GUEST_SPEAKING when Aris is actively streaming speech
    val indicatorState = when {
        isSpeaking || isStreaming -> VoiceIndicatorState.ARIS_SPEAKING
        isListening -> VoiceIndicatorState.GUEST_SPEAKING
        else -> VoiceIndicatorState.STANDBY
    }

    val activeColor by animateColorAsState(
        targetValue = when (indicatorState) {
            VoiceIndicatorState.GUEST_SPEAKING -> IndicatorGreen
            VoiceIndicatorState.ARIS_SPEAKING -> IndicatorBlue
            VoiceIndicatorState.STANDBY -> IndicatorMuted
        },
        animationSpec = tween(300),
        label = "activeColor"
    )

    val isExpanded = indicatorState == VoiceIndicatorState.STANDBY

    Row(
        modifier = Modifier.fillMaxHeight(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        val iconRes = if (indicatorState == VoiceIndicatorState.ARIS_SPEAKING && arisSpeakingIconRes != null) {
            arisSpeakingIconRes
        } else {
            R.drawable.microphone
        }

        Image(
            painter = painterResource(iconRes),
            contentDescription = stringResource(R.string.listening),
            modifier = Modifier
                .height(60.dp)
                .debouncedClick(
                    debounceTimeMs = 500L,
                    enabled = !isSpeaking && !isStreaming,
                    onClick = onClick
                ),
            contentScale = ContentScale.Fit,
            colorFilter = if (indicatorState == VoiceIndicatorState.STANDBY) {
                ColorFilter.tint(IndicatorMuted)
            } else {
                ColorFilter.tint(activeColor)
            }
        )

        Spacer(modifier = Modifier.width(16.dp))

        Column(
            modifier = Modifier.fillMaxHeight(),
            verticalArrangement = Arrangement.Center
        ) {
            // Left Equalizer Waveform Bars
            if (!isExpanded) {
                Row {
                    AudioWaveformBars(
                        modifier = Modifier,
                        indicatorState = indicatorState,
                        voiceVolume = voiceVolume,
                        activeColor = activeColor,
                        isLeft = true
                    )

                    AudioWaveformBars(
                        modifier = Modifier,
                        indicatorState = indicatorState,
                        voiceVolume = voiceVolume,
                        activeColor = activeColor,
                        isLeft = true
                    )
                }

            }

            // Status Subtitle Text
            Text(
                text = when {
                    isSpeaking -> "Aris Speaking..."
                    isStreaming -> "Aris Thinking..."
                    isListening -> "Listening...Speak Now"
                    else -> "Tap to Speak to Aris"
                },
                modifier = Modifier.padding(bottom = if (isExpanded) 0.dp else 12.dp),
                color = activeColor.copy(alpha = 0.9f),
                style = sfTypography.titleSmall,
                letterSpacing = 1.sp
            )
        }
    }
}

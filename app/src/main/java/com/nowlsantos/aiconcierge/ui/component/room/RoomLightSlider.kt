package com.nowlsantos.aiconcierge.ui.component.room

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.nowlsantos.aiconcierge.ui.theme.GoldPen
import com.nowlsantos.aiconcierge.ui.theme.PeninsulaCharcoal
import com.nowlsantos.aiconcierge.ui.theme.PeninsulaGold
import com.nowlsantos.aiconcierge.ui.theme.PeninsulaGreyCard
import com.nowlsantos.aiconcierge.ui.theme.WhitePen
import com.nowlsantos.aiconcierge.ui.theme.sfTypography
import com.nowlsantos.aiconcierge.utils.capitalizeEachWord
import kotlin.math.roundToInt

/**
 * LIGHT SLIDER STATE
 * Dedicated component state for discrete 6-step light level dimmer.
 */
data class LightSliderState(
    val lightLevel: Int = 0
)

/**
 * ROOM LIGHT SLIDER
 * Discrete 6-step light level dimmer (0 - 1 - 2 - 3 - 4 - 5) with custom vertical pill thumb,
 * smooth 800ms AI animation ONLY when not actively dragged by the guest.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoomLightSlider(
    state: LightSliderState,
    activeZone: String,
    onLightLevelChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isDragged by interactionSource.collectIsDraggedAsState()

    val animatedLevel by animateFloatAsState(
        targetValue = state.lightLevel.toFloat(),
        animationSpec = if (isDragged) snap() else tween(durationMillis = 800, easing = FastOutSlowInEasing),
        label = "lightLevelAnimation"
    )

    val activeStep = animatedLevel.roundToInt()

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Slider(
            value = animatedLevel,
            onValueChange = { onLightLevelChange(it.roundToInt()) },
            valueRange = 0f..5f,
            steps = 4,
            interactionSource = interactionSource,
            thumb = {
                Box(
                    modifier = Modifier
                        .size(width = 14.dp, height = 32.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(GoldPen)
                )
            },
            colors = SliderDefaults.colors(
                thumbColor = GoldPen,
                activeTrackColor = GoldPen,
                inactiveTrackColor = WhitePen.copy(alpha = 0.2f)
            ),
            track = { sliderState ->
                SliderDefaults.Track(
                    sliderState = sliderState,
                    modifier = Modifier.height(10.dp), // Custom track height (Default is usually smaller)
                    colors = SliderDefaults.colors(
                        activeTrackColor = GoldPen,
                        inactiveTrackColor = PeninsulaCharcoal,
                        activeTickColor = PeninsulaGold,
                        inactiveTickColor = PeninsulaGreyCard
                    ),
                    drawTick = { offset: Offset, color: Color ->
                        // Draw a custom small circle or rectangle for each tick mark
                        drawCircle(
                            color = color,
                            radius = 4.dp.toPx(),
                            center = offset
                        )
                    }
                )
            },
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            (0..5).forEach { step ->
                Text(
                    text = "$step",
                    style = sfTypography.titleMedium,
                    color = if (activeStep == step) GoldPen else WhitePen.copy(alpha = 0.5f)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "${activeZone.lowercase().capitalizeEachWord()} Light",
            //text = "${activeZone.lowercase().capitalizeFirstLetter()} Light",
            style = sfTypography.titleMedium,
            color = WhitePen,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}
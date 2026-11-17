package com.nowlsantos.aiconcierge.ui.component.room

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.nowlsantos.aiconcierge.ui.theme.GoldPen
import com.nowlsantos.aiconcierge.ui.theme.GreyPen
import com.nowlsantos.aiconcierge.ui.theme.PeninsulaCharcoal
import com.nowlsantos.aiconcierge.ui.theme.PeninsulaGold
import com.nowlsantos.aiconcierge.ui.theme.PeninsulaGreyCard
import com.nowlsantos.aiconcierge.ui.theme.WhitePen
import com.nowlsantos.aiconcierge.ui.theme.sfTypography
import com.nowlsantos.aiconcierge.utils.capitalizeEachWord

enum class CurtainThumbStyle {
    VERTICAL_HANDLE, // Option A: 14.dp x 32.dp Vertical Curtain Pull Handle
    SOFT_SQUARE,      // Option B: 28.dp x 28.dp Soft Square Knob
    CLASSIC_ROUND     // Option C: 28.dp x 28.dp Classic Round Knob
}

/**
 * CURTAIN SLIDER STATE
 * Dedicated component state for motorized curtain range limits (Left: 0.0..0.47, Right: 0.52..1.0).
 */
data class CurtainSliderState(
    val curtainRange: ClosedFloatingPointRange<Float> = 0f..1f
)

/**
 * CURTAIN RANGE SLIDER
 * Dual-thumb RangeSlider with strict 47% physical track limits, customizable thumb shape,
 * 0ms instant touch-dragging response, and smooth 1,200ms drape animation on voice commands.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CurtainSlider(
    state: CurtainSliderState,
    activeZone: String,
    onCurtainRangeChange: (ClosedFloatingPointRange<Float>) -> Unit,
    modifier: Modifier = Modifier,
    thumbStyle: CurtainThumbStyle = CurtainThumbStyle.VERTICAL_HANDLE
) {
    // Local slider position synced with state.curtainRange whenever state updates externally
    var sliderRange by remember(state.curtainRange) {
        val s = state.curtainRange.start.coerceIn(0f, 1f)
        val e = state.curtainRange.endInclusive.coerceIn(0f, 1f)
        mutableStateOf(s..e)
    }

    val displayStart = sliderRange.start.coerceIn(0f, 1f)
    val displayEnd = sliderRange.endInclusive.coerceIn(0f, 1f)
    val activeRange = displayStart..displayEnd

    val leftPct = (displayStart * 100).toInt()
    val rightPct = (displayEnd * 100).toInt()

    val startInteractionSource = remember { MutableInteractionSource() }
    val endInteractionSource = remember { MutableInteractionSource() }

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        RangeSlider(
            value = activeRange,
            onValueChange = { range ->
                val newStart = range.start.coerceIn(0f, 0.47f)
                val newEnd = range.endInclusive.coerceIn(0.53f, 1f)
                val newRange = newStart..newEnd
                sliderRange = newRange
                onCurtainRangeChange(newRange)
            },
            startThumb = {
                when (thumbStyle) {
                    CurtainThumbStyle.VERTICAL_HANDLE -> {
                        Box(
                            modifier = Modifier
                                .size(width = 14.dp, height = 32.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(GoldPen)
                        )
                    }
                    CurtainThumbStyle.SOFT_SQUARE -> {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(GoldPen)
                        )
                    }
                    CurtainThumbStyle.CLASSIC_ROUND -> {
                        SliderDefaults.Thumb(
                            interactionSource = startInteractionSource,
                            thumbSize = DpSize(28.dp, 28.dp),
                            colors = SliderDefaults.colors(thumbColor = GreyPen)
                        )
                    }
                }
            },
            endThumb = {
                when (thumbStyle) {
                    CurtainThumbStyle.VERTICAL_HANDLE -> {
                        Box(
                            modifier = Modifier
                                .size(width = 14.dp, height = 32.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(GoldPen)
                        )
                    }
                    CurtainThumbStyle.SOFT_SQUARE -> {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(GoldPen)
                        )
                    }
                    CurtainThumbStyle.CLASSIC_ROUND -> {
                        SliderDefaults.Thumb(
                            interactionSource = endInteractionSource,
                            thumbSize = DpSize(28.dp, 28.dp),
                            colors = SliderDefaults.colors(thumbColor = GreyPen)
                        )
                    }
                }
            },
            valueRange = 0f..1f,
            colors = SliderDefaults.colors(
                thumbColor = GreyPen,
                activeTickColor = GreyPen,
                activeTrackColor = GoldPen,
                inactiveTrackColor = WhitePen.copy(alpha = 0.2f)
            ),
            track = { rangeSliderState ->
                SliderDefaults.Track(
                    rangeSliderState = rangeSliderState,
                    modifier = Modifier.height(12.dp), // Custom track height (Default is usually smaller)
                    colors = SliderDefaults.colors(
                        activeTrackColor = GoldPen,
                        inactiveTrackColor = PeninsulaCharcoal,
                        activeTickColor = PeninsulaGold,
                        inactiveTickColor = PeninsulaGreyCard
                    ),
                    drawStopIndicator = { _ ->
                        // Optional: Custom stop/tick indicator drawing or leave empty to hide
                    }
                )
            },
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Left: $leftPct%", style = sfTypography.labelLarge, color = WhitePen.copy(alpha = 0.7f))
            Text("Right: $rightPct%", style = sfTypography.labelLarge, color = WhitePen.copy(alpha = 0.7f))
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "${activeZone.lowercase().capitalizeEachWord()} Curtains",
            //text = "${activeZone.lowercase().capitalizeFirstLetter()} Curtains",
            style = sfTypography.titleMedium,
            color = WhitePen,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

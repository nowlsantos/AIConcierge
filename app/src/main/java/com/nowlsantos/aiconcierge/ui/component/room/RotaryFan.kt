package com.nowlsantos.aiconcierge.ui.component.room

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nowlsantos.aiconcierge.ui.theme.BackgroundPen
import com.nowlsantos.aiconcierge.ui.theme.DarkPen
import com.nowlsantos.aiconcierge.ui.theme.GoldPen
import com.nowlsantos.aiconcierge.ui.theme.RedPen
import com.nowlsantos.aiconcierge.ui.theme.WhitePen
import com.nowlsantos.aiconcierge.ui.theme.sfTypography
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

/**
 * ROTARY FAN STATE
 * Dedicated component state for rotary fan speed knob.
 */
data class RotaryFanState(
    val fanSpeedIndex: Int = 0 // Default 0: OFF, 1: LOW, 2: MID, 3: HIGH, 4: MAX
)

/**
 * ROTARY FAN KNOB
 * Interactive stepped rotary dial knob matching Pen luxury specs with red indicator notch,
 * arc labels (OFF, LOW, MID, HIGH, MAX), mechanical end-stop clamping, drag gesture snapping,
 * and smooth 800ms AI rotation animation ONLY when not actively dragged.
 */
@Composable
fun RotaryFan(
    state: RotaryFanState,
    onFanSpeedChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val levels = listOf("OFF", "LOW", "MID", "HIGH", "MAX")
    // Angles for steps across upper arc: 180° (OFF), 135° (LOW), 90° (MID), 45° (HIGH), 0° (MAX)
    val stepAngles = listOf(180f, 135f, 90f, 45f, 0f)

    val activeIndex = state.fanSpeedIndex.coerceIn(0, 4)
    val currentActiveIndex by rememberUpdatedState(activeIndex)

    var isDragging by remember { mutableStateOf(false) }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier.size(220.dp),
            contentAlignment = Alignment.Center
        ) {
            // STEPPED TEXT LABELS (OFF, LOW, MID, HIGH, MAX)
            val radius = 150.dp
            levels.forEachIndexed { index, label ->
                val angleDeg = stepAngles[index]
                val angleRad = Math.toRadians(angleDeg.toDouble())
                // Convert polar to Cartesian offsets relative to center
                val offsetX = (radius.value * cos(angleRad)).dp
                val offsetY = (-radius.value * sin(angleRad)).dp

                val isSelected = index == activeIndex

                Text(
                    text = label,
                    style = sfTypography.labelLarge,
                    color = if (isSelected) GoldPen else WhitePen.copy(alpha = 0.4f),
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    modifier = Modifier
                        .offset(x = offsetX, y = offsetY)
                        .clickable { onFanSpeedChange(index) }
                )
            }

            // ROTARY DIAL KNOB WITH RED NOTCH & MECHANICAL CLAMP DRAG GESTURE
            Box(
                modifier = Modifier
                    .size(250.dp)
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragStart = { isDragging = true },
                            onDragEnd = { isDragging = false },
                            onDragCancel = { isDragging = false },
                            onDrag = { change, _ ->
                                val center = Offset(size.width / 2f, size.height / 2f)
                                val touchPos = change.position
                                val dx = touchPos.x - center.x
                                val dy = center.y - touchPos.y // Invert Y for screen coordinates

                                var angleDeg = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
                                if (angleDeg < 0f) {
                                    angleDeg = if (dx < 0) 180f else 0f
                                }

                                // Find nearest raw step index (0..4)
                                val rawIndex = stepAngles.indices.minByOrNull { i ->
                                    abs(stepAngles[i] - angleDeg)
                                } ?: currentActiveIndex

                                // MECHANICAL CLAMP: Prevent jumping across bottom barrier (0 <-> 4)
                                val stepDiff = abs(rawIndex - currentActiveIndex)
                                val nextIndex = if (stepDiff > 1) {
                                    // Clamp jump to adjacent step only
                                    if (rawIndex > currentActiveIndex) {
                                        (currentActiveIndex + 1).coerceAtMost(4)
                                    } else {
                                        (currentActiveIndex - 1).coerceAtLeast(0)
                                    }
                                } else {
                                    rawIndex
                                }

                                if (nextIndex != currentActiveIndex) {
                                    onFanSpeedChange(nextIndex)
                                }
                            }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                // Outer Dial Body & Red Indicator Notch Canvas
                val targetAngle = stepAngles[activeIndex]
                val animatedAngle by animateFloatAsState(
                    targetValue = targetAngle,
                    animationSpec = if (isDragging) snap() else tween(durationMillis = 800, easing = FastOutSlowInEasing),
                    label = "fanNotchRotation"
                )
                val activeAngle = animatedAngle

                Canvas(modifier = Modifier.fillMaxSize()) {
                    val centerPx = Offset(size.width / 2f, size.height / 2f)
                    val outerRadiusPx = size.width / 2f

                    // Outer dark dial ring background
                    drawCircle(
                        color = DarkPen.copy(alpha = 0.5f),
                        radius = outerRadiusPx
                    )

                    drawCircle(
                        color = WhitePen.copy(alpha = 0.2f),
                        radius = outerRadiusPx,
                        style = Stroke(width = 5.dp.toPx())
                    )

                    // Red Indicator Notch pointing to active speed
                    val rad = Math.toRadians(activeAngle.toDouble())
                    val notchLength = 16.dp.toPx()
                    val startDist = outerRadiusPx - 26.dp.toPx()
                    val endDist = startDist + notchLength

                    val startX = centerPx.x + (startDist * cos(rad)).toFloat()
                    val startY = centerPx.y - (startDist * sin(rad)).toFloat()
                    val endX = centerPx.x + (endDist * cos(rad)).toFloat()
                    val endY = centerPx.y - (endDist * sin(rad)).toFloat()

                    drawLine(
                        color = RedPen, // Vivid Red Notch
                        start = Offset(startX, startY),
                        end = Offset(endX, endY),
                        strokeWidth = 12.dp.toPx(),
                        cap = StrokeCap.Square
                    )
                }

                // Inner Center Knob ("FAN")
                Surface(
                    shape = CircleShape,
                    color = BackgroundPen,
                    border = BorderStroke(1.dp, WhitePen.copy(alpha = 0.15f)),
                    modifier = Modifier.size(90.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "FAN",
                            style = sfTypography.titleMedium,
                            color = WhitePen,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Fan Speed",
            style = sfTypography.titleMedium,
            color = WhitePen,
            fontWeight = FontWeight.Medium
        )
    }
}

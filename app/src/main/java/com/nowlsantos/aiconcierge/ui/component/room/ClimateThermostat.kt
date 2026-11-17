package com.nowlsantos.aiconcierge.ui.component.room

import androidx.compose.animation.*
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nowlsantos.aiconcierge.ui.theme.BackgroundPen
import com.nowlsantos.aiconcierge.ui.theme.BlackPen
import com.nowlsantos.aiconcierge.ui.theme.DarkPen
import com.nowlsantos.aiconcierge.ui.theme.GoldPen
import com.nowlsantos.aiconcierge.ui.theme.WhitePen
import com.nowlsantos.aiconcierge.ui.theme.sfTypography
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * THERMOSTAT STATE
 * Dedicated component state for climate gauge (16°C  30°C / 60°F  86°F).
 */
data class ThermostatState(
    val targetTempC: Int = 20, // 16°C to 30°C limits (Default 20°C = 68°F)
    val isCelsius: Boolean = true
) {
    val targetTempF: Int get() = ((targetTempC * 9 / 5) + 32)
}

/**
 * CLIMATE THERMOSTAT
 * Interactive 270° circular slider with smooth 1000ms AI-driven thumb gliding animation,
 * scale-aware unit conversion (°C: 16..30 ➔ 28.5% left | °F: 0..100 ➔ 68% right side arc sweep),
 * and direct manual drag control.
 */
@Composable
fun ClimateThermostat(
    state: ThermostatState,
    onTempChange: (Int) -> Unit,
    onUnitToggle: (Boolean) -> Unit
) {
    val activeTempC = state.targetTempC.coerceIn(16, 30)
    val activeTempF = state.targetTempF.coerceIn(32, 100)
    var isDragging by remember { mutableStateOf(false) }

    // Target gauge progress on the 270° arc track based on active temperature unit scale
    val targetProgress = if (state.isCelsius) {
        ((activeTempC - 16f) / 14f).coerceIn(0f, 1f) // 16°C  30°C scale (20°C = 28.5% progress)
    } else {
        (activeTempF / 100f).coerceIn(0f, 1f)         // 0°F  100°F scale (68°F = 68.0% progress!)
    }

    var dragProgress by remember(state.isCelsius) {
        mutableFloatStateOf(targetProgress)
    }

    // Smooth 1000ms thumb knob & arc gauge animation on voice commands or °C / °F switch toggles
    val animatedProgress by animateFloatAsState(
        targetValue = if (isDragging) dragProgress else targetProgress,
        animationSpec = if (isDragging) snap() else tween(durationMillis = 1000, easing = FastOutSlowInEasing),
        label = "thermostatProgress"
    )

    val activeProgress = if (isDragging) dragProgress else animatedProgress

    LaunchedEffect(activeTempC, state.isCelsius) {
        if (!isDragging) {
            dragProgress = targetProgress
        }
    }

    Column(
        modifier = Modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(290.dp)
                .pointerInput(state.isCelsius) {
                    detectDragGestures(
                        onDragStart = { isDragging = true },
                        onDragEnd = { isDragging = false },
                        onDragCancel = { isDragging = false },
                        onDrag = { change, _ ->
                            val center = Offset(size.width / 2f, size.height / 2f)
                            val touchPos = change.position
                            val dx = touchPos.x - center.x
                            val dy = touchPos.y - center.y

                            val angleDeg = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
                            var normalizedAngle = angleDeg - 135f
                            if (normalizedAngle < 0f) normalizedAngle += 360f

                            if (normalizedAngle > 270f) {
                                normalizedAngle = if (normalizedAngle > 315f) 0f else 270f
                            }

                            val newProgress = (normalizedAngle / 270f).coerceIn(0f, 1f)
                            dragProgress = newProgress

                            val newTempC = if (state.isCelsius) {
                                (16f + newProgress * 14f).roundToInt().coerceIn(16, 30)
                            } else {
                                val newTempF = (newProgress * 100f).roundToInt().coerceIn(60, 86)
                                ((newTempF - 32) * 5 / 9).coerceIn(16, 30)
                            }

                            if (newTempC != activeTempC) {
                                onTempChange(newTempC)
                            }
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            // CIRCULAR THERMOSTAT GAUGE & PERFECTLY CENTERED DRAGGABLE THUMB CANVAS
            Canvas(modifier = Modifier.fillMaxSize().padding(20.dp)) {
                val strokeWidth = 5.dp.toPx()
                val padding = strokeWidth / 2f
                val arcSize = Size(size.width - strokeWidth, size.height - strokeWidth)
                val radiusPx = arcSize.width / 2f
                val centerPx = Offset(size.width / 2f, size.height / 2f)

                // 1. Draw the background track first
                drawArc(
                    color = DarkPen.copy(alpha = 0.5f), // Fainter background
                    startAngle = 135f,
                    sweepAngle = 270f,
                    useCenter = false,
                    topLeft = Offset(padding, padding),
                    size = arcSize,
                    //style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )

                // Background Track Arc (270° sweep starting at 135°)
                drawArc(
                    color = WhitePen.copy(alpha = 0.15f),
                    startAngle = 135f,
                    sweepAngle = 270f,
                    useCenter = false,
                    topLeft = Offset(padding, padding),
                    size = arcSize,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )

                // Active Progress Arc (Animated 1000ms for AI commands & switch toggles)
                val sweepAngle = activeProgress * 270f
                drawArc(
                    color = GoldPen,
                    startAngle = 135f,
                    sweepAngle = sweepAngle,
                    useCenter = false,
                    topLeft = Offset(padding, padding),
                    size = arcSize,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )

                // Draggable Gold Thumb Knob (Animates smoothly along the 270° arc track line)
                val thumbAngleDeg = 135f + sweepAngle
                val thumbAngleRad = Math.toRadians(thumbAngleDeg.toDouble())
                val thumbX = centerPx.x + (radiusPx * cos(thumbAngleRad)).toFloat()
                val thumbY = centerPx.y + (radiusPx * sin(thumbAngleRad)).toFloat()

                // Thumb Outer Glow & Inner Circle
                drawCircle(
                    color = GoldPen,
                    radius = 16.dp.toPx(),
                    center = Offset(thumbX, thumbY)
                )
                drawCircle(
                    color = BlackPen,
                    radius = 8.dp.toPx(),
                    center = Offset(thumbX, thumbY)
                )
            }

            // CENTER CIRCLE DISPLAY (ANIMATED CONTENT TRANSITION FOR TEMPERATURE VALUE & UNIT)
            Surface(
                shape = CircleShape,
                color = BackgroundPen.copy(alpha = 0.5f),
                border = BorderStroke(1.dp, color = WhitePen.copy(0.2f)),
                modifier = Modifier.size(110.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    val displayTemp = if (state.isCelsius) {
                        "${state.targetTempC} °C"
                    } else {
                        "${state.targetTempF} °F"
                    }

                    AnimatedContent(
                        targetState = displayTemp,
                        transitionSpec = {
                            (slideInVertically { height -> height / 2 } + fadeIn(tween(400))).togetherWith(
                                slideOutVertically { height -> -height / 2 } + fadeOut(tween(400))
                            )
                        },
                        label = "tempDisplayAnimation"
                    ) { animatedText ->
                        Text(
                            text = animatedText,
                            style = sfTypography.headlineSmall,
                            color = WhitePen,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Column(modifier = Modifier
                .fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom
            ) {
                // Temperature °C / °F Switch
                Row(
                    //modifier = Modifier.align(Alignment.BottomCenter),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "°C",
                        style = sfTypography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (state.isCelsius) GoldPen else WhitePen.copy(alpha = 0.5f)
                    )
                    Switch(
                        checked = !state.isCelsius,
                        onCheckedChange = { isFahrenheitChecked ->
                            onUnitToggle(!isFahrenheitChecked)
                        },
                        modifier = Modifier.scale(1.1f),
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = GoldPen,
                            checkedTrackColor = WhitePen.copy(alpha = 0.2f),
                            uncheckedThumbColor = GoldPen,
                            uncheckedTrackColor = WhitePen.copy(alpha = 0.2f)
                        )
                    )
                    Text(
                        text = "°F",
                        style = sfTypography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (!state.isCelsius) GoldPen else WhitePen.copy(alpha = 0.5f)
                    )
                }

                Text(
                    text = "Temperature",
                    //modifier = Modifier.padding(vertical = 16.dp),
                    style = sfTypography.titleMedium,
                    color = WhitePen,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 1.sp
                )
            }
        } // end of Box
    }
}

/*
// ==================== COMPOSE PREVIEWS ====================

@Preview(name = "Celsius Mode (20°C -> 28.5% Progress Left Side)")
@Composable
fun ClimateThermostatCelsiusPreview() {
    Surface(color = BackgroundPen, modifier = Modifier.padding(16.dp)) {
        ClimateThermostat(
            state = ThermostatState(targetTempC = 20, isCelsius = true),
            onTempChange = {},
            onUnitToggle = {}
        )
    }
}

@Preview(name = "Fahrenheit Mode (68°F -> 68.0% Progress Right Side Sweep)")
@Composable
fun ClimateThermostatFahrenheitPreview() {
    Surface(color = BackgroundPen, modifier = Modifier.padding(16.dp)) {
        ClimateThermostat(
            state = ThermostatState(targetTempC = 20, isCelsius = false),
            onTempChange = {},
            onUnitToggle = {}
        )
    }
}
*/

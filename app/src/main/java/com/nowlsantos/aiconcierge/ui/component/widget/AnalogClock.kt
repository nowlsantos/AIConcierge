package com.nowlsantos.aiconcierge.ui.component.widget

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.center
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asComposePaint
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.nowlsantos.aiconcierge.R
import com.nowlsantos.aiconcierge.ui.theme.BlackPen
import com.nowlsantos.aiconcierge.ui.theme.GoldPen
import com.nowlsantos.aiconcierge.ui.theme.GreyPen
import com.nowlsantos.aiconcierge.ui.theme.PeninsulaGold
import com.nowlsantos.aiconcierge.ui.theme.RedPen
import kotlinx.coroutines.delay
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun AnalogClock(modifier: Modifier = Modifier) {
    var currentTime by remember { mutableStateOf<ZonedDateTime?>(null) }
    val londonZoneId = remember { ZoneId.of("Europe/London") }
    //val clockFaceColor = MaterialTheme.colorScheme.surface
    //val hourHandColor = GoldPen
    val minuteHandColor = GreyPen
    val secondHandColor = RedPen
    val hourMarkerColor = GoldPen
    val minuteMarkerColor = GoldPen
    val markerLength = 5.dp
    val hourMarkerWidth = 2.5.dp
    val minuteMarkerWidth = 1.5f.dp
    //val clockStrokeWidth = 4.dp
    val hourHandWidth = 8.dp
    val minuteHandWidth = 5.dp
    val secondHandWidth = 3.dp
    //time: Calendar = Calendar.getInstance()

    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (true) {
                currentTime = ZonedDateTime.now(londonZoneId)
                delay(1000.milliseconds) // Update every second while visible
            }
        }
    }

    Box(contentAlignment = Alignment.Center) {
        Image(
            painter = painterResource(R.drawable.clock_face3),
            contentDescription = "Analog Clock",
            modifier = Modifier
                .scale(0.98f)
                .circleShadow(
                    color = BlackPen,
                    alpha = 0.7f,
                    shadowBlurRadius = 16.dp,
                    offsetY = 10.dp
                ),
            contentScale = ContentScale.Fit,
        )

        Canvas(modifier = modifier.aspectRatio(1f)) {
            val center = size.center
            val radius = size.minDimension / 2f
            val now = currentTime // Capture current time for this draw pass

            // --- Draw Clock Face ---
            // Outer circle
            /*drawCircle(
                color = clockFaceColor,
                radius = radius,
                center = center,
                style = Stroke(width = clockStrokeWidth.toPx())
            )*/

            // --- Draw Markers ---
            for (i in 1..60) {
                val angleRad = (i * 6 - 90) * (PI / 180f).toFloat() // 6 degrees per minute, -90 to start at 12
                val isHourMarker = i % 5 == 0
                val markerLengthPx = if (isHourMarker) markerLength.toPx() else markerLength.toPx() / 2
                val markerColor = if (isHourMarker) hourMarkerColor else minuteMarkerColor
                val markerStrokeWidth = (if (isHourMarker) hourMarkerWidth else minuteMarkerWidth).toPx()

                val start = Offset(
                    x = center.x + (radius - markerLengthPx) * cos(angleRad),
                    y = center.y + (radius - markerLengthPx) * sin(angleRad)
                )
                val end = Offset(
                    x = center.x + radius * cos(angleRad),
                    y = center.y + radius * sin(angleRad)
                )
                drawLine(
                    color = markerColor,
                    start = start,
                    end = end,
                    strokeWidth = markerStrokeWidth,
                    cap = StrokeCap.Round
                )
            }

            // --- Draw Hands (only if time is available) ---
            if (now != null) {
                val hour = now.hour % 12 // 0-11
                val minute = now.minute
                val second = now.second

                // Calculate hand lengths relative to radius
                val secondHandLength = radius * 0.9f
                val secondHandTailLength = radius * 0.2f
                val minuteHandLength = radius * 0.7f
                val hourHandLength = radius * 0.5f

                // --- Minute Hand ---
                // Include seconds for smoother minute hand movement
                val minuteAngle = (minute * 6f) + (second * 0.1f) - 90f
                rotate(degrees = minuteAngle, pivot = center) {
                    drawLine(
                        color = minuteHandColor,
                        start = center,
                        end = Offset(center.x, center.y - minuteHandLength),
                        strokeWidth = minuteHandWidth.toPx(),
                        cap = StrokeCap.Round
                    )
                }

                // --- Hour Hand ---
                // Include minutes and seconds for smoother hour hand movement
                val hourAngle = (hour * 30f) + (minute * 0.5f) + (second * (0.5f/60f)) - 90f
                rotate(degrees = hourAngle, pivot = center) {
                    drawLine(
                        color = GoldPen,
                        start = center,
                        end = Offset(center.x, center.y - hourHandLength),
                        strokeWidth = hourHandWidth.toPx(),
                        cap = StrokeCap.Round
                    )
                }

                // --- Second Hand ---
                val secondAngle = (second * 6f - 90f) // 6 degrees per second, -90 to start at 12
                rotate(degrees = secondAngle, pivot = center) {
                    drawLine(
                        color = secondHandColor,
                        start = Offset(center.x, center.y + secondHandTailLength), // Offset counterbalance tail past center
                        end = Offset(center.x, center.y - secondHandLength), // Draw upwards towards the tip
                        strokeWidth = secondHandWidth.toPx(),
                        cap = StrokeCap.Round // Rounded end
                    )
                } // Rotation is automatically undone here

                // Center dot
                drawCircle(
                    color = PeninsulaGold,
                    radius = hourHandWidth.toPx() * 1.5f,
                    center = center
                )
            }
        }
    }
}

/**
 * Creates a perfectly circular drop shadow (Optimized with remember cache).
 */
@Composable
private fun Modifier.circleShadow(
    color: Color = BlackPen,
    alpha: Float = 0.2f,
    shadowBlurRadius: Dp = 8.dp,
    offsetY: Dp = 4.dp,
    offsetX: Dp = 0.dp
): Modifier {
    val shadowColor = remember(color, alpha) { color.copy(alpha = alpha).toArgb() }
    val transparentColor = remember(color) { color.copy(alpha = 0f).toArgb() }
    val paint = remember(color) {
        android.graphics.Paint().apply {
            this.color = transparentColor
        }
    }

    return this.drawBehind {
        paint.color = transparentColor
        paint.setShadowLayer(
            shadowBlurRadius.toPx(),
            offsetX.toPx(),
            offsetY.toPx(),
            shadowColor
        )

        drawIntoCanvas { canvas ->
            val radius = minOf(size.width, size.height) / 2f
            canvas.drawCircle(
                center = Offset(size.width / 2f, size.height / 2f),
                radius = radius,
                paint = paint.asComposePaint()
            )
        }
    }
}
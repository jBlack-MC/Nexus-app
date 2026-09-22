package com.example.nexus.ui

import android.animation.ValueAnimator
import android.os.Build
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.nexus.ui.theme.BrandBorder
import com.example.nexus.ui.theme.BrandDeepBg
import com.example.nexus.ui.theme.BrandGlow
import com.example.nexus.ui.theme.BrandGreen
import com.example.nexus.ui.theme.BrandIconBg
import com.example.nexus.ui.theme.BrandPurple
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.sin

@Composable
fun SplashIntroScreen(onFinished: () -> Unit) {
    val reducedMotion = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !ValueAnimator.areAnimatorsEnabled()
    val timeline = remember { Animatable(0f) }

    LaunchedEffect(reducedMotion) {
        if (reducedMotion) {
            timeline.snapTo(1f)
            delay(450)
            onFinished()
        } else {
            timeline.snapTo(0f)
            timeline.animateTo(1f, animationSpec = tween(durationMillis = 900, easing = LinearEasing))
            onFinished()
        }
    }

    val t = timeline.value
    val frame = eased(segment(t, 0f, 0.14f))
    val iconScale = 0.9f + (0.1f * frame)
    val iconAlpha = frame

    val spinnerIn = segment(t, 0.04f, 0.17f)
    val spinnerTurn = segment(t, 0.04f, 0.21f)
    val spinnerOut = segment(t, 0.21f, 0.28f)
    val spinnerAlpha = when {
        t < 0.04f -> 0f
        t < 0.21f -> 1f
        t < 0.28f -> 1f - spinnerOut
        else -> 0f
    }
    val spinnerScale = when {
        t < 0.17f -> 0.3f + 0.7f * eased(spinnerIn)
        t < 0.21f -> 1f
        t < 0.28f -> 1f - (0.6f * spinnerOut)
        else -> 0.4f
    }
    val spinnerRotation = 760f * eased(spinnerTurn)

    val dot1 = segment(t, 0.21f, 0.42f)
    val dot2 = segment(t, 0.225f, 0.435f)
    val dot3 = segment(t, 0.24f, 0.45f)
    val dot4 = segment(t, 0.255f, 0.465f)
    val centerDot = segment(t, 0.24f, 0.31f)

    val leftLine = segment(t, 0.43f, 0.52f)
    val diagLine = segment(t, 0.52f, 0.62f)
    val rightLine = segment(t, 0.62f, 0.71f)

    val highlight = segment(t, 0.71f, 0.83f)
    val connectedFade = segment(t, 0.83f, 0.88f)
    val connectedAlpha = 1f - connectedFade
    val connectedScale = 1f + (0.02f * sin(highlight * PI).toFloat())

    val monogramAlpha = segment(t, 0.87f, 0.92f)
    val monogramScale = 0.94f + (0.06f * monogramAlpha)

    val flashAlpha = triangularPulse(t, 0.82f, 0.9f, 0.86f)
    val settle = sin(segment(t, 0.9f, 1f) * PI).toFloat()
    val settleScale = 1f + (0.035f * settle)

    val word = eased(segment(t, 0.9f, 1f))

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colorStops = arrayOf(
                        0f to Color(0xFF121218),
                        1f to BrandDeepBg
                    ),
                    radius = with(LocalDensity.current) { 560.dp.toPx() },
                    center = Offset.Unspecified
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(22.dp)
        ) {
            NexusIcon(
                modifier = Modifier
                    .size(220.dp)
                    .graphicsLayer {
                        alpha = iconAlpha
                        scaleX = iconScale * settleScale
                        scaleY = iconScale * settleScale
                    },
                spinnerAlpha = spinnerAlpha,
                spinnerScale = spinnerScale,
                spinnerRotation = spinnerRotation,
                dotProgress = floatArrayOf(dot1, dot2, dot3, dot4),
                centerDot = centerDot,
                lineProgress = floatArrayOf(leftLine, diagLine, rightLine),
                connectedAlpha = connectedAlpha,
                connectedScale = connectedScale,
                monogramAlpha = monogramAlpha,
                monogramScale = monogramScale,
                flashAlpha = flashAlpha
            )

            Text(
                text = "Nexus",
                style = TextStyle(
                    brush = Brush.linearGradient(listOf(BrandPurple, BrandGreen)),
                    fontSize = 38.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = (-0.2).sp
                ),
                modifier = Modifier.graphicsLayer {
                    alpha = word
                    translationY = (1f - word) * 10f
                }
            )
        }
    }
}

@Composable
private fun NexusIcon(
    modifier: Modifier,
    spinnerAlpha: Float,
    spinnerScale: Float,
    spinnerRotation: Float,
    dotProgress: FloatArray,
    centerDot: Float,
    lineProgress: FloatArray,
    connectedAlpha: Float,
    connectedScale: Float,
    monogramAlpha: Float,
    monogramScale: Float,
    flashAlpha: Float
) {
    Canvas(modifier = modifier) {
        val frame = size.minDimension
        val scale = frame / 160f
        val stroke6 = 6f * scale
        val stroke20 = 20f * scale
        val corner = 31f * scale
        val inset = 1f * scale
        val center = Offset(size.width / 2f, size.height / 2f)

        drawRoundRect(
            color = BrandIconBg,
            topLeft = Offset(inset, inset),
            size = Size(size.width - inset * 2, size.height - inset * 2),
            cornerRadius = CornerRadius(corner, corner)
        )
        drawRoundRect(
            color = BrandBorder,
            topLeft = Offset(inset, inset),
            size = Size(size.width - inset * 2, size.height - inset * 2),
            cornerRadius = CornerRadius(corner, corner),
            style = Stroke(width = scale)
        )

        val breathe = 0.1f + (0.1f * (sin(lineProgress[1] * PI).toFloat() + 1f) / 2f)
        drawCircle(
            brush = Brush.radialGradient(
                colorStops = arrayOf(0f to BrandGlow.copy(alpha = 0.45f), 1f to Color.Transparent),
                center = center,
                radius = 66f * scale
            ),
            center = center,
            radius = 66f * scale,
            alpha = breathe
        )

        val grad = Brush.linearGradient(listOf(BrandPurple, BrandGreen), start = Offset.Zero, end = Offset(size.width, size.height))
        val p30 = 30f * scale
        val p130 = 130f * scale

        drawIntoCenter(center, connectedScale) {
            drawProgressLine(Offset(p30, p30), Offset(p30, p130), lineProgress[0], BrandGreen, stroke6, connectedAlpha)
            drawProgressLine(Offset(p30, p30), Offset(p130, p130), lineProgress[1], grad, stroke6, connectedAlpha)
            drawProgressLine(Offset(p130, p30), Offset(p130, p130), lineProgress[2], BrandPurple, stroke6, connectedAlpha)

            drawDot(dotProgress[0], Offset(50f * scale, 50f * scale), BrandPurple, 9f * scale, connectedAlpha)
            drawDot(dotProgress[1], Offset(50f * scale, -50f * scale), BrandGreen, 9f * scale, connectedAlpha)
            drawDot(dotProgress[2], Offset(-50f * scale, -50f * scale), BrandPurple, 9f * scale, connectedAlpha)
            drawDot(dotProgress[3], Offset(-50f * scale, 50f * scale), BrandGreen, 9f * scale, connectedAlpha)

            if (spinnerAlpha > 0f) {
                drawArc(
                    brush = grad,
                    startAngle = spinnerRotation,
                    sweepAngle = 100f,
                    useCenter = false,
                    topLeft = Offset(center.x - (14f * scale * spinnerScale), center.y - (14f * scale * spinnerScale)),
                    size = Size(28f * scale * spinnerScale, 28f * scale * spinnerScale),
                    alpha = spinnerAlpha * connectedAlpha,
                    style = Stroke(width = 3f * scale, cap = StrokeCap.Round)
                )
            }

            if (centerDot > 0f) {
                drawCircle(
                    brush = grad,
                    center = center,
                    radius = (9f * scale) * centerDot,
                    alpha = centerDot * connectedAlpha
                )
            }
        }

        drawIntoCenter(center, monogramScale) {
            drawRoundRect(
                color = BrandPurple,
                topLeft = Offset(30f * scale, 30f * scale),
                size = Size(20f * scale, 100f * scale),
                cornerRadius = CornerRadius(6f * scale, 6f * scale),
                alpha = monogramAlpha
            )
            drawRoundRect(
                color = BrandGreen,
                topLeft = Offset(110f * scale, 30f * scale),
                size = Size(20f * scale, 100f * scale),
                cornerRadius = CornerRadius(6f * scale, 6f * scale),
                alpha = monogramAlpha
            )
            drawLine(
                brush = grad,
                start = Offset(40f * scale, 30f * scale),
                end = Offset(120f * scale, 130f * scale),
                strokeWidth = stroke20,
                cap = StrokeCap.Round,
                alpha = monogramAlpha
            )
        }

        if (flashAlpha > 0f) {
            drawCircle(
                color = Color.White,
                center = center,
                radius = 100f * scale,
                alpha = flashAlpha
            )
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawProgressLine(
    start: Offset,
    end: Offset,
    progress: Float,
    color: Color,
    width: Float,
    alpha: Float
) {
    if (progress <= 0f) return
    val p = progress.coerceIn(0f, 1f)
    val point = Offset(
        x = start.x + (end.x - start.x) * p,
        y = start.y + (end.y - start.y) * p
    )
    drawLine(
        color = color,
        start = start,
        end = point,
        strokeWidth = width,
        cap = StrokeCap.Round,
        alpha = alpha
    )
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawProgressLine(
    start: Offset,
    end: Offset,
    progress: Float,
    brush: Brush,
    width: Float,
    alpha: Float
) {
    if (progress <= 0f) return
    val p = progress.coerceIn(0f, 1f)
    val point = Offset(
        x = start.x + (end.x - start.x) * p,
        y = start.y + (end.y - start.y) * p
    )
    drawLine(
        brush = brush,
        start = start,
        end = point,
        strokeWidth = width,
        cap = StrokeCap.Round,
        alpha = alpha
    )
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawDot(
    progress: Float,
    targetOffset: Offset,
    color: Color,
    radius: Float,
    alphaMultiplier: Float
) {
    if (progress <= 0f) return
    val p = progress.coerceIn(0f, 1f)
    val center = Offset(size.width / 2f + targetOffset.x * p, size.height / 2f + targetOffset.y * p)
    drawCircle(color = color, center = center, radius = radius, alpha = p * alphaMultiplier)
}

private inline fun androidx.compose.ui.graphics.drawscope.DrawScope.drawIntoCenter(
    center: Offset,
    scale: Float,
    crossinline block: androidx.compose.ui.graphics.drawscope.DrawScope.() -> Unit
) {
    withTransform({
        translate(left = center.x * (1f - scale), top = center.y * (1f - scale))
        scale(scaleX = scale, scaleY = scale)
    }) {
        block()
    }
}

private fun segment(time: Float, start: Float, end: Float): Float {
    if (time <= start) return 0f
    if (time >= end) return 1f
    return (time - start) / (end - start)
}

private fun eased(value: Float): Float = FastOutSlowInEasing.transform(value.coerceIn(0f, 1f))

private fun triangularPulse(time: Float, start: Float, end: Float, peak: Float): Float {
    if (time <= start || time >= end) return 0f
    return if (time < peak) {
        ((time - start) / (peak - start)).coerceIn(0f, 1f)
    } else {
        (1f - (time - peak) / (end - peak)).coerceIn(0f, 1f)
    }
}


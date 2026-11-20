package com.example.ui.onboarding

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun OnboardingRingGraphic(modifier: Modifier = Modifier) {
    val amber = MaterialTheme.colorScheme.primary
    val border = MaterialTheme.colorScheme.outline
    val surface = MaterialTheme.colorScheme.surface
    val bone = MaterialTheme.colorScheme.onBackground

    val transition = rememberInfiniteTransition(label = "ring")
    val sweepOffset by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 16000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "sweep"
    )

    Box(modifier = modifier.size(220.dp), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val outerR = size.minDimension * 0.44f
            val midR = size.minDimension * 0.33f
            val innerR = size.minDimension * 0.21f

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(amber.copy(alpha = 0.14f), Color.Transparent),
                    center = center,
                    radius = outerR * 1.15f
                ),
                radius = outerR * 1.15f,
                center = center
            )

            // Precision dial ticks around outer perimeter
            for (i in 0 until 36) {
                val angle = Math.toRadians((i * 10).toDouble())
                val isMajor = i % 3 == 0
                val startR = if (isMajor) outerR - 10.dp.toPx() else outerR - 5.dp.toPx()
                val start = Offset(
                    center.x + (startR * cos(angle)).toFloat(),
                    center.y + (startR * sin(angle)).toFloat()
                )
                val end = Offset(
                    center.x + (outerR * cos(angle)).toFloat(),
                    center.y + (outerR * sin(angle)).toFloat()
                )
                drawLine(
                    color = if (isMajor) amber.copy(alpha = 0.65f) else border,
                    start = start,
                    end = end,
                    strokeWidth = if (isMajor) 2.dp.toPx() else 1.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }

            drawCircle(
                color = border,
                radius = midR,
                center = center,
                style = Stroke(width = 1.5.dp.toPx())
            )

            drawArc(
                color = amber,
                startAngle = sweepOffset - 90f,
                sweepAngle = 220f,
                useCenter = false,
                topLeft = Offset(center.x - midR, center.y - midR),
                size = Size(midR * 2, midR * 2),
                style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
            )

            drawCircle(
                color = surface,
                radius = innerR,
                center = center
            )
            drawCircle(
                color = border,
                radius = innerR,
                center = center,
                style = Stroke(width = 1.dp.toPx())
            )

            // Minimal geometric lock glyph at center
            val bodyW = innerR * 0.72f
            val bodyH = innerR * 0.54f
            val shackleR = bodyW * 0.34f
            drawArc(
                color = bone,
                startAngle = 180f,
                sweepAngle = 180f,
                useCenter = false,
                topLeft = Offset(center.x - shackleR, center.y - bodyH * 0.85f),
                size = Size(shackleR * 2, shackleR * 2),
                style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
            )
            drawRoundRect(
                color = amber,
                topLeft = Offset(center.x - bodyW / 2f, center.y - bodyH * 0.25f),
                size = Size(bodyW, bodyH),
                cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
            )
        }
    }
}

@Composable
fun OnboardingSealGraphic(modifier: Modifier = Modifier) {
    val amber = MaterialTheme.colorScheme.primary
    val border = MaterialTheme.colorScheme.outline
    val surface = MaterialTheme.colorScheme.surface
    val plate = MaterialTheme.colorScheme.surfaceVariant
    val bone = MaterialTheme.colorScheme.onBackground

    Box(modifier = modifier.size(220.dp), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val outerR = size.minDimension * 0.42f
            val innerR = size.minDimension * 0.26f

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(amber.copy(alpha = 0.14f), Color.Transparent),
                    center = center,
                    radius = outerR * 1.15f
                ),
                radius = outerR * 1.15f,
                center = center
            )

            val outerHex = Path()
            val innerHex = Path()
            val vertices = mutableListOf<Offset>()
            for (i in 0 until 6) {
                val angle = (PI / 3.0) * i - (PI / 2.0)
                val ox = center.x + (outerR * cos(angle)).toFloat()
                val oy = center.y + (outerR * sin(angle)).toFloat()
                val ix = center.x + (innerR * cos(angle)).toFloat()
                val iy = center.y + (innerR * sin(angle)).toFloat()
                vertices.add(Offset(ox, oy))
                if (i == 0) {
                    outerHex.moveTo(ox, oy)
                    innerHex.moveTo(ix, iy)
                } else {
                    outerHex.lineTo(ox, oy)
                    innerHex.lineTo(ix, iy)
                }
            }
            outerHex.close()
            innerHex.close()

            drawPath(path = outerHex, color = surface)
            drawPath(path = outerHex, color = border, style = Stroke(width = 1.5.dp.toPx()))
            drawPath(path = innerHex, color = plate)
            drawPath(path = innerHex, color = amber.copy(alpha = 0.7f), style = Stroke(width = 1.5.dp.toPx()))

            // Radial cryptographic struts & vertex nodes
            vertices.forEachIndexed { idx, pt ->
                drawLine(
                    color = border,
                    start = center,
                    end = pt,
                    strokeWidth = 1.dp.toPx()
                )
                drawCircle(
                    color = if (idx % 2 == 0) amber else bone.copy(alpha = 0.7f),
                    radius = if (idx % 2 == 0) 4.dp.toPx() else 2.5.dp.toPx(),
                    center = pt
                )
            }

            // Center core dot
            drawCircle(color = amber, radius = 8.dp.toPx(), center = center)
            drawCircle(color = surface, radius = 3.dp.toPx(), center = center)
        }
    }
}

@Composable
fun OnboardingGateGraphic(
    isActive: Boolean,
    modifier: Modifier = Modifier
) {
    val accent = if (isActive) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary
    val border = MaterialTheme.colorScheme.outline
    val surface = MaterialTheme.colorScheme.surface
    val plate = MaterialTheme.colorScheme.surfaceVariant

    Box(modifier = modifier.size(220.dp), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val w = size.minDimension * 0.62f
            val h = size.minDimension * 0.72f
            val top = center.y - h / 2f
            val left = center.x - w / 2f
            val right = center.x + w / 2f
            val bottom = center.y + h / 2f

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(accent.copy(alpha = 0.16f), Color.Transparent),
                    center = center,
                    radius = size.minDimension * 0.48f
                ),
                radius = size.minDimension * 0.48f,
                center = center
            )

            val shieldPath = Path().apply {
                moveTo(center.x, top)
                lineTo(right, top + h * 0.18f)
                lineTo(right, top + h * 0.56f)
                quadraticTo(right, bottom - h * 0.12f, center.x, bottom)
                quadraticTo(left, bottom - h * 0.12f, left, top + h * 0.56f)
                lineTo(left, top + h * 0.18f)
                close()
            }

            drawPath(path = shieldPath, color = surface)
            drawPath(path = shieldPath, color = accent, style = Stroke(width = 2.dp.toPx()))

            // Precision gate bars inside shield
            val gateTop = center.y - 24.dp.toPx()
            val gateBottom = center.y + 24.dp.toPx()
            val barWidth = 6.dp.toPx()
            for (offsetIndex in -1..1) {
                val x = center.x + offsetIndex * 18.dp.toPx() - barWidth / 2f
                drawRoundRect(
                    color = if (offsetIndex == 0) accent else plate,
                    topLeft = Offset(x, gateTop),
                    size = Size(barWidth, gateBottom - gateTop),
                    cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx())
                )
            }

            // Horizontal gate crossbeam
            drawLine(
                color = border,
                start = Offset(left + 16.dp.toPx(), center.y),
                end = Offset(right - 16.dp.toPx(), center.y),
                strokeWidth = 2.dp.toPx(),
                cap = StrokeCap.Round
            )
            drawCircle(
                color = accent,
                radius = 5.dp.toPx(),
                center = center
            )
        }
    }
}

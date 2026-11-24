package com.auramusic.presentation.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.auramusic.presentation.theme.AuroraPrimary
import com.auramusic.presentation.theme.AuroraSecondary

/**
 * The Aurora cat. Drawn entirely with Compose Canvas, so it scales to any size without assets.
 * It bobs gently, blinks, sways its tail, wears headphones and floats music notes while a
 * track is playing.
 */
@Composable
fun CatMascot(
    modifier: Modifier = Modifier,
    isPlaying: Boolean = false,
    size: Dp = 120.dp,
    bodyColor: Color = AuroraPrimary,
    accentColor: Color = AuroraSecondary
) {
    val transition = rememberInfiniteTransition(label = "cat")
    val bob by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = if (isPlaying) 520 else 1500,
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "catBob"
    )
    val tail by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = if (isPlaying) 900 else 2200,
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "catTail"
    )
    val blink by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 3400
                0f at 0
                0f at 3050
                1f at 3140
                0f at 3230
            },
            repeatMode = RepeatMode.Restart
        ),
        label = "catBlink"
    )
    val notePhase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "catNotes"
    )

    Canvas(modifier = modifier.then(Modifier.size(size))) {
        val w = this.size.width
        val h = this.size.height
        val cx = w / 2f
        val bobOffset = (bob - 0.5f) * h * if (isPlaying) 0.045f else 0.02f
        val headR = w * 0.235f
        val headCy = h * 0.50f + bobOffset
        val bodyR = w * 0.25f
        val bodyCy = h * 0.78f + bobOffset

        // Tail
        val sway = (tail - 0.5f) * 2f
        val tailPath = Path().apply {
            moveTo(cx + bodyR * 0.65f, bodyCy)
            quadraticBezierTo(
                cx + bodyR * 1.5f,
                bodyCy - h * 0.02f + sway * h * 0.03f,
                cx + bodyR * 1.25f,
                bodyCy - h * 0.18f + sway * h * 0.06f
            )
        }
        drawPath(
            path = tailPath,
            color = bodyColor.copy(alpha = 0.85f),
            style = Stroke(width = w * 0.05f, cap = StrokeCap.Round)
        )

        // Body
        drawRoundRect(
            color = bodyColor.copy(alpha = 0.26f),
            topLeft = Offset(cx - bodyR, bodyCy - bodyR * 0.55f),
            size = Size(bodyR * 2f, bodyR * 1.45f),
            cornerRadius = CornerRadius(bodyR * 0.7f, bodyR * 0.7f)
        )
        drawRoundRect(
            color = bodyColor.copy(alpha = 0.35f),
            topLeft = Offset(cx - bodyR * 0.55f, bodyCy - bodyR * 0.1f),
            size = Size(bodyR * 1.1f, bodyR * 0.9f),
            cornerRadius = CornerRadius(bodyR * 0.45f, bodyR * 0.45f)
        )

        // Ears
        val earTipY = headCy - headR * 1.38f
        val leftEar = Path().apply {
            moveTo(cx - headR * 0.88f, headCy - headR * 0.32f)
            lineTo(cx - headR * 0.58f, earTipY)
            lineTo(cx - headR * 0.12f, headCy - headR * 0.72f)
            close()
        }
        val rightEar = Path().apply {
            moveTo(cx + headR * 0.88f, headCy - headR * 0.32f)
            lineTo(cx + headR * 0.58f, earTipY)
            lineTo(cx + headR * 0.12f, headCy - headR * 0.72f)
            close()
        }
        drawPath(leftEar, bodyColor)
        drawPath(rightEar, bodyColor)
        val innerLeft = Path().apply {
            moveTo(cx - headR * 0.68f, headCy - headR * 0.45f)
            lineTo(cx - headR * 0.56f, headCy - headR * 1.08f)
            lineTo(cx - headR * 0.3f, headCy - headR * 0.68f)
            close()
        }
        val innerRight = Path().apply {
            moveTo(cx + headR * 0.68f, headCy - headR * 0.45f)
            lineTo(cx + headR * 0.56f, headCy - headR * 1.08f)
            lineTo(cx + headR * 0.3f, headCy - headR * 0.68f)
            close()
        }
        drawPath(innerLeft, Color(0xFFFFA7C4).copy(alpha = 0.75f))
        drawPath(innerRight, Color(0xFFFFA7C4).copy(alpha = 0.75f))

        // Head
        drawCircle(color = bodyColor, radius = headR, center = Offset(cx, headCy))

        // Eyes
        val eyeY = headCy + headR * 0.04f
        val eyeDx = headR * 0.42f
        val ink = Color(0xFF20142E)
        if (blink > 0.5f) {
            drawLine(
                color = ink,
                start = Offset(cx - eyeDx - headR * 0.12f, eyeY),
                end = Offset(cx - eyeDx + headR * 0.12f, eyeY),
                strokeWidth = w * 0.012f,
                cap = StrokeCap.Round
            )
            drawLine(
                color = ink,
                start = Offset(cx + eyeDx - headR * 0.12f, eyeY),
                end = Offset(cx + eyeDx + headR * 0.12f, eyeY),
                strokeWidth = w * 0.012f,
                cap = StrokeCap.Round
            )
        } else {
            drawCircle(ink, radius = headR * 0.135f, center = Offset(cx - eyeDx, eyeY))
            drawCircle(ink, radius = headR * 0.135f, center = Offset(cx + eyeDx, eyeY))
            drawCircle(
                Color.White,
                radius = headR * 0.045f,
                center = Offset(cx - eyeDx + headR * 0.045f, eyeY - headR * 0.045f)
            )
            drawCircle(
                Color.White,
                radius = headR * 0.045f,
                center = Offset(cx + eyeDx + headR * 0.045f, eyeY - headR * 0.045f)
            )
        }

        // Nose
        val nose = Path().apply {
            moveTo(cx - headR * 0.07f, headCy + headR * 0.32f)
            lineTo(cx + headR * 0.07f, headCy + headR * 0.32f)
            lineTo(cx, headCy + headR * 0.43f)
            close()
        }
        drawPath(nose, Color(0xFFFFA7C4))

        // Whiskers
        val whiskerColor = bodyColor.copy(alpha = 0.65f)
        repeat(2) { side ->
            val dir = if (side == 0) -1f else 1f
            repeat(2) { line ->
                val startY = headCy + headR * (0.28f + line * 0.14f)
                drawLine(
                    color = whiskerColor,
                    start = Offset(cx + dir * headR * 0.35f, startY),
                    end = Offset(cx + dir * headR * 0.95f, startY - headR * 0.08f + line * headR * 0.12f),
                    strokeWidth = w * 0.009f,
                    cap = StrokeCap.Round
                )
            }
        }

        if (isPlaying) {
            // Headphones
            drawArc(
                color = accentColor,
                startAngle = 195f,
                sweepAngle = 150f,
                useCenter = false,
                topLeft = Offset(cx - headR * 1.08f, headCy - headR * 1.12f),
                size = Size(headR * 2.16f, headR * 2.16f),
                style = Stroke(width = w * 0.045f, cap = StrokeCap.Round)
            )
            drawRoundRect(
                color = accentColor,
                topLeft = Offset(cx - headR * 1.16f, headCy - headR * 0.28f),
                size = Size(headR * 0.32f, headR * 0.72f),
                cornerRadius = CornerRadius(headR * 0.16f)
            )
            drawRoundRect(
                color = accentColor,
                topLeft = Offset(cx + headR * 0.84f, headCy - headR * 0.28f),
                size = Size(headR * 0.32f, headR * 0.72f),
                cornerRadius = CornerRadius(headR * 0.16f)
            )

            // Floating notes
            repeat(3) { index ->
                val progress = ((notePhase + index / 3f) % 1f)
                val dir = if (index % 2 == 0) -1f else 1f
                val x = cx + dir * w * (0.31f + index * 0.035f)
                val y = headCy - headR * 1.25f - progress * h * 0.18f
                val alpha = (1f - progress).coerceIn(0f, 1f) * 0.9f
                val noteColor = accentColor.copy(alpha = alpha)
                drawCircle(noteColor, radius = w * 0.017f, center = Offset(x, y))
                drawLine(
                    color = noteColor,
                    start = Offset(x + w * 0.016f, y),
                    end = Offset(x + w * 0.016f, y - h * 0.05f),
                    strokeWidth = w * 0.008f,
                    cap = StrokeCap.Round
                )
            }
        }
    }
}

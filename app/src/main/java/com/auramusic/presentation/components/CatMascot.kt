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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
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
import kotlinx.coroutines.delay

/**
 * Aurora the cat. Drawn entirely with Compose Canvas, so it scales to any size without assets.
 * It bobs, blinks, sways its striped tail, waves its paws, wears headphones while playing and
 * floats music notes. Tap it and it purrs out hearts.
 */
@Composable
fun CatMascot(
    modifier: Modifier = Modifier,
    isPlaying: Boolean = false,
    size: Dp = 120.dp,
    bodyColor: Color = AuroraPrimary,
    accentColor: Color = AuroraSecondary,
    onClick: (() -> Unit)? = null
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

    var happy by remember { mutableStateOf(false) }
    LaunchedEffect(happy) {
        if (happy) {
            delay(1400)
            happy = false
        }
    }

    Box(
        modifier = modifier
            .then(Modifier.size(size))
            .clickable(enabled = onClick != null) {
                happy = true
                onClick?.invoke()
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val w = this.size.width
            val h = this.size.height
            if (w <= 0f || h <= 0f) return@Canvas
            val cx = w / 2f
            val bobOffset = (bob - 0.5f) * h * if (isPlaying) 0.045f else 0.02f
            val headR = w * 0.235f
            val headCy = h * 0.50f + bobOffset
            val bodyR = w * 0.25f
            val bodyCy = h * 0.78f + bobOffset
            val ink = Color(0xFF20142E)

            // Tail with stripes
            val sway = (tail - 0.5f) * 2f
            val tailEndX = cx + bodyR * 1.25f
            val tailEndY = bodyCy - h * 0.18f + sway * h * 0.06f
            val tailPath = Path().apply {
                moveTo(cx + bodyR * 0.65f, bodyCy)
                quadraticBezierTo(
                    cx + bodyR * 1.5f,
                    bodyCy - h * 0.02f + sway * h * 0.03f,
                    tailEndX,
                    tailEndY
                )
            }
            drawPath(
                path = tailPath,
                color = bodyColor.copy(alpha = 0.85f),
                style = Stroke(width = w * 0.05f, cap = StrokeCap.Round)
            )
            drawCircle(
                color = bodyColor.copy(alpha = 0.96f),
                radius = w * 0.026f,
                center = Offset(tailEndX, tailEndY)
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

            // Waving paws
            val pawLift = if (isPlaying) (bob - 0.5f) * h * 0.035f else 0f
            drawCircle(
                color = bodyColor.copy(alpha = 0.92f),
                radius = bodyR * 0.30f,
                center = Offset(cx - bodyR * 0.6f, bodyCy + bodyR * 0.32f - pawLift)
            )
            drawCircle(
                color = bodyColor.copy(alpha = 0.92f),
                radius = bodyR * 0.30f,
                center = Offset(cx + bodyR * 0.6f, bodyCy + bodyR * 0.32f + pawLift)
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

            // Blush
            val eyeY = headCy + headR * 0.04f
            val eyeDx = headR * 0.42f
            drawCircle(
                color = Color(0xFFFF7B8B).copy(alpha = 0.45f),
                radius = headR * 0.17f,
                center = Offset(cx - headR * 0.72f, eyeY + headR * 0.24f)
            )
            drawCircle(
                color = Color(0xFFFF7B8B).copy(alpha = 0.45f),
                radius = headR * 0.17f,
                center = Offset(cx + headR * 0.72f, eyeY + headR * 0.24f)
            )

            // Eyes
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

            // Nose and little smile
            val nose = Path().apply {
                moveTo(cx - headR * 0.07f, headCy + headR * 0.32f)
                lineTo(cx + headR * 0.07f, headCy + headR * 0.32f)
                lineTo(cx, headCy + headR * 0.43f)
                close()
            }
            drawPath(nose, Color(0xFFFFA7C4))
            val mouthY = headCy + headR * 0.46f
            val mouth = Path().apply {
                moveTo(cx - headR * 0.09f, mouthY)
                quadraticBezierTo(cx - headR * 0.045f, mouthY + headR * 0.13f, cx, mouthY)
                quadraticBezierTo(cx + headR * 0.045f, mouthY + headR * 0.13f, cx + headR * 0.09f, mouthY)
            }
            drawPath(
                mouth,
                ink,
                style = Stroke(width = w * 0.008f, cap = StrokeCap.Round)
            )

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

                // Sparkles
                repeat(3) { index ->
                    val angle = notePhase * 6.28f + index * 2.09f
                    val r = w * 0.32f
                    val x = cx + kotlin.math.cos(angle) * r
                    val y = headCy + kotlin.math.sin(angle) * r * 0.7f
                    val spark = Color.White.copy(alpha = 0.55f)
                    drawLine(
                        spark,
                        Offset(x - w * 0.015f, y),
                        Offset(x + w * 0.015f, y),
                        strokeWidth = w * 0.006f,
                        cap = StrokeCap.Round
                    )
                    drawLine(
                        spark,
                        Offset(x, y - w * 0.015f),
                        Offset(x, y + w * 0.015f),
                        strokeWidth = w * 0.006f,
                        cap = StrokeCap.Round
                    )
                }
            }

            // Purr hearts
            if (happy) {
                repeat(3) { index ->
                    val progress = ((notePhase + index / 3f) % 1f)
                    val x = cx + (index - 1) * w * 0.2f
                    val y = headCy - headR * 1.5f - progress * h * 0.2f
                    val s = w * 0.04f
                    val heart = Path().apply {
                        moveTo(x, y + s * 0.35f)
                        cubicTo(x - s * 1.1f, y - s * 0.35f, x - s * 0.45f, y - s, x, y - s * 0.45f)
                        cubicTo(x + s * 0.45f, y - s, x + s * 1.1f, y - s * 0.35f, x, y + s * 0.35f)
                        close()
                    }
                    drawPath(
                        heart,
                        Color(0xFFFF7B8B).copy(alpha = (1f - progress).coerceIn(0f, 1f))
                    )
                }
            }
        }
    }
}

package com.auramusic.presentation.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.auramusic.presentation.theme.AuroraBackground
import com.auramusic.presentation.theme.AuroraPrimary
import com.auramusic.presentation.theme.AuroraSecondary

/** Soft moving aurora light behind the app content. */
@Composable
fun AuroraBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val transition = rememberInfiniteTransition(label = "aurora")
    val drift by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 9000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "drift"
    )
    val driftTwo by transition.animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 12000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "driftTwo"
    )

    Box(modifier = modifier.fillMaxSize().background(AuroraBackground)) {
        Canvas(Modifier.fillMaxSize()) {
            val radiusOne = size.minDimension * 0.85f
            val centerOne = Offset(
                size.width * (0.18f + drift * 0.28f),
                size.height * (0.12f + drift * 0.12f)
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(AuroraPrimary.copy(alpha = 0.20f), Color.Transparent),
                    center = centerOne,
                    radius = radiusOne
                ),
                radius = radiusOne,
                center = centerOne
            )

            val radiusTwo = size.minDimension * 0.7f
            val centerTwo = Offset(
                size.width * (0.9f - driftTwo * 0.35f),
                size.height * (0.68f + driftTwo * 0.12f)
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(AuroraSecondary.copy(alpha = 0.13f), Color.Transparent),
                    center = centerTwo,
                    radius = radiusTwo
                ),
                radius = radiusTwo,
                center = centerTwo
            )

            val radiusThree = size.minDimension * 0.55f
            val centerThree = Offset(
                size.width * (0.35f + driftTwo * 0.4f),
                size.height * (0.95f - drift * 0.15f)
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFFFFA7C4).copy(alpha = 0.09f), Color.Transparent),
                    center = centerThree,
                    radius = radiusThree
                ),
                radius = radiusThree,
                center = centerThree
            )
        }
        content()
    }
}

/** Small animated equalizer, used in the hero, mini player and full player. */
@Composable
fun EqualizerBars(
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
    color: Color = AuroraSecondary,
    barCount: Int = 4
) {
    val transition = rememberInfiniteTransition(label = "equalizer")
    val levels = (0 until barCount).map { index ->
        transition.animateFloat(
            initialValue = 0.25f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(
                    durationMillis = 380 + index * 120,
                    easing = FastOutSlowInEasing
                ),
                repeatMode = RepeatMode.Reverse
            ),
            label = "bar$index"
        )
    }
    Canvas(modifier) {
        if (size.width <= 0f || size.height <= 0f) return@Canvas
        val gap = size.width * 0.16f
        val barWidth = (size.width - gap * (barCount - 1)) / barCount
        levels.forEachIndexed { index, level ->
            val fraction = if (isPlaying) level.value else 0.16f
            val barHeight = (size.height * fraction).coerceAtLeast(barWidth)
            drawRoundRect(
                color = color.copy(alpha = if (isPlaying) 0.9f else 0.3f),
                topLeft = Offset(index * (barWidth + gap), size.height - barHeight),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(barWidth / 2f)
            )
        }
    }
}

/** Album art that gently breathes while playing. */
@Composable
fun PulsingArtwork(
    url: String?,
    size: Dp,
    radius: Dp = 28.dp,
    isPlaying: Boolean
) {
    val transition = rememberInfiniteTransition(label = "artwork")
    val pulse by transition.animateFloat(
        initialValue = 1f,
        targetValue = 1.03f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )
    val scale = if (isPlaying) pulse else 1f
    Box(
        modifier = Modifier
            .size(size)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .shadow(
                elevation = 28.dp,
                shape = RoundedCornerShape(radius),
                ambientColor = AuroraPrimary,
                spotColor = AuroraPrimary
            )
            .clip(RoundedCornerShape(radius))
    ) {
        Artwork(url, size, radius)
    }
}

/** Home hero card: animated gradient, greeting, equalizer and the cat. */
@Composable
fun HomeHero(
    isPlaying: Boolean,
    modifier: Modifier = Modifier
) {
    val transition = rememberInfiniteTransition(label = "hero")
    val shift by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 7000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "heroShift"
    )
    val shape = RoundedCornerShape(28.dp)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .shadow(
                elevation = 24.dp,
                shape = shape,
                ambientColor = AuroraPrimary,
                spotColor = AuroraPrimary
            )
            .clip(shape)
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        AuroraPrimary.copy(alpha = 0.30f),
                        AuroraSecondary.copy(alpha = 0.14f),
                        Color(0xFF1A1226)
                    ),
                    start = Offset(0f, 0f),
                    end = Offset(700f + shift * 400f, 600f)
                )
            )
            .border(1.dp, Color.White.copy(alpha = 0.08f), shape)
            .padding(horizontal = 20.dp, vertical = 18.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Aurora Music", style = MaterialTheme.typography.headlineMedium)
                Spacer(Modifier.height(2.dp))
                Text(
                    "Unlimited. Ad-free. Yours.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = AuroraSecondary
                )
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    EqualizerBars(
                        isPlaying = isPlaying,
                        modifier = Modifier.size(width = 24.dp, height = 16.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        if (isPlaying) "Now playing" else "Tap a track to start",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            CatMascot(
                modifier = Modifier.size(112.dp),
                isPlaying = isPlaying
            )
        }
    }
}

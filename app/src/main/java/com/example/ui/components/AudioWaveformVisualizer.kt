package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.ui.theme.AmberGlow
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.MagentaGlow
import com.example.ui.theme.NeonPurple

@Composable
fun AudioWaveformVisualizer(
    amplitudes: FloatArray,
    isPlaying: Boolean,
    modifier: Modifier = Modifier
) {
    val pulse = remember { Animatable(0.5f) }

    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            pulse.animateTo(
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(600, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                )
            )
        } else {
            pulse.snapTo(0.2f)
        }
    }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
    ) {
        val count = amplitudes.size.coerceAtLeast(1)
        val barWidth = (size.width / (count * 1.6f)).coerceIn(4f, 16f)
        val spacing = (size.width - (count * barWidth)) / (count + 1)

        val centerY = size.height / 2f

        for (i in 0 until count) {
            val amp = if (isPlaying) {
                (amplitudes.getOrElse(i) { 0.2f } * pulse.value).coerceIn(0.1f, 1f)
            } else {
                0.1f
            }

            val barHeight = (size.height * 0.9f * amp).coerceAtLeast(6f)
            val left = spacing + i * (barWidth + spacing)
            val top = centerY - (barHeight / 2f)

            val color = when (i % 3) {
                0 -> ElectricCyan
                1 -> NeonPurple
                else -> MagentaGlow
            }

            drawRoundRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        color.copy(alpha = 0.9f),
                        AmberGlow.copy(alpha = 0.8f)
                    ),
                    startY = top,
                    endY = top + barHeight
                ),
                topLeft = Offset(left, top),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
            )
        }
    }
}

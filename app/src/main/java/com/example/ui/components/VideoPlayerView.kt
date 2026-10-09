package com.example.ui.components

import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CropFree
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Badge
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.service.VideoAiResult
import com.example.ui.theme.AmberGlow
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.DeepSurface
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.MagentaGlow
import com.example.ui.theme.NeonPurple
import com.example.ui.theme.VoidBlack
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin

enum class VideoAspectRatio(val ratio: Float, val label: String) {
    LANDSCAPE_16_9(16f / 9f, "16:9"),
    PORTRAIT_9_16(9f / 16f, "9:16"),
    SQUARE_1_1(1f, "1:1"),
    CINEMATIC_21_9(21f / 9f, "21:9")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoPlayerView(
    videoResult: VideoAiResult,
    imageUri: Uri?,
    isPlaying: Boolean,
    waveformAmplitudes: FloatArray,
    onPlayPauseToggle: () -> Unit,
    onSeek: (Float) -> Unit,
    onMuteToggle: () -> Unit,
    isMuted: Boolean,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var currentProgress by remember { mutableFloatStateOf(0f) }
    var currentSeconds by remember { mutableFloatStateOf(0f) }
    var showControls by remember { mutableStateOf(true) }
    var isLooping by remember { mutableStateOf(true) }
    var isFullscreen by remember { mutableStateOf(false) }
    var selectedAspectRatio by remember { mutableStateOf(VideoAspectRatio.LANDSCAPE_16_9) }

    // Video playback timer
    val effectiveDurationSec = videoResult.durationSeconds.coerceAtLeast(5L).toFloat()

    LaunchedEffect(isPlaying, isLooping) {
        while (isPlaying) {
            delay(50)
            currentSeconds += 0.05f
            if (currentSeconds >= effectiveDurationSec) {
                if (isLooping || videoResult.isInfinite) {
                    currentSeconds = 0f
                } else {
                    currentSeconds = effectiveDurationSec
                    onPlayPauseToggle()
                }
            }
            currentProgress = (currentSeconds / effectiveDurationSec).coerceIn(0f, 1f)
        }
    }

    // Auto-hide controls after 4 seconds
    LaunchedEffect(showControls, isPlaying) {
        if (showControls && isPlaying) {
            delay(4000)
            showControls = false
        }
    }

    // Calculate camera animation based on progress and cameraMotion
    val progressNorm = currentProgress
    val scale = when (videoResult.cameraMotion) {
        "Parallax Zoom-In 3D" -> 1.0f + 0.25f * progressNorm
        "Orbital Drone Flythrough" -> 1.15f - 0.15f * sin(progressNorm * Math.PI.toFloat())
        "Hyperdrive Dolly" -> 1.0f + 0.4f * progressNorm
        else -> 1.05f + 0.1f * sin(progressNorm * Math.PI.toFloat())
    }

    val translationX = when (videoResult.cameraMotion) {
        "Cinematic Pan" -> -40f + 80f * progressNorm
        "Orbital Drone Flythrough" -> 35f * sin(progressNorm * 2 * Math.PI.toFloat())
        else -> 15f * sin(progressNorm * Math.PI.toFloat())
    }

    val translationY = when (videoResult.cameraMotion) {
        "Parallax Zoom-In 3D" -> -20f * progressNorm
        "Orbital Drone Flythrough" -> 15f * cos(progressNorm * 2 * Math.PI.toFloat())
        else -> 0f
    }

    val rotZ = when (videoResult.cameraMotion) {
        "Orbital Drone Flythrough" -> 3f * sin(progressNorm * 2 * Math.PI.toFloat())
        else -> 0f
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(VoidBlack)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                showControls = !showControls
            }
            .testTag("video_player_container"),
        contentAlignment = Alignment.Center
    ) {
        // Video Render Canvas / Image Display
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(selectedAspectRatio.ratio)
                .clip(RoundedCornerShape(16.dp))
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            if (imageUri != null) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(imageUri)
                        .crossfade(true)
                        .build(),
                    contentDescription = "Fotograma animado de video AI",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                            this.translationX = translationX
                            this.translationY = translationY
                            this.rotationZ = rotZ
                        }
                )
            } else {
                // Procedural Cinematic Canvas scene
                ProceduralCinematicScene(
                    progress = currentProgress,
                    mood = videoResult.mood,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                            this.translationX = translationX
                            this.translationY = translationY
                        }
                )
            }

            // Atmospheric particle overlay & cinematic light sweep
            AtmosphericParticlesOverlay(
                progress = currentProgress,
                visualFx = videoResult.visualFx,
                modifier = Modifier.fillMaxSize()
            )

            // Live Audio Waveform bar over video bottom
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 24.dp)
            ) {
                AudioWaveformVisualizer(
                    amplitudes = waveformAmplitudes,
                    isPlaying = isPlaying,
                    modifier = Modifier.fillMaxWidth(0.9f).align(Alignment.Center)
                )
            }
        }

        // Top Badges (AI Model + Sound indicator)
        Row(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = VoidBlack.copy(alpha = 0.75f),
                border = androidx.compose.foundation.BorderStroke(1.dp, ElectricCyan.copy(alpha = 0.6f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .background(if (isPlaying) ElectricCyan else Color.Gray, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Google Video AI",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = VoidBlack.copy(alpha = 0.75f),
                border = androidx.compose.foundation.BorderStroke(1.dp, NeonPurple.copy(alpha = 0.6f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (isMuted) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                        contentDescription = "Audio activo",
                        tint = AmberGlow,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = videoResult.audioMode,
                        fontSize = 10.sp,
                        color = AmberGlow,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        // Top Right Controls (Aspect Ratio & Mute)
        Row(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Aspect ratio toggle
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = VoidBlack.copy(alpha = 0.75f),
                modifier = Modifier.clickable {
                    selectedAspectRatio = when (selectedAspectRatio) {
                        VideoAspectRatio.LANDSCAPE_16_9 -> VideoAspectRatio.PORTRAIT_9_16
                        VideoAspectRatio.PORTRAIT_9_16 -> VideoAspectRatio.SQUARE_1_1
                        VideoAspectRatio.SQUARE_1_1 -> VideoAspectRatio.CINEMATIC_21_9
                        VideoAspectRatio.CINEMATIC_21_9 -> VideoAspectRatio.LANDSCAPE_16_9
                    }
                }
            ) {
                Text(
                    text = selectedAspectRatio.label,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = ElectricCyan,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                )
            }

            IconButton(
                onClick = onMuteToggle,
                modifier = Modifier
                    .size(36.dp)
                    .background(VoidBlack.copy(alpha = 0.75f), CircleShape)
                    .testTag("mute_toggle_button")
            ) {
                Icon(
                    imageVector = if (isMuted) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                    contentDescription = if (isMuted) "Activar sonido" else "Silenciar",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        // Center Play / Pause Big Button when paused or tapped
        AnimatedVisibility(
            visible = showControls || !isPlaying,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            IconButton(
                onClick = onPlayPauseToggle,
                modifier = Modifier
                    .size(64.dp)
                    .background(ElectricCyan.copy(alpha = 0.9f), CircleShape)
                    .testTag("center_play_button")
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isPlaying) "Pausar video" else "Reproducir video",
                    tint = VoidBlack,
                    modifier = Modifier.size(36.dp)
                )
            }
        }

        // Bottom Controls Bar (Scrubber, Time, Loop, Aspect Ratio)
        AnimatedVisibility(
            visible = showControls,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, VoidBlack.copy(alpha = 0.95f))
                        )
                    )
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                // Scrubber Bar
                Slider(
                    value = currentProgress,
                    onValueChange = { newVal ->
                        currentProgress = newVal
                        currentSeconds = newVal * effectiveDurationSec
                        onSeek(newVal)
                    },
                    colors = SliderDefaults.colors(
                        thumbColor = ElectricCyan,
                        activeTrackColor = ElectricCyan,
                        inactiveTrackColor = BorderSubtle
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(28.dp)
                        .testTag("video_progress_slider")
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        IconButton(
                            onClick = onPlayPauseToggle,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = "Play/Pausa",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        IconButton(
                            onClick = {
                                currentProgress = 0f
                                currentSeconds = 0f
                                onSeek(0f)
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Replay,
                                contentDescription = "Reiniciar",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        // Timecode
                        val currMin = (currentSeconds / 60).toInt()
                        val currSec = (currentSeconds % 60).toInt()
                        val timeString = if (videoResult.isInfinite) {
                            String.format("%02d:%02d / %s", currMin, currSec, videoResult.durationText)
                        } else {
                            val totalMin = (effectiveDurationSec / 60).toInt()
                            val totalSec = (effectiveDurationSec % 60).toInt()
                            String.format("%02d:%02d / %02d:%02d", currMin, currSec, totalMin, totalSec)
                        }

                        Text(
                            text = timeString,
                            fontSize = 11.sp,
                            color = Color.White,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Loop button
                        IconButton(
                            onClick = { isLooping = !isLooping },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Repeat,
                                contentDescription = "Repetir en bucle",
                                tint = if (isLooping) ElectricCyan else Color.Gray,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ProceduralCinematicScene(
    progress: Float,
    mood: String,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        // Background cinematic atmosphere gradient
        val bgBrush = when (mood) {
            "Cyberpunk" -> Brush.radialGradient(
                colors = listOf(NeonPurple, DeepSurface, VoidBlack),
                center = Offset(w * 0.5f, h * 0.4f),
                radius = w * 0.8f
            )
            "Nature" -> Brush.radialGradient(
                colors = listOf(Color(0xFF0F766E), Color(0xFF064E3B), VoidBlack),
                center = Offset(w * 0.5f, h * 0.6f),
                radius = w * 0.9f
            )
            "Epic" -> Brush.radialGradient(
                colors = listOf(Color(0xFF4338CA), Color(0xFF1E1B4B), VoidBlack),
                center = Offset(w * 0.5f, h * 0.3f),
                radius = w * 0.85f
            )
            else -> Brush.radialGradient(
                colors = listOf(ElectricCyan.copy(alpha = 0.4f), DeepSurface, VoidBlack),
                center = Offset(w * 0.5f, h * 0.5f),
                radius = w * 0.8f
            )
        }

        drawRect(brush = bgBrush)

        // Draw animated geometric grid or horizon
        val horizonY = h * 0.65f
        val lineCount = 12
        for (i in 0..lineCount) {
            val yOffset = horizonY + (h - horizonY) * ((i + progress) % lineCount) / lineCount
            drawLine(
                color = ElectricCyan.copy(alpha = 0.15f * (yOffset / h)),
                start = Offset(0f, yOffset),
                end = Offset(w, yOffset),
                strokeWidth = 1.5f
            )
        }

        // Sun / Energy Core
        val coreX = w * 0.5f
        val coreY = horizonY - 40f
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(AmberGlow, MagentaGlow.copy(alpha = 0.5f), Color.Transparent),
                center = Offset(coreX, coreY),
                radius = 120f
            ),
            radius = 120f,
            center = Offset(coreX, coreY)
        )
    }
}

@Composable
fun AtmosphericParticlesOverlay(
    progress: Float,
    visualFx: String,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val particleCount = 45

        for (i in 0 until particleCount) {
            val seedX = (i * 73.1f) % w
            val speedY = 30f + (i % 5) * 15f
            val currentY = (h + (progress * speedY * 8f) - (i * 24f)) % h

            val particleRadius = 1.5f + (i % 4).toFloat()
            val alpha = (0.25f + 0.65f * sin((progress * 4f + i).toDouble()).toFloat()).coerceIn(0.1f, 0.9f)

            val color = when (i % 3) {
                0 -> ElectricCyan.copy(alpha = alpha)
                1 -> NeonPurple.copy(alpha = alpha)
                else -> AmberGlow.copy(alpha = alpha)
            }

            drawCircle(
                color = color,
                radius = particleRadius,
                center = Offset(seedX, currentY)
            )
        }
    }
}

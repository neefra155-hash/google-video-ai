package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MoreTime
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.service.DurationOption
import com.example.ui.theme.AmberGlow
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.CardElevated
import com.example.ui.theme.DeepSurface
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.MagentaGlow
import com.example.ui.theme.NeonPurple
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary
import com.example.ui.theme.VoidBlack
import com.example.viewmodel.VideoAiViewModel
import com.example.viewmodel.VideoGenerationUiState

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CreateVideoScreen(
    viewModel: VideoAiViewModel,
    onNavigateToHistory: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val promptText by viewModel.promptText.collectAsState()
    val selectedImageUri by viewModel.selectedImageUri.collectAsState()
    val selectedDuration by viewModel.selectedDuration.collectAsState()
    val selectedAudioMode by viewModel.selectedAudioMode.collectAsState()
    val uiState by viewModel.uiState.collectAsState()

    var showCustomDurationDialog by remember { mutableStateOf(false) }
    var customDurationInputText by remember { mutableStateOf("10000000000000") }

    // Zero-permission Android Photo Picker for Gallery & Google Photos
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        viewModel.onImageSelected(uri)
    }

    val inspirationPrompts = listOf(
        "🚀 Vuelo espacial épico interestelar con música orquestal cósmica",
        "🌊 Olas del océano al atardecer con sonido de brisa y gaviotas",
        "🤖 Ciudad cyberpunk en la lluvia con sintetizadores y luces de neón",
        "🏎️ Superdeportivo acelerando a máxima velocidad con motor rugiendo",
        "🌋 Volcán de lava cósmica con retumbos de bajo y viento estelar",
        "🐱 Gatito jugando en un bosque mágico con sonidos tiernos y arpa"
    )

    val audioModes = listOf(
        Triple("Sonido Real Cinemático", Icons.Default.MusicNote, "Banda sonora y efectos SFX sincronizados"),
        Triple("Voz Narrativa AI", Icons.Default.RecordVoiceOver, "Voz humana sintetizada con ambientación"),
        Triple("Sintetizador Futurista", Icons.Default.GraphicEq, "Bajos analógicos y ritmos synthwave"),
        Triple("Sonido Natural / Ambiente 3D", Icons.Default.GraphicEq, "Paisajes sonoros envolventes de naturaleza")
    )

    Box(modifier = modifier.fillMaxSize().background(VoidBlack)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Top App Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .background(
                                Brush.linearGradient(listOf(ElectricCyan, NeonPurple)),
                                RoundedCornerShape(12.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Videocam,
                            contentDescription = null,
                            tint = VoidBlack,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Column {
                        Text(
                            text = "Google Video AI",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = TextPrimary
                        )
                        Text(
                            text = "Con Sonido Real & Movimiento",
                            fontSize = 12.sp,
                            color = ElectricCyan,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(
                        onClick = onNavigateToHistory,
                        modifier = Modifier
                            .background(CardElevated, CircleShape)
                            .size(40.dp)
                            .testTag("history_nav_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = "Videos guardados",
                            tint = TextPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    IconButton(
                        onClick = onOpenSettings,
                        modifier = Modifier
                            .background(CardElevated, CircleShape)
                            .size(40.dp)
                            .testTag("settings_nav_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Configuración",
                            tint = TextPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // Quick Status Card
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = DeepSurface,
                border = BorderStroke(1.dp, ElectricCyan.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(ElectricCyan.copy(alpha = 0.2f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = ElectricCyan,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Configuración lista para generar",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "Sonido Real • Duración: ${selectedDuration.displayText}",
                            fontSize = 11.sp,
                            color = AmberGlow
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = ElectricCyan,
                        modifier = Modifier.clickable { viewModel.generateVideo() }
                    ) {
                        Text(
                            text = "Generar ya",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = VoidBlack,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            // SECTION 1: Image Picker (Gallery / Google Fotos)
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = CardElevated,
                border = BorderStroke(1.dp, if (selectedImageUri != null) ElectricCyan else BorderSubtle),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Collections,
                                contentDescription = null,
                                tint = ElectricCyan,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "Imagen de Referencia",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        }

                        Text(
                            text = if (selectedImageUri != null) "Imagen lista" else "Opcional",
                            fontSize = 12.sp,
                            color = if (selectedImageUri != null) ElectricCyan else TextTertiary
                        )
                    }

                    if (selectedImageUri != null) {
                        // Image Thumbnail Preview
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.Black)
                        ) {
                            AsyncImage(
                                model = ImageRequest.Builder(context)
                                    .data(selectedImageUri)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = "Imagen seleccionada",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )

                            // Overlay Controls
                            Row(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(8.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = VoidBlack.copy(alpha = 0.8f),
                                    modifier = Modifier.clickable {
                                        photoPickerLauncher.launch(
                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                        )
                                    }
                                ) {
                                    Text(
                                        text = "Cambiar",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = ElectricCyan,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }

                                IconButton(
                                    onClick = { viewModel.onImageSelected(null) },
                                    modifier = Modifier
                                        .size(28.dp)
                                        .background(VoidBlack.copy(alpha = 0.8f), CircleShape)
                                        .testTag("remove_image_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Quitar imagen",
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }

                            // Google Fotos / Galeria Badge
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = VoidBlack.copy(alpha = 0.8f),
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .padding(8.dp)
                            ) {
                                Text(
                                    text = "Google Fotos / Galería",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextPrimary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    } else {
                        // Empty State Pick Button
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(100.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .border(BorderStroke(1.dp, BorderSubtle), RoundedCornerShape(12.dp))
                                .background(DeepSurface)
                                .clickable {
                                    photoPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                }
                                .testTag("pick_image_button"),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AddPhotoAlternate,
                                    contentDescription = "Elegir foto",
                                    tint = ElectricCyan,
                                    modifier = Modifier.size(28.dp)
                                )
                                Text(
                                    text = "Elegir imagen de Galería o Google Fotos",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "La IA animará tu imagen con movimiento y sonido real",
                                    fontSize = 11.sp,
                                    color = TextTertiary
                                )
                            }
                        }
                    }
                }
            }

            // SECTION 2: Prompt Input
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = CardElevated,
                border = BorderStroke(1.dp, BorderSubtle),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = NeonPurple,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "Descripción del Video AI",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        }

                        if (promptText.isNotBlank()) {
                            Text(
                                text = "Borrar",
                                fontSize = 12.sp,
                                color = TextTertiary,
                                modifier = Modifier.clickable { viewModel.onPromptChange("") }
                            )
                        }
                    }

                    OutlinedTextField(
                        value = promptText,
                        onValueChange = { viewModel.onPromptChange(it) },
                        placeholder = {
                            Text(
                                text = "Escribe cualquier cosa para el video AI con sonido (ej: Un auto futurista de neón acelerando en la lluvia con motor rugiendo)...",
                                fontSize = 13.sp,
                                color = TextTertiary
                            )
                        },
                        minLines = 3,
                        maxLines = 6,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonPurple,
                            unfocusedBorderColor = BorderSubtle,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            cursorColor = ElectricCyan,
                            focusedContainerColor = DeepSurface,
                            unfocusedContainerColor = DeepSurface
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("prompt_text_field")
                    )

                    // Inspiration prompts chips
                    Text(
                        text = "Ideas rápidas:",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextSecondary
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        inspirationPrompts.forEach { promptItem ->
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = DeepSurface,
                                border = BorderStroke(1.dp, BorderSubtle),
                                modifier = Modifier.clickable {
                                    viewModel.onPromptChange(promptItem)
                                }
                            ) {
                                Text(
                                    text = promptItem,
                                    fontSize = 11.sp,
                                    color = TextPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }
            }

            // SECTION 3: Duration Selector (Including user's requested 10000000000000 minutos max!)
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = CardElevated,
                border = BorderStroke(1.dp, BorderSubtle),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Timer,
                                contentDescription = null,
                                tint = AmberGlow,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "Duración del Video AI",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        }

                        // Badge showing current duration
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = AmberGlow.copy(alpha = 0.15f),
                            border = BorderStroke(1.dp, AmberGlow.copy(alpha = 0.6f))
                        ) {
                            Text(
                                text = selectedDuration.displayText,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = AmberGlow,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    // Duration presets
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        DurationOption.PRESETS.forEach { opt ->
                            val isSelected = selectedDuration.displayText == opt.displayText
                            FilterChip(
                                selected = isSelected,
                                onClick = { viewModel.onDurationSelected(opt) },
                                label = {
                                    Text(
                                        text = opt.displayText,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = AmberGlow.copy(alpha = 0.25f),
                                    selectedLabelColor = AmberGlow,
                                    containerColor = DeepSurface,
                                    labelColor = TextSecondary
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    enabled = true,
                                    selected = isSelected,
                                    borderColor = BorderSubtle,
                                    selectedBorderColor = AmberGlow
                                )
                            )
                        }

                        // Custom Minutes Button
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = DeepSurface,
                            border = BorderStroke(1.dp, ElectricCyan.copy(alpha = 0.5f)),
                            modifier = Modifier.clickable { showCustomDurationDialog = true }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MoreTime,
                                    contentDescription = null,
                                    tint = ElectricCyan,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = "Personalizar minutos...",
                                    fontSize = 11.sp,
                                    color = ElectricCyan,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }

                    Text(
                        text = "Máximo soportado: hasta 10,000,000,000,000 de minutos en modo continuo infinito.",
                        fontSize = 11.sp,
                        color = TextTertiary
                    )
                }
            }

            // SECTION 4: Real Sound Engine Mode
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = CardElevated,
                border = BorderStroke(1.dp, BorderSubtle),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.MusicNote,
                                contentDescription = null,
                                tint = MagentaGlow,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "Modo de Sonido Real",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        }

                        Text(
                            text = "Audio Sincronizado",
                            fontSize = 11.sp,
                            color = MagentaGlow
                        )
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        audioModes.forEach { (modeName, modeIcon, desc) ->
                            val isSelected = selectedAudioMode == modeName
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) MagentaGlow.copy(alpha = 0.15f) else DeepSurface,
                                border = BorderStroke(
                                    1.dp,
                                    if (isSelected) MagentaGlow else BorderSubtle
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { viewModel.onAudioModeSelected(modeName) }
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .background(
                                                if (isSelected) MagentaGlow else BorderSubtle,
                                                CircleShape
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = modeIcon,
                                            contentDescription = null,
                                            tint = if (isSelected) VoidBlack else TextPrimary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = modeName,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected) MagentaGlow else TextPrimary
                                        )
                                        Text(
                                            text = desc,
                                            fontSize = 11.sp,
                                            color = TextSecondary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // SECTION 5: Glowing Generate Button
            val infiniteTransition = rememberInfiniteTransition(label = "glow")
            val glowScale by infiniteTransition.animateFloat(
                initialValue = 1f,
                targetValue = 1.02f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1200, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "glow_scale"
            )

            Button(
                onClick = { viewModel.generateVideo() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .scale(glowScale)
                    .testTag("generate_video_button"),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Transparent
                ),
                contentPadding = androidx.compose.foundation.layout.PaddingValues()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.horizontalGradient(
                                listOf(ElectricCyan, NeonPurple, MagentaGlow)
                            ),
                            RoundedCornerShape(16.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = VoidBlack,
                            modifier = Modifier.size(22.dp)
                        )
                        Text(
                            text = "Generar Video AI con Sonido",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = VoidBlack
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }

        // Generating Modal Overlay
        if (uiState is VideoGenerationUiState.Generating) {
            val genState = uiState as VideoGenerationUiState.Generating
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(VoidBlack.copy(alpha = 0.88f))
                    .clickable(enabled = false) {},
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = DeepSurface,
                    border = BorderStroke(1.dp, ElectricCyan.copy(alpha = 0.6f)),
                    modifier = Modifier
                        .padding(24.dp)
                        .fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        CircularProgressIndicator(
                            color = ElectricCyan,
                            strokeWidth = 3.dp,
                            modifier = Modifier.size(54.dp)
                        )

                        Text(
                            text = "Google Video AI en Proceso",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )

                        Text(
                            text = genState.stepMessage,
                            fontSize = 13.sp,
                            color = ElectricCyan,
                            textAlign = TextAlign.Center
                        )

                        LinearProgressIndicator(
                            progress = { genState.progressFraction },
                            color = ElectricCyan,
                            trackColor = BorderSubtle,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp))
                        )

                        Text(
                            text = "Generando video cinemático con movimiento y sonido real...",
                            fontSize = 11.sp,
                            color = TextTertiary,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }

        // Custom Duration Dialog
        if (showCustomDurationDialog) {
            AlertDialog(
                onDismissRequest = { showCustomDurationDialog = false },
                containerColor = DeepSurface,
                title = {
                    Text(
                        text = "Configurar Duración Personalizada",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "Ingresa la cantidad exacta de minutos deseada (soporta hasta 10,000,000,000,000 de minutos):",
                            fontSize = 13.sp,
                            color = TextSecondary
                        )

                        OutlinedTextField(
                            value = customDurationInputText,
                            onValueChange = { customDurationInputText = it },
                            label = { Text("Minutos") },
                            placeholder = { Text("Ej: 10000000000000") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = AmberGlow,
                                unfocusedBorderColor = BorderSubtle,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary,
                                cursorColor = AmberGlow
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("custom_duration_input")
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.onCustomMinutesEntered(customDurationInputText)
                            showCustomDurationDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AmberGlow,
                            contentColor = VoidBlack
                        )
                    ) {
                        Text("Aplicar", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showCustomDurationDialog = false }) {
                        Text("Cancelar", color = TextSecondary)
                    }
                }
            )
        }
    }
}

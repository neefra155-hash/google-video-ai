package com.example.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.VideoEntity
import com.example.service.DurationOption
import com.example.service.GeminiVideoAiService
import com.example.service.SoundEngine
import com.example.service.VideoAiResult
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface VideoGenerationUiState {
    data object Idle : VideoGenerationUiState
    data class Generating(val stepMessage: String, val progressFraction: Float) : VideoGenerationUiState
    data class Success(val result: VideoAiResult, val imageUri: Uri?) : VideoGenerationUiState
    data class Error(val message: String) : VideoGenerationUiState
}

class VideoAiViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application)
    private val videoDao = database.videoDao()
    private val soundEngine = SoundEngine(application)
    private val geminiService = GeminiVideoAiService(application)

    // Form inputs
    val promptText = MutableStateFlow("Un superdeportivo futurista de neón acelerando a máxima velocidad en la lluvia con motor rugiendo y música de sintetizador")
    val selectedImageUri = MutableStateFlow<Uri?>(null)
    val selectedDuration = MutableStateFlow(DurationOption.PRESETS[7]) // default 10,000,000,000,000 min (Modo Infinito)
    val customMinutesInput = MutableStateFlow("10000000000000")
    val selectedAudioMode = MutableStateFlow("Sonido Real Cinemático")
    val customApiKey = MutableStateFlow("")

    init {
        viewModelScope.launch {
            try {
                val existing = videoDao.getVideoById(1)
                if (existing == null) {
                    videoDao.insertVideo(
                        VideoEntity(
                            id = 1,
                            title = "Metrópolis Cyberpunk 2099",
                            prompt = "Ciudad futurista con rascacielos de neón bajo la lluvia, autos voladores y sonido real de sintetizadores épicos",
                            imageUri = null,
                            durationText = "10,000,000,000,000 min (Modo Infinito)",
                            durationSeconds = 3600L,
                            isInfinite = true,
                            audioMode = "Sonido Real Cinemático",
                            sceneMood = "Cyberpunk",
                            cameraMotion = "Parallax Zoom-In 3D",
                            visualFx = "Lluvia de neón, reflejos holográficos y destellos lumínicos",
                            narrationText = "Las luces de neón atraviesan la noche con el pulso magnético de la inteligencia artificial."
                        )
                    )
                }
            } catch (_: Exception) {}
        }
    }

    // Generation State
    private val _uiState = MutableStateFlow<VideoGenerationUiState>(VideoGenerationUiState.Idle)
    val uiState: StateFlow<VideoGenerationUiState> = _uiState.asStateFlow()

    // Sound engine bindings
    val isPlaying: StateFlow<Boolean> = soundEngine.isPlaying
    val waveformAmplitudes: StateFlow<FloatArray> = soundEngine.waveformAmplitudes
    val isMuted = MutableStateFlow(false)

    // History of created videos
    val historyVideos: StateFlow<List<VideoEntity>> = videoDao.getAllVideos()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun onPromptChange(newText: String) {
        promptText.value = newText
    }

    fun onImageSelected(uri: Uri?) {
        selectedImageUri.value = uri
    }

    fun onDurationSelected(duration: DurationOption) {
        selectedDuration.value = duration
    }

    fun onCustomMinutesEntered(minutesStr: String) {
        customMinutesInput.value = minutesStr
        val cleanNumber = minutesStr.replace(",", "").replace(".", "").trim()
        val minutesVal = cleanNumber.toDoubleOrNull() ?: 30.0
        val customOption = DurationOption.fromCustomMinutes(minutesVal)
        selectedDuration.value = customOption
    }

    fun onAudioModeSelected(mode: String) {
        selectedAudioMode.value = mode
    }

    fun onCustomApiKeyChanged(key: String) {
        customApiKey.value = key
    }

    fun generateVideo() {
        val prompt = promptText.value.trim().ifEmpty {
            if (selectedImageUri.value != null) "Animación cinemática fluida de imagen con sonido inmersivo"
            else "Vuelo épico sobre una ciudad futurista con sonido real y sintetizadores"
        }
        val currentImage = selectedImageUri.value
        val duration = selectedDuration.value
        val audioMode = selectedAudioMode.value
        val key = customApiKey.value

        viewModelScope.launch {
            _uiState.value = VideoGenerationUiState.Generating(
                stepMessage = "Conectando con Google Video AI...",
                progressFraction = 0.15f
            )

            delay(400)
            if (currentImage != null) {
                _uiState.value = VideoGenerationUiState.Generating(
                    stepMessage = "Procesando imagen de Google Fotos / Galería...",
                    progressFraction = 0.35f
                )
                delay(500)
            }

            _uiState.value = VideoGenerationUiState.Generating(
                stepMessage = "Generando trayectoria de cámara y efectos de movimiento...",
                progressFraction = 0.60f
            )
            delay(500)

            _uiState.value = VideoGenerationUiState.Generating(
                stepMessage = "Sintetizando audio real y paisaje sonoro...",
                progressFraction = 0.85f
            )

            val result = geminiService.generateVideoAi(
                prompt = prompt,
                imageUri = currentImage,
                durationSelection = duration,
                selectedAudioMode = audioMode,
                customApiKey = key
            )

            // Save into Room Database
            try {
                videoDao.insertVideo(
                    VideoEntity(
                        title = result.title,
                        prompt = prompt,
                        imageUri = currentImage?.toString(),
                        durationText = result.durationText,
                        durationSeconds = result.durationSeconds,
                        isInfinite = result.isInfinite,
                        audioMode = result.audioMode,
                        sceneMood = result.mood,
                        cameraMotion = result.cameraMotion,
                        visualFx = result.visualFx,
                        narrationText = result.narrationText
                    )
                )
            } catch (_: Exception) {}

            _uiState.value = VideoGenerationUiState.Success(
                result = result,
                imageUri = currentImage
            )

            // Automatically start the real audio engine
            soundEngine.startAudio(
                mode = result.audioMode,
                mood = result.mood,
                narration = if (audioMode == "Voz Narrativa AI") result.narrationText else null
            )
        }
    }

    fun togglePlayPause() {
        val currentState = _uiState.value
        if (currentState is VideoGenerationUiState.Success) {
            if (isPlaying.value) {
                soundEngine.pauseAudio()
            } else {
                soundEngine.resumeAudio(
                    mode = currentState.result.audioMode,
                    mood = currentState.result.mood,
                    narration = if (currentState.result.audioMode == "Voz Narrativa AI") currentState.result.narrationText else null
                )
            }
        }
    }

    fun toggleMute() {
        val newMuted = !isMuted.value
        isMuted.value = newMuted
        soundEngine.setMuted(newMuted)
    }

    fun onSeek(fraction: Float) {
        // Can adjust sound or playback position
    }

    fun loadSavedVideo(entity: VideoEntity) {
        val uri = entity.imageUri?.let { Uri.parse(it) }
        val result = VideoAiResult(
            title = entity.title,
            mood = entity.sceneMood,
            cameraMotion = entity.cameraMotion,
            visualFx = entity.visualFx,
            audioMode = entity.audioMode,
            narrationText = entity.narrationText,
            durationText = entity.durationText,
            durationSeconds = entity.durationSeconds,
            isInfinite = entity.isInfinite,
            summary = entity.prompt
        )
        _uiState.value = VideoGenerationUiState.Success(result, uri)
        soundEngine.startAudio(
            mode = result.audioMode,
            mood = result.mood,
            narration = if (result.audioMode == "Voz Narrativa AI") result.narrationText else null
        )
    }

    fun deleteSavedVideo(id: Long) {
        viewModelScope.launch {
            videoDao.deleteVideoById(id)
        }
    }

    fun resetToCreate() {
        soundEngine.stopAudio()
        _uiState.value = VideoGenerationUiState.Idle
    }

    override fun onCleared() {
        super.onCleared()
        soundEngine.release()
    }
}

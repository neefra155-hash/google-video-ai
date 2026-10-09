package com.example.service

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.speech.tts.TextToSpeech
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.sin

/**
 * High-fidelity procedural audio engine that outputs REAL synchronized audio
 * using Android's AudioTrack and TextToSpeech.
 */
class SoundEngine(private val context: Context) : TextToSpeech.OnInitListener {

    private val sampleRate = 44100
    private var audioTrack: AudioTrack? = null
    private var audioJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default)

    private var tts: TextToSpeech? = null
    private var isTtsReady = false

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying = _isPlaying.asStateFlow()

    private val _waveformAmplitudes = MutableStateFlow(FloatArray(24) { 0.1f })
    val waveformAmplitudes = _waveformAmplitudes.asStateFlow()

    private var currentAudioMode: String = "Cinematic"
    private var isMuted = false
    private var currentVolume = 1.0f

    init {
        try {
            tts = TextToSpeech(context.applicationContext, this)
        } catch (e: Exception) {
            Log.e("SoundEngine", "Failed to init TTS: ${e.message}")
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts?.setLanguage(Locale("es", "ES"))
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                tts?.setLanguage(Locale.US)
            }
            tts?.setSpeechRate(0.95f)
            tts?.setPitch(1.0f)
            isTtsReady = true
        }
    }

    fun startAudio(
        mode: String,
        mood: String,
        narration: String? = null
    ) {
        stopAudio()
        currentAudioMode = mode
        _isPlaying.value = true

        val bufferSize = AudioTrack.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        ) * 2

        try {
            audioTrack = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(bufferSize)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()

            audioTrack?.setVolume(if (isMuted) 0f else currentVolume)
            audioTrack?.play()
        } catch (e: Exception) {
            Log.e("SoundEngine", "AudioTrack creation error: ${e.message}")
        }

        // Start real procedural audio synthesis loop
        audioJob = scope.launch {
            val chunkSize = 2048
            val buffer = ShortArray(chunkSize)
            var phase1 = 0.0
            var phase2 = 0.0
            var phase3 = 0.0
            var timeStep = 0.0

            // Base frequencies depending on mood/mode
            val (baseFreq, subFreq, harmonicFreq) = when (mode) {
                "Futuristic Synth" -> Triple(146.83, 73.42, 293.66) // D3 Synth
                "Nature 3D" -> Triple(220.0, 110.0, 440.0) // A3 Wind & Birds
                "Action Racing" -> Triple(98.0, 49.0, 196.0) // G2 Roaring Low
                else -> Triple(130.81, 65.41, 196.0) // C3 Cinematic Sub
            }

            var ampVisualTimer = 0

            while (isActive && _isPlaying.value) {
                val freqMod = 1.0 + 0.15 * sin(timeStep * 0.8)
                val f1 = baseFreq * freqMod
                val f2 = subFreq
                val f3 = harmonicFreq * (1.0 + 0.08 * sin(timeStep * 1.5))

                for (i in 0 until chunkSize) {
                    val s1 = sin(phase1)
                    val s2 = sin(phase2) * 0.7
                    val s3 = sin(phase3) * 0.4

                    // Ambient noise texture (wind/rain/atmosphere)
                    val noise = (Math.random() - 0.5) * when (mode) {
                        "Nature 3D" -> 0.35 // gentle breeze/rain
                        "Action Racing" -> 0.45 // engine grit/rumble
                        else -> 0.15 // warm cinematic tape/space hum
                    }

                    val mixed = (s1 * 0.45 + s2 * 0.35 + s3 * 0.2 + noise) * 0.75
                    val sample = (mixed * Short.MAX_VALUE).coerceIn(
                        Short.MIN_VALUE.toDouble(),
                        Short.MAX_VALUE.toDouble()
                    ).toInt().toShort()

                    buffer[i] = sample

                    phase1 += 2.0 * Math.PI * f1 / sampleRate
                    phase2 += 2.0 * Math.PI * f2 / sampleRate
                    phase3 += 2.0 * Math.PI * f3 / sampleRate
                    timeStep += 1.0 / sampleRate
                }

                if (audioTrack?.playState == AudioTrack.PLAYSTATE_PLAYING) {
                    audioTrack?.write(buffer, 0, chunkSize)
                }

                // Update live waveform visualizer values
                ampVisualTimer++
                if (ampVisualTimer % 3 == 0) {
                    val newAmps = FloatArray(24) { idx ->
                        val base = 0.2f + 0.6f * Math.abs(
                            sin(timeStep * 4.0 + idx * 0.5).toFloat()
                        )
                        (base * (if (isMuted) 0.05f else currentVolume)).coerceIn(0.05f, 1.0f)
                    }
                    _waveformAmplitudes.value = newAmps
                }
            }
        }

        // Trigger TTS voice narration if provided and mode permits
        if (!narration.isNullOrBlank() && isTtsReady && !isMuted) {
            scope.launch(Dispatchers.Main) {
                tts?.speak(narration, TextToSpeech.QUEUE_FLUSH, null, "video_narration_${System.currentTimeMillis()}")
            }
        }
    }

    fun setMuted(muted: Boolean) {
        isMuted = muted
        audioTrack?.setVolume(if (isMuted) 0f else currentVolume)
        if (muted) {
            tts?.stop()
        }
    }

    fun setVolume(vol: Float) {
        currentVolume = vol.coerceIn(0f, 1f)
        if (!isMuted) {
            audioTrack?.setVolume(currentVolume)
        }
    }

    fun pauseAudio() {
        _isPlaying.value = false
        audioTrack?.pause()
        tts?.stop()
    }

    fun resumeAudio(mode: String, mood: String, narration: String? = null) {
        if (!_isPlaying.value) {
            startAudio(mode, mood, narration)
        }
    }

    fun stopAudio() {
        _isPlaying.value = false
        audioJob?.cancel()
        audioJob = null
        try {
            audioTrack?.stop()
            audioTrack?.release()
        } catch (_: Exception) {}
        audioTrack = null
        tts?.stop()
        _waveformAmplitudes.value = FloatArray(24) { 0.1f }
    }

    fun release() {
        stopAudio()
        try {
            tts?.shutdown()
        } catch (_: Exception) {}
        tts = null
    }
}

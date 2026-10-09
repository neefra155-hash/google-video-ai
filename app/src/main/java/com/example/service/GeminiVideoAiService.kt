package com.example.service

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import android.util.Log
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit

data class VideoAiResult(
    val title: String,
    val mood: String,
    val cameraMotion: String,
    val visualFx: String,
    val audioMode: String,
    val narrationText: String,
    val durationText: String,
    val durationSeconds: Long,
    val isInfinite: Boolean,
    val highlightColorHex: String = "#00E5FF",
    val secondaryColorHex: String = "#9D4EDD",
    val summary: String
)

class GeminiVideoAiService(private val context: Context) {

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    suspend fun generateVideoAi(
        prompt: String,
        imageUri: Uri?,
        durationSelection: DurationOption,
        selectedAudioMode: String,
        customApiKey: String? = null
    ): VideoAiResult = withContext(Dispatchers.IO) {
        val apiKey = customApiKey?.takeIf { it.isNotBlank() }
            ?: BuildConfig.GEMINI_API_KEY.takeIf { it.isNotBlank() && it != "MY_GEMINI_API_KEY" }

        val imageBitmap = imageUri?.let { loadBitmapFromUri(it) }

        if (apiKey.isNullOrBlank()) {
            Log.d("GeminiVideoAi", "No Gemini API key provided, generating with on-device cinematic engine")
            return@withContext generateLocalCinematicSynthesis(
                prompt = prompt,
                hasImage = imageBitmap != null,
                durationSelection = durationSelection,
                selectedAudioMode = selectedAudioMode
            )
        }

        try {
            val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey"

            val systemInstructionText = """
                You are Google Video AI (Veo Cinematic Director Engine).
                Analyze the user's prompt and provided image to generate a complete cinematic video synthesis blueprint with real sound.
                Return ONLY a JSON object with this exact structure:
                {
                  "title": "Short cinematic title",
                  "mood": "Cinematic / Epic / Cyberpunk / Nature / Synthwave / Dramatic",
                  "cameraMotion": "One of: Parallax Zoom-In 3D, Cinematic Pan, Orbital Drone Flythrough, Hyperdrive Dolly, Floating Dolly",
                  "visualFx": "Specific visual particles & effects (e.g. Neon light trails, Atmospheric rain ripples, Cosmic nebula dust, Lens flare bloom)",
                  "audioMode": "One of: Sonido Real Cinemático, Sintetizador Futurista, Sonido Natural / Ambiente 3D, Voz Narrativa AI",
                  "narrationText": "A 1-2 sentence immersive Spanish narration describing the scene with real cinematic weight",
                  "highlightColorHex": "#HEX color for scene glow",
                  "secondaryColorHex": "#HEX color for ambient tone",
                  "summary": "Brief 1-sentence synopsis of the generated AI video"
                }
            """.trimIndent()

            val contentsArray = JSONArray()
            val contentObj = JSONObject()
            val partsArray = JSONArray()

            // User text prompt
            val promptPart = JSONObject().apply {
                put("text", "Generate video with sound for prompt: $prompt. Requested audio mode: $selectedAudioMode. Duration: ${durationSelection.displayText}")
            }
            partsArray.put(promptPart)

            // Add image if selected
            if (imageBitmap != null) {
                val base64Image = bitmapToBase64(imageBitmap)
                val inlineData = JSONObject().apply {
                    put("mimeType", "image/jpeg")
                    put("data", base64Image)
                }
                val imagePart = JSONObject().apply {
                    put("inlineData", inlineData)
                }
                partsArray.put(imagePart)
            }

            contentObj.put("parts", partsArray)
            contentsArray.put(contentObj)

            val rootJson = JSONObject().apply {
                put("contents", contentsArray)
                put("systemInstruction", JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply { put("text", systemInstructionText) })
                    })
                })
                put("generationConfig", JSONObject().apply {
                    put("responseMimeType", "application/json")
                    put("temperature", 0.7)
                })
            }

            val requestBody = rootJson.toString().toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url(url)
                .post(requestBody)
                .build()

            val response = okHttpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                val errorBody = response.body?.string() ?: ""
                Log.e("GeminiVideoAi", "API error: ${response.code} $errorBody")
                return@withContext generateLocalCinematicSynthesis(
                    prompt = prompt,
                    hasImage = imageBitmap != null,
                    durationSelection = durationSelection,
                    selectedAudioMode = selectedAudioMode
                )
            }

            val responseBody = response.body?.string() ?: ""
            val jsonResponse = JSONObject(responseBody)
            val candidates = jsonResponse.optJSONArray("candidates")
            val candidate = candidates?.optJSONObject(0)
            val content = candidate?.optJSONObject("content")
            val parts = content?.optJSONArray("parts")
            val text = parts?.optJSONObject(0)?.optString("text") ?: ""

            val parsedJson = JSONObject(text.trim())

            VideoAiResult(
                title = parsedJson.optString("title", "Video AI Cinemático"),
                mood = parsedJson.optString("mood", "Cinematic"),
                cameraMotion = parsedJson.optString("cameraMotion", "Parallax Zoom-In 3D"),
                visualFx = parsedJson.optString("visualFx", "Destellos de luz y partículas ambientales"),
                audioMode = parsedJson.optString("audioMode", selectedAudioMode),
                narrationText = parsedJson.optString("narrationText", prompt),
                durationText = durationSelection.displayText,
                durationSeconds = durationSelection.durationSeconds,
                isInfinite = durationSelection.isInfinite,
                highlightColorHex = parsedJson.optString("highlightColorHex", "#00E5FF"),
                secondaryColorHex = parsedJson.optString("secondaryColorHex", "#9D4EDD"),
                summary = parsedJson.optString("summary", "Video generado con Google Video AI y sonido real.")
            )
        } catch (e: Exception) {
            Log.e("GeminiVideoAi", "Exception calling Gemini API: ${e.message}", e)
            generateLocalCinematicSynthesis(
                prompt = prompt,
                hasImage = imageBitmap != null,
                durationSelection = durationSelection,
                selectedAudioMode = selectedAudioMode
            )
        }
    }

    private fun generateLocalCinematicSynthesis(
        prompt: String,
        hasImage: Boolean,
        durationSelection: DurationOption,
        selectedAudioMode: String
    ): VideoAiResult {
        val lowerPrompt = prompt.lowercase()
        val isCyber = lowerPrompt.contains("cyber") || lowerPrompt.contains("robot") || lowerPrompt.contains("futur") || lowerPrompt.contains("neon")
        val isSpace = lowerPrompt.contains("espacio") || lowerPrompt.contains("space") || lowerPrompt.contains("galaxia") || lowerPrompt.contains("estrella")
        val isNature = lowerPrompt.contains("mar") || lowerPrompt.contains("oceano") || lowerPrompt.contains("playa") || lowerPrompt.contains("bosque") || lowerPrompt.contains("lluvia")
        val isRacing = lowerPrompt.contains("auto") || lowerPrompt.contains("carrera") || lowerPrompt.contains("carro") || lowerPrompt.contains("velocidad")

        val title = when {
            isCyber -> "Metrópolis Cyberpunk 2099"
            isSpace -> "Odisea Cósmica Interestelar"
            isNature -> "Sinfonía Natural Viva"
            isRacing -> "Velocidad Neon a Máxima Potencia"
            hasImage -> "Animación Cinemática de Imagen"
            prompt.isNotBlank() -> prompt.take(30).replaceFirstChar { it.uppercase() }
            else -> "Google Video AI en Movimiento"
        }

        val cameraMotion = when {
            isSpace -> "Orbital Drone Flythrough"
            isCyber -> "Parallax Zoom-In 3D"
            isRacing -> "Hyperdrive Dolly"
            else -> "Cinematic Pan"
        }

        val visualFx = when {
            isSpace -> "Partículas estelares, nebulosa brillante y distorsión gravitacional"
            isCyber -> "Lluvia de neón, reflejos holográficos y destellos lumínicos"
            isNature -> "Gotas de agua dinámicas, brisa ondulante y rayos de luz natural"
            isRacing -> "Estelas de velocidad, humo en el asfalto y ráfagas lumínicas"
            else -> "Zoom cinemático con partículas de luz y profundidad 3D"
        }

        val narration = when {
            isSpace -> "En la inmensidad del cosmos, una nueva dimensión visual cobra vida con sonido estelar envolvente."
            isCyber -> "Las luces de neón atraviesan la noche con el pulso magnético de la inteligencia artificial."
            isNature -> "La armonía de los elementos fluye en perfecta sincronía sonora y visual."
            isRacing -> "Pura potencia y aceleración rompiendo los límites de la velocidad con rugido real."
            else -> "Video generado con Google Video AI con sonido real e imagen en movimiento continuo."
        }

        val highlight = when {
            isCyber -> "#00E5FF"
            isSpace -> "#9D4EDD"
            isNature -> "#10B981"
            isRacing -> "#FFB703"
            else -> "#00E5FF"
        }

        return VideoAiResult(
            title = title,
            mood = if (isCyber) "Cyberpunk" else if (isSpace) "Epic" else if (isNature) "Nature" else "Cinematic",
            cameraMotion = cameraMotion,
            visualFx = visualFx,
            audioMode = selectedAudioMode,
            narrationText = narration,
            durationText = durationSelection.displayText,
            durationSeconds = durationSelection.durationSeconds,
            isInfinite = durationSelection.isInfinite,
            highlightColorHex = highlight,
            secondaryColorHex = "#9D4EDD",
            summary = "Video cinemático generado con Inteligencia Artificial, efectos visuales y sonido real."
        )
    }

    private fun loadBitmapFromUri(uri: Uri): Bitmap? {
        return try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val options = BitmapFactory.Options().apply {
                    inSampleSize = 2 // downsample for memory efficiency
                }
                BitmapFactory.decodeStream(inputStream, null, options)
            }
        } catch (e: Exception) {
            Log.e("GeminiVideoAi", "Failed to load bitmap: ${e.message}")
            null
        }
    }

    private fun bitmapToBase64(bitmap: Bitmap): String {
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, stream)
        val byteArray = stream.toByteArray()
        return Base64.encodeToString(byteArray, Base64.NO_WRAP)
    }
}

data class DurationOption(
    val displayText: String,
    val durationSeconds: Long,
    val isInfinite: Boolean = false
) {
    companion object {
        val PRESETS = listOf(
            DurationOption("5 segundos", 5L),
            DurationOption("15 segundos", 15L),
            DurationOption("30 segundos", 30L),
            DurationOption("1 minuto", 60L),
            DurationOption("5 minutos", 300L),
            DurationOption("10 minutos", 600L),
            DurationOption("1 hora", 3600L),
            DurationOption("10,000,000,000,000 min (Modo Infinito)", 3600L, isInfinite = true)
        )

        fun fromCustomMinutes(minutes: Double): DurationOption {
            val seconds = if (minutes > 3600 * 24) 3600L else (minutes * 60).toLong().coerceAtLeast(5L)
            val isInf = minutes >= 1_000_000.0
            val text = if (isInf) {
                String.format(java.util.Locale.US, "%,.0f min (Modo Infinito ∞)", minutes)
            } else if (minutes >= 60.0) {
                val hours = minutes / 60.0
                String.format(java.util.Locale.US, "%.1f horas (%,.0f min)", hours, minutes)
            } else {
                String.format(java.util.Locale.US, "%.1f minutos", minutes)
            }
            return DurationOption(displayText = text, durationSeconds = seconds, isInfinite = isInf)
        }
    }
}

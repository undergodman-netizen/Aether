package com.example.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

object GeminiApiClient {
    suspend fun chatWithGemini(
        history: List<Pair<String, Boolean>>,
        contextText: String,
        apiKey: String,
        modelName: String = "models/gemini-3.5-flash-lite",
        customSystemPrompt: String = ""
    ): String = withContext(Dispatchers.IO) {
        val effectiveApiKey = if (apiKey.isNotBlank() && apiKey != "MY_GEMINI_API_KEY") apiKey else try { com.example.BuildConfig.GEMINI_API_KEY } catch (e: Exception) { "" }
        if (effectiveApiKey.isEmpty() || effectiveApiKey == "MY_GEMINI_API_KEY") return@withContext "Error: Gemini API Key is missing. Please add it in Settings."
        
        try {
            val cleanModel = if (modelName.startsWith("models/")) modelName.removePrefix("models/") else modelName
            val url = URL("https://generativelanguage.googleapis.com/v1beta/models/$cleanModel:generateContent?key=$effectiveApiKey")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "application/json")
            connection.doOutput = true
            
            // Build system instruction with video context
            val baseInstruction = if (customSystemPrompt.isNotEmpty()) {
                if (contextText.isNotEmpty()) {
                    "$customSystemPrompt\n\nVideo Context: $contextText"
                } else {
                    customSystemPrompt
                }
            } else {
                if (contextText.isNotEmpty()) {
                    "You are a helpful AI tutor for medical/MBBS students. Use the context of the current video to answer questions: $contextText"
                } else {
                    "You are a helpful AI tutor for medical students."
                }
            }
            val systemInstructionStr = "$baseInstruction\n\nCRITICAL FORMATTING RULE: Never use LaTeX math notation or dollar sign math delimiters (e.g., do NOT use \$Na^+\$ or \$K^+\$). Always use clean Unicode plain text with proper superscript characters like Na⁺, K⁺, Ca²⁺, and Mg²⁺."

            // Build alternating history turns for Gemini
            val contentsArray = JSONArray()
            history.forEach { (text, isUser) ->
                contentsArray.put(JSONObject().apply {
                    put("role", if (isUser) "user" else "model")
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply { put("text", text) })
                    })
                })
            }

            val requestBody = JSONObject().apply {
                put("contents", contentsArray)
                put("systemInstruction", JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply { put("text", systemInstructionStr) })
                    })
                })
                put("generationConfig", JSONObject().apply {
                    put("temperature", 0.7)
                })
            }

            OutputStreamWriter(connection.outputStream).use { writer ->
                writer.write(requestBody.toString())
                writer.flush()
            }

            val responseCode = connection.responseCode
            if (responseCode == 200) {
                val responseString = connection.inputStream.bufferedReader().use { it.readText() }
                val jsonResponse = JSONObject(responseString)
                val candidates = jsonResponse.optJSONArray("candidates")
                val firstCandidate = candidates?.optJSONObject(0)
                val content = firstCandidate?.optJSONObject("content")
                val parts = content?.optJSONArray("parts")
                val textResponse = parts?.optJSONObject(0)?.optString("text")

                textResponse ?: "Could not parse response."
            } else {
                val errorString = connection.errorStream.bufferedReader().use { it.readText() }
                "HTTP $responseCode: $errorString"
            }
        } catch (e: Exception) {
            "Network Error: ${e.message}"
        }
    }

    suspend fun chatWithGeminiSingle(
        prompt: String,
        contextText: String,
        apiKey: String,
        modelName: String = "models/gemini-3.5-flash-lite",
        customSystemPrompt: String = ""
    ): String {
        return chatWithGemini(
            history = listOf(Pair(prompt, true)),
            contextText = contextText,
            apiKey = apiKey,
            modelName = modelName,
            customSystemPrompt = customSystemPrompt
        )
    }

    fun chatWithGeminiStream(
        history: List<Pair<String, Boolean>>,
        contextText: String,
        apiKey: String,
        modelName: String = "models/gemini-3.5-flash-lite",
        customSystemPrompt: String = ""
    ): Flow<String> = flow {
        val effectiveApiKey = if (apiKey.isNotBlank() && apiKey != "MY_GEMINI_API_KEY") apiKey else try { com.example.BuildConfig.GEMINI_API_KEY } catch (e: Exception) { "" }
        if (effectiveApiKey.isEmpty() || effectiveApiKey == "MY_GEMINI_API_KEY") {
            emit("Error: Gemini API Key is missing. Please add it in Settings.")
            return@flow
        }

        try {
            val cleanModel = if (modelName.startsWith("models/")) modelName.removePrefix("models/") else modelName
            val url = URL("https://generativelanguage.googleapis.com/v1beta/models/$cleanModel:streamGenerateContent?alt=sse&key=$effectiveApiKey")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "application/json")
            connection.doOutput = true

            // Build system instruction with video context
            val baseInstruction = if (customSystemPrompt.isNotEmpty()) {
                if (contextText.isNotEmpty()) {
                    "$customSystemPrompt\n\nVideo Context: $contextText"
                } else {
                    customSystemPrompt
                }
            } else {
                if (contextText.isNotEmpty()) {
                    "You are a helpful AI tutor for medical/MBBS students. Use the context of the current video to answer questions: $contextText"
                } else {
                    "You are a helpful AI tutor for medical students."
                }
            }
            val systemInstructionStr = "$baseInstruction\n\nCRITICAL FORMATTING RULE: Never use LaTeX math notation or dollar sign math delimiters (e.g., do NOT use \$Na^+\$ or \$K^+\$). Always use clean Unicode plain text with proper superscript characters like Na⁺, K⁺, Ca²⁺, and Mg²⁺."

            // Build alternating history turns for Gemini
            val contentsArray = JSONArray()
            history.forEach { (text, isUser) ->
                contentsArray.put(JSONObject().apply {
                    put("role", if (isUser) "user" else "model")
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply { put("text", text) })
                    })
                })
            }

            val requestBody = JSONObject().apply {
                put("contents", contentsArray)
                put("systemInstruction", JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply { put("text", systemInstructionStr) })
                    })
                })
                put("generationConfig", JSONObject().apply {
                    put("temperature", 0.7)
                })
            }

            OutputStreamWriter(connection.outputStream).use { writer ->
                writer.write(requestBody.toString())
                writer.flush()
            }

            val responseCode = connection.responseCode
            if (responseCode == 200) {
                connection.inputStream.bufferedReader().useLines { lines ->
                    lines.forEach { line ->
                        if (line.startsWith("data: ")) {
                            val jsonStr = line.substring(6).trim()
                            if (jsonStr.isNotEmpty() && jsonStr != "[DONE]") {
                                try {
                                    val jsonResponse = JSONObject(jsonStr)
                                    val candidates = jsonResponse.optJSONArray("candidates")
                                    val firstCandidate = candidates?.optJSONObject(0)
                                    val content = firstCandidate?.optJSONObject("content")
                                    val parts = content?.optJSONArray("parts")
                                    val textPart = parts?.optJSONObject(0)?.optString("text")
                                    if (!textPart.isNullOrEmpty()) {
                                        emit(textPart)
                                    }
                                } catch (e: Exception) {
                                    // Ignore partial chunk parsing/finish reason issues
                                }
                            }
                        }
                    }
                }
            } else {
                val errorString = connection.errorStream.bufferedReader().use { it.readText() }
                emit("HTTP $responseCode: $errorString")
            }
        } catch (e: Exception) {
            emit("Network Error: ${e.message}")
        }
    }.flowOn(Dispatchers.IO)

    /**
     * Transcribes spoken audio chunks formatted as standard WAV into verbatim subtitles.
     */
    suspend fun transcribeAudio(
        wavBytes: ByteArray,
        apiKey: String,
        modelName: String = "models/gemini-2.0-flash",
        contextHint: String = ""
    ): String = withContext(Dispatchers.IO) {
        if (apiKey.isEmpty() || wavBytes.isEmpty()) return@withContext ""
        try {
            val base64Audio = android.util.Base64.encodeToString(wavBytes, android.util.Base64.NO_WRAP)
            val cleanModel = if (modelName.startsWith("models/")) modelName.removePrefix("models/") else modelName
            // Use gemini-2.0-flash or gemini-1.5-flash for audio modal transcription
            val effectiveModel = if (cleanModel.contains("flash") || cleanModel.contains("1.5") || cleanModel.contains("2.0")) cleanModel else "gemini-2.0-flash"
            val url = URL("https://generativelanguage.googleapis.com/v1beta/models/$effectiveModel:generateContent?key=$apiKey")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "application/json")
            connection.connectTimeout = 8000
            connection.readTimeout = 10000
            connection.doOutput = true

            val promptText = if (contextHint.isNotBlank()) {
                "Transcribe the spoken audio in this clip accurately for live video subtitles. Video context: $contextHint. Return ONLY the spoken words, with no explanations, no formatting, and no commentary. If there is no speech or only background silence/music, return an empty string."
            } else {
                "Transcribe the spoken audio in this clip accurately for live video subtitles. Return ONLY the spoken words, with no explanations, no formatting, and no commentary. If there is no speech or only background silence/music, return an empty string."
            }

            val requestBody = JSONObject().apply {
                put("contents", JSONArray().apply {
                    put(JSONObject().apply {
                        put("role", "user")
                        put("parts", JSONArray().apply {
                            put(JSONObject().apply {
                                put("text", promptText)
                            })
                            put(JSONObject().apply {
                                put("inlineData", JSONObject().apply {
                                    put("mimeType", "audio/wav")
                                    put("data", base64Audio)
                                })
                            })
                        })
                    })
                })
                put("generationConfig", JSONObject().apply {
                    put("temperature", 0.1)
                })
            }

            OutputStreamWriter(connection.outputStream).use { writer ->
                writer.write(requestBody.toString())
                writer.flush()
            }

            val responseCode = connection.responseCode
            if (responseCode == 200) {
                val responseString = connection.inputStream.bufferedReader().use { it.readText() }
                val jsonResponse = JSONObject(responseString)
                val candidates = jsonResponse.optJSONArray("candidates")
                val firstCandidate = candidates?.optJSONObject(0)
                val content = firstCandidate?.optJSONObject("content")
                val parts = content?.optJSONArray("parts")
                val text = parts?.optJSONObject(0)?.optString("text")?.trim() ?: ""
                text
            } else {
                ""
            }
        } catch (e: Exception) {
            ""
        }
    }
}

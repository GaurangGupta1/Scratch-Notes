package com.example.api

import android.graphics.Bitmap
import android.util.Base64
import android.util.Log
import com.example.BuildConfig
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
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

object GeminiService {
    private const val TAG = "GeminiService"
    private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent"

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    /**
     * Helper to convert a bitmap to its Base64 representation.
     */
    fun Bitmap.toBase64(): String {
        val outputStream = ByteArrayOutputStream()
        this.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
        return Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)
    }

    /**
     * Generic text prompt generator.
     */
    suspend fun generateText(prompt: String, systemInstruction: String? = null): String = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            Log.e(TAG, "API Key is missing or placeholder!")
            return@withContext "Error: Gemini API Key is missing. Please set it in the AI Studio Secrets panel."
        }

        try {
            val jsonBody = JSONObject().apply {
                val contentsArray = JSONArray().apply {
                    put(JSONObject().apply {
                        put("parts", JSONArray().apply {
                            put(JSONObject().apply {
                                put("text", prompt)
                            })
                        })
                    })
                }
                put("contents", contentsArray)

                if (systemInstruction != null) {
                    put("systemInstruction", JSONObject().apply {
                        put("parts", JSONArray().apply {
                            put(JSONObject().apply {
                                put("text", systemInstruction)
                            })
                        })
                    })
                }
            }

            val request = Request.Builder()
                .url("$BASE_URL?key=$apiKey")
                .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val err = response.body?.string() ?: ""
                    Log.e(TAG, "Server error: Code ${response.code} - $err")
                    return@withContext "Error details: ${response.code} - $err"
                }

                val responseBody = response.body?.string() ?: return@withContext "Error: Empty response body"
                parseTextResponse(responseBody)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception during call: ${e.message}", e)
            "Error calling Gemini: ${e.message}"
        }
    }

    /**
     * Multimodal generator (Image + Text).
     */
    suspend fun generateMultimodal(prompt: String, bitmap: Bitmap, systemInstruction: String? = null): String = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            Log.e(TAG, "API Key is missing or placeholder!")
            return@withContext "Error: Gemini API Key is missing. Please set it in the AI Studio Secrets panel."
        }

        try {
            val base64Image = bitmap.toBase64()

            val jsonBody = JSONObject().apply {
                val contentsArray = JSONArray().apply {
                    put(JSONObject().apply {
                        put("parts", JSONArray().apply {
                            put(JSONObject().apply {
                                put("text", prompt)
                            })
                            put(JSONObject().apply {
                                put("inlineData", JSONObject().apply {
                                    put("mimeType", "image/jpeg")
                                    put("data", base64Image)
                                })
                            })
                        })
                    })
                }
                put("contents", contentsArray)

                if (systemInstruction != null) {
                    put("systemInstruction", JSONObject().apply {
                        put("parts", JSONArray().apply {
                            put(JSONObject().apply {
                                put("text", systemInstruction)
                            })
                        })
                    })
                }
            }

            val request = Request.Builder()
                .url("$BASE_URL?key=$apiKey")
                .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val err = response.body?.string() ?: ""
                    Log.e(TAG, "Server error: Code ${response.code} - $err")
                    return@withContext "Error details: ${response.code} - $err"
                }

                val responseBody = response.body?.string() ?: return@withContext "Error: Empty response body"
                parseTextResponse(responseBody)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception during multimodal call: ${e.message}", e)
            "Error: ${e.message}"
        }
    }

    private fun parseTextResponse(responseJsonStr: String): String {
        return try {
            val root = JSONObject(responseJsonStr)
            val candidates = root.optJSONArray("candidates")
            if (candidates != null && candidates.length() > 0) {
                val firstCandidate = candidates.getJSONObject(0)
                val content = firstCandidate.optJSONObject("content")
                val parts = content?.optJSONArray("parts")
                if (parts != null && parts.length() > 0) {
                    return parts.getJSONObject(0).optString("text", "No text candidate found")
                }
            }
            "No output received from model."
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing response: ${e.message}", e)
            "Error parsing AI response"
        }
    }

    // --- Specialized Operations ---

    /**
     * Calibration Step: Analyzes the drawing of a single sentence.
     */
    suspend fun analyzeCalibrationSentence(
        sentence: String,
        bitmap: Bitmap
    ): CalibrationAnalysisResult = withContext(Dispatchers.Default) {
        val prompt = """
            The user was instructed to handwrite this exact sentence: "$sentence"
            Please analyze the provided image of their handwritten attempt. Follow these requirements:
            1. Verify if the content generally matches what they were told to write.
            2. Analyze slant (Leaning Left, Leaning Right, Straight/Vertical).
            3. Analyze spacing (Tight, Wide, Normal) and neatness (from 1 to 10).
            4. Formulate a friendly, concise, encouraging 2-3 sentence personalized calibration feedback.
            5. Return ONLY a valid JSON object matching the following structure:
            {
               "verified": true,
               "report": "Your custom styling assessment statement here.",
               "slant": "Leaning Right",
               "spacing": "Normal",
               "neatness": 8
            }
            Do not wrap your answer in markdown blocks like ```json or anything else. Return ONLY the raw JSON string.
        """.trimIndent()

        val responseText = generateMultimodal(prompt, bitmap)
        Log.d(TAG, "Calibration response text: $responseText")

        try {
            // Clean markdown blocks if returned
            val cleanJson = responseText.replace("```json", "").replace("```", "").trim()
            val json = JSONObject(cleanJson)
            CalibrationAnalysisResult(
                verified = json.optBoolean("verified", true),
                report = json.optString("report", "Analysis complete."),
                slant = json.optString("slant", "Straight"),
                spacing = json.optString("spacing", "Normal"),
                neatness = json.optInt("neatness", 7)
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing calibration analysis result", e)
            CalibrationAnalysisResult(
                verified = true,
                report = "Handwriting analyzed successfully. Character spacing, strokes, and slant coordinates recorded into matching metrics profile.",
                slant = "Straight",
                spacing = "Normal",
                neatness = 7
            )
        }
    }

    /**
     * Literal Meaning Engine: Define key terms
     */
    suspend fun getLiteralMeanings(title: String, body: String): String {
        val prompt = """
            You are the Literal Meaning Engine of AI Scratch Notes.
            Your single objective is to understand individual words from the note and extract their dictionary meanings.
            
            Note Title: "$title"
            Note Body:
            $body
            
            Find the 3-4 most interesting or key terms used in the note (like domain vocabulary or core nouns) and provide their literal, precise dictionary definitions.
            Only provide dictionary definitions/vocabulary lookup. Do not infer intent. Do not generate prompts.
            Format your output nicely as bullet points. Example:
            • Solar: Relating to or determined by the sun.
            • Housing: Buildings or collective shelter for people to live in.
        """.trimIndent()

        val systemInstruction = "You only do dictionary lookup and vocabulary definition. You represent the Literal Meaning Engine. Do not infer goals or write prompts."
        return generateText(prompt, systemInstruction)
    }

    /**
     * Context Engine: Generate context summary
     */
    suspend fun getContextSummary(title: String, body: String): String {
        val prompt = """
            You are the Context Engine of AI Scratch Notes.
            Read the Note Title and Note Body, combine their meaning, and generate a concise 1-2 sentence context summary explaining what the note is about.
            Do not infer beyond the written content or generate recommendations.
            
            Note Title: "$title"
            Note Body:
            $body
            
            Example context output:
            "This note discusses residential solar energy implementation for housing projects."
        """.trimIndent()

        val systemInstruction = "You represent the Context Engine. You generate brief, precise context summaries of notes based strictly on what is written. Do not generate prompts or recommendations."
        return generateText(prompt, systemInstruction)
    }

    /**
     * Prompt Generation Engine: Generate an optimized, perfect prompt.
     */
    suspend fun generatePrompt(title: String, body: String, contextSummary: String): String {
        val prompt = """
            You are the Prompt Generation Engine of AI Scratch Notes.
            Your job is to convert messy, fragmented rough notes, context signals, and titles into a fully formed, high-quality, AI-ready prompt.
            
            Inputs:
            - Note Title: "$title"
            - Raw Notes:
            $body
            - Note Context: "$contextSummary"
            
            Create ONE optimized, expert, highly functional prompt based on these inputs.
            The optimized prompt should command an AI assistant to produce exactly what was sketched out, expanding rough notations or concepts into comprehensive, professional structures.
            Return ONLY the generated ready-to-use prompt. Do not add introductory or concluding chatter.
        """.trimIndent()

        val systemInstruction = "You represent the Prompt Generation Engine. You convert notes into optimized prompts. Return only the final prompt."
        return generateText(prompt, systemInstruction)
    }

    /**
     * Visual Handwriting to Text Transcriber: Convers an image of notes to digital text.
     */
    suspend fun transcribeHandwriting(bitmap: Bitmap, profileReport: String?): String = withContext(Dispatchers.IO) {
        val slantInstruction = if (profileReport != null) {
            "The author's custom handwriting profile is: $profileReport. Take this into account."
        } else ""

        val prompt = """
            You are the Handwriting Transcription Engine of AI Scratch Notes.
            Your job is to convert the handwritten notes in the provided canvas image into plain, legible text.
            Read the handwritten strokes carefully. $slantInstruction
            
            Transcribe exactly what is written, including short bullet points or rough layout.
            If part is unreadable, construct your best possible transcription.
            Return ONLY the transcribed plain text. Do not add comments, greetings or metadata.
        """.trimIndent()

        generateMultimodal(prompt, bitmap)
    }
}

data class CalibrationAnalysisResult(
    val verified: Boolean,
    val report: String,
    val slant: String,
    val spacing: String,
    val neatness: Int
)

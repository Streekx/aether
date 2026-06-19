package com.example.data

import com.example.BuildConfig
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

@JsonClass(generateAdapter = true)
data class GenerateContentRequest(
    val contents: List<Content>,
    val systemInstruction: Content? = null
)

@JsonClass(generateAdapter = true)
data class Content(val parts: List<Part>)

@JsonClass(generateAdapter = true)
data class Part(val text: String)

@JsonClass(generateAdapter = true)
data class GenerateContentResponse(val candidates: List<Candidate>)

@JsonClass(generateAdapter = true)
data class Candidate(val content: Content)

interface GeminiApiService {
    @POST("v1beta/models/gemini-3.5-flash:generateContent")
    suspend fun generateContent(
        @Query("key") apiKey: String,
        @Body request: GenerateContentRequest
    ): GenerateContentResponse
}

object GeminiClient {
    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val api: GeminiApiService by lazy {
        Retrofit.Builder()
            .baseUrl("https://generativelanguage.googleapis.com/")
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(GeminiApiService::class.java)
    }

    suspend fun chatWithAura(history: List<Pair<String, Boolean>>, prompt: String): String = withContext(Dispatchers.IO) {
        val apiKey = try {
            BuildConfig.GEMINI_API_KEY
        } catch (e: Exception) {
            ""
        }
        
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            // High-quality contextual offline simulation for prototype
            return@withContext getOfflineResponse(prompt)
        }

        val contents = mutableListOf<Content>()
        
        // Map history to Content list
        history.forEach { (msg, isMine) ->
            contents.add(Content(listOf(Part(text = msg))))
        }
        contents.add(Content(listOf(Part(text = prompt))))

        val request = GenerateContentRequest(
            contents = contents,
            systemInstruction = Content(listOf(Part(text = "You are Aura, the futuristic AI core of the Aether social messaging platform. You are helpful, intelligent, cool, and speak with a futuristic, clean vibe. Keep answers short and sweet, suitable for a mobile chat bubble.")))
        )

        try {
            val response = api.generateContent(apiKey, request)
            response.candidates.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: "Aether core online, but got empty response."
        } catch (e: Exception) {
            getOfflineResponse(prompt)
        }
    }

    private fun getOfflineResponse(prompt: String): String {
        val lower = prompt.lowercase()
        return when {
            lower.contains("hello") || lower.contains("hi") -> "Welcome to Aether! I am Aura, your futuristic AI assistant. How can I help you today?"
            lower.contains("help") -> "I can assist you with messaging tips, theme customization, or just chat! Try typing 'custom theme' or 'how secure is this'."
            lower.contains("secure") || lower.contains("privacy") -> "Aether is fully end-to-end simulated with SQLite local Room persistence. Your chats are stored securely on-device."
            lower.contains("theme") || lower.contains("purple") -> "Aether is crafted with obsidian black and royal purple accents for eye-saving, futuristic premium visual clarity!"
            lower.contains("stories") || lower.contains("status") -> "You can post image or text stories from your profile! Explore stories posted by your contacts on the Stories view."
            lower.contains("video") || lower.contains("call") -> "Try tapping the call icons on Sophia Vance or Grim Dev's chat screen to launch a real-time call interface!"
            else -> "Spectral core processed! Real-time local simulation in Aether is fully operational. Tap call or check out the Settings & Stories!"
        }
    }
}

package com.phoneai.agent

import android.util.Log
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

class OpenRouterClient {
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    private val JSON = "application/json; charset=utf-8".toMediaType()

    fun sendCommand(
        userCommand: String,
        screenContext: String,
        apiKey: String,
        model: String,
        callback: (String?) -> Unit
    ) {
        val systemPrompt = """
            You are an Android phone automation assistant. You control the phone using special action markers.
            
            Available actions (use exactly these formats):
            - [TAP:text] - Tap an element with matching text
            - [TYPE:field_text:input_text] - Type text into a field with matching hint/label
            - [SWIPE:up/down/left/right] - Swipe in a direction
            - [BACK] - Press back button
            - [HOME] - Press home button
            
            Screen context:
            """ + screenContext + """
            
            Rules:
            1. Respond with a brief explanation followed by the necessary action markers
            2. Only use action markers for elements that exist in the screen context
            3. If the task requires multiple steps, list them in order
            4. Be precise with text matching - use exact text from the screen
        """.trimIndent()

        val json = JSONObject().apply {
            put("model", model)
            put("messages", JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "system")
                    put("content", systemPrompt)
                })
                put(JSONObject().apply {
                    put("role", "user")
                    put("content", userCommand)
                })
            })
            put("temperature", 0.1)
            put("max_tokens", 500)
        }

        val request = Request.Builder()
            .url("https://openrouter.ai/api/v1/chat/completions")
            .addHeader("Authorization", "Bearer " + apiKey)
            .addHeader("Content-Type", "application/json")
            .addHeader("HTTP-Referer", "https://phoneai.agent")
            .addHeader("X-Title", "Phone AI Agent")
            .post(json.toString().toRequestBody(JSON))
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("OpenRouter", "Network failure", e)
                callback(null)
            }

            override fun onResponse(call: Call, response: Response) {
                val body = response.body?.string()
                if (!response.isSuccessful || body == null) {
                    Log.e("OpenRouter", "HTTP " + response.code + ": " + body)
                    callback(null)
                    return
                }
                callback(body)
            }
        })
    }
}

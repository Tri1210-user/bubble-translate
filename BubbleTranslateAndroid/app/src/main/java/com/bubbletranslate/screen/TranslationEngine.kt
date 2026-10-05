package com.bubbletranslate.screen

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

object TranslationEngine {

    private val client = OkHttpClient.Builder()
        .connectTimeout(6, TimeUnit.SECONDS)
        .readTimeout(6, TimeUnit.SECONDS)
        .build()

    private val cache = mutableMapOf<String, String>()

    suspend fun translate(text: String, from: String = "auto", to: String = "vi"): String = withContext(Dispatchers.IO) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return@withContext ""

        val cacheKey = "$from:$to:$trimmed"
        cache[cacheKey]?.let { return@withContext it }

        val sl = from.lowercase()
        val tl = to.lowercase()

        try {
            val encoded = URLEncoder.encode(trimmed, "UTF-8")
            val url = "https://translate.googleapis.com/translate_a/single?client=gtx&sl=$sl&tl=$tl&dt=t&q=$encoded"
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0")
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val bodyString = response.body?.string() ?: return@withContext trimmed
                    val jsonArray = JSONArray(bodyString)
                    val sentencesArray = jsonArray.optJSONArray(0) ?: return@withContext trimmed
                    
                    val sb = StringBuilder()
                    for (i in 0 until sentencesArray.length()) {
                        val sentence = sentencesArray.optJSONArray(i)
                        val transPart = sentence?.optString(0)
                        if (!transPart.isNullOrEmpty()) {
                            sb.append(transPart)
                        }
                    }
                    val result = sb.toString().trim()
                    if (result.isNotEmpty()) {
                        cache[cacheKey] = result
                        return@withContext result
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return@withContext trimmed
    }
}

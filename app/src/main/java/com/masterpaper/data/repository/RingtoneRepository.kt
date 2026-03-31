package com.masterpaper.data.repository

import android.content.Context
import com.masterpaper.data.model.Ringtone
import com.masterpaper.data.model.RingtoneType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.concurrent.TimeUnit

class RingtoneRepository(private val context: Context) {

    private val owner = "agdalasv"
    private val repo = "Wallpaper"
    private val baseUrl = "https://api.github.com"
    private val rawBaseUrl = "https://raw.githubusercontent.com/$owner/$repo/main/ring"
    
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .build()
    
    private val gson = Gson()

    suspend fun getRingtones(): List<Ringtone> = withContext(Dispatchers.IO) {
        val ringtones = mutableListOf<Ringtone>()
        
        try {
            val url = "$baseUrl/repos/$owner/$repo/contents/ring"
            val request = Request.Builder().url(url).build()
            
            val response = client.newCall(request).execute()
            val body = response.body?.string()
            
            if (body != null) {
                val type = object : TypeToken<List<GithubContent>>() {}.type
                val contents: List<GithubContent> = gson.fromJson(body, type)
                
                contents
                    .filter { it.type == "file" && isAudio(it.name) }
                    .forEach { content ->
                        val rawName = content.name.substringBeforeLast(".")
                        val name = formatRingtoneName(rawName)
                        
                        ringtones.add(
                            Ringtone(
                                id = content.name,
                                name = name,
                                uri = content.downloadUrl ?: "$rawBaseUrl/${content.name}"
                            )
                        )
                    }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        ringtones.sortedBy { it.name }
    }
    
    private fun isAudio(fileName: String): Boolean {
        val audioExtensions = listOf(".mp3", ".ogg", ".m4a", ".wav", ".aac")
        return audioExtensions.any { fileName.lowercase().endsWith(it) }
    }
    
    private fun formatRingtoneName(rawName: String): String {
        return rawName
            .replace("_", " ")
            .split("-")
            .joinToString(" ") { word ->
                word.lowercase().replaceFirstChar { 
                    if (word.isNotEmpty()) it.uppercase() else "" 
                }
            }.trim()
    }
}

data class GithubContent(
    val name: String,
    val path: String,
    val type: String,
    val downloadUrl: String?
)
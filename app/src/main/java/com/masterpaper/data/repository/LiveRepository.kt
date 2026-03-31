package com.masterpaper.data.repository

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.masterpaper.data.model.GithubContent
import com.masterpaper.data.model.LiveVideo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import okhttp3.OkHttpClient
import okhttp3.Request

class LiveRepository {

    private val owner = "agdalasv"
    private val repo = "Wallpaper"
    private val baseUrl = "https://api.github.com"
    private val rawBaseUrl = "https://raw.githubusercontent.com/$owner/$repo/main"
    private val folderName = "live"

    private val token = com.masterpaper.BuildConfig.GITHUB_TOKEN

    private val client = OkHttpClient.Builder()
        .addInterceptor { chain ->
            val request = chain.request().newBuilder()
                .addHeader("Authorization", "Bearer $token")
                .addHeader("Accept", "application/vnd.github+json")
                .build()
            chain.proceed(request)
        }
        .build()

    private val gson = Gson()

    fun getLiveVideos(): Flow<List<LiveVideo>> = flow {
        val videos = fetchLiveVideos()
        emit(videos)
    }.flowOn(Dispatchers.IO)

    private suspend fun fetchLiveVideos(): List<LiveVideo> = withContext(Dispatchers.IO) {
        try {
            val url = "$baseUrl/repos/$owner/$repo/contents/$folderName"
            val request = Request.Builder().url(url).build()

            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: return@withContext emptyList()

            val type = object : TypeToken<List<GithubContent>>() {}.type
            val contents: List<GithubContent> = gson.fromJson(body, type)

            contents
                .filter { it.type == "file" && isVideo(it.name) }
                .map { content ->
                    LiveVideo(
                        id = content.sha,
                        name = formatVideoName(content.name),
                        downloadUrl = content.downloadUrl ?: "",
                        thumbnailUrl = content.downloadUrl ?: ""
                    )
                }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    private fun isVideo(fileName: String): Boolean {
        val videoExtensions = listOf(".mp4", ".webm", ".mov", ".avi")
        return videoExtensions.any { fileName.lowercase().endsWith(it) }
    }

    private fun formatVideoName(rawName: String): String {
        return rawName.substringBeforeLast(".")
            .replace("_", " ")
            .split("-")
            .joinToString(" ") { word ->
                word.lowercase().replaceFirstChar {
                    if (word.isNotEmpty()) it.uppercase() else ""
                }
            }.trim()
    }
}

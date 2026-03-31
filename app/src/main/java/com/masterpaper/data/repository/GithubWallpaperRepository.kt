package com.masterpaper.data.repository

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.masterpaper.data.model.Category
import com.masterpaper.data.model.GithubContent
import com.masterpaper.data.model.Wallpaper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

class GithubWallpaperRepository : WallpaperRepository {

    private val owner = "agdalasv"
    private val repo = "Wallpaper"
    private val baseUrl = "https://api.github.com"
    private val rawBaseUrl = "https://raw.githubusercontent.com/$owner/$repo/main"
    
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

    override fun getCategories(): Flow<List<Category>> = flow {
        val categories = fetchCategories()
        emit(categories)
    }.flowOn(Dispatchers.IO)

    override fun getRecentWallpapers(): Flow<List<Wallpaper>> = flow {
        val wallpapers = mutableListOf<Wallpaper>()
        val categories = fetchCategories()
        
        for (category in categories) {
            try {
                val categoryWallpapers = fetchWallpapersFromCategory(category.id)
                wallpapers.addAll(categoryWallpapers)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        
        emit(wallpapers.take(20))
    }.flowOn(Dispatchers.IO)

    override fun getWallpapersByCategory(categoryId: String): Flow<List<Wallpaper>> = flow {
        val wallpapers = fetchWallpapersFromCategory(categoryId)
        emit(wallpapers)
    }.flowOn(Dispatchers.IO)

    override fun getWallpaperById(wallpaperId: String): Flow<Wallpaper?> = flow {
        val categories = fetchCategories()
        
        for (category in categories) {
            try {
                val wallpapers = fetchWallpapersFromCategory(category.id)
                val found = wallpapers.find { it.id == wallpaperId }
                if (found != null) {
                    emit(found)
                    return@flow
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        
        emit(null)
    }.flowOn(Dispatchers.IO)

    private suspend fun fetchCategories(): List<Category> = withContext(Dispatchers.IO) {
        val url = "$baseUrl/repos/$owner/$repo/contents"
        val request = Request.Builder().url(url).build()
        
        val response = client.newCall(request).execute()
        val body = response.body?.string() ?: return@withContext emptyList()
        
        val type = object : TypeToken<List<GithubContent>>() {}.type
        val contents: List<GithubContent> = gson.fromJson(body, type)
        
        contents
            .filter { it.type == "dir" && it.name != "ring" && it.name != "live" }
            .map { content ->
                Category(
                    id = content.name,
                    name = content.name.replace("_", " ").replaceFirstChar { it.uppercase() },
                    imageUrl = "$rawBaseUrl/${content.name}/thumbnail.jpg",
                    wallpaperCount = 0
                )
            }
    }

    private suspend fun fetchWallpapersFromCategory(categoryName: String): List<Wallpaper> = withContext(Dispatchers.IO) {
        try {
            val url = "$baseUrl/repos/$owner/$repo/contents/$categoryName"
            val request = Request.Builder().url(url).build()
            
            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: return@withContext emptyList()
            
            val type = object : TypeToken<List<GithubContent>>() {}.type
            val contents: List<GithubContent> = gson.fromJson(body, type)
            
            contents
                .filter { it.type == "file" && isImage(it.name) }
                .map { content ->
                    Wallpaper(
                        id = content.sha,
                        name = content.name,
                        url = content.downloadUrl ?: "",
                        thumbnailUrl = content.downloadUrl ?: "",
                        categoryId = categoryName
                    )
                }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    private fun isImage(fileName: String): Boolean {
        val imageExtensions = listOf(".jpg", ".jpeg", ".png", ".webp", ".gif")
        return imageExtensions.any { fileName.lowercase().endsWith(it) }
    }
}

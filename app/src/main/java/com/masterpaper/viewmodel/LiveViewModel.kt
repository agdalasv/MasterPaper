package com.masterpaper.viewmodel

import android.app.Application
import android.app.WallpaperManager
import android.content.ComponentName
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.masterpaper.data.model.LiveVideo
import com.masterpaper.data.repository.LiveRepository
import com.masterpaper.wallpaper.VideoLiveWallpaperService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

class LiveViewModel(application: Application) : AndroidViewModel(application) {

    private val liveRepository = LiveRepository()
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    private val _videos = MutableStateFlow<List<LiveVideo>>(emptyList())
    val videos: StateFlow<List<LiveVideo>> = _videos.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _downloadProgress = MutableStateFlow(0)
    val downloadProgress: StateFlow<Int> = _downloadProgress.asStateFlow()

    private val _setWallpaperSuccess = MutableStateFlow<Boolean?>(null)
    val setWallpaperSuccess: StateFlow<Boolean?> = _setWallpaperSuccess.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    init {
        loadVideos()
    }

    fun loadVideos() {
        viewModelScope.launch {
            _isLoading.value = true
            liveRepository.getLiveVideos().collect { list ->
                _videos.value = list
                _isLoading.value = false
            }
        }
    }

    fun setLiveWallpaper(video: LiveVideo, onDone: () -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            _downloadProgress.value = 0
            _errorMessage.value = null
            
            val success = downloadVideoSetWallpaper(getApplication(), video)
            _setWallpaperSuccess.value = success
            
            if (!success) {
                kotlinx.coroutines.delay(3000)
                _setWallpaperSuccess.value = null
            }
            _isLoading.value = false
            onDone()
        }
    }

    fun clearSuccessMessage() {
        _setWallpaperSuccess.value = null
        _errorMessage.value = null
        _downloadProgress.value = 0
    }

    private suspend fun downloadVideoSetWallpaper(context: android.content.Context, video: LiveVideo): Boolean = withContext(Dispatchers.IO) {
        try {
            _downloadProgress.value = 5
            
            // Save location
            val masterPaperDir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "Masterpaper")
            val liveDir = File(masterPaperDir, "live")
            
            try {
                if (!liveDir.exists()) {
                    liveDir.mkdirs()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Cannot create external dir: ${e.message}")
            }
            
            val videoFileName = "live_${System.currentTimeMillis()}.mp4"
            val externalVideoFile = if (liveDir.exists()) {
                File(liveDir, videoFileName)
            } else {
                File(context.cacheDir, videoFileName)
            }
            
            _downloadProgress.value = 15
            
            // Download video
            val request = Request.Builder()
                .url(video.downloadUrl)
                .build()

            val response = httpClient.newCall(request).execute()
            
            if (!response.isSuccessful) {
                _errorMessage.value = "Error al descargar: ${response.code}"
                return@withContext false
            }
            
            val body = response.body
            val contentLength = body?.contentLength() ?: -1
            
            body?.byteStream()?.use { inputStream ->
                val outputStream = FileOutputStream(externalVideoFile)
                val buffer = ByteArray(8192)
                var bytesRead: Int
                var totalBytesRead = 0L
                
                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                    outputStream.write(buffer, 0, bytesRead)
                    totalBytesRead += bytesRead
                    
                    if (contentLength > 0) {
                        val progress = ((totalBytesRead * 25) / contentLength + 15).toInt().coerceIn(15, 40)
                        _downloadProgress.value = progress
                    }
                }
                
                outputStream.flush()
                outputStream.close()
            }
            
            _downloadProgress.value = 40
            
            if (!externalVideoFile.exists() || externalVideoFile.length() == 0L) {
                _errorMessage.value = "Error al guardar video"
                return@withContext false
            }
            
            // Save to internal storage
            try {
                val internalDir = File(context.filesDir, "wallpaper")
                if (!internalDir.exists()) {
                    internalDir.mkdirs()
                }
                val internalFile = File(internalDir, "live_wallpaper.mp4")
                externalVideoFile.copyTo(internalFile, overwrite = true)
                
                // Save path for service
                val prefs = context.getSharedPreferences("live_wallpaper_prefs", Context.MODE_PRIVATE)
                prefs.edit().putString("video_path", internalFile.absolutePath).apply()
            } catch (e: Exception) {
                Log.w(TAG, "Could not copy to internal: ${e.message}")
            }
            
            _downloadProgress.value = 60
            
            // Extract frame from video
            val bitmap = extractFirstFrame(externalVideoFile.absolutePath)
            if (bitmap == null) {
                _errorMessage.value = "No se pudo procesar el video"
                return@withContext false
            }
            
            _downloadProgress.value = 75
            
            // Save frame as image
            val frameFileName = videoFileName.replace(".mp4", ".jpg")
            val frameFile = File(liveDir, frameFileName)
            FileOutputStream(frameFile).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
            }
            
            // Add to MediaStore
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && liveDir.exists()) {
                    val contentValues = ContentValues().apply {
                        put(MediaStore.Images.Media.DISPLAY_NAME, frameFileName)
                        put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                        put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/Masterpaper/live")
                    }
                    context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                }
            } catch (e: Exception) {
                Log.w(TAG, "Could not add to MediaStore: ${e.message}")
            }
            
            _downloadProgress.value = 85
            
            // Set as wallpaper - directly set bitmap
            val wallpaperManager = WallpaperManager.getInstance(context)
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                // Try to set lock screen first
                try {
                    wallpaperManager.setBitmap(bitmap, null, true, WallpaperManager.FLAG_LOCK)
                    Log.d(TAG, "Set lock screen wallpaper successfully")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to set lock screen: ${e.message}")
                    // Fallback: try setting both
                    try {
                        wallpaperManager.setBitmap(bitmap)
                    } catch (e2: Exception) {
                        Log.e(TAG, "Failed to set wallpaper: ${e2.message}")
                    }
                }
            } else {
                wallpaperManager.setBitmap(bitmap)
            }
            
            bitmap.recycle()
            
            // Also try to open live wallpaper picker for video
            withContext(Dispatchers.Main) {
                try {
                    val intent = Intent("android.service.wallpaper.CHANGE_LIVE_WALLPAPER").apply {
                        putExtra("android.service.wallpaper.LIVE_WALLPAPER_COMPONENT",
                            ComponentName(context, VideoLiveWallpaperService::class.java))
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                } catch (e: Exception) {
                    Log.e(TAG, "Could not open live wallpaper picker: ${e.message}")
                }
            }
            
            _downloadProgress.value = 100
            
            true
        } catch (e: Exception) {
            e.printStackTrace()
            _errorMessage.value = "Error: ${e.message}"
            false
        }
    }

    private fun extractFirstFrame(videoPath: String): Bitmap? {
        return try {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(videoPath)
            val bitmap = retriever.getFrameAtTime(1000000, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
            retriever.release()
            bitmap
        } catch (e: Exception) {
            Log.e(TAG, "Error extracting frame: ${e.message}")
            null
        }
    }

    companion object {
        private const val TAG = "LiveViewModel"
    }
}

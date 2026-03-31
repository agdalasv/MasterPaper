package com.masterpaper.wallpaper

import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.os.Environment
import android.service.wallpaper.WallpaperService
import android.util.Log
import android.view.SurfaceHolder
import java.io.File

class VideoLiveWallpaperService : WallpaperService() {

    override fun onCreateEngine(): Engine {
        return VideoWallpaperEngine()
    }

    inner class VideoWallpaperEngine : Engine() {

        private var mediaPlayer: MediaPlayer? = null
        private var currentVideoPath: String? = null

        override fun onSurfaceCreated(holder: SurfaceHolder) {
            super.onSurfaceCreated(holder)
            Log.d(TAG, "Surface created")
            loadAndPlayVideo(holder)
        }

        override fun onSurfaceChanged(
            holder: SurfaceHolder,
            format: Int,
            width: Int,
            height: Int
        ) {
            super.onSurfaceChanged(holder, format, width, height)
            Log.d(TAG, "Surface changed: ${width}x${height}")
        }

        override fun onVisibilityChanged(visible: Boolean) {
            super.onVisibilityChanged(visible)
            Log.d(TAG, "Visibility changed: $visible")
            if (visible) {
                mediaPlayer?.start()
            } else {
                mediaPlayer?.pause()
            }
        }

        override fun onSurfaceDestroyed(holder: SurfaceHolder) {
            super.onSurfaceDestroyed(holder)
            Log.d(TAG, "Surface destroyed")
            releasePlayer()
        }

        override fun onDestroy() {
            super.onDestroy()
            releasePlayer()
        }

        private fun loadAndPlayVideo(holder: SurfaceHolder) {
            try {
                // Try multiple locations to find the video
                val videoPath = findVideoFile()
                
                if (videoPath == null) {
                    Log.e(TAG, "No video file found in any location!")
                    return
                }
                
                if (currentVideoPath == videoPath && mediaPlayer != null) {
                    Log.d(TAG, "Video already loaded, resuming")
                    mediaPlayer?.start()
                    return
                }
                
                currentVideoPath = videoPath
                Log.d(TAG, "Loading video from: $videoPath")
                
                releasePlayer()
                
                mediaPlayer = MediaPlayer().apply {
                    setDataSource(videoPath)
                    setDisplay(holder)
                    isLooping = true
                    
                    setOnPreparedListener { mp ->
                        Log.d(TAG, "Video prepared, starting playback")
                        mp.start()
                    }
                    
                    setOnErrorListener { mp, what, extra ->
                        Log.e(TAG, "MediaPlayer error: what=$what extra=$extra")
                        mp.release()
                        mediaPlayer = null
                        true
                    }
                    
                    setOnCompletionListener {
                        Log.d(TAG, "Video completed, looping")
                    }
                    
                    prepareAsync()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading video: ${e.message}", e)
            }
        }

        private fun findVideoFile(): String? {
            // Priority 1: Check SharedPreferences for last used video
            try {
                val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                val savedPath = prefs.getString(KEY_VIDEO_PATH, null)
                if (savedPath != null && File(savedPath).exists()) {
                    Log.d(TAG, "Found video from prefs: $savedPath")
                    return savedPath
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error reading prefs: ${e.message}")
            }

            // Priority 2: Internal storage
            val internalPath = File(filesDir, "wallpaper/live_wallpaper.mp4").absolutePath
            if (File(internalPath).exists()) {
                Log.d(TAG, "Found video in internal: $internalPath")
                return internalPath
            }

            // Priority 3: External storage - most recent mp4
            val externalDirs = listOf(
                File("/storage/emulated/0/Pictures/Masterpaper/live"),
                File("/storage/emulated/0/DCIM/Masterpaper/live"),
                File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "Masterpaper/live")
            )

            for (dir in externalDirs) {
                if (dir.exists() && dir.isDirectory) {
                    val mp4Files = dir.listFiles()?.filter { it.extension.equals("mp4", ignoreCase = true) }
                    if (!mp4Files.isNullOrEmpty()) {
                        val latest = mp4Files.maxByOrNull { it.lastModified() }
                        if (latest != null) {
                            Log.d(TAG, "Found video in external: ${latest.absolutePath}")
                            return latest.absolutePath
                        }
                    }
                }
            }

            return null
        }

        private fun releasePlayer() {
            try {
                mediaPlayer?.stop()
                mediaPlayer?.release()
            } catch (e: Exception) {
                Log.e(TAG, "Error releasing player: ${e.message}")
            }
            mediaPlayer = null
        }
    }

    companion object {
        private const val TAG = "VideoLiveWallpaper"
        private const val PREFS_NAME = "live_wallpaper_prefs"
        private const val KEY_VIDEO_PATH = "video_path"
        
        private const val ACTION_CHANGE_LIVE_WALLPAPER = "android.service.wallpaper.CHANGE_LIVE_WALLPAPER"
        private const val EXTRA_LIVE_WALLPAPER_COMPONENT = "android.service.wallpaper.LIVE_WALLPAPER_COMPONENT"

        fun setLiveWallpaper(context: Context): Intent {
            return Intent().apply {
                action = ACTION_CHANGE_LIVE_WALLPAPER
                putExtra(
                    EXTRA_LIVE_WALLPAPER_COMPONENT,
                    android.content.ComponentName(context, VideoLiveWallpaperService::class.java)
                )
            }
        }

        fun saveVideoPath(context: Context, videoPath: String) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().putString(KEY_VIDEO_PATH, videoPath).apply()
            Log.d(TAG, "Saved video path: $videoPath")
        }
    }
}

package com.masterpaper.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.masterpaper.MainActivity
import com.masterpaper.R
import com.masterpaper.data.repository.PreferencesRepository
import kotlinx.coroutines.flow.first
import okhttp3.OkHttpClient
import okhttp3.Request
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class VersionCheckWorker(
    private val context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        const val CHANNEL_ID = "masterpaper_updates"
        const val NOTIFICATION_ID = 1001
        const val WORK_NAME = "version_check_work"
        const val LAST_CHECKED_VERSION = "last_checked_version"
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val prefsRepository = PreferencesRepository(context)
            val currentVersion = prefsRepository.getAppVersion().first()
            val savedVersion = prefsRepository.getString(LAST_CHECKED_VERSION).first() ?: "0"

            val url = "https://api.github.com/repos/agdalasv/MasterPaper/contents"
            val client = OkHttpClient()
            val request = Request.Builder().url(url).build()
            val response = client.newCall(request).execute()

            if (response.isSuccessful) {
                val body = response.body?.string() ?: return@withContext Result.failure()
                
                val type = object : TypeToken<List<GithubContent>>() {}.type
                val contents: List<GithubContent> = Gson().fromJson(body, type)
                
                val hasNewContent = contents.any { it.name == "UPDATE_AVAILABLE" }
                
                if (hasNewContent && savedVersion != "UPDATE_AVAILABLE") {
                    showNotification()
                    prefsRepository.saveString(LAST_CHECKED_VERSION, "UPDATE_AVAILABLE")
                }
            }

            Result.success()
        } catch (e: Exception) {
            Result.failure()
        }
    }

    private fun showNotification() {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Actualizaciones",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notificaciones de nuevas versiones"
            }
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_info_details)
            .setContentTitle("Nueva actualización disponible")
            .setContentText("Hay nuevo contenido disponible en MasterPaper")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    private data class GithubContent(
        val name: String,
        val type: String,
        val sha: String
    )
}
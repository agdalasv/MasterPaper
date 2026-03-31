package com.masterpaper.viewmodel

import android.app.Application
import android.content.ContentValues
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.provider.Settings
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.masterpaper.data.model.Ringtone
import com.masterpaper.data.model.RingtoneType
import com.masterpaper.data.repository.RingtoneRepository
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

class RingtoneViewModel(application: Application) : AndroidViewModel(application) {
    
    private val ringtoneRepository = RingtoneRepository(application)
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .build()
    
    private val _ringtones = MutableStateFlow<List<Ringtone>>(emptyList())
    val ringtones: StateFlow<List<Ringtone>> = _ringtones.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _playingRingtone = MutableStateFlow<Ringtone?>(null)
    val playingRingtone: StateFlow<Ringtone?> = _playingRingtone.asStateFlow()
    
    private val _setRingtoneSuccess = MutableStateFlow<Boolean?>(null)
    val setRingtoneSuccess: StateFlow<Boolean?> = _setRingtoneSuccess.asStateFlow()
    
    private val _needsWriteSettingsPermission = MutableStateFlow(false)
    val needsWriteSettingsPermission: StateFlow<Boolean> = _needsWriteSettingsPermission.asStateFlow()
    
    init {
        loadRingtones()
    }
    
    fun loadRingtones() {
        viewModelScope.launch {
            _isLoading.value = true
            val list = ringtoneRepository.getRingtones()
            _ringtones.value = list
            _isLoading.value = false
        }
    }
    
    fun checkWriteSettingsPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.System.canWrite(getApplication())
        } else {
            true
        }
    }
    
    fun requestWriteSettingsPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            _needsWriteSettingsPermission.value = true
        }
    }
    
    @Suppress("UNUSED_PARAMETER")
    fun onWriteSettingsPermissionResult(granted: Boolean) {
        _needsWriteSettingsPermission.value = false
    }
    
    fun playRingtone(ringtone: Ringtone) {
        _playingRingtone.value = ringtone
    }
    
    fun stopPlaying() {
        _playingRingtone.value = null
    }
    
    fun setAsRingtone(ringtone: Ringtone, type: RingtoneType) {
        viewModelScope.launch {
            if (!checkWriteSettingsPermission()) {
                requestWriteSettingsPermission()
                _setRingtoneSuccess.value = false
                return@launch
            }
            
            _isLoading.value = true
            val success = setRingtone(getApplication(), ringtone, type)
            _setRingtoneSuccess.value = success
            if (!success) {
                kotlinx.coroutines.delay(2000)
                _setRingtoneSuccess.value = null
            }
            _isLoading.value = false
        }
    }
    
    fun clearSuccessMessage() {
        _setRingtoneSuccess.value = null
    }
    
    private suspend fun setRingtone(context: android.content.Context, ringtone: Ringtone, type: RingtoneType): Boolean = withContext(Dispatchers.IO) {
        try {
            val fileUri = downloadAndSaveRingtone(context, ringtone, type)
            if (fileUri != null) {
                val ringtoneType = when (type) {
                    RingtoneType.RINGTONE -> RingtoneManager.TYPE_RINGTONE
                    RingtoneType.NOTIFICATION -> RingtoneManager.TYPE_NOTIFICATION
                    RingtoneType.ALARM -> RingtoneManager.TYPE_ALARM
                }
                
                RingtoneManager.setActualDefaultRingtoneUri(context, ringtoneType, fileUri)
                true
            } else {
                false
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
    
    private fun downloadAndSaveRingtone(context: android.content.Context, ringtone: Ringtone, type: RingtoneType): Uri? {
        return try {
            val fileName = ringtone.id.substringBeforeLast(".")
            val extension = ringtone.id.substringAfterLast(".")
            val mimeType = when (extension.lowercase()) {
                "mp3" -> "audio/mpeg"
                "ogg" -> "audio/ogg"
                "m4a" -> "audio/mp4"
                "wav" -> "audio/wav"
                "aac" -> "audio/aac"
                else -> "audio/mpeg"
            }
            
            val cacheDir = File(context.cacheDir, "ringtones")
            if (!cacheDir.exists()) cacheDir.mkdirs()
            val tempFile = File(cacheDir, "${fileName}.${extension}")
            
            if (!tempFile.exists()) {
                val request = Request.Builder().url(ringtone.uri).build()
                val response = httpClient.newCall(request).execute()
                
                response.body?.byteStream()?.use { inputStream ->
                    FileOutputStream(tempFile).use { output ->
                        inputStream.copyTo(output)
                    }
                }
            }
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, "${fileName}.${extension}")
                    put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_RINGTONES)
                    put(MediaStore.Audio.Media.IS_RINGTONE, true)
                    put(MediaStore.Audio.Media.IS_NOTIFICATION, type == RingtoneType.NOTIFICATION)
                    put(MediaStore.Audio.Media.IS_ALARM, type == RingtoneType.ALARM)
                }
                
                val resolver = context.contentResolver
                val uri = resolver.insert(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, contentValues)
                
                uri?.let { mediaUri ->
                    resolver.openOutputStream(mediaUri)?.use { outputStream ->
                        tempFile.inputStream().use { inputStream ->
                            inputStream.copyTo(outputStream)
                        }
                    }
                }
                
                uri
            } else {
                val ringtoneDir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_RINGTONES), "")
                if (!ringtoneDir.exists()) ringtoneDir.mkdirs()
                
                val destFile = File(ringtoneDir, "${fileName}.${extension}")
                tempFile.copyTo(destFile, overwrite = true)
                
                val values = ContentValues().apply {
                    put(MediaStore.MediaColumns.DATA, destFile.absolutePath)
                    put(MediaStore.MediaColumns.TITLE, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                    put(MediaStore.Audio.Media.IS_RINGTONE, true)
                }
                
                val resolver = context.contentResolver
                resolver.insert(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, values)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            try {
                val fileName = ringtone.id.substringBeforeLast(".")
                val extension = ringtone.id.substringAfterLast(".")
                
                val cacheDir = File(context.cacheDir, "ringtones")
                if (!cacheDir.exists()) cacheDir.mkdirs()
                val tempFile = File(cacheDir, "${fileName}.${extension}")
                
                if (!tempFile.exists()) {
                    val request = Request.Builder().url(ringtone.uri).build()
                    val response = httpClient.newCall(request).execute()
                    
                    response.body?.byteStream()?.use { inputStream ->
                        FileOutputStream(tempFile).use { output ->
                            inputStream.copyTo(output)
                        }
                    }
                }
                
                Uri.fromFile(tempFile)
            } catch (e2: Exception) {
                e2.printStackTrace()
                null
            }
        }
    }
}
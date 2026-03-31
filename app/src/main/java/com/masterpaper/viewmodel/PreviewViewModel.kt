package com.masterpaper.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.masterpaper.data.model.Wallpaper
import com.masterpaper.data.repository.GithubWallpaperRepository
import com.masterpaper.data.repository.WallpaperRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class PreviewViewModel(
    private val repository: WallpaperRepository = GithubWallpaperRepository()
) : ViewModel() {

    private val _wallpaper = MutableStateFlow<Wallpaper?>(null)
    val wallpaper: StateFlow<Wallpaper?> = _wallpaper.asStateFlow()

    private val _isDownloading = MutableStateFlow(false)
    val isDownloading: StateFlow<Boolean> = _isDownloading.asStateFlow()

    private val _isSettingWallpaper = MutableStateFlow(false)
    val isSettingWallpaper: StateFlow<Boolean> = _isSettingWallpaper.asStateFlow()

    private val _downloadSuccess = MutableStateFlow<Boolean?>(null)
    val downloadSuccess: StateFlow<Boolean?> = _downloadSuccess.asStateFlow()

    private val _wallpaperSetSuccess = MutableStateFlow<Boolean?>(null)
    val wallpaperSetSuccess: StateFlow<Boolean?> = _wallpaperSetSuccess.asStateFlow()

    fun loadWallpaper(wallpaperId: String) {
        viewModelScope.launch {
            repository.getWallpaperById(wallpaperId).collect { wallpaper ->
                _wallpaper.value = wallpaper
            }
        }
    }

    fun setDownloading(downloading: Boolean) {
        _isDownloading.value = downloading
    }

    fun setSettingWallpaper(setting: Boolean) {
        _isSettingWallpaper.value = setting
    }

    fun setDownloadSuccess(success: Boolean) {
        _downloadSuccess.value = success
    }

    fun setWallpaperSetSuccess(success: Boolean) {
        _wallpaperSetSuccess.value = success
    }

    fun clearMessages() {
        _downloadSuccess.value = null
        _wallpaperSetSuccess.value = null
    }
}

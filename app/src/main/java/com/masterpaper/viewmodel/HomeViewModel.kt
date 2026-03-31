package com.masterpaper.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.masterpaper.data.model.UiState
import com.masterpaper.data.model.Wallpaper
import com.masterpaper.data.repository.GithubWallpaperRepository
import com.masterpaper.data.repository.WallpaperRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class HomeViewModel(
    private val repository: WallpaperRepository = GithubWallpaperRepository()
) : ViewModel() {

    private val _wallpapers = MutableStateFlow<UiState<List<Wallpaper>>>(UiState.Loading)
    val wallpapers: StateFlow<UiState<List<Wallpaper>>> = _wallpapers.asStateFlow()

    init {
        loadWallpapers()
    }

    fun loadWallpapers() {
        viewModelScope.launch {
            _wallpapers.value = UiState.Loading
            repository.getRecentWallpapers().collect { wallpapersList ->
                _wallpapers.value = UiState.Success(wallpapersList)
            }
        }
    }
}

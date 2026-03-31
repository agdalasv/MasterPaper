package com.masterpaper.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.masterpaper.data.model.Category
import com.masterpaper.data.model.UiState
import com.masterpaper.data.model.Wallpaper
import com.masterpaper.data.repository.GithubWallpaperRepository
import com.masterpaper.data.repository.WallpaperRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class CategoriesViewModel(
    private val repository: WallpaperRepository = GithubWallpaperRepository()
) : ViewModel() {

    private val _categories = MutableStateFlow<UiState<List<Category>>>(UiState.Loading)
    val categories: StateFlow<UiState<List<Category>>> = _categories.asStateFlow()

    private val _categoryWallpapers = MutableStateFlow<UiState<List<Wallpaper>>>(UiState.Loading)
    val categoryWallpapers: StateFlow<UiState<List<Wallpaper>>> = _categoryWallpapers.asStateFlow()

    private val _selectedCategory = MutableStateFlow<Category?>(null)
    val selectedCategory: StateFlow<Category?> = _selectedCategory.asStateFlow()

    init {
        loadCategories()
    }

    fun loadCategories() {
        viewModelScope.launch {
            _categories.value = UiState.Loading
            repository.getCategories().collect { categoriesList ->
                _categories.value = UiState.Success(categoriesList)
            }
        }
    }

    fun selectCategory(category: Category) {
        _selectedCategory.value = category
        loadWallpapersByCategory(category.id)
    }

    fun loadWallpapersByCategory(categoryId: String) {
        viewModelScope.launch {
            _categoryWallpapers.value = UiState.Loading
            repository.getWallpapersByCategory(categoryId).collect { wallpapersList ->
                _categoryWallpapers.value = UiState.Success(wallpapersList)
            }
        }
    }

    fun clearSelection() {
        _selectedCategory.value = null
    }
}

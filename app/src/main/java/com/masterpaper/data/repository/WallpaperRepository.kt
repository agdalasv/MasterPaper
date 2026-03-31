package com.masterpaper.data.repository

import com.masterpaper.data.model.Category
import com.masterpaper.data.model.Wallpaper
import kotlinx.coroutines.flow.Flow

interface WallpaperRepository {
    fun getCategories(): Flow<List<Category>>
    fun getRecentWallpapers(): Flow<List<Wallpaper>>
    fun getWallpapersByCategory(categoryId: String): Flow<List<Wallpaper>>
    fun getWallpaperById(wallpaperId: String): Flow<Wallpaper?>
}

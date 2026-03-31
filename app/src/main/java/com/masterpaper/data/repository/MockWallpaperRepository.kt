package com.masterpaper.data.repository

import com.masterpaper.data.model.Category
import com.masterpaper.data.model.Wallpaper
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class MockWallpaperRepository : WallpaperRepository {

    private val mockCategories = listOf(
        Category("1", "Abstractos", "https://picsum.photos/seed/abstract/400/400", 12),
        Category("2", "Paisajes", "https://picsum.photos/seed/landscape/400/400", 15),
        Category("3", "Animales", "https://picsum.photos/seed/animals/400/400", 10),
        Category("4", "Tecnologia", "https://picsum.photos/seed/tech/400/400", 8),
        Category("5", "Deportes", "https://picsum.photos/seed/sports/400/400", 6),
        Category("6", "Arte", "https://picsum.photos/seed/art/400/400", 14)
    )

    private val mockWallpapers = listOf(
        Wallpaper("w1", "Abstract Blue", "https://picsum.photos/seed/w1/1080/1920", "https://picsum.photos/seed/w1/400/800", "1"),
        Wallpaper("w2", "Mountain View", "https://picsum.photos/seed/w2/1080/1920", "https://picsum.photos/seed/w2/400/800", "2"),
        Wallpaper("w3", "Lion Portrait", "https://picsum.photos/seed/w3/1080/1920", "https://picsum.photos/seed/w3/400/800", "3"),
        Wallpaper("w4", "Tech Future", "https://picsum.photos/seed/w4/1080/1920", "https://picsum.photos/seed/w4/400/800", "4"),
        Wallpaper("w5", "Football Action", "https://picsum.photos/seed/w5/1080/1920", "https://picsum.photos/seed/w5/400/800", "5"),
        Wallpaper("w6", "Modern Art", "https://picsum.photos/seed/w6/1080/1920", "https://picsum.photos/seed/w6/400/800", "6"),
        Wallpaper("w7", "Ocean Waves", "https://picsum.photos/seed/w7/1080/1920", "https://picsum.photos/seed/w7/400/800", "1"),
        Wallpaper("w8", "Sunset Beach", "https://picsum.photos/seed/w8/1080/1920", "https://picsum.photos/seed/w8/400/800", "2"),
        Wallpaper("w9", "Eagle Flight", "https://picsum.photos/seed/w9/1080/1920", "https://picsum.photos/seed/w9/400/800", "3"),
        Wallpaper("w10", "Robot AI", "https://picsum.photos/seed/w10/1080/1920", "https://picsum.photos/seed/w10/400/800", "4"),
        Wallpaper("w11", "Basketball", "https://picsum.photos/seed/w11/1080/1920", "https://picsum.photos/seed/w11/400/800", "5"),
        Wallpaper("w12", "Graffiti", "https://picsum.photos/seed/w12/1080/1920", "https://picsum.photos/seed/w12/400/800", "6")
    )

    override fun getCategories(): Flow<List<Category>> = flow {
        delay(500)
        emit(mockCategories)
    }

    override fun getRecentWallpapers(): Flow<List<Wallpaper>> = flow {
        delay(800)
        emit(mockWallpapers.sortedByDescending { it.id })
    }

    override fun getWallpapersByCategory(categoryId: String): Flow<List<Wallpaper>> = flow {
        delay(600)
        emit(mockWallpapers.filter { it.categoryId == categoryId })
    }

    override fun getWallpaperById(wallpaperId: String): Flow<Wallpaper?> = flow {
        delay(300)
        emit(mockWallpapers.find { it.id == wallpaperId })
    }
}

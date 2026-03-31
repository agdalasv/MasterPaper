package com.masterpaper.data.model

data class Wallpaper(
    val id: String,
    val name: String,
    val url: String,
    val thumbnailUrl: String,
    val categoryId: String,
    val width: Int = 1080,
    val height: Int = 1920,
    val fileSize: Long = 0L
)

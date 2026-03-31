package com.masterpaper.data.model

data class LiveVideo(
    val id: String,
    val name: String,
    val downloadUrl: String,
    val thumbnailUrl: String,
    val duration: String = ""
)

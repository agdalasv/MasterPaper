package com.masterpaper.data.model

import com.google.gson.annotations.SerializedName

data class GithubContent(
    val name: String,
    val path: String,
    val sha: String,
    val size: Int,
    val url: String,
    @SerializedName("html_url")
    val htmlUrl: String,
    @SerializedName("git_url")
    val gitUrl: String,
    @SerializedName("download_url")
    val downloadUrl: String?,
    val type: String
)

data class GithubRepoContent(
    val content: String?,
    val encoding: String?,
    @SerializedName("download_url")
    val downloadUrl: String?
)

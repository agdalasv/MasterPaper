package com.masterpaper.data.remote

import com.masterpaper.data.model.GithubContent
import com.masterpaper.data.model.GithubRepoContent
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Url

interface GithubApiService {

    @GET("repos/{owner}/{repo}/contents")
    suspend fun getRepositoryContents(
        @Path("owner") owner: String,
        @Path("repo") repo: String
    ): List<GithubContent>

    @GET("repos/{owner}/{repo}/contents/{path}")
    suspend fun getContentByPath(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("path") path: String
    ): Any

    @GET
    suspend fun downloadFile(@Url url: String): GithubRepoContent
}

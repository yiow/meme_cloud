package com.memecloud.data.api

import com.memecloud.data.model.ApiResponse
import com.google.gson.annotations.SerializedName
import retrofit2.http.GET

data class LocalMemeItem(
    val id: String,
    @SerializedName("file_url") val fileUrl: String,
    @SerializedName("thumbnail_url") val thumbnailUrl: String,
    val description: String,
)

interface DanmakuApi {
    @GET("api/danmaku/memes/local")
    suspend fun getLocalMemes(): ApiResponse<List<LocalMemeItem>>
}

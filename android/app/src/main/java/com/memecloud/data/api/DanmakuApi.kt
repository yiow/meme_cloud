package com.memecloud.data.api

import com.memecloud.data.model.ApiResponse
import com.google.gson.annotations.SerializedName
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.*

data class LocalMemeItem(
    val id: String,
    @SerializedName("file_url") val fileUrl: String,
    @SerializedName("thumbnail_url") val thumbnailUrl: String,
    val description: String,
    val filename: String? = null
)

data class RenameRequest(val new_name: String)

interface DanmakuApi {
    @GET("api/danmaku/memes/local")
    suspend fun getLocalMemes(): ApiResponse<List<LocalMemeItem>>

    @Multipart
    @POST("api/danmaku/emojis/upload")
    suspend fun uploadEmoji(
        @Part file: MultipartBody.Part,
        @Part("description") description: RequestBody
    ): ApiResponse<Map<String, Any>>

    @DELETE("api/danmaku/memes/local/{filename}")
    suspend fun deleteLocalMeme(@Path("filename", encoded = true) filename: String): ApiResponse<Map<String, Any>>

    @PUT("api/danmaku/memes/local/{filename}")
    suspend fun renameLocalMeme(
        @Path("filename", encoded = true) filename: String,
        @Body body: RenameRequest
    ): ApiResponse<Map<String, Any>>
}

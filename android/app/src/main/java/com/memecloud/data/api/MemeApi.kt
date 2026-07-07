package com.memecloud.data.api

import com.memecloud.data.model.ApiResponse
import com.memecloud.data.model.MemeItem
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * 表情包 API — 检索 / 社区 / 收藏
 */
interface MemeApi {

    @GET("api/memes/search")
    suspend fun searchMemes(
        @Query("q") query: String,
        @Query("mode") mode: String = "text",  // text | image | camera
        @Query("top_k") topK: Int = 10
    ): ApiResponse<List<MemeItem>>

    @GET("api/memes/community")
    suspend fun getCommunityFeed(
        @Query("sort") sort: String = "recommend",  // recommend | latest | follow
        @Query("page") page: Int = 1,
        @Query("size") size: Int = 20
    ): ApiResponse<List<MemeItem>>

    @GET("api/memes/{id}")
    suspend fun getMemeDetail(@Path("id") memeId: String): ApiResponse<MemeItem>

    @POST("api/memes/{id}/like")
    suspend fun toggleLike(@Path("id") memeId: String): ApiResponse<Boolean>

    @POST("api/memes/{id}/collect")
    suspend fun toggleCollect(@Path("id") memeId: String): ApiResponse<Boolean>

    @GET("api/memes/hot-tags")
    suspend fun getHotTags(): ApiResponse<List<String>>
}

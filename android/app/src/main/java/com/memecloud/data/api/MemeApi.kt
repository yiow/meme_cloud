package com.memecloud.data.api

import com.memecloud.data.model.*
import retrofit2.http.*

/**
 * 表情包 API — 检索 / 社区
 */
interface MemeApi {

    // ── 检索 ──

    @GET("api/memes/search")
    suspend fun searchMemes(
        @Query("q") query: String,
        @Query("mode") mode: String = "text",
        @Query("top_k") topK: Int = 10
    ): ApiResponse<List<MemeItem>>

    @GET("api/memes/{id}")
    suspend fun getMemeDetail(@Path("id") memeId: String): ApiResponse<MemeItem>

    @POST("api/memes/{id}/like")
    suspend fun toggleLike(@Path("id") memeId: String): ApiResponse<Map<String, Any>>

    @POST("api/memes/{id}/collect")
    suspend fun toggleCollect(@Path("id") memeId: String): ApiResponse<Map<String, Any>>

    @GET("api/memes/hot-tags")
    suspend fun getHotTags(): ApiResponse<List<String>>

    // ── 社区广场 ──

    @GET("api/community/posts")
    suspend fun getCommunityFeed(
        @Query("sort") sort: String = "recommend",
        @Query("page") page: Int = 1,
        @Query("size") size: Int = 20
    ): ApiResponse<PaginatedPosts>

    @GET("api/community/posts/{postId}")
    suspend fun getPostDetail(@Path("postId") postId: Long): ApiResponse<PostDetail>

    @POST("api/community/posts")
    suspend fun createPost(@Body request: PostCreateRequest): ApiResponse<Map<String, Any>>

    @DELETE("api/community/posts/{postId}")
    suspend fun deletePost(@Path("postId") postId: Long): ApiResponse<Map<String, Any>>

    @POST("api/community/posts/{postId}/like")
    suspend fun toggleCommunityLike(@Path("postId") postId: Long): ApiResponse<Map<String, Any>>

    @POST("api/community/posts/{postId}/comments")
    suspend fun addComment(
        @Path("postId") postId: Long,
        @Body request: CommentCreateRequest
    ): ApiResponse<Map<String, Any>>
}

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

    @POST("api/community/posts/{postId}/collect")
    suspend fun toggleCollect(@Path("postId") postId: Long): ApiResponse<Map<String, Any>>

    @GET("api/community/collections")
    suspend fun getCollections(@Query("page") page: Int = 1, @Query("size") size: Int = 20): ApiResponse<PaginatedPosts>

    @POST("api/community/posts/{postId}/comments")
    suspend fun addComment(
        @Path("postId") postId: Long,
        @Body request: CommentCreateRequest
    ): ApiResponse<Map<String, Any>>

    // ── 排行榜 ──

    @GET("api/ranking")
    suspend fun getRanking(
        @Query("period") period: String = "today",
        @Query("size") size: Int = 50
    ): ApiResponse<RankingResponse>

    // ── 关注系统 ──

    @POST("api/follow/{followingId}")
    suspend fun toggleFollow(@Path("followingId") followingId: Long): ApiResponse<FollowToggleResult>

    @GET("api/follow/following/{userId}")
    suspend fun getFollowing(@Path("userId") userId: Long): ApiResponse<FollowListResponse>

    @GET("api/follow/followers/{userId}")
    suspend fun getFollowers(@Path("userId") userId: Long): ApiResponse<FollowListResponse>

    // ── 用户主页 ──

    @GET("api/user/profile/{userId}")
    suspend fun getUserProfile(@Path("userId") userId: Long): ApiResponse<UserProfile>

    @GET("api/user/me")
    suspend fun getMyProfile(): ApiResponse<UserProfile>

    // ── 话题挑战 ──

    @GET("api/topics")
    suspend fun getTopics(
        @Query("include_ended") includeEnded: Boolean = false
    ): ApiResponse<TopicListResponse>

    @GET("api/topics/{topicId}")
    suspend fun getTopicDetail(@Path("topicId") topicId: Long): ApiResponse<TopicBrief>

    @POST("api/topics/{topicId}/submit")
    suspend fun submitToTopic(
        @Path("topicId") topicId: Long,
        @Body request: TopicSubmitRequest
    ): ApiResponse<Map<String, Any>>

    @GET("api/topics/{topicId}/submissions")
    suspend fun getTopicSubmissions(
        @Path("topicId") topicId: Long,
        @Query("page") page: Int = 1,
        @Query("size") size: Int = 20
    ): ApiResponse<SubmissionListResponse>

    @POST("api/topics/submissions/{submissionId}/vote")
    suspend fun voteSubmission(@Path("submissionId") submissionId: Long): ApiResponse<Map<String, Any>>
    
    // 表情库
    @GET("api/danmaku/emojis/all")
    suspend fun getAllEmojis(): ApiResponse<List<EmojiItem>>
}

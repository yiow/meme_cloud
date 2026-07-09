package com.memecloud.data.api

import com.memecloud.data.model.ApiResponse
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.*

/**
 * 模仿大赛 API
 */

// ── 随机目标 ──
data class RandomTarget(
    val emoji_id: String,
    val label: String,
    val image_url: String
)

// ── 提交打分结果 ──
data class SubmitResult(
    val match_id: Int,
    val score: Int,
    val matched_label: String,
    val is_exact_match: Boolean,
    val avg_distance: Double,
    val user_result: PlayerResult? = null,
)

data class PlayerResult(
    val user_id: Int,
    val nickname: String,
    val score: Int,
    val photo_url: String?,
    val rank: Int
)

// ── 排行榜 ──
data class LeaderboardItem(
    val rank: Int,
    val user_id: Int,
    val nickname: String,
    val high_score: Int,
    val total_matches: Int,
    val wins: Int
)

// ── 历史记录 ──
data class GameHistoryItem(
    val match_id: Int,
    val target_label: String,
    val target_image: String,
    val score: Int,
    val photo_label: String?,
    val created_at: String
)

interface GameApi {

    @GET("api/game/random-target")
    suspend fun getRandomTarget(): ApiResponse<RandomTarget>

    @Multipart
    @POST("api/game/submit")
    suspend fun submitPhoto(
        @Part file: MultipartBody.Part,
        @Part("target_label") targetLabel: RequestBody,
        @Part("target_emoji_id") targetEmojiId: RequestBody,
        @Part("target_image") targetImage: RequestBody,
        @Part("user_id") userId: RequestBody,
        @Part("match_id") matchId: RequestBody,
    ): ApiResponse<SubmitResult>

    @GET("api/game/match/{matchId}")
    suspend fun getMatchResult(@Path("matchId") matchId: Int): ApiResponse<SubmitResult>

    @FormUrlEncoded
    @POST("api/game/camera-submit")
    suspend fun cameraSubmit(
        @Field("target_label") targetLabel: String,
        @Field("target_emoji_id") targetEmojiId: String,
        @Field("target_image") targetImage: String,
        @Field("user_id") userId: Int = 1,
        @Field("match_id") matchId: Int = 0
    ): ApiResponse<SubmitResult>

    @GET("api/game/leaderboard")
    suspend fun getLeaderboard(@Query("limit") limit: Int = 20): ApiResponse<List<LeaderboardItem>>

    @GET("api/game/history")
    suspend fun getHistory(@Query("user_id") userId: Int = 1): ApiResponse<List<GameHistoryItem>>
}

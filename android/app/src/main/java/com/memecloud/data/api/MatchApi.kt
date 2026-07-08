package com.memecloud.data.api

import com.memecloud.data.model.ApiResponse
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.Body
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Query

/**
 * 表情包匹配 API — 手势/表情特征 → KNN → meme
 */
data class MatchRequest(
    val feature: List<Float>,
    val mode: String = "gesture"   // gesture | expr
)

data class MatchResult(
    val label: String?,
    val image_url: String?,
    val confidence: Double,
    val is_neutral: Boolean
)

data class LabelItem(
    val label: String,
    val images: List<String>
)

data class RecordRequest(
    val label: String,
    val mode: String = "gesture",
    val samples: List<List<Float>>
)

interface MatchApi {

    @POST("api/match/gesture")
    suspend fun matchGesture(@Body request: MatchRequest): ApiResponse<MatchResult>

    @POST("api/match/expression")
    suspend fun matchExpression(@Body request: MatchRequest): ApiResponse<MatchResult>

    @Multipart
    @POST("api/match/photo")
    suspend fun matchPhoto(
        @Part file: MultipartBody.Part,
        @Part("mode") mode: RequestBody
    ): ApiResponse<MatchResult>

    @GET("api/match/labels")
    suspend fun getLabels(@Query("mode") mode: String = "gesture"): ApiResponse<List<LabelItem>>

    @POST("api/match/record")
    suspend fun record(@Body request: RecordRequest): ApiResponse<Map<String, Any?>>

    // ── PC 摄像头直连（绕过 AVD 虚拟摄像头）──

    @FormUrlEncoded
    @POST("api/match/camera/snapshot")
    suspend fun cameraSnapshot(
        @Field("mode") mode: String
    ): ApiResponse<MatchResult>

    @GET("api/match/camera/status")
    suspend fun cameraStatus(): ApiResponse<Map<String, Any?>>
}

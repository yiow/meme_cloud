package com.memecloud.data.api

import com.memecloud.data.model.ApiResponse
import com.memecloud.data.model.LoginRequest
import com.memecloud.data.model.RegisterRequest
import com.memecloud.data.model.User
import retrofit2.http.Body
import retrofit2.http.POST

/**
 * 认证 API — Retrofit 接口定义
 */
interface AuthApi {

    @POST("api/auth/login")
    suspend fun login(@Body request: LoginRequest): ApiResponse<User>

    @POST("api/auth/register")
    suspend fun register(@Body request: RegisterRequest): ApiResponse<User>
}

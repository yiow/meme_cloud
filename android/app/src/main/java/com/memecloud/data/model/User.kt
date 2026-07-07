package com.memecloud.data.model

/**
 * 通用 API 响应 — 对应后端 ApiResponse
 */
data class ApiResponse<T>(
    val code: Int,
    val msg: String,
    val data: T?
) {
    val isSuccess get() = code == 0
}

/**
 * 登录/注册成功后返回的 data — 对应后端 AuthResponse
 */
data class AuthData(
    val token: String,
    val user: UserBrief
)

/**
 * 用户简要信息 — 对应后端 UserBrief
 */
data class UserBrief(
    val id: Long,
    val username: String,
    val nickname: String?,
    val avatar_url: String?,
    val bio: String?,
    val points: Int,
    val follower_count: Int,
    val following_count: Int
)

/**
 * 登录请求体
 */
data class LoginRequest(
    val username: String,
    val password: String
)

/**
 * 注册请求体
 */
data class RegisterRequest(
    val username: String,
    val password: String,
    val nickname: String
)

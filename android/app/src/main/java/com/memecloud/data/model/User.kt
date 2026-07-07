package com.memecloud.data.model

/**
 * 用户信息 — 对应后端 JWT 用户表
 */
data class User(
    val id: String = "",
    val username: String = "",
    val nickname: String = "",
    val avatar: String = "",       // 头像 URL
    val bio: String = "",           // 个人简介
    val memeCount: Int = 0,         // 发布的表情包数
    val followerCount: Int = 0,
    val followingCount: Int = 0,
    val token: String = ""          // JWT token
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
    val nickname: String = ""
)

/**
 * 通用 API 响应
 */
data class ApiResponse<T>(
    val code: Int,
    val message: String,
    val data: T?
) {
    val isSuccess get() = code == 200
}

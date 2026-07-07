package com.memecloud.data.model

/**
 * 表情包数据模型
 */
data class MemeItem(
    val id: String = "",
    val imageUrl: String = "",
    val thumbnailUrl: String = "",
    val title: String = "",
    val author: String = "",
    val authorAvatar: String = "",
    val tags: List<String> = emptyList(),
    val likeCount: Int = 0,
    val commentCount: Int = 0,
    val isLiked: Boolean = false,
    val isCollected: Boolean = false,
    val createdAt: String = "",
    val source: String = ""  // "community" | "library" | "search"
)

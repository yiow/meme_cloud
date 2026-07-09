package com.memecloud.data.model

import com.google.gson.annotations.SerializedName

/**
 * 社区帖子简要信息 — 对应后端 PostBrief
 */
data class PostBrief(
    val id: Long = 0,
    @SerializedName("image_url") val imageUrl: String = "",
    @SerializedName("thumbnail_url") val thumbnailUrl: String? = null,
    val caption: String? = null,
    val tags: List<String> = emptyList(),
    val author: AuthorBrief = AuthorBrief(),
    @SerializedName("like_count") val likeCount: Int = 0,
    @SerializedName("comment_count") val commentCount: Int = 0,
    @SerializedName("is_liked") val isLiked: Boolean = false,
    @SerializedName("created_at") val createdAt: String = ""
)

/**
 * 帖子详情 — 对应后端 PostDetail（含评论列表）
 */
data class PostDetail(
    val id: Long = 0,
    @SerializedName("image_url") val imageUrl: String = "",
    @SerializedName("thumbnail_url") val thumbnailUrl: String? = null,
    val caption: String? = null,
    val tags: List<String> = emptyList(),
    val author: AuthorBrief = AuthorBrief(),
    @SerializedName("like_count") val likeCount: Int = 0,
    @SerializedName("comment_count") val commentCount: Int = 0,
    @SerializedName("is_liked") val isLiked: Boolean = false,
    @SerializedName("created_at") val createdAt: String = "",
    val comments: List<CommentBrief> = emptyList()
)

/**
 * 评论简要 — 对应后端 CommentBrief
 */
data class CommentBrief(
    val id: Long = 0,
    val content: String = "",
    val author: AuthorBrief = AuthorBrief(),
    @SerializedName("created_at") val createdAt: String = ""
)

/**
 * 作者简要 — 对应后端 AuthorBrief
 */
data class AuthorBrief(
    val id: Long = 0,
    val username: String = "",
    val nickname: String? = null,
    @SerializedName("avatar_url") val avatarUrl: String? = null
)

/**
 * 分页响应 — 对应后端 PaginatedPosts
 */
data class PaginatedPosts(
    val items: List<PostBrief> = emptyList(),
    val page: Int = 1,
    val size: Int = 20,
    @SerializedName("has_more") val hasMore: Boolean = false
)

/**
 * 创建帖子请求体
 */
data class PostCreateRequest(
    @SerializedName("image_url") val imageUrl: String,
    @SerializedName("thumbnail_url") val thumbnailUrl: String? = null,
    val caption: String? = null,
    val tags: List<String> = emptyList()
)

/**
 * 发表评论请求体
 */
data class CommentCreateRequest(
    val content: String
)

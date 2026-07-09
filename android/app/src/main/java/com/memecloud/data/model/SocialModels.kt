package com.memecloud.data.model

import com.google.gson.annotations.SerializedName

// ── 排行榜 ──

data class RankPost(
    val rank: Int = 0,
    val id: Long = 0,
    @SerializedName("image_url") val imageUrl: String = "",
    @SerializedName("thumbnail_url") val thumbnailUrl: String? = null,
    val caption: String? = null,
    @SerializedName("author_id") val authorId: Long = 0,
    @SerializedName("author_name") val authorName: String = "",
    @SerializedName("author_avatar") val authorAvatar: String? = null,
    @SerializedName("like_count") val likeCount: Int = 0,
    @SerializedName("comment_count") val commentCount: Int = 0,
    @SerializedName("is_liked") val isLiked: Boolean = false
)

data class RankingResponse(
    val items: List<RankPost> = emptyList()
)

// ── 关注 ──

data class FollowUserBrief(
    val id: Long = 0,
    val username: String = "",
    val nickname: String? = null,
    @SerializedName("avatar_url") val avatarUrl: String? = null,
    val bio: String? = null,
    @SerializedName("is_followed") val isFollowed: Boolean = false
)

data class FollowToggleResult(
    @SerializedName("is_followed") val isFollowed: Boolean = false,
    @SerializedName("follower_count") val followerCount: Int = 0,
    @SerializedName("following_count") val followingCount: Int = 0
)

data class FollowListResponse(
    val items: List<FollowUserBrief> = emptyList()
)

// ── 用户主页 ──

data class UserProfile(
    val id: Long = 0,
    val username: String = "",
    val nickname: String? = null,
    @SerializedName("avatar_url") val avatarUrl: String? = null,
    val bio: String? = null,
    val points: Int = 0,
    @SerializedName("follower_count") val followerCount: Int = 0,
    @SerializedName("following_count") val followingCount: Int = 0,
    @SerializedName("post_count") val postCount: Int = 0,
    @SerializedName("created_at") val createdAt: String = ""
)

// ── 话题挑战 ──

data class TopicBrief(
    val id: Long = 0,
    val title: String = "",
    val description: String? = null,
    @SerializedName("cover_url") val coverUrl: String? = null,
    @SerializedName("start_time") val startTime: String = "",
    @SerializedName("end_time") val endTime: String = "",
    val status: Int = 1,
    @SerializedName("submission_count") val submissionCount: Int = 0
)

data class TopicListResponse(
    val items: List<TopicBrief> = emptyList()
)

data class TopicSubmitRequest(
    @SerializedName("post_id") val postId: Long
)

data class TopicSubmissionBrief(
    val id: Long = 0,
    @SerializedName("topic_id") val topicId: Long = 0,
    @SerializedName("user_id") val userId: Long = 0,
    @SerializedName("post_id") val postId: Long = 0,
    @SerializedName("post_image_url") val postImageUrl: String = "",
    @SerializedName("post_caption") val postCaption: String? = null,
    @SerializedName("user_name") val userName: String = "",
    @SerializedName("user_avatar") val userAvatar: String? = null,
    @SerializedName("vote_count") val voteCount: Int = 0,
    @SerializedName("created_at") val createdAt: String = ""
)

data class SubmissionListResponse(
    val items: List<TopicSubmissionBrief> = emptyList()
)

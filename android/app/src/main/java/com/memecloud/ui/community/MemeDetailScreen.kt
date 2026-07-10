package com.memecloud.ui.community

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.memecloud.data.model.CommentBrief
import com.memecloud.data.model.CommentCreateRequest
import com.memecloud.data.model.PostDetail
import com.memecloud.data.network.RetrofitClient
import kotlinx.coroutines.launch
import retrofit2.HttpException

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MemeDetailScreen(
    postId: Long,
    onBack: () -> Unit
) {
    var detail by remember { mutableStateOf<PostDetail?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var commentText by remember { mutableStateOf("") }
    var isSendingComment by remember { mutableStateOf(false) }
    var isFollowing by remember { mutableStateOf(false) }
    var isFollowLoading by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val api = remember { RetrofitClient.memeApi }

    fun loadDetail() {
        scope.launch {
            isLoading = true
            try {
                val result = api.getPostDetail(postId)
                if (result.isSuccess) {
                    detail = result.data
                    // 检查是否已关注该作者
                    val detailData = result.data!!
                    try {
                        val meRes = api.getMyProfile()
                        if (meRes.isSuccess && meRes.data != null) {
                            val myId = meRes.data.id
                            val followRes = api.getFollowing(myId)
                            if (followRes.isSuccess && followRes.data != null) {
                                isFollowing = followRes.data.items.any { it.id == detailData.author.id }
                            }
                        }
                    } catch (_: Exception) { }
                }
            } catch (e: HttpException) {
                Toast.makeText(context, "加载失败: ${e.code()}", Toast.LENGTH_SHORT).show()
            } catch (_: Exception) { }
            isLoading = false
        }
    }

    LaunchedEffect(postId) { loadDetail() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("表情包详情", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回", tint = MaterialTheme.colorScheme.onSurface)
                    }
                },
                windowInsets = WindowInsets(0),
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        bottomBar = {
            // 评论输入栏
            Surface(
                shadowElevation = 8.dp,
                color = MaterialTheme.colorScheme.surface
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = commentText,
                        onValueChange = { commentText = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("写评论...", fontSize = 14.sp) },
                        singleLine = true,
                        shape = RoundedCornerShape(20.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary
                        )
                    )
                    Spacer(Modifier.width(8.dp))
                    IconButton(
                        onClick = {
                            if (commentText.isBlank()) return@IconButton
                            isSendingComment = true
                            scope.launch {
                                try {
                                    val result = api.addComment(postId, CommentCreateRequest(commentText.trim()))
                                    if (result.isSuccess) {
                                        commentText = ""
                                        loadDetail()
                                    }
                                } catch (e: HttpException) {
                                    Toast.makeText(context, "评论失败", Toast.LENGTH_SHORT).show()
                                } catch (_: Exception) { }
                                isSendingComment = false
                            }
                        },
                        enabled = commentText.isNotBlank() && !isSendingComment
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, "发送",
                            tint = if (commentText.isNotBlank()) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    ) { innerPadding ->
        if (isLoading) {
            Box(Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        val d = detail ?: return@Scaffold

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentPadding = PaddingValues(16.dp)
        ) {
            // 大图
            item {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(d.imageUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = d.caption,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp)),
                    contentScale = ContentScale.FillWidth
                )
            }

            // 作者 & 互动
            item {
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.AccountCircle, null, Modifier.size(36.dp),
                        tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(8.dp))
                    Column(Modifier.weight(1f)) {
                        Text(d.author.nickname ?: d.author.username,
                            fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Text(d.createdAt.take(16).replace("T", " "),
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    // 关注按钮
                    if (d.author.id > 0) {
                        if (isFollowLoading) {
                            CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                        } else {
                            if (isFollowing) {
                                OutlinedButton(
                                    onClick = {
                                        isFollowLoading = true
                                        scope.launch {
                                            try {
                                                val r = api.toggleFollow(d.author.id)
                                                if (r.isSuccess) {
                                                    isFollowing = r.data?.isFollowed ?: false
                                                }
                                            } catch (_: Exception) { }
                                            isFollowLoading = false
                                        }
                                    },
                                    shape = RoundedCornerShape(16.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                                ) { Text("已关注", fontSize = 12.sp) }
                            } else {
                                Button(
                                    onClick = {
                                        isFollowLoading = true
                                        scope.launch {
                                            try {
                                                val r = api.toggleFollow(d.author.id)
                                                if (r.isSuccess) {
                                                    isFollowing = r.data?.isFollowed ?: false
                                                }
                                            } catch (_: Exception) { }
                                            isFollowLoading = false
                                        }
                                    },
                                    shape = RoundedCornerShape(16.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                                ) { Text("+ 关注", fontSize = 12.sp) }
                            }
                        }
                    }
                }

                if (!d.caption.isNullOrBlank()) {
                    Spacer(Modifier.height(12.dp))
                    Text(d.caption, fontSize = 15.sp)
                }

                if (d.tags.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        d.tags.forEach { tag ->
                            SuggestionChip(
                                onClick = { },
                                label = { Text("#$tag", fontSize = 12.sp) }
                            )
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        var liked by remember { mutableStateOf(d.isLiked) }
                        var likeCount by remember { mutableIntStateOf(d.likeCount) }
                        IconButton(onClick = {
                            scope.launch {
                                try {
                                    val result = api.toggleCommunityLike(d.id)
                                    if (result.isSuccess) {
                                        val newLiked = result.data?.get("is_liked") as? Boolean ?: false
                                        liked = newLiked
                                        likeCount = if (newLiked) likeCount + 1 else (likeCount - 1).coerceAtLeast(0)
                                    }
                                } catch (_: Exception) { }
                            }
                        }) {
                            Icon(
                                if (liked) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                                null,
                                tint = if (liked) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text("$likeCount", fontSize = 14.sp)
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.ChatBubbleOutline, null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.width(4.dp))
                        Text("${d.commentCount}", fontSize = 14.sp)
                    }
                }

                Divider(Modifier.padding(vertical = 12.dp))
            }

            // 评论区标题
            item {
                Text("评论 (${d.commentCount})", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(Modifier.height(8.dp))
            }

            // 评论列表
            if (d.comments.isEmpty()) {
                item {
                    Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        Text("暂无评论，来说两句吧", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } else {
                items(d.comments) { comment ->
                    CommentItem(comment)
                }
            }
        }
    }
}

@Composable
private fun CommentItem(comment: CommentBrief) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.AccountCircle, null, Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
                Spacer(Modifier.width(8.dp))
                Text(
                    comment.author.nickname ?: comment.author.username,
                    fontSize = 13.sp, fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.weight(1f))
                Text(
                    comment.createdAt.take(16).replace("T", " "),
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(comment.content, fontSize = 14.sp)
        }
    }
}

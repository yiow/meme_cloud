package com.memecloud.ui.community

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.memecloud.data.api.MemeApi
import com.memecloud.data.model.PaginatedPosts
import com.memecloud.data.model.PostBrief
import com.memecloud.data.network.RetrofitClient
import kotlinx.coroutines.launch
import retrofit2.HttpException

private val SORT_TABS = listOf("推荐", "最新", "关注")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommunityScreen(
    onGoRanking: () -> Unit = {},
    onGoChallenge: () -> Unit = {},
    onGoBounty: () -> Unit = {},
    onGoPublish: () -> Unit = {},
    onGoDetail: (Long) -> Unit = {}
) {
    var selectedSort by remember { mutableIntStateOf(0) }
    var posts by remember { mutableStateOf(listOf<PostBrief>()) }
    var page by remember { mutableIntStateOf(1) }
    var hasMore by remember { mutableStateOf(true) }
    var isRefreshing by remember { mutableStateOf(false) }
    var isLoadingMore by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf<Long?>(null) }

    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val api = remember { RetrofitClient.memeApi }

    val sortParam = when (selectedSort) {
        0 -> "recommend"
        1 -> "latest"
        else -> "follow"
    }

    fun loadFeed(refresh: Boolean = false) {
        scope.launch {
            if (refresh) {
                isRefreshing = true
                page = 1
            } else {
                if (isLoadingMore || !hasMore) return@launch
                isLoadingMore = true
            }
            try {
                val result = api.getCommunityFeed(sort = sortParam, page = if (refresh) 1 else page, size = 20)
                val data = result.data
                if (result.isSuccess && data != null) {
                    posts = if (refresh) data.items else posts + data.items
                    hasMore = data.hasMore
                    page = data.page + 1
                }
            } catch (e: HttpException) {
                Toast.makeText(context, "加载失败: ${e.code()}", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, "网络错误", Toast.LENGTH_SHORT).show()
            } finally {
                isRefreshing = false
                isLoadingMore = false
            }
        }
    }

    LaunchedEffect(selectedSort) { loadFeed(refresh = true) }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            TopAppBar(
                title = { Text("社区广场", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = onGoChallenge) { Icon(Icons.Filled.EmojiEvents, "话题挑战") }
                    IconButton(onClick = onGoRanking) { Icon(Icons.Filled.Whatshot, "排行榜") }
                    IconButton(onClick = onGoBounty) { Icon(Icons.Filled.Redeem, "悬赏") }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                SORT_TABS.forEachIndexed { index, label ->
                    Text(
                        label,
                        modifier = Modifier.clickable { selectedSort = index },
                        fontWeight = if (selectedSort == index) FontWeight.Bold else FontWeight.Normal,
                        color = if (selectedSort == index) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = if (selectedSort == index) 16.sp else 14.sp
                    )
                }
            }

            LazyVerticalStaggeredGrid(
                columns = StaggeredGridCells.Fixed(2),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalItemSpacing = 10.dp
            ) {
                items(posts) { post ->
                    MemeCard(
                        post = post,
                        onClick = { onGoDetail(post.id) },
                        onDelete = { showDeleteDialog = post.id },
                        onToggleLike = {
                            scope.launch {
                                try {
                                    val result = api.toggleCommunityLike(post.id)
                                    if (result.isSuccess) {
                                        val liked = result.data?.get("is_liked") as? Boolean ?: false
                                        posts = posts.map { p ->
                                            if (p.id == post.id) p.copy(
                                                isLiked = liked,
                                                likeCount = if (liked) p.likeCount + 1 else (p.likeCount - 1).coerceAtLeast(0)
                                            ) else p
                                        }
                                    }
                                } catch (_: Exception) { }
                            }
                        }
                    )
                }
                // 加载更多指示器
                if (isLoadingMore) {
                    item(span = StaggeredGridItemSpan.FullLine) {
                        Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                        }
                    }
                }
            }
        }

        // 发布 FAB
        FloatingActionButton(
            onClick = onGoPublish,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
            containerColor = MaterialTheme.colorScheme.primary
        ) {
            Icon(Icons.Filled.Add, "发布")
        }
    }

    // 删除确认弹窗
    showDeleteDialog?.let { postId ->
        AlertDialog(
            onDismissRequest = { showDeleteDialog = null },
            title = { Text("删除表情包") },
            text = { Text("确定删除这个表情包吗？删除后无法恢复。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = null
                        scope.launch {
                            try {
                                val result = api.deletePost(postId)
                                if (result.isSuccess) {
                                    posts = posts.filter { it.id != postId }
                                    Toast.makeText(context, "已删除", Toast.LENGTH_SHORT).show()
                                }
                            } catch (e: HttpException) {
                                Toast.makeText(context, "删除失败: ${e.code()}", Toast.LENGTH_SHORT).show()
                            } catch (_: Exception) { }
                        }
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) { Text("确定删除") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = null }) { Text("取消") }
            }
        )
    }

    // 下拉刷新 — SwipeRefresh (Compose Material)
    if (isRefreshing && posts.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    }
}

@Composable
private fun MemeCard(
    post: PostBrief,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    onToggleLike: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp)
    ) {
        Box {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(post.imageUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = post.caption,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp)),
                contentScale = ContentScale.Crop
            )

            // 底部信息栏
            Surface(
                modifier = Modifier.align(Alignment.BottomStart).fillMaxWidth(),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                shape = RoundedCornerShape(bottomStart = 12.dp, bottomEnd = 12.dp)
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    Text(
                        post.author.nickname ?: post.author.username,
                        fontSize = 12.sp, fontWeight = FontWeight.Bold,
                        maxLines = 1, overflow = TextOverflow.Ellipsis
                    )
                    if (!post.caption.isNullOrBlank()) {
                        Text(
                            post.caption, fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2, overflow = TextOverflow.Ellipsis
                        )
                    }
                    Spacer(Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = onToggleLike,
                            modifier = Modifier.size(20.dp)
                        ) {
                            Icon(
                                if (post.isLiked) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                                null,
                                Modifier.size(16.dp),
                                tint = if (post.isLiked) MaterialTheme.colorScheme.error
                                else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text("${post.likeCount}", fontSize = 11.sp)
                        Spacer(Modifier.width(12.dp))
                        Icon(Icons.Filled.ChatBubbleOutline, null, Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.width(4.dp))
                        Text("${post.commentCount}", fontSize = 11.sp)

                        Spacer(Modifier.weight(1f))

                        // 长按菜单
                        Box {
                            IconButton(
                                onClick = { showMenu = true },
                                modifier = Modifier.size(20.dp)
                            ) {
                                Icon(Icons.Filled.MoreVert, null, Modifier.size(16.dp))
                            }
                            DropdownMenu(
                                expanded = showMenu,
                                onDismissRequest = { showMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("删除", color = MaterialTheme.colorScheme.error) },
                                    onClick = {
                                        showMenu = false
                                        onDelete()
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

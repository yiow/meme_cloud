package com.memecloud.ui.community

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ─── Mock 社区帖子数据 ──────────────────────────────

private data class CommunityPost(
    val id: String, val imageRes: Int, val author: String,
    val likes: Int, val comments: Int, val height: Int
)

private val MOCK_POSTS = List(20) { i ->
    CommunityPost(
        id = "post_$i",
        imageRes = 0,
        author = listOf("表情帝", "梗图达人", "猫奴小王", "摸鱼大师", "斗图冠军")[i % 5],
        likes = (10..999).random(),
        comments = (0..50).random(),
        height = listOf(180, 220, 260, 200, 300)[i % 5]
    )
}

private val SORT_TABS = listOf("推荐", "最新", "关注")

// ─── 社区广场主页 ──────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommunityScreen(
    onGoRanking: () -> Unit = {},
    onGoChallenge: () -> Unit = {},
    onGoBounty: () -> Unit = {},
    onGoPublish: () -> Unit = {}
) {
    var selectedSort by remember { mutableIntStateOf(0) }

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

        // 排序Tab
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

        // 双列瀑布流
        LazyVerticalStaggeredGrid(
            columns = StaggeredGridCells.Fixed(2),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalItemSpacing = 10.dp
        ) {
            items(MOCK_POSTS) { post ->
                MemeCard(post)
            }
        }
    }

    // 发布 FAB
    Box(modifier = Modifier.fillMaxSize()) {
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
}

@Composable
private fun MemeCard(post: CommunityPost) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(post.height.dp)
            .clickable { },
        shape = RoundedCornerShape(12.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // 占位图 — 后续 Coil 加载实际图片
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Filled.TagFaces, null,
                        modifier = Modifier.size(32.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                    )
                }
            }

            // 底部信息
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .padding(8.dp)
            ) {
                Text(post.author, fontSize = 12.sp, fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface, maxLines = 1)
                Spacer(Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Favorite, null, Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.error)
                    Spacer(Modifier.width(4.dp))
                    Text("${post.likes}", fontSize = 11.sp)
                    Spacer(Modifier.width(12.dp))
                    Icon(Icons.Filled.ChatBubbleOutline, null, Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("${post.comments}", fontSize = 11.sp)
                }
            }
        }
    }
}

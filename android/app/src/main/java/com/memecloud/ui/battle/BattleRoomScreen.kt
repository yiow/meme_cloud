package com.memecloud.ui.battle

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.memecloud.data.api.LocalMemeItem
import com.memecloud.data.network.RetrofitClient
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random

private data class BarrageMsg(val id: Long, val meme: LocalMemeItem?, val sender: String, val lane: Int)

private val SENDERS = listOf("表情帝", "猫奴小王", "摸鱼大师", "斗图冠军", "社恐星人", "干饭王")

private const val BASE_URL = "http://10.0.2.2:9000"

private const val LANE_COUNT = 6

@Composable

fun BattleRoomScreen(roomId: String, roomName: String, onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val laneLastTime = remember { MutableList(LANE_COUNT) { 0L } }
    var messages by remember { mutableStateOf(listOf<BarrageMsg>()) }
    var msgId by remember { mutableLongStateOf(0L) }
    val onlineCount = remember { mutableIntStateOf(Random.nextInt(5, 35)) }
    var localMemes by remember { mutableStateOf(listOf<LocalMemeItem>()) }

    fun pickLane(): Int {
        val now = System.currentTimeMillis()

        // 找“最空闲”的轨道（间隔时间最大）
        val lane = (0 until LANE_COUNT).maxBy {
            now - laneLastTime[it]
        }

        laneLastTime[lane] = now
        return lane
    }

    // 加载本地表情包
    LaunchedEffect(Unit) {
        try {
            val resp = RetrofitClient.danmakuApi.getLocalMemes()
            if (resp.isSuccess && resp.data != null) localMemes = resp.data
        } catch (_: Exception) {}
    }

    // 模拟弹幕消息到达
    LaunchedEffect(localMemes) {
        if (localMemes.isEmpty()) return@LaunchedEffect
        while (true) {
            delay(Random.nextLong(800, 2500))
            val meme = localMemes.random()
            messages = (messages + BarrageMsg(
                id = ++msgId,
                meme = meme,
                sender = SENDERS.random(),
                lane = pickLane()
            )).takeLast(50)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // ── 顶部信息栏 ──
        Surface(color = MaterialTheme.colorScheme.primary) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Filled.ArrowBack, null, tint = MaterialTheme.colorScheme.onPrimary)
                }
                Column(Modifier.weight(1f)) {
                    Text(roomName, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimary, fontSize = 18.sp)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Circle, null, Modifier.size(8.dp),
                            tint = androidx.compose.ui.graphics.Color(0xFF22C55E))
                        Spacer(Modifier.width(4.dp))
                        Text("${onlineCount.intValue} 人在线", fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f))
                    }
                }
                IconButton(onClick = { }) {
                    Icon(Icons.Filled.Share, null, tint = MaterialTheme.colorScheme.onPrimary)
                }
            }
        }

        // ── 弹幕区 ──
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.background)
        ) {
            if (messages.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("这个房间还没人说话~\n发个表情包暖暖场吧！",
                        textAlign = TextAlign.Center, fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            val laneHeight = 100.dp
            messages.forEach { msg ->
                key(msg.id) {
                    AnimatedBarrage(context = context, msg = msg, modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = laneHeight * msg.lane)
                    )
                }
            }
        }

        // ── 表情包选择栏 ──
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant,
            tonalElevation = 4.dp
        ) {
            Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                if (localMemes.isEmpty()) {
                    Box(Modifier.fillMaxWidth().height(64.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                    }
                } else {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(localMemes) { meme ->
                            Surface(
                                modifier = Modifier.size(64.dp).clip(RoundedCornerShape(10.dp)),
                                color = MaterialTheme.colorScheme.surface,
                                onClick = {
                                    scope.launch {
                                        messages = (messages + BarrageMsg(
                                            id = ++msgId,
                                            meme = meme,
                                            sender = "我",
                                            lane = pickLane()
                                        )).takeLast(50)
                                    }
                                }
                            ) {
                                AsyncImage(
                                    model = ImageRequest.Builder(context)
                                        .data("$BASE_URL${meme.thumbnailUrl}")
                                        .crossfade(true)
                                        .build(),
                                    contentDescription = meme.description,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AnimatedBarrage(
    context: android.content.Context,
    msg: BarrageMsg,
    modifier: Modifier
) {
    val offsetX = remember { Animatable(1000f) }

    LaunchedEffect(msg.id) {
        offsetX.snapTo(1000f)
        offsetX.animateTo(
            targetValue = -2000f,
            animationSpec = tween(durationMillis = 12000, easing = LinearEasing)
        )
    }

    Column(
        modifier = modifier
            .offset(x = offsetX.value.dp)
            .wrapContentWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(
                if (msg.sender == "我")
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                else
                    MaterialTheme.colorScheme.surface
            )
            .padding(6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 👉 上面：图片
        msg.meme?.let {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data("$BASE_URL${it.thumbnailUrl}")
                    .crossfade(true)
                    .build(),
                contentDescription = it.description,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(128.dp)
                    .clip(RoundedCornerShape(8.dp))
            )
        }

        Spacer(Modifier.height(4.dp))

        // 👉 下面：昵称
        Text(
            text = msg.sender,
            fontSize = 15.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1
        )
    }
}


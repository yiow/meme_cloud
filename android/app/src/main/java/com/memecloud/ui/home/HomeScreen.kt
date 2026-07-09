package com.memecloud.ui.home

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color as AndroidColor
import androidx.compose.foundation.Image
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
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.memecloud.data.api.SearchResult
import com.memecloud.data.network.RetrofitClient
import kotlinx.coroutines.*
import java.net.HttpURLConnection
import java.net.URL

/**
 * MJPEG 流解码 composable — 长连接读流 → 后台解码 Bitmap → Image 显示
 */
@Composable
fun MjpegStreamView(streamUrl: String, enabled: Boolean = true, modifier: Modifier = Modifier) {
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    val scope = rememberCoroutineScope()
    var connectionError by remember { mutableStateOf<String?>(null) }

    // 连接超时：10秒内没收到帧就提示
    LaunchedEffect(enabled) {
        if (!enabled) return@LaunchedEffect
        connectionError = null
        bitmap = null
        delay(10000)
        if (bitmap == null && connectionError == null) {
            connectionError = "\u8FDE\u63A5\u8D85\u65F6\uFF0C\u8BF7\u68C0\u67E5\u540E\u7AEF\u670D\u52A1"
        }
    }

    DisposableEffect(streamUrl, enabled) {
        if (!enabled) return@DisposableEffect onDispose { }
        var running = true
        val job = scope.launch(Dispatchers.IO) {
            try {
                val url = URL(streamUrl)
                val conn = url.openConnection() as HttpURLConnection
                conn.connectTimeout = 3000
                conn.readTimeout = 0
                conn.connect()

                if (conn.responseCode != 200) {
                    connectionError = "\u540E\u7AEF\u8FD4\u56DE ${conn.responseCode}"
                    conn.disconnect()
                    return@launch
                }

                val input = conn.inputStream
                val buf = ByteArray(128 * 1024)
                val jpgBuf = ByteArray(512 * 1024)
                var jpgLen = 0

                var boundary = ByteArray(0)
                var i = 0
                while (running) {
                    val b = input.read(); if (b == -1) break
                    if (i >= buf.size) break
                    buf[i++] = b.toByte()
                    if (b == '\n'.code && i >= 2 && buf[i - 2] == '\r'.toByte()) {
                        val line = String(buf, 0, i - 2, Charsets.UTF_8)
                        if (line.startsWith("--")) {
                            boundary = ("\r\n$line\r\n").toByteArray()
                        }
                        break
                    }
                }
                if (boundary.isEmpty()) { conn.disconnect(); return@launch }

                val ring = ByteArray(boundary.size)
                var ringPos = 0

                fun ringMatch(): Boolean {
                    for (k in boundary.indices) {
                        if (ring[(ringPos + k) % boundary.size] != boundary[k]) return false
                    }
                    return true
                }

                var inHeaders = true
                var headerBlankLine = 0

                while (running) {
                    val b = input.read(); if (b == -1) break
                    val byte = b.toByte()

                    if (inHeaders) {
                        if (byte == '\r'.toByte() || byte == '\n'.toByte()) {
                            headerBlankLine++
                        } else {
                            headerBlankLine = 0
                        }
                        if (headerBlankLine >= 4) {
                            inHeaders = false
                            jpgLen = 0
                        }
                        continue
                    }

                    ring[ringPos] = byte
                    ringPos = (ringPos + 1) % boundary.size

                    if (jpgLen < jpgBuf.size) {
                        jpgBuf[jpgLen++] = byte
                    }

                    if (ringMatch()) {
                        val frameLen = jpgLen - boundary.size
                        if (frameLen > 4) {
                            val bmp = BitmapFactory.decodeByteArray(jpgBuf, 0, frameLen)
                            if (bmp != null) {
                                withContext(Dispatchers.Main) { bitmap = bmp }
                            }
                        }
                        jpgLen = 0
                        inHeaders = true
                        headerBlankLine = 0
                    }
                }
                input.close()
                conn.disconnect()
            } catch (e: Exception) {
                if (e !is CancellationException) { e.printStackTrace(); connectionError = e.message?.take(50) ?: "\u8FDE\u63A5\u5931\u8D25" }
            }
        }
        onDispose {
            running = false
            job.cancel()
        }
    }

    Box(modifier = modifier.background(Color.Black), contentAlignment = Alignment.Center) {
        if (!enabled) {
            Text("点击开启摄像头", color = Color.Gray, fontSize = 16.sp)
        } else {
            if (connectionError != null) {
                Text(
                    text = connectionError!!,
                    color = Color(AndroidColor.parseColor("#FF6666")),
                    fontSize = 14.sp,
                    modifier = Modifier.padding(16.dp)
                )
            } else {
                bitmap?.let {
                    Image(
                        bitmap = it.asImageBitmap(),
                        contentDescription = "摄像头预览",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )
                } ?: Text("摄像头连接中…", color = Color.Gray, fontSize = 14.sp)
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen() {
    val context = LocalContext.current

    // 摄像头开关
    var isCameraOn by remember { mutableStateOf(false) }

    // ── 匹配状态 ──
    var detectedLabel by remember { mutableStateOf("") }
    var matchedImageUrl by remember { mutableStateOf<String?>(null) }
    var matchedImages by remember { mutableStateOf<List<String>>(emptyList()) }
    var mode by remember { mutableStateOf("gesture") }
    var matchConfidence by remember { mutableFloatStateOf(0f) }
    var matchError by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    // ── 搜索状态 ──
    var searchQuery by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf<List<SearchResult>>(emptyList()) }
    var isSearching by remember { mutableStateOf(false) }
    val searchFocusRequester = remember { FocusRequester() }

    val scope = rememberCoroutineScope()
    val streamUrl = "http://10.0.2.2:9000/api/match/camera/stream"

    // ── 标签搜索 ──
    fun performSearch(query: String) {
        searchQuery = query
        if (query.isBlank()) { searchResults = emptyList(); return }
        scope.launch {
            isSearching = true
            try {
                val r = RetrofitClient.matchApi.searchLabels(query, mode)
                if (r.isSuccess && r.data != null) {
                    searchResults = r.data
                }
            } catch (_: Exception) {}
            isSearching = false
        }
    }

    // ── 拍照匹配 ──
    fun takePhotoAndMatch() {
        scope.launch {
            isLoading = true
            matchError = ""
            try {
                val r = RetrofitClient.matchApi.cameraSnapshot(mode)
                if (r.isSuccess && r.data != null) {
                    detectedLabel = r.data.label ?: "无匹配"
                    matchedImageUrl = r.data.image_url
                    matchedImages = r.data.images ?: emptyList()
                    matchConfidence = r.data.confidence.toFloat()
                } else {
                    matchError = r.msg
                }
            } catch (e: Exception) {
                matchError = "网络: ${e.message?.take(40) ?: "?"}"
            } finally {
                isLoading = false
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("表情云库", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                ),
                actions = {
                    IconButton(onClick = { }) {
                        Icon(Icons.Filled.Notifications, "通知", tint = MaterialTheme.colorScheme.onPrimary)
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { if (isCameraOn) takePhotoAndMatch() },
                containerColor = if (isLoading) MaterialTheme.colorScheme.secondary
                else if (!isCameraOn) MaterialTheme.colorScheme.surfaceVariant
                else MaterialTheme.colorScheme.primary
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(Icons.Filled.CameraAlt, contentDescription = "拍照匹配",
                        tint = MaterialTheme.colorScheme.onPrimary)
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding)
        ) {
            // ── 摄像头预览区 ──
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.45f)
                    .clipToBounds(),
                contentAlignment = Alignment.Center
            ) {
                MjpegStreamView(
                    streamUrl = streamUrl,
                    enabled = isCameraOn,
                    modifier = Modifier.fillMaxSize()
                )

                // 摄像头开关\u6309\u94AE
                IconButton(
                    onClick = { isCameraOn = !isCameraOn },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(12.dp)
                        .size(48.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(
                            if (isCameraOn) Color(0xCC00AA00)
                            else Color(0xCC555555)
                        )
                ) {
                    Icon(
                        imageVector = if (isCameraOn) Icons.Filled.Videocam else Icons.Filled.VideocamOff,
                        contentDescription = if (isCameraOn) "\u5173\u95ED\u6444\u50CF\u5934" else "\u5F00\u542F\u6444\u50CF\u5934",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }

                // 匹配结果浮动标签
                if (detectedLabel.isNotEmpty()) {
                    Surface(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(12.dp),
                        color = Color(AndroidColor.parseColor("#CC000000")),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            " $detectedLabel ",
                            color = Color(AndroidColor.parseColor("#7FFFD4")),
                            fontSize = 16.sp,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            // ── 模式切换 ──
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilterChip(
                    selected = mode == "gesture",
                    onClick = { mode = "gesture" },
                    label = { Text("👋 手势") }
                )
                FilterChip(
                    selected = mode == "expr",
                    onClick = { mode = "expr" },
                    label = { Text("😀 表情") }
                )
                Spacer(Modifier.weight(1f))
                if (isLoading) {
                    Text("分析中...", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            // ── 搜索栏 ──
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("搜索表情包…") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .focusRequester(searchFocusRequester),
                singleLine = true,
                trailingIcon = {
                    Row {
                        if (isSearching) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp).padding(4.dp),
                                strokeWidth = 2.dp
                            )
                        }
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = ""; searchResults = emptyList() }) {
                                Icon(Icons.Filled.Close, "清除")
                            }
                        }
                        IconButton(onClick = { performSearch(searchQuery) }) {
                            Icon(Icons.Filled.Search, "搜索")
                        }
                    }
                },
                shape = RoundedCornerShape(24.dp)
            )

            // ── 搜索结果 ──
            if (searchResults.isNotEmpty()) {
                Spacer(Modifier.height(4.dp))
                LazyRow(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(searchResults) { result ->
                        Column(
                            modifier = Modifier.width(100.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            AsyncImage(
                                model = ImageRequest.Builder(context)
                                    .data("http://10.0.2.2:9000${result.images.firstOrNull() ?: ""}")
                                    .crossfade(true)
                                    .build(),
                                contentDescription = result.label,
                                modifier = Modifier
                                    .size(80.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color.LightGray)
                            )
                            Text(
                                result.label, fontSize = 11.sp,
                                maxLines = 1,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            // ── 错误信息 ──
            if (matchError.isNotEmpty()) {
                Surface(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    color = Color(AndroidColor.parseColor("#33FF0000")),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(matchError, fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(8.dp))
                }
            }

            // ── 匹配结果 ──
            if (matchedImageUrl != null) {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("匹配结果", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                Text(detectedLabel, style = MaterialTheme.typography.headlineSmall,
                                    color = MaterialTheme.colorScheme.primary)
                                Text("置信度 ${(matchConfidence * 100).toInt()}%",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }

                        if (matchedImages.size > 1) {
                            Spacer(Modifier.height(8.dp))
                            Text("全部图片 (${matchedImages.size})", fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.height(4.dp))
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                items(matchedImages) { img ->
                                    AsyncImage(
                                        model = ImageRequest.Builder(context)
                                            .data("http://10.0.2.2:9000${img}")
                                            .crossfade(true)
                                            .build(),
                                        contentDescription = null,
                                        modifier = Modifier
                                            .size(72.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color.White)
                                    )
                                }
                            }
                        } else {
                            Spacer(Modifier.height(4.dp))
                            AsyncImage(
                                model = ImageRequest.Builder(context)
                                    .data("http://10.0.2.2:9000${matchedImageUrl}")
                                    .crossfade(true)
                                    .build(),
                                contentDescription = null,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(120.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color.White)
                            )
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
            }

            // ── 热门标签 ──
            Text("热门标签", style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 16.dp))
            LazyRow(modifier = Modifier.padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val tags = listOf("单手托腮","摊手","捂嘴","手比OK","双手抱头","举手","挠头","大笑","震惊","无语")
                items(tags) { tag ->
                    SuggestionChip(
                        onClick = { performSearch(tag) },
                        label = { Text(tag, fontSize = 12.sp) }
                    )
                }
            }

            Spacer(Modifier.height(8.dp))
        }
    }
}

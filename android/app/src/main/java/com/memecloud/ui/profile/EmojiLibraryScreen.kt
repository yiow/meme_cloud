package com.memecloud.ui.profile

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
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
import com.memecloud.data.api.LocalMemeItem
import com.memecloud.data.api.RenameRequest
import com.memecloud.data.network.RetrofitClient
import com.memecloud.data.network.toFullUrl
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmojiLibraryScreen(onBack: () -> Unit) {
    var memes by remember { mutableStateOf(listOf<LocalMemeItem>()) }
    var isLoading by remember { mutableStateOf(true) }
    var showDeleteDialog by remember { mutableStateOf<LocalMemeItem?>(null) }
    var showRenameDialog by remember { mutableStateOf<LocalMemeItem?>(null) }
    var renameText by remember { mutableStateOf("") }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val api = remember { RetrofitClient.danmakuApi }

    fun loadMemes() {
        scope.launch {
            try {
                val result = api.getLocalMemes()
                if (result.isSuccess && result.data != null) {
                    memes = result.data
                }
            } catch (e: Exception) {
                Toast.makeText(context, "加载表情包失败", Toast.LENGTH_SHORT).show()
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(Unit) { loadMemes() }

    val uploadLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri ?: return@rememberLauncherForActivityResult
        scope.launch {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val bytes = inputStream?.readBytes()
                inputStream?.close()
                if (bytes == null) return@launch
                val requestFile = bytes.toRequestBody("image/*".toMediaTypeOrNull())
                val filePart = MultipartBody.Part.createFormData("file", "upload.png", requestFile)
                val descPart = "upload".toRequestBody("text/plain".toMediaTypeOrNull())
                val result = api.uploadEmoji(filePart, descPart)
                if (result.isSuccess) {
                    Toast.makeText(context, "上传成功", Toast.LENGTH_SHORT).show()
                    loadMemes()
                } else {
                    Toast.makeText(context, "上传失败", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "上传失败", Toast.LENGTH_SHORT).show()
            }
        }
    }

    showDeleteDialog?.let { meme ->
        AlertDialog(
            onDismissRequest = { showDeleteDialog = null },
            title = { Text("删除表情包") },
            text = { Text("确定要删除「${meme.description}」吗？") },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            try {
                                val filename = meme.filename ?: return@launch
                                val result = api.deleteLocalMeme(filename)
                                if (result.isSuccess) {
                                    Toast.makeText(context, "已删除", Toast.LENGTH_SHORT).show()
                                    showDeleteDialog = null
                                    loadMemes()
                                }
                            } catch (e: Exception) { Toast.makeText(context, "重命名失败: ${e.localizedMessage}", Toast.LENGTH_SHORT).show() }
                        }
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text("删除") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = null }) { Text("取消") }
            }
        )
    }


    showRenameDialog?.let { meme ->
        AlertDialog(
            onDismissRequest = { showRenameDialog = null },
            title = { Text("重命名") },
            text = {
                OutlinedTextField(
                    value = renameText,
                    onValueChange = { renameText = it },
                    label = { Text("新名称") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        try {
                            val result = api.renameLocalMeme(meme.filename ?: return@launch, RenameRequest(renameText.trim()))
                            if (result.isSuccess) {
                                showRenameDialog = null
                                loadMemes()
                            } else {
                                Toast.makeText(context, result.msg, Toast.LENGTH_SHORT).show()
                            }
                        } catch (e: Exception) { Toast.makeText(context, "重命名失败: ${e.localizedMessage}", Toast.LENGTH_SHORT).show() }
                    }
                }) { Text("确定") }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = null }) { Text("取消") }
            }
        )
    }


    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("我的表情库", fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, null, tint = MaterialTheme.colorScheme.onSurface)
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
        floatingActionButton = {
            FloatingActionButton(
                onClick = { uploadLauncher.launch("image/*") },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Filled.Add, "上传表情包")
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (memes.isEmpty()) {
                Text("暂无表情包", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.Center))
            } else {
                LazyVerticalGrid(
                    modifier = Modifier.fillMaxSize(),
                    columns = GridCells.Fixed(3),
                    contentPadding = PaddingValues(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(memes) { meme ->
                        MemeGridCard(meme = meme,
                            onRename = { renameText = meme.description; showRenameDialog = meme },
                            onDelete = { if (meme.filename != null) showDeleteDialog = meme })
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MemeGridCard(meme: LocalMemeItem, onRename: () -> Unit = {}, onDelete: () -> Unit = {}) {
    var showMenu by remember { mutableStateOf(false) }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .combinedClickable(onClick = {}, onLongClick = { showMenu = true }),
        shape = RoundedCornerShape(12.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(meme.thumbnailUrl.toFullUrl())
                    .crossfade(true)
                    .build(),
                contentDescription = meme.description,
                modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(12.dp)),
                contentScale = ContentScale.Crop
            )
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth(),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                shape = RoundedCornerShape(bottomStart = 12.dp, bottomEnd = 12.dp)
            ) {
                Text(meme.description, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
            }
            
            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false }
            ) {
                DropdownMenuItem(
                    text = { Text("重命名") },
                    onClick = { showMenu = false; onRename() }
                )
                DropdownMenuItem(
                    text = { Text("删除", color = MaterialTheme.colorScheme.error) },
                    onClick = { showMenu = false; onDelete() }
                )
            }
        }
    }
}

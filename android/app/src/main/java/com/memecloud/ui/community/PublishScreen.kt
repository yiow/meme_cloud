package com.memecloud.ui.community

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.memecloud.data.model.PostCreateRequest
import com.memecloud.data.network.RetrofitClient
import com.memecloud.data.network.toFullUrl
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import kotlinx.coroutines.launch
import retrofit2.HttpException

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PublishScreen(
    onBack: () -> Unit,
    onPublishSuccess: () -> Unit = {}
) {
    // 简化版：用 URL 代替真实选图流程（真实场景会用 ActivityResultContracts 拍照/选图）
    var uploadedUrl by remember { mutableStateOf("") }
    var isUploading by remember { mutableStateOf(false) }
    var caption by remember { mutableStateOf("") }
    var tagInput by remember { mutableStateOf("") }
    var tags by remember { mutableStateOf(listOf<String>()) }
    var isPublishing by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val api = remember { RetrofitClient.memeApi }
    val uploadApi = remember { RetrofitClient.danmakuApi }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri ?: return@rememberLauncherForActivityResult
        isUploading = true
        scope.launch {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val bytes = inputStream?.readBytes()
                inputStream?.close()
                if (bytes == null) return@launch
                val requestFile = bytes.toRequestBody("image/*".toMediaTypeOrNull())
                val filePart = MultipartBody.Part.createFormData("file", "upload.png", requestFile)
                val descPart = "community_post".toRequestBody("text/plain".toMediaTypeOrNull())
                val result = uploadApi.uploadEmoji(filePart, descPart)
                if (result.isSuccess && result.data != null) {
                    val fileUrl = result.data["file_url"] as? String ?: return@launch
                    uploadedUrl = fileUrl.toFullUrl()
                } else {
                    Toast.makeText(context, "上传失败", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "上传失败", Toast.LENGTH_SHORT).show()
            } finally {
                isUploading = false
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("发布表情包") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回")
                    }
                },
                actions = {
                    TextButton(
                        onClick = {
                            if (uploadedUrl.isBlank()) {
                                Toast.makeText(context, "请先选择图片", Toast.LENGTH_SHORT).show()
                                return@TextButton
                            }
                            isPublishing = true
                            scope.launch {
                                try {
                                    val req = PostCreateRequest(
                                        imageUrl = uploadedUrl,
                                        caption = caption.trim().ifBlank { null },
                                        tags = tags
                                    )
                                    val result = api.createPost(req)
                                    if (result.isSuccess) {
                                        Toast.makeText(context, "发布成功", Toast.LENGTH_SHORT).show()
                                        onPublishSuccess()
                                    } else {
                                        Toast.makeText(context, result.msg, Toast.LENGTH_SHORT).show()
                                    }
                                } catch (e: HttpException) {
                                    Toast.makeText(context, "发布失败: ${e.code()}", Toast.LENGTH_SHORT).show()
                                } catch (_: Exception) {
                                    Toast.makeText(context, "网络错误", Toast.LENGTH_SHORT).show()
                                }
                                isPublishing = false
                            }
                        },
                        enabled = !isPublishing
                    ) {
                        Text("发布", color = MaterialTheme.colorScheme.onPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 图片选择区
            Text("图片", fontWeight = FontWeight.Bold, fontSize = 16.sp)

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .clickable { galleryLauncher.launch("image/*") },
                contentAlignment = Alignment.Center
            ) {
                if (isUploading) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Spacer(Modifier.height(8.dp))
                        Text("上传中...", fontSize = 14.sp)
                    }
                } else if (uploadedUrl.isNotBlank()) {
                    AsyncImage(
                        model = uploadedUrl,
                        contentDescription = "预览",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(240.dp),
                        contentScale = ContentScale.Fit
                    )
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Filled.AddPhotoAlternate, null,
                            Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                        Spacer(Modifier.height(8.dp))
                        Text("点击选择图片", fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            Divider()

            // 配文
            Text("配文（可选）", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            OutlinedTextField(
                value = caption,
                onValueChange = { caption = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("写下你想说的话...") },
                maxLines = 3,
                shape = RoundedCornerShape(12.dp)
            )

            Divider()

            // 标签
            Text("标签", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = tagInput,
                    onValueChange = { tagInput = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("输入标签后按添加") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
                Spacer(Modifier.width(8.dp))
                Button(
                    onClick = {
                        val t = tagInput.trim()
                        if (t.isNotBlank() && t !in tags && tags.size < 10) {
                            tags = tags + t
                            tagInput = ""
                        }
                    },
                    enabled = tagInput.isNotBlank() && tags.size < 10
                ) {
                    Text("添加")
                }
            }

            if (tags.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    tags.forEach { tag ->
                        InputChip(
                            selected = false,
                            onClick = { tags = tags - tag },
                            label = { Text("#$tag") },
                            trailingIcon = { Icon(Icons.Filled.Close, null, Modifier.size(16.dp)) }
                        )
                    }
                }
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

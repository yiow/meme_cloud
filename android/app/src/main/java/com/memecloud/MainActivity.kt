package com.memecloud

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.memecloud.data.network.RetrofitClient
import com.memecloud.ui.navigation.AppNavigation
import com.memecloud.ui.theme.MemeCloudTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // 读取保存的 token，有则跳过登录
        val savedToken = getSharedPreferences("memecloud", MODE_PRIVATE)
            .getString("auth_token", null)
        RetrofitClient.authToken = savedToken
        setContent {
            MemeCloudTheme {
                AppNavigation(isLoggedIn = savedToken != null)
            }
        }
    }
}

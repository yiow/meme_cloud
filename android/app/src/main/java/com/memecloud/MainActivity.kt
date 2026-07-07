package com.memecloud

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.memecloud.ui.navigation.AppNavigation
import com.memecloud.ui.theme.MemeCloudTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MemeCloudTheme {
                // TODO: 从本地存储或启动参数判断登录状态
                AppNavigation(isLoggedIn = false)
            }
        }
    }
}

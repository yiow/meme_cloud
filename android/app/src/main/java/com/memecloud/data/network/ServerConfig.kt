package com.memecloud.data.network

/**
 * 服务器地址配置 — 所有硬编码 URL 的唯一入口
 *
 * 模拟器：HOST = "10.0.2.2"
 * 真机：  HOST = "你电脑的局域网 IP"（如 192.168.1.100）
 */
object ServerConfig {
    // ⚠️ 真机调试时改这里
    //const val HOST = "192.168.31.235"
    const val HOST = "10.0.2.2"
    const val PORT = 9000

    val BASE_URL get() = "http://$HOST:$PORT"
    val WS_URL get() = "ws://$HOST:$PORT"
}

/** Helper — 拼接完整 URL */
fun String.toFullUrl(): String = "${ServerConfig.BASE_URL}$this"

/** Helper — 拼接完整 WS URL */
fun String.toWsUrl(): String = "${ServerConfig.WS_URL}$this"

package com.memecloud.data.network

import com.memecloud.data.api.AuthApi
import com.memecloud.data.api.DanmakuApi
import com.memecloud.data.api.MatchApi
import com.memecloud.data.api.MemeApi
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Retrofit 单例 — 统一管理网络请求
 *
 * Android 模拟器通过 10.0.2.2 访问宿主机 localhost。
 * 真机调试时改为电脑的局域网 IP（如 192.168.x.x）。
 */
object RetrofitClient {

    // ⚠️ 真机调试时改为你的电脑 IP
     private const val BASE_URL = "http://10.0.2.2:9000/"

    /** 当前登录用户的 Token，登录成功后由外部设置 */
    var authToken: String? = null

    private val authInterceptor = Interceptor { chain ->
        val request = chain.request()
        val token = authToken
        if (token != null) {
            val newRequest = request.newBuilder()
                .header("Authorization", "Bearer $token")
                .build()
            chain.proceed(newRequest)
        } else {
            chain.proceed(request)
        }
    }

    private val okHttpClient: OkHttpClient by lazy {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(logging)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .build()
    }

    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    val authApi: AuthApi by lazy { retrofit.create(AuthApi::class.java) }
    val memeApi: MemeApi by lazy { retrofit.create(MemeApi::class.java) }
    val matchApi: MatchApi by lazy { retrofit.create(MatchApi::class.java) }
    val danmakuApi: DanmakuApi by lazy { retrofit.create(DanmakuApi::class.java) }
}

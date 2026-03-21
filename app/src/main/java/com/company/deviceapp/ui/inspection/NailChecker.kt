package com.company.deviceapp.ui.inspection

import android.graphics.Bitmap
import android.util.Base64
import android.util.Log
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

object NailChecker {
    private const val TAG = "NailChecker"
    // 请替换为你从百度AI控制台获取的有效 token
    private const val ACCESS_TOKEN = "24.be5d418c8f7a8a92a654332f76c043cdc.2592000.1776070915.282335-122367098"
    private const val API_URL = "https://aip.baidubce.com/rest/2.0/image-classify/v1/gesture?access_token=$ACCESS_TOKEN"
    
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .connectionPool(ConnectionPool(0, 1, TimeUnit.NANOSECONDS))
        .retryOnConnectionFailure(true)
        .addInterceptor(HttpLoggingInterceptor())
        .build()

    fun checkNail(bitmap: Bitmap, isBackOfHand: Boolean = false, onResult: (isQualified: Boolean, message: String) -> Unit) {
        Log.d(TAG, "==> 开始手势识别请求")
        try {
            val baos = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 40, baos)
            val bytes = baos.toByteArray()

            val base64Image = Base64.encodeToString(bytes, Base64.NO_WRAP)
            val encodedImage = URLEncoder.encode(base64Image, "UTF-8")
            
            Log.d(TAG, "图片处理完成，准备发送...")

            val body = RequestBody.create(
                "application/x-www-form-urlencoded".toMediaType(), 
                "image=$encodedImage"
            )

            val request = Request.Builder()
                .url(API_URL)
                .post(body)
                .addHeader("Content-Type", "application/x-www-form-urlencoded")
                .build()

            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    Log.e(TAG, "【网络错误】请求失败: ${e.message}")
                    onResult(false, "连接失败")
                }

                override fun onResponse(call: Call, response: Response) {
                    response.use { res ->
                        val responseBody = res.body?.string() ?: ""
                        Log.d(TAG, "<== 收到响应, Code: ${res.code}, Body: $responseBody")
                        
                        if (!res.isSuccessful) {
                            onResult(false, "API错误: ${res.code}")
                            return
                        }

                        if (responseBody.contains("\"classname\":\"Five\"")) {
                            Log.i(TAG, "识别到五指手势 (Five)，通过")
                            onResult(true, "识别成功")
                        } else {
                            Log.i(TAG, "未识别到五指手势")
                        }
                    }
                }
            })
        } catch (e: Exception) {
            Log.e(TAG, "处理异常: ${e.message}")
            onResult(false, "处理异常")
        }
    }
}
class HttpLoggingInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        Log.d("OkHttp", "发起请求: ${request.url}")
        return chain.proceed(request)
    }
}

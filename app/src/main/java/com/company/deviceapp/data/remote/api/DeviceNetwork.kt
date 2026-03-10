package com.company.deviceapp.data.remote.api

import com.company.deviceapp.data.remote.dto.ApiResponse
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

// ==========================================
// 1. 设备初始化相关的 API 接口
// ==========================================
interface DeviceApiService {

    // 流程 2：设备心跳 (严格对应文档 GET 请求与参数)
    @GET("/cabinet/findDeviceInfo")
    suspend fun heartbeat(
        @Query("type") type: Int,
        @Query("serialNum") serialNum: String
    ): ApiResponse<HeartbeatData>

    // 流程 3：获取 MQ 服务地址 (严格对应文档 POST 表单请求)
    @FormUrlEncoded
    @POST("/deviceIot/getDeviceIdBySerialNumber")
    suspend fun getMqConfig(
        @Field("serialNumber") serialNumber: String
    ): ApiResponse<MqConfigData>
}

// ==========================================
// 2. 接口返回的数据实体类 (严格对照文档 JSON)
// ==========================================
data class HeartbeatData(
    val deviceType: String,
    val deviceName: String,
    val deviceId: String,
    val autoupdate: String,
    val companyName: String
)

data class MqConfigData(
    val deviceType: String,
    val deviceId: String,
    val uris: List<String>,
    val username: String,
    val password: String,
    val encrypt: String,
    val qrCode: String?,
    val deviceName: String,
    val secret: String
)

// ==========================================
// 3. 企业级网络客户端工厂 (快速构建 Retrofit)
// ==========================================
object RetrofitClient {
    fun create(baseUrl: String): DeviceApiService {
        val retrofit = Retrofit.Builder()
            .baseUrl(baseUrl)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        return retrofit.create(DeviceApiService::class.java)
    }
}
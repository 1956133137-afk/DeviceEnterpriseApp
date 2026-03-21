package com.company.deviceapp.data.remote.api

import com.company.deviceapp.data.remote.dto.ApiResponse
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface DeviceApiService {

    @GET("/cabinet/findDeviceInfo")
    suspend fun heartbeat(
        @Query("type") type: Int,
        @Query("serialNum") serialNum: String
    ): ApiResponse<HeartbeatData>

    @FormUrlEncoded
    @POST("/deviceIot/getDeviceIdBySerialNumber")
    suspend fun getMqConfig(
        @Field("serialNumber") serialNumber: String
    ): ApiResponse<MqConfigData>
}

data class HeartbeatData(
    val deviceType: String?,
    val deviceName: String?,
    val deviceId: String?,
    val deviceAddress: String?,
    val autoUpdate: String?,
    val companyName: String?
)

data class MqConfigData(
    val deviceType: String?,
    val deviceId: String?,
    val uris: List<String>?,
    val username: String?,
    val password: String?,
    val encrypt: String?,
    val qrCode: String?,
    val deviceName: String?,
    val secret: String?
)

object RetrofitClient {
    fun create(baseUrl: String): DeviceApiService {
        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(DeviceApiService::class.java)
    }
}
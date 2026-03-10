package com.company.deviceapp.data.remote.api

import com.company.deviceapp.data.remote.dto.ApiResponse
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Query

interface MorningInspectionApiService {

    // 3.3 获取上架的问卷调查
    @GET("/system/group/device/active-group")
    suspend fun getActiveQuestionnaires(
        @Query("deviceSn") deviceSn: String
    ): ApiResponse<List<QuestionnaireDto>>

    // 3.5 获取当天最近的一条晨检记录
    @GET("/inspectionRecord/getLastRecord")
    suspend fun getLastRecord(
        @Query("personId") personId: String
    ): ApiResponse<InspectionRecordDto>

    // 3.4 晨检记录上传 (严格遵循 multipart/form-data)
    @Multipart
    @POST("/uploadInspectionRecord")
    suspend fun uploadInspectionRecord(
        @Part faceImg: MultipartBody.Part?,      // 非必填
        @Part handImg1: MultipartBody.Part,      // 必传
        @Part handImg2: MultipartBody.Part,      // 必传
        @Part("memberUserId") memberUserId: RequestBody,
        @Part("inspectionTime") inspectionTime: RequestBody,
        @Part("temperature") temperature: RequestBody,
        @Part("identifyType") identifyType: RequestBody,
        @Part("openDoor") openDoor: RequestBody,
        @Part("handType") handType: RequestBody,
        @Part("healthCertificate") healthCertificate: RequestBody,
        @Part("tempType") tempType: RequestBody,
        @Part("status") status: RequestBody,
        @Part("username") username: RequestBody,
        @Part("inspectionDesc") inspectionDesc: RequestBody,
        @Part("sn") sn: RequestBody,
        @Part("surveyAnswers") surveyAnswers: RequestBody?
    ): ApiResponse<Any>
}

// --- 对应 3.3 问卷响应体 ---
data class QuestionnaireDto(
    val id: String, // 文档为 long/string，统一用 String
    val questionTitle: String,
    val isRequired: Boolean,
    val orderNum: Int?,
    val answerList: List<AnswerOptionDto>
)

data class AnswerOptionDto(
    val id: String,
    val optionText: String,
    val orderNum: Int?,
    val isOtherOption: Boolean
)

// --- 对应 3.5 最近记录响应体 ---
data class InspectionRecordDto(
    val id: String?,
    val memberUserId: String?,
    val username: String?,
    val identifyType: String?,
    val tempType: String?,
    val temperature: String?,
    val handType: String?,
    val faceImg: String?,
    val handImg1: String?,
    val handImg2: String?,
    val healthCertificate: String?,
    val openDoor: String?,
    val status: String?,
    val inspectionTime: String?,
    val inspectionDesc: String?
)
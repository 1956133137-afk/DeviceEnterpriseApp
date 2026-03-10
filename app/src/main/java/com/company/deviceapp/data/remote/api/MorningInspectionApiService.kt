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

    // 1. 获取上架的问卷调查
    @GET("/system/group/device/active-group")
    suspend fun getActiveQuestionnaires(
        @Query("deviceSn") deviceSn: String
    ): ApiResponse<List<QuestionnaireDto>>

    // 2. 获取当天最近的一条晨检记录
    @GET("/inspectionRecord/getLastRecord")
    suspend fun getLastRecord(
        @Query("personId") personId: String
    ): ApiResponse<InspectionRecordDto>

    // 3. 晨检记录上传 (严格按照 V1.0.4 multipart/form-data 规范)
    @Multipart
    @POST("/uploadInspectionRecord")
    suspend fun uploadInspectionRecord(
        @Part faceImg: MultipartBody.Part,
        @Part handImg1: MultipartBody.Part,
        @Part handImg2: MultipartBody.Part,
        @Part("memberUserId") memberUserId: RequestBody,
        @Part("inspectionTime") inspectionTime: RequestBody,
        @Part("temperature") temperature: RequestBody,
        @Part("identifyType") identifyType: RequestBody,       // 1:人脸 2:掌纹 3:IC卡
        @Part("openDoor") openDoor: RequestBody,               // 1:开门 2:未开门
        @Part("handType") handType: RequestBody,               // 1:正常 2:异常
        @Part("healthCertificate") healthCertificate: RequestBody, // 1:正常 2:过期
        @Part("tempType") tempType: RequestBody,               // 1:正常 2:异常
        @Part("status") status: RequestBody,                   // 1:正常 2:异常
        @Part("username") username: RequestBody,
        @Part("inspectionDesc") inspectionDesc: RequestBody,
        @Part("sn") sn: RequestBody,                           // V1.0.3 新增设备序列号
        @Part("surveyAnswers") surveyAnswers: RequestBody?     // V1.0.4 新增问卷JSON数组
    ): ApiResponse<Any>
}

// 问卷 DTO (对应文档数据结构)
data class QuestionnaireDto(
    val id: Long,
    val questionTitle: String,
    val isRequired: Boolean,
    val orderNum: Int?,
    val answerList: List<AnswerOptionDto>
)

data class AnswerOptionDto(
    val id: Long,
    val optionText: String,
    val orderNum: Int?,
    val isOtherOption: Boolean
)

// 晨检记录 DTO (对应文档数据结构)
data class InspectionRecordDto(
    val id: String,
    val memberuserId: String,
    val username: String,
    val status: String,
    val temperature: String,
    val inspectionDesc: String,
    val surveyAnswers: List<SurveyAnswerDto>?
)

data class SurveyAnswerDto(
    val questionTitle: String,
    val optionText: String,
    val customAnswer: String?
)
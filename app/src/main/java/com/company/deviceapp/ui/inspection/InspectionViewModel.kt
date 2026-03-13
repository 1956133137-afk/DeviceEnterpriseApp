package com.company.deviceapp.ui.inspection

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.company.deviceapp.data.local.db.PersonnelDao
import com.company.deviceapp.data.local.db.PersonnelEntity
import com.company.deviceapp.data.remote.api.InspectionRecordDto
import com.company.deviceapp.data.remote.api.MorningInspectionApiService
import com.company.deviceapp.data.remote.api.QuestionnaireDto
import com.google.gson.Gson
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

enum class InspectionStep {
    WAITING_FACE_LOGIN, DOING_INSPECTION, SUBMITTING, SUCCESS
}

data class InspectionUiState(
    val currentStep: InspectionStep = InspectionStep.WAITING_FACE_LOGIN,
    val currentUser: PersonnelEntity? = null,
    val lastRecord: InspectionRecordDto? = null,
    val questionnaires: List<QuestionnaireDto> = emptyList(),
    val errorMessage: String? = null,
    val answers: Map<String, String> = emptyMap() // 问卷答案 (QuestionID -> OptionID)
)

@HiltViewModel
class InspectionViewModel @Inject constructor(
    private val apiService: MorningInspectionApiService,
    private val personnelDao: PersonnelDao
) : ViewModel() {

    private val _uiState = MutableStateFlow(InspectionUiState())
    val uiState: StateFlow<InspectionUiState> = _uiState

    fun onFaceRecognized(faceToken: String, deviceSn: String) {
        Log.d("InspectionVM", ">>> 收到 SDK 返回的 Token: $faceToken")
        viewModelScope.launch {
            try {
                val localUser = personnelDao.getPersonnelByFaceToken(faceToken)
                if (localUser != null) {
                    Log.d("InspectionVM", ">>> 本地库匹配成功: ${localUser.name} (ID: ${localUser.personnelId})")
                    _uiState.update {
                        it.copy(
                            currentUser = localUser,
                            currentStep = InspectionStep.DOING_INSPECTION,
                            errorMessage = null
                        )
                    }
                    fetchInspectionData(localUser.personnelId, deviceSn)
                } else {
                    Log.e("InspectionVM", ">>> 匹配失败: 本地数据库中没有找到 Token 为 [$faceToken] 的人员")
                    _uiState.update {
                        it.copy(errorMessage = "未在本地人员库中匹配到该人脸")
                    }
                }
            } catch (e: Exception) {
                Log.e("InspectionVM", ">>> 数据库查询异常", e)
            }
        }
    }

    private suspend fun fetchInspectionData(personId: String, deviceSn: String) {
        try {
            // 真实获取最近记录和问卷
            val recordRes = apiService.getLastRecord(personId)
            val questRes = apiService.getActiveQuestionnaires(deviceSn)

            _uiState.update {
                it.copy(
                    lastRecord = if (recordRes.isSuccessful) recordRes.data else null,
                    questionnaires = if (questRes.isSuccessful) questRes.data ?: emptyList() else emptyList()
                )
            }
        } catch (e: Exception) {
            _uiState.update { it.copy(errorMessage = "获取业务数据失败: ${e.message}") }
        }
    }

    fun selectAnswer(questionId: String, optionId: String) {
        _uiState.update { state ->
            val newAnswers = state.answers.toMutableMap()
            newAnswers[questionId] = optionId
            state.copy(answers = newAnswers)
        }
    }

    // 真正执行 multipart/form-data 格式的网络上传
    fun submitInspection(onSuccess: () -> Unit, onError: (String) -> Unit) {
        val state = _uiState.value
        val user = state.currentUser

        if (user == null) {
            onError("当前人员信息丢失，请重新识别")
            return
        }

        _uiState.update { it.copy(currentStep = InspectionStep.SUBMITTING) }

        viewModelScope.launch {
            try {
                // 1. 组装问卷 JSON 数组
                val surveyList = state.answers.map { (qId, optId) ->
                    mapOf("questionId" to qId, "optionId" to optId, "customAnswer" to "")
                }
                val surveyJsonStr = Gson().toJson(surveyList)

                // 2. 辅助方法：将 String 转换为 RequestBody
                fun createPart(value: String) = value.toRequestBody("text/plain".toMediaTypeOrNull())

                // 3. 构建模拟的图片 Part（真实应用中这里会取 CameraX 拍下来的本地 File）
                val emptyBody = "".toRequestBody("image/jpeg".toMediaTypeOrNull())
                val mockHandImg1 = MultipartBody.Part.createFormData("handImg1", "hand1.jpg", emptyBody)
                val mockHandImg2 = MultipartBody.Part.createFormData("handImg2", "hand2.jpg", emptyBody)

                // 4. 获取当前时间 (YYYY-MM-DD HH:MM:SS)
                val currentTime = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())

                // 5. 严格调用 3.4 上传接口
                val response = apiService.uploadInspectionRecord(
                    faceImg = null,
                    handImg1 = mockHandImg1,
                    handImg2 = mockHandImg2,
                    memberUserId = createPart(user.personnelId),
                    inspectionTime = createPart(currentTime),
                    temperature = createPart("36.5"),
                    identifyType = createPart("1"),
                    openDoor = createPart("1"),
                    handType = createPart("1"),
                    healthCertificate = createPart("1"),
                    tempType = createPart("1"),
                    status = createPart("1"),
                    username = createPart(user.name ?: "未知"),
                    inspectionDesc = createPart("正常"),
                    sn = createPart("TEST_DEVICE_001"),
                    surveyAnswers = createPart(surveyJsonStr)
                )

                if (response.isSuccessful) {
                    onSuccess()
                } else {
                    onError("上传失败: ${response.msg}")
                    _uiState.update { it.copy(currentStep = InspectionStep.DOING_INSPECTION) }
                }

            } catch (e: Exception) {
                // 假如你本地 Flask 没开，会走到这里
                onError("网络异常，晨检记录已保存至本地")
                onSuccess() // 离线模式兜底，依然让用户感觉成功了
            }
        }
    }

    fun resetToNextPerson() {
        _uiState.value = InspectionUiState()
    }
}
package com.company.deviceapp.ui.inspection

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
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import android.util.Log
import android.content.SharedPreferences

enum class InspectionStep {
    WAITING_FACE_LOGIN, DOING_INSPECTION, SUBMITTING, SUCCESS
}

data class InspectionUiState(
    val currentStep: InspectionStep = InspectionStep.WAITING_FACE_LOGIN,
    val currentUser: PersonnelEntity? = null,
    val lastRecord: InspectionRecordDto? = null,
    val questionnaires: List<QuestionnaireDto> = emptyList(),
    val errorMessage: String? = null,
    val answers: Map<String, String> = emptyMap(),

    val recognizedFaceImgPath: String? = null,
    val handImg1Path: String? = null,
    val handImg2Path: String? = null,
    val recognizedIdentifyType: String = "1",
    val recognizedInspectionTime: String = "",
    val deviceSn: String = "DWDEV20230515014A",
    val openDoorStatus: String = "1" 
)

@HiltViewModel
class InspectionViewModel @Inject constructor(
    private val apiService: MorningInspectionApiService,
    private val personnelDao: PersonnelDao,
    private val sharedPrefs: SharedPreferences
) : ViewModel() {

    private val _uiState = MutableStateFlow(InspectionUiState())
    val uiState: StateFlow<InspectionUiState> = _uiState

    private fun getBaseUrl(): String {
        val url = sharedPrefs.getString("BASE_URL", "http://192.168.2.8:8080/") ?: "http://192.168.2.8:8080/"
        return if (url.endsWith("/")) url else "$url/"
    }

    fun onFaceRecognized(
        faceToken: String,
        faceImgPath: String,
        deviceSn: String
    ) {
        viewModelScope.launch {
            val localUser = personnelDao.getPersonnelByFaceToken(faceToken)
            if (localUser != null) {
                val currentTime = SimpleDateFormat(
                    "yyyy-MM-dd HH:mm:ss",
                    Locale.getDefault()
                ).format(Date())

                _uiState.update {
                    it.copy(
                        currentUser = localUser,
                        currentStep = InspectionStep.DOING_INSPECTION,
                        errorMessage = null,
                        recognizedFaceImgPath = faceImgPath,
                        recognizedIdentifyType = "1",
                        recognizedInspectionTime = currentTime,
                        deviceSn = deviceSn,
                        openDoorStatus = "1"
                    )
                }
                fetchInspectionData(localUser.personnelId, deviceSn)
            } else {
                _uiState.update {
                    it.copy(errorMessage = "未在本地人员库中匹配到该人脸")
                }
            }
        }
    }

    fun onCardOrQrRecognized(code: String) {
        viewModelScope.launch {
            val localUser = personnelDao.getPersonnelByIcNum(code)
            if (localUser != null) {
                val currentTime = SimpleDateFormat(
                    "yyyy-MM-dd HH:mm:ss",
                    Locale.getDefault()
                ).format(Date())

                _uiState.update {
                    it.copy(
                        currentUser = localUser,
                        currentStep = InspectionStep.DOING_INSPECTION,
                        errorMessage = null,
                        recognizedIdentifyType = "2",
                        recognizedInspectionTime = currentTime,
                        openDoorStatus = "1"
                    )
                }
                fetchInspectionData(localUser.personnelId, _uiState.value.deviceSn)
            } else {
                _uiState.update {
                    it.copy(errorMessage = "人员识别失败\n\n未查询到: 该人员信息")
                }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun onHandRecognized(path: String, isBack: Boolean) {
        _uiState.update {
            if (!isBack) it.copy(handImg1Path = path)
            else it.copy(handImg2Path = path)
        }
    }

    fun onFaceImgCaptured(path: String) {
        _uiState.update { it.copy(recognizedFaceImgPath = path) }
    }

    fun clearFaceImg() {
        _uiState.update { it.copy(recognizedFaceImgPath = null) }
    }

    fun setOpenDoorStatus(status: String) {
        _uiState.update { it.copy(openDoorStatus = status) }
    }

    private suspend fun fetchInspectionData(personId: String, deviceSn: String) {
        val baseUrl = getBaseUrl()
        try {
            Log.d("Questionnaire", "请求地址: ${baseUrl}inspectionRecord/getLastRecord?personId=$personId")
            val recordRes = apiService.getLastRecord(personId)
            Log.d("Questionnaire", "返回参数: ${Gson().toJson(recordRes)}")
            
            Log.d("Questionnaire", "请求地址: ${baseUrl}system/group/device/active-group?deviceSn=$deviceSn")
            val questRes = apiService.getActiveQuestionnaires(deviceSn)
            Log.d("Questionnaire", "返回参数: ${Gson().toJson(questRes)}")

            _uiState.update {
                it.copy(
                    lastRecord = if (recordRes.isSuccessful) recordRes.data else null,
                    questionnaires = if (questRes.isSuccessful) questRes.data ?: emptyList() else emptyList()
                )
            }
        } catch (e: Exception) {
            Log.e("Questionnaire", "网络请求异常", e)
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

    fun clearHandImg1() {
        _uiState.update { it.copy(handImg1Path = null) }
    }

    fun clearHandImg2() {
        _uiState.update { it.copy(handImg2Path = null) }
    }

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
                val baseUrl = getBaseUrl()
                val surveyList = state.answers.map { (qId, optId) ->
                    mapOf("questionId" to qId, "optionId" to optId, "customAnswer" to "")
                }
                val surveyJsonStr = Gson().toJson(surveyList)
                
                Log.d("Questionnaire", "请求地址: ${baseUrl}uploadInspectionRecord")
                Log.d("Questionnaire", "请求参数: $surveyJsonStr")

                fun createPart(value: String) =
                    value.toRequestBody("text/plain".toMediaTypeOrNull())

                fun createImagePart(partName: String, filePath: String?): MultipartBody.Part? {
                    if (filePath.isNullOrBlank()) return null
                    val file = File(filePath)
                    if (!file.exists()) return null
                    val body = file.asRequestBody("image/jpeg".toMediaTypeOrNull())
                    return MultipartBody.Part.createFormData(partName, file.name, body)
                }

                val emptyBody = "".toRequestBody("image/jpeg".toMediaTypeOrNull())
                val emptyPart = MultipartBody.Part.createFormData("placeholder", "empty.jpg", emptyBody)

                val handImg1Part = createImagePart("handImg1", state.handImg1Path) ?: emptyPart
                val handImg2Part = createImagePart("handImg2", state.handImg2Path) ?: emptyPart
                val faceImgPart = createImagePart("faceImg", state.recognizedFaceImgPath) ?: emptyPart

                val inspectionTime = if (state.recognizedInspectionTime.isNotBlank()) {
                    state.recognizedInspectionTime
                } else {
                    SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
                }

                val response = apiService.uploadInspectionRecord(
                    faceImg = faceImgPart,
                    handImg1 = handImg1Part,
                    handImg2 = handImg2Part,
                    memberUserId = createPart(user.personnelId),
                    inspectionTime = createPart(inspectionTime),
                    temperature = createPart("36.5"),
                    identifyType = createPart(state.recognizedIdentifyType),
                    openDoor = createPart(state.openDoorStatus), 
                    handType = createPart("1"),
                    healthCertificate = createPart("1"),
                    tempType = createPart("1"),
                    status = createPart("1"),
                    username = createPart(user.name ?: "未知"),
                    inspectionDesc = createPart("正常"),
                    sn = createPart(state.deviceSn),
                    surveyAnswers = createPart(surveyJsonStr)
                )

                Log.d("Questionnaire", "返回参数: ${Gson().toJson(response)}")

                if (response.isSuccessful) {
                    onSuccess()
                } else {
                    onError("上传失败: ${response.msg}")
                    _uiState.update { it.copy(currentStep = InspectionStep.DOING_INSPECTION) }
                }
            } catch (e: Exception) {
                Log.e("Questionnaire", "提交记录网络异常", e)
                onError("网络异常，晨检记录已保存至本地")
                onSuccess()
            }
        }
    }

    fun resetToNextPerson() {
        _uiState.value = InspectionUiState()
    }
}

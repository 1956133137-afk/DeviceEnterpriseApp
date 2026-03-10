package com.company.deviceapp.ui.inspection

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.company.deviceapp.data.local.db.PersonnelDao
import com.company.deviceapp.data.local.db.PersonnelEntity
import com.company.deviceapp.data.remote.api.InspectionRecordDto
import com.company.deviceapp.data.remote.api.MorningInspectionApiService
import com.company.deviceapp.data.remote.api.QuestionnaireDto
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class InspectionStep {
    WAITING_FACE_LOGIN, // 等待人脸识别
    DOING_INSPECTION,   // 晨检业务中 (问卷+拍照)
    SUBMITTING,         // 提交中
    SUCCESS             // 成功
}

data class InspectionUiState(
    val currentStep: InspectionStep = InspectionStep.WAITING_FACE_LOGIN,
    val currentUser: PersonnelEntity? = null,
    val lastRecord: InspectionRecordDto? = null,
    val questionnaires: List<QuestionnaireDto> = emptyList(),
    val errorMessage: String? = null
)

@HiltViewModel
class InspectionViewModel @Inject constructor(
    private val apiService: MorningInspectionApiService,
    private val personnelDao: PersonnelDao
) : ViewModel() {

    private val _uiState = MutableStateFlow(InspectionUiState())
    val uiState: StateFlow<InspectionUiState> = _uiState

    // 模拟摄像头识别到人脸 ID
    fun onFaceRecognized(personnelId: String, deviceSn: String) {
        viewModelScope.launch {
            // 1. 从本地数据库验证人员
            val user = personnelDao.getPersonnelById(personnelId)
            if (user != null) {
                _uiState.update { it.copy(currentUser = user, currentStep = InspectionStep.DOING_INSPECTION) }

                // 2. 严格按文档：登录成功后，并发获取【最近一条记录】和【上架问卷】
                fetchInspectionData(user.personnelId, deviceSn)
            } else {
                _uiState.update { it.copy(errorMessage = "未在本地库找到该人员，请联系管理员下发权限") }
            }
        }
    }

    private suspend fun fetchInspectionData(personId: String, deviceSn: String) {
        try {
            val recordResponse = apiService.getLastRecord(personId)
            val questionResponse = apiService.getActiveQuestionnaires(deviceSn)

            _uiState.update {
                it.copy(
                    lastRecord = if (recordResponse.isSuccessful) recordResponse.data else null,
                    questionnaires = if (questionResponse.isSuccessful) questionResponse.data ?: emptyList() else emptyList()
                )
            }
        } catch (e: Exception) {
            _uiState.update { it.copy(errorMessage = "获取业务数据失败: ${e.message}") }
        }
    }

    // 重置状态，等待下一个人
    fun resetToNextPerson() {
        _uiState.value = InspectionUiState()
    }
}
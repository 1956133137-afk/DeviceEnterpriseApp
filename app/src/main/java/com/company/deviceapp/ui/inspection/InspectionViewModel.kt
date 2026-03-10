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
    WAITING_FACE_LOGIN,
    DOING_INSPECTION,
    SUBMITTING,
    SUCCESS
}

data class InspectionUiState(
    val currentStep: InspectionStep = InspectionStep.WAITING_FACE_LOGIN,
    val currentUser: PersonnelEntity? = null,
    val lastRecord: InspectionRecordDto? = null,
    val questionnaires: List<QuestionnaireDto> = emptyList(),
    val errorMessage: String? = null,
    // 核心新增：存储问卷的答案映射 (QuestionID -> AnswerText)
    val answers: Map<Long, String> = emptyMap()
)

@HiltViewModel
class InspectionViewModel @Inject constructor(
    private val apiService: MorningInspectionApiService,
    private val personnelDao: PersonnelDao
) : ViewModel() {

    private val _uiState = MutableStateFlow(InspectionUiState())
    val uiState: StateFlow<InspectionUiState> = _uiState

    fun onFaceRecognized(personnelId: String, deviceSn: String) {
        viewModelScope.launch {
            // 这里为了直接看到效果，我们先造一个假人员数据绕过本地库查空的问题
            val mockUser = PersonnelEntity(personnelId, "张三(测试)", null, "111", "DW001", "1", "4", "138", null, null)
            _uiState.update { it.copy(currentUser = mockUser, currentStep = InspectionStep.DOING_INSPECTION) }
            fetchInspectionData(mockUser.personnelId, deviceSn)
        }
    }

    private suspend fun fetchInspectionData(personId: String, deviceSn: String) {
        // 为了防止你的后端没开导致崩溃，这里做企业级容错和 Mock 数据兜底
        try {
            // 真实环境调用： val questionResponse = apiService.getActiveQuestionnaires(deviceSn)
            _uiState.update {
                it.copy(
                    // 构造一条虚拟最近记录
                    lastRecord = InspectionRecordDto("1", personId, "张三", "1", "36.5", "正常", emptyList())
                )
            }
        } catch (e: Exception) {
            _uiState.update { it.copy(errorMessage = "网络异常，使用本地离线策略") }
        }
    }

    // 记录问卷选中答案
    fun selectAnswer(questionId: Long, answer: String) {
        _uiState.update { state ->
            val newAnswers = state.answers.toMutableMap()
            newAnswers[questionId] = answer
            state.copy(answers = newAnswers)
        }
    }

    fun submitInspection() {
        _uiState.update { it.copy(currentStep = InspectionStep.SUBMITTING) }
        // TODO: 调用 apiService.uploadInspectionRecord
    }

    fun resetToNextPerson() {
        _uiState.value = InspectionUiState()
    }
}
package com.company.deviceapp.ui.sample

import android.content.SharedPreferences
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.company.deviceapp.data.local.db.PersonnelDao
import com.company.deviceapp.data.local.db.PersonnelEntity
import com.company.deviceapp.data.remote.api.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

enum class SampleCabinetStep {
    WAITING_LOGIN, DOING_REGISTRATION, SUBMITTING, SUCCESS
}

data class SampleCabinetUiState(
    val currentStep: SampleCabinetStep = SampleCabinetStep.WAITING_LOGIN,
    val operatorName: String? = null,
    val operatorId: String? = null,
    val cabinetName: String = "智能留样柜01号",
    val statusMessage: String = "系统就绪",

    val dishList: List<DishDto> = emptyList(),
    val mealList: List<MealDto> = emptyList(),

    val selectedDish: DishDto? = null,
    val selectedMeal: MealDto? = null,
    val capturedImgPath: String? = null,
    val weightInput: String = "",
    val tickCode: String = "",
    val status: String = "1",
    
    val errorMessage: String? = null,
    val isLoading: Boolean = false,
    val deviceSn: String = ""
)

@HiltViewModel
class SampleCabinetViewModel @Inject constructor(
    private val apiService: SampleCabinetApiService,
    private val personnelDao: PersonnelDao,
    private val sharedPrefs: SharedPreferences
) : ViewModel() {
    private val _uiState = MutableStateFlow(SampleCabinetUiState())
    val uiState: StateFlow<SampleCabinetUiState> = _uiState

    init {
        val sn = sharedPrefs.getString("DEVICE_SN", null) 
            ?: sharedPrefs.getString("serialNum", "DWDEV202201200353") 
            ?: "DWDEV202201200353"
        _uiState.update { it.copy(deviceSn = sn) }
    }

    fun onFaceRecognized(faceToken: String) {
        viewModelScope.launch {
            val user = personnelDao.getPersonnelByFaceToken(faceToken)
            if (user != null) {
                _uiState.update {
                    it.copy(
                        currentStep = SampleCabinetStep.DOING_REGISTRATION,
                        operatorName = user.name,
                        operatorId = user.personnelId,
                        statusMessage = "验证成功"
                    )
                }
                fetchMenuData(_uiState.value.deviceSn)
            } else {
                _uiState.update { it.copy(errorMessage = "未找到人员信息") }
            }
        }
    }

    fun onCardOrQrRecognized(code: String) {
        viewModelScope.launch {
            val user = personnelDao.getPersonnelByIcNum(code)
            if (user != null) {
                _uiState.update {
                    it.copy(
                        currentStep = SampleCabinetStep.DOING_REGISTRATION,
                        operatorName = user.name,
                        operatorId = user.personnelId,
                        statusMessage = "验证成功"
                    )
                }
                fetchMenuData(_uiState.value.deviceSn)
            } else {
                _uiState.update {
                    it.copy(errorMessage = "人员识别失败\n未查询到卡号: $code")
                }
            }
        }
    }

    private fun fetchMenuData(snCode: String) {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            try {
                val response = apiService.selectBySNCode(snCode)
                if (response.isSuccessful && response.data != null) {
                    _uiState.update {
                        it.copy(
                            dishList = response.data.dishList,
                            mealList = response.data.meal,
                            isLoading = false
                        )
                    }
                } else {
                    _uiState.update { it.copy(errorMessage = response.msg, isLoading = false) }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "获取菜单失败", isLoading = false) }
            }
        }
    }

    fun submitSampleRecord() {
        val state = _uiState.value
        val dish = state.selectedDish ?: return
        val meal = state.selectedMeal ?: return

        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.CHINA)
        val currentTime = sdf.format(Date())

        val weightValue = state.weightInput.filter { it.isDigit() || it == '.' }.toDoubleOrNull() ?: 0.0

        val request = SampleRecordRequest(
            snCode = state.deviceSn,
            operationId = state.operatorId ?: "0",
            operationTime = currentTime,
            name = state.cabinetName,
            status = state.status,
            dishSampleList = listOf(
                DishSampleItem(
                    dishCode = dish.dishCode,
                    tickCodeValue = state.tickCode.ifBlank { "N/A" },
                    mealCode = meal.dictValue,
                    imagePath = state.capturedImgPath ?: "",
                    weight = weightValue
                )
            )
        )

        Log.d("SampleSubmit", "JSON Request: $request")

        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            try {
                val response = apiService.saveOrUpdateDishSample(request)
                if (response.isSuccessful) {
                    Log.d("SampleSubmit", "提交成功, 成功处理条数: ${response.data}")
                    _uiState.update { it.copy(isLoading = false, currentStep = SampleCabinetStep.SUCCESS) }
                    kotlinx.coroutines.delay(1500)
                    reset()
                } else {
                    Log.e("SampleSubmit", "提交失败: ${response.msg}")
                    _uiState.update { it.copy(errorMessage = response.msg, isLoading = false) }
                }
            } catch (e: Exception) {
                Log.e("SampleSubmit", "提交异常", e)
                _uiState.update { it.copy(errorMessage = "提交记录异常", isLoading = false) }
            }
        }
    }

    fun selectDish(dish: DishDto) {
        _uiState.update { it.copy(selectedDish = dish) }
    }

    fun selectMeal(meal: MealDto) {
        _uiState.update { it.copy(selectedMeal = meal) }
    }

    fun setStatus(status: String) {
        _uiState.update { it.copy(status = status) }
    }

    fun onPhotoCaptured(path: String) {
        _uiState.update { it.copy(capturedImgPath = path) }
    }

    fun updateWeight(weight: String) {
        _uiState.update { it.copy(weightInput = weight) }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun resetToNextSample() {
        reset()
    }

    fun reset() {
        _uiState.update { 
            SampleCabinetUiState(
                deviceSn = it.deviceSn,
                cabinetName = it.cabinetName 
            ) 
        }
    }
}

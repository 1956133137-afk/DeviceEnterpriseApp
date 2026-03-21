package com.company.deviceapp.data.remote.api

import com.company.deviceapp.data.remote.dto.ApiResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface SampleCabinetApiService {

    /**
     * 1. 获取菜单数据接口
     */
    @GET("/sample/sample/selectBySNCode")
    suspend fun selectBySNCode(
        @Query("SNCode") snCode: String
    ): ApiResponse<MenuDataDto>

    /**
     * 3.1 留样、取样记录接口
     */
    @POST("/sample/sample/saveOrUpdateDishSample")
    suspend fun saveOrUpdateDishSample(
        @Body request: SampleRecordRequest
    ): ApiResponse<Int>
}

// --- 响应数据结构 ---

data class MenuDataDto(
    val dishList: List<DishDto>,
    val dishCategoryList: List<DishCategoryDto>,
    val tenant: TenantDto?,
    val dept: DeptDto?,
    val meal: List<MealDto>
)

data class DishDto(
    val dishCode: String,
    val dishName: String,
    val weight: String?,
    val imagePath: String?,
    val dishCategoryId: String
)

data class DishCategoryDto(
    val dishCategoryId: String,
    val dishCategoryName: String
)

data class TenantDto(
    val id: String,
    val name: String
)

data class DeptDto(
    val deptId: String,
    val deptName: String
)

data class MealDto(
    val dictValue: String,
    val dictLabel: String
)

// --- 请求参数数据结构 ---

data class SampleRecordRequest(
    val snCode: String,
    val dishSampleList: List<DishSampleItem>,
    val operationId: String,
    val operationTime: String,
    val name: String,
    val status: String // 1=留样，2=取样
)

data class DishSampleItem(
    val dishCode: String,
    val tickCodeValue: String,
    val mealCode: String,
    val imagePath: String,
    val weight: Any 
)

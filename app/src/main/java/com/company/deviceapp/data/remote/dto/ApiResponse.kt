package com.company.deviceapp.data.remote.dto

/**
 *  API 响应封装类
 */
data class ApiResponse<T>(
    val code: String?,
    val msg: String?,
    val message: String?,
    val success: Boolean?,
    val data: T?
) {
    val isSuccessful: Boolean
        get() = code == "00" || code == "200" || success == true
}
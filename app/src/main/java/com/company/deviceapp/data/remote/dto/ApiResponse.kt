package com.company.deviceapp.data.remote.dto

/**
 * 企业级统一 API 响应封装类
 * 严格适配文档中不同接口的返回格式差异
 */
data class ApiResponse<T>(
    val code: String?,     // 兼容数字 "200" 和 字符串 "00"
    val msg: String?,      // 有的接口叫 msg
    val message: String?,  // 有的接口叫 message
    val success: Boolean?, // 针对 MQ配置接口 的 success 字段
    val data: T?           // 泛型数据体
) {
    // 统一判断请求是否成功的辅助属性
    val isSuccessful: Boolean
        get() = code == "00" || code == "200" || success == true
}
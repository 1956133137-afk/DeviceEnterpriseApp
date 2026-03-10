// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    
    // 显式在根目录声明 Hilt 和 KSP 插件版本
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.ksp) apply false
}
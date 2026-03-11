package com.company.deviceapp.di

import android.content.Context
import android.content.SharedPreferences
import androidx.room.Room
import com.company.deviceapp.data.local.db.AppDatabase
import com.company.deviceapp.data.local.db.PersonnelDao
import com.company.deviceapp.data.local.db.PersonnelEntity
import com.company.deviceapp.data.remote.api.MorningInspectionApiService
import com.company.deviceapp.mqtt.MqttClientManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    // 1. 全局提供本地存储 SharedPreferences
    @Provides
    @Singleton
    fun provideSharedPreferences(@ApplicationContext context: Context): SharedPreferences {
        return context.getSharedPreferences("device_config", Context.MODE_PRIVATE)
    }

    // 2. 网络框架动态读取保存的 URL
    @Provides
    @Singleton
    fun provideRetrofit(sharedPreferences: SharedPreferences): Retrofit {
        // 如果本地没存过，才使用默认的 192.168.1.100
        val baseUrl = sharedPreferences.getString("BASE_URL", "http://192.168.2.8:8080/")
            ?: "http://192.168.1.100:8080/"

        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideMorningInspectionApiService(retrofit: Retrofit): MorningInspectionApiService {
        return retrofit.create(MorningInspectionApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "device_enterprise_db"
        ).build()
    }

    @Provides
    @Singleton
    fun providePersonnelDao(database: AppDatabase): PersonnelDao {
        return database.personnelDao()
    }

    @Provides
    @Singleton
    fun provideMqttClientManager(
        @ApplicationContext context: Context,
        personnelDao: PersonnelDao
    ): MqttClientManager {
        return MqttClientManager(context, personnelDao)
    }
}
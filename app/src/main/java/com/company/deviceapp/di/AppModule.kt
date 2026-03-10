package com.company.deviceapp.di

import com.company.deviceapp.data.local.db.PersonnelDao
import com.company.deviceapp.data.local.db.PersonnelEntity
import com.company.deviceapp.data.remote.api.MorningInspectionApiService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideRetrofit(): Retrofit {
        return Retrofit.Builder()
            // 兼容文档中的基础地址
            .baseUrl("http://192.168.1.100:8080/")
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
    fun providePersonnelDao(): PersonnelDao {
        // 临时匿名实现，保证 Hilt 编译 100% 成功。后续再接入真实的 Room Database
        return object : PersonnelDao {
            override suspend fun insertOrUpdatePersonnel(personnel: PersonnelEntity) {}
            override suspend fun deletePersonnelById(id: String) {}
            override suspend fun deleteAllPersonnel() {}
            override suspend fun getPersonnelById(id: String): PersonnelEntity? = null
        }
    }
}
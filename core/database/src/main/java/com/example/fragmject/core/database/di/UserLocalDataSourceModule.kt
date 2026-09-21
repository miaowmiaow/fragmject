package com.example.fragmject.core.database.di

import com.example.fragmject.core.data.contract.local.UserLocalDataSource
import com.example.fragmject.core.database.local.UserLocalDataSourceImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * 用户本地数据源的 Hilt 绑定：把 Room 适配器 [UserLocalDataSourceImpl]
 * 绑定到 data-contract 的 [UserLocalDataSource]。
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class UserLocalDataSourceModule {

    @Binds
    @Singleton
    abstract fun bindUserLocalDataSource(impl: UserLocalDataSourceImpl): UserLocalDataSource
}

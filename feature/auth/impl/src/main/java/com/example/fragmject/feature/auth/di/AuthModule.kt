package com.example.fragmject.feature.auth.di

import com.example.fragmject.core.navigation.AuthStateProvider
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * 登录态契约绑定：把 [AuthStateProviderImpl] 绑定到 core:navigation 的 [AuthStateProvider]。
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class AuthModule {

    @Binds
    @Singleton
    abstract fun bindAuthStateProvider(impl: AuthStateProviderImpl): AuthStateProvider
}

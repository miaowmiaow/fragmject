package com.example.fragmject.feature.auth.di

import com.example.fragmject.feature.auth.AuthSession
import com.example.fragmject.feature.auth.AuthSessionImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * 认证会话契约绑定：把 [AuthSessionImpl] 绑定到 feature:auth:api 的 [AuthSession]。
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class AuthModule {

    @Binds
    @Singleton
    abstract fun bindAuthSession(impl: AuthSessionImpl): AuthSession
}

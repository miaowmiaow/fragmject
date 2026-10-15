package com.example.fragmject.feature.authenticator.di

import com.example.fragmject.core.navigation.runtime.NavContentContributor
import com.example.fragmject.feature.authenticator.nav.AuthenticatorNavContentContributor
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet

/**
 * 把 Authenticator 的路由渲染注册贡献给 app 组合根。
 *
 * 这是 feature 与组合根之间的**唯一**装配点：app 只依赖本模块即可，
 * 不需要（也不应该）显式引用任何页面 Composable。
 */
@Module
@InstallIn(SingletonComponent::class)
object AuthenticatorNavModule {

    @Provides
    @IntoSet
    fun provideAuthenticatorNavContributor(): NavContentContributor = AuthenticatorNavContentContributor
}

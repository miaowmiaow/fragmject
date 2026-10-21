package com.example.fragmject.feature.authenticator.di

import com.example.fragmject.feature.authenticator.utils.Clock
import com.example.fragmject.feature.authenticator.utils.SntpOffsetProvider
import com.example.fragmject.feature.authenticator.utils.SystemClock
import com.example.fragmject.feature.authenticator.utils.TimeOffsetProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * 时间相关端口的生产绑定。
 *
 * 放在 feature 模块的 DI 里而不是 core：这两个端口目前只有 Authenticator 使用，
 * 过早下沉会把「验证器特有的对时需求」变成全局契约。
 */
@Module
@InstallIn(SingletonComponent::class)
object TimeModule {

    @Provides
    fun provideClock(): Clock = SystemClock

    @Provides
    fun provideTimeOffsetProvider(): TimeOffsetProvider = SntpOffsetProvider
}

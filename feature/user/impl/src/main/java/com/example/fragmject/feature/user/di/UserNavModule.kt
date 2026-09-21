package com.example.fragmject.feature.user.di

import com.example.fragmject.core.navigation.NavContentContributor
import com.example.fragmject.feature.user.nav.UserNavContentContributor
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet

/**
 * User 导航贡献者的 Hilt multibinding。
 */
@Module
@InstallIn(SingletonComponent::class)
object UserNavModule {

    @Provides
    @IntoSet
    fun provideUserNavContributor(): NavContentContributor = UserNavContentContributor
}

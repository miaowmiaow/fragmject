package com.example.fragmject.feature.home.di

import com.example.fragmject.core.navigation.NavContentContributor
import com.example.fragmject.feature.home.nav.HomeNavContentContributor
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet

/**
 * Home 导航贡献者的 Hilt multibinding。
 */
@Module
@InstallIn(SingletonComponent::class)
object HomeNavModule {

    @Provides
    @IntoSet
    fun provideHomeNavContributor(): NavContentContributor = HomeNavContentContributor
}

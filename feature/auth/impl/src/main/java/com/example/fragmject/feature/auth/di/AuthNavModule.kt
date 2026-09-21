package com.example.fragmject.feature.auth.di

import com.example.fragmject.core.navigation.NavContentContributor
import com.example.fragmject.feature.auth.nav.AuthNavContentContributor
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet

/**
 * Auth 导航贡献者的 Hilt multibinding。
 */
@Module
@InstallIn(SingletonComponent::class)
object AuthNavModule {

    @Provides
    @IntoSet
    fun provideAuthNavContributor(): NavContentContributor = AuthNavContentContributor
}

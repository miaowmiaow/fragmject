package com.example.fragmject.feature.demo.di

import com.example.fragmject.core.navigation.NavContentContributor
import com.example.fragmject.feature.demo.nav.DemoNavContentContributor
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet

/**
 * Demo 导航贡献者的 Hilt multibinding。
 */
@Module
@InstallIn(SingletonComponent::class)
object DemoNavModule {

    @Provides
    @IntoSet
    fun provideDemoNavContributor(): NavContentContributor = DemoNavContentContributor
}

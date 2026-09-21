package com.example.fragmject.feature.search.di

import com.example.fragmject.core.navigation.NavContentContributor
import com.example.fragmject.feature.search.nav.SearchNavContentContributor
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet

/**
 * Search 导航贡献者的 Hilt multibinding。
 */
@Module
@InstallIn(SingletonComponent::class)
object SearchNavModule {

    @Provides
    @IntoSet
    fun provideSearchNavContributor(): NavContentContributor = SearchNavContentContributor
}

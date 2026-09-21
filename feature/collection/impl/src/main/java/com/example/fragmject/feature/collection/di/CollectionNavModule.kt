package com.example.fragmject.feature.collection.di

import com.example.fragmject.core.navigation.NavContentContributor
import com.example.fragmject.feature.collection.nav.CollectionNavContentContributor
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet

/**
 * Collection 导航贡献者的 Hilt multibinding。
 */
@Module
@InstallIn(SingletonComponent::class)
object CollectionNavModule {

    @Provides
    @IntoSet
    fun provideCollectionNavContributor(): NavContentContributor = CollectionNavContentContributor
}

package com.example.fragmject.feature.picture.di

import com.example.fragmject.core.navigation.NavContentContributor
import com.example.fragmject.feature.picture.nav.PictureNavContentContributor
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet

/**
 * Picture 导航贡献者的 Hilt multibinding。
 */
@Module
@InstallIn(SingletonComponent::class)
object PictureNavModule {

    @Provides
    @IntoSet
    fun providePictureNavContributor(): NavContentContributor = PictureNavContentContributor
}

package com.example.fragmject.feature.picture.di

import com.example.fragmject.core.navigation.runtime.NavContentContributor
import com.example.fragmject.core.navigation.runtime.NavFlowScopeContributor
import com.example.fragmject.feature.picture.nav.PictureFlowScopeContributor
import com.example.fragmject.feature.picture.nav.PictureNavContentContributor
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet

/**
 * Picture 导航贡献者与流程作用域贡献者的 Hilt multibinding。
 */
@Module
@InstallIn(SingletonComponent::class)
object PictureNavModule {

    @Provides
    @IntoSet
    fun providePictureNavContributor(): NavContentContributor = PictureNavContentContributor

    @Provides
    @IntoSet
    fun providePictureFlowScopeContributor(impl: PictureFlowScopeContributor): NavFlowScopeContributor = impl
}

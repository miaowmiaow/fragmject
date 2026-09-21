package com.example.fragmject.feature.article.di

import com.example.fragmject.core.navigation.NavContentContributor
import com.example.fragmject.feature.article.nav.ArticleNavContentContributor
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet

/**
 * Article 导航贡献者的 Hilt multibinding。
 */
@Module
@InstallIn(SingletonComponent::class)
object ArticleNavModule {

    @Provides
    @IntoSet
    fun provideArticleNavContributor(): NavContentContributor = ArticleNavContentContributor
}

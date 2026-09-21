package com.example.fragmject.app.navigation

import com.example.fragmject.core.navigation.contracts.ArticleNavigator
import com.example.fragmject.core.navigation.contracts.AuthNavigator
import com.example.fragmject.core.navigation.contracts.CollectionNavigator
import com.example.fragmject.core.navigation.contracts.DemoNavigator
import com.example.fragmject.core.navigation.contracts.HomeNavigator
import com.example.fragmject.core.navigation.contracts.PictureNavigator
import com.example.fragmject.core.navigation.contracts.SearchNavigator
import com.example.fragmject.core.navigation.contracts.UserNavigator
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * 语义导航契约的 Hilt 绑定：把 core:navigation-contracts 下的接口
 * 绑定到 app 组合根提供的实现（AppXxxNavigator）。
 *
 * 实现类均标注 @Singleton，@Binds 保持其单例作用域。
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class NavigationModule {

    @Binds
    abstract fun bindArticleNavigator(impl: AppArticleNavigator): ArticleNavigator

    @Binds
    abstract fun bindUserNavigator(impl: AppUserNavigator): UserNavigator

    @Binds
    abstract fun bindAuthNavigator(impl: AppAuthNavigator): AuthNavigator

    @Binds
    abstract fun bindCollectionNavigator(impl: AppCollectionNavigator): CollectionNavigator

    @Binds
    abstract fun bindSearchNavigator(impl: AppSearchNavigator): SearchNavigator

    @Binds
    abstract fun bindDemoNavigator(impl: AppDemoNavigator): DemoNavigator

    @Binds
    abstract fun bindPictureNavigator(impl: AppPictureNavigator): PictureNavigator

    @Binds
    abstract fun bindHomeNavigator(impl: AppHomeNavigator): HomeNavigator
}

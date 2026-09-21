package com.example.fragmject.app.navigation

import com.example.fragmject.core.navigation.contracts.ArticleNavigator
import com.example.fragmject.core.navigation.contracts.AuthNavigator
import com.example.fragmject.core.navigation.contracts.CollectionNavigator
import com.example.fragmject.core.navigation.contracts.DemoNavigator
import com.example.fragmject.core.navigation.contracts.HomeNavigator
import com.example.fragmject.core.navigation.contracts.PictureNavigator
import com.example.fragmject.core.navigation.contracts.SearchNavigator
import com.example.fragmject.core.navigation.contracts.UserNavigator
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 语义导航契约实现聚合 Holder。
 *
 * app 组合根通过 [NavigationModule] 将 8 个语义 Navigator 接口绑定到 [AppXxxNavigator]
 * 实现；本类聚合它们，供 MainActivity 一次性注入后经 CompositionLocalProvider 提供给
 * Compose 环境，避免 Activity 声明 8 个注入字段。
 */
@Singleton
class AppNavigatorBundle @Inject constructor(
    val articleNavigator: ArticleNavigator,
    val userNavigator: UserNavigator,
    val authNavigator: AuthNavigator,
    val collectionNavigator: CollectionNavigator,
    val searchNavigator: SearchNavigator,
    val demoNavigator: DemoNavigator,
    val pictureNavigator: PictureNavigator,
    val homeNavigator: HomeNavigator,
)

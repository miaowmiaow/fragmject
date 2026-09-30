package com.example.fragmject.feature.home.nav

import androidx.compose.runtime.key
import com.example.fragmject.core.navigation.runtime.LocalDetailContent
import com.example.fragmject.core.navigation.runtime.LocalOnClearDetail
import com.example.fragmject.core.navigation.runtime.LocalSelectedDetailKey
import com.example.fragmject.core.navigation.runtime.NavContentContributor
import com.example.fragmject.core.navigation.runtime.NavContentRegistry
import com.example.fragmject.core.navigation.contract.LocalArticleNavigator
import com.example.fragmject.core.navigation.contract.LocalHomeNavigator
import com.example.fragmject.core.navigation.contract.LocalUserNavigator
import com.example.fragmject.feature.home.MainNavKey
import com.example.fragmject.feature.home.SystemNavKey
import com.example.fragmject.feature.home.ui.main.MainScreen
import com.example.fragmject.feature.home.ui.system.SystemScreen

/**
 * Home Feature 导航内容贡献者：注册本域 NavKey → 渲染器映射。
 *
 * [MainNavKey] 在 Expanded 模式下需要「详情面板」运行时状态（selectedDetailKey /
 * onClearDetail / detailContent），这些状态由 app 组合根通过 CompositionLocal
 * 提供，在注册渲染器的 Composable lambda 内读取，避免显式参数跨层传递。
 */
object HomeNavContentContributor : NavContentContributor {
    override fun contribute(registry: NavContentRegistry) {
        registry.register<MainNavKey> {
            MainScreen(
                selectedDetailKey = LocalSelectedDetailKey.current,
                onClearDetail = LocalOnClearDetail.current,
                detailContent = LocalDetailContent.current,
            )
        }
        registry.register<SystemNavKey> { navKey ->
            key(navKey.cid) {
                SystemScreen(
                    cid = navKey.cid,
                    actions = homeNavActions(
                        articleNavigator = LocalArticleNavigator.current,
                        userNavigator = LocalUserNavigator.current,
                        homeNavigator = LocalHomeNavigator.current,
                    ),
                )
            }
        }
    }
}
package com.example.fragmject.feature.user.nav

import androidx.compose.runtime.key
import com.example.fragmject.core.navigation.NavContentContributor
import com.example.fragmject.core.navigation.NavContentRegistry
import com.example.fragmject.feature.user.BrowseHistoryNavKey
import com.example.fragmject.feature.user.MyCoinNavKey
import com.example.fragmject.feature.user.RankNavKey
import com.example.fragmject.feature.user.SettingNavKey
import com.example.fragmject.feature.user.UserNavKey
import com.example.fragmject.feature.user.ui.history.BrowseHistoryScreen
import com.example.fragmject.feature.user.ui.mycoin.MyCoinScreen
import com.example.fragmject.feature.user.ui.rank.RankScreen
import com.example.fragmject.feature.user.ui.setting.SettingScreen
import com.example.fragmject.feature.user.ui.user.UserScreen

/**
 * User Feature 导航内容贡献者：注册本域 NavKey → 渲染器映射。
 */
object UserNavContentContributor : NavContentContributor {
    override fun contribute(registry: NavContentRegistry) {
        registry.register<BrowseHistoryNavKey> { _, callbacks ->
            BrowseHistoryScreen(onNavigate = callbacks.onNavigate, onNavigateUp = callbacks.onNavigateUp)
        }
        registry.register<MyCoinNavKey> { _, callbacks ->
            MyCoinScreen(onNavigate = callbacks.onNavigate, onNavigateUp = callbacks.onNavigateUp)
        }
        registry.register<RankNavKey> { _, callbacks ->
            RankScreen(onNavigate = callbacks.onNavigate, onNavigateUp = callbacks.onNavigateUp)
        }
        registry.register<SettingNavKey> { _, callbacks ->
            SettingScreen(onNavigate = callbacks.onNavigate, onNavigateUp = callbacks.onNavigateUp)
        }
        registry.register<UserNavKey> { navKey, callbacks ->
            key(navKey.userId) {
                UserScreen(userId = navKey.userId, onNavigate = callbacks.onNavigate, onNavigateUp = callbacks.onNavigateUp)
            }
        }
    }
}

package com.example.fragmject.feature.user.nav

import androidx.compose.runtime.key
import com.example.fragmject.core.navigation.runtime.NavContentContributor
import com.example.fragmject.core.navigation.runtime.NavContentRegistry
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
        registry.register<BrowseHistoryNavKey> {
            BrowseHistoryScreen()
        }
        registry.register<MyCoinNavKey> {
            MyCoinScreen()
        }
        registry.register<RankNavKey> {
            RankScreen()
        }
        registry.register<SettingNavKey> {
            SettingScreen()
        }
        registry.register<UserNavKey> { navKey ->
            key(navKey.userId) {
                UserScreen(userId = navKey.userId)
            }
        }
    }
}

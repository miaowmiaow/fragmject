package com.example.fragmject.feature.user.nav

import androidx.compose.runtime.key
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
 * User Feature 内容自注册：将本 feature 的 NavKey → 渲染器映射下沉到 feature 内部，
 * 全屏 entry 与面板 detailContent 共用，app 装配层无需 import 具体 NavKey/Screen。
 */
fun NavContentRegistry.registerUserNavContents() {
    register<BrowseHistoryNavKey> { _, callbacks ->
        BrowseHistoryScreen(onNavigate = callbacks.onNavigate, onNavigateUp = callbacks.onNavigateUp)
    }
    register<MyCoinNavKey> { _, callbacks ->
        MyCoinScreen(onNavigate = callbacks.onNavigate, onNavigateUp = callbacks.onNavigateUp)
    }
    register<RankNavKey> { _, callbacks ->
        RankScreen(onNavigate = callbacks.onNavigate, onNavigateUp = callbacks.onNavigateUp)
    }
    register<SettingNavKey> { _, callbacks ->
        SettingScreen(onNavigate = callbacks.onNavigate, onNavigateUp = callbacks.onNavigateUp)
    }
    register<UserNavKey> { navKey, callbacks ->
        key(navKey.userId) {
            UserScreen(userId = navKey.userId, onNavigate = callbacks.onNavigate, onNavigateUp = callbacks.onNavigateUp)
        }
    }
}

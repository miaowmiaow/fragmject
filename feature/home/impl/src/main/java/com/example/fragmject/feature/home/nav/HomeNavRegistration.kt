package com.example.fragmject.feature.home.nav

import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.navigation3.runtime.NavKey
import com.example.fragmject.core.navigation.NavContentRegistry
import com.example.fragmject.feature.home.MainNavKey
import com.example.fragmject.feature.home.SystemNavKey
import com.example.fragmject.feature.home.ui.main.MainScreen
import com.example.fragmject.feature.home.ui.system.SystemScreen

/**
 * Home Feature 内容自注册。
 *
 * [MainNavKey] 需接收 Expanded 模式状态（selectedDetailKey/onClearDetail）与
 * 右侧详情面板渲染器（detailContent）。
 */
fun NavContentRegistry.registerHomeNavContents(
    selectedDetailKey: NavKey?,
    onClearDetail: () -> Unit,
    detailContent: @Composable (NavKey, (NavKey) -> Unit, () -> Unit) -> Unit,
) {
    register<MainNavKey> { _, callbacks ->
        MainScreen(
            onNavigate = callbacks.onNavigate,
            selectedDetailKey = selectedDetailKey,
            onClearDetail = onClearDetail,
            detailContent = detailContent,
        )
    }
    register<SystemNavKey> { navKey, callbacks ->
        key(navKey.cid) {
            SystemScreen(cid = navKey.cid, onNavigate = callbacks.onNavigate, onNavigateUp = callbacks.onNavigateUp)
        }
    }
}

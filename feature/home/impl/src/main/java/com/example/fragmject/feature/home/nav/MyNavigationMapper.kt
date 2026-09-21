package com.example.fragmject.feature.home.nav

import androidx.navigation3.runtime.NavKey
import com.example.fragmject.feature.auth.LoginNavKey
import com.example.fragmject.feature.collection.MyCollectNavKey
import com.example.fragmject.feature.collection.MyShareNavKey
import com.example.fragmject.feature.demo.DemoNavKey
import com.example.fragmject.feature.user.BrowseHistoryNavKey
import com.example.fragmject.feature.user.MyCoinNavKey
import com.example.fragmject.feature.user.SettingNavKey
import com.example.fragmject.feature.user.UserNavKey

/**
 * 「我的」菜单页语义导航动作。
 *
 * 页面层只上抛语义事件（进入用户主页 / 登录 / 打开各功能入口），
 * 不直接接触任何 NavKey；跨 feature NavKey 的创建集中在本文件（导航编排层）完成。
 *
 * 注：登录态判断（已登录 → 用户主页，未登录 → 登录页）是页面层的业务逻辑，
 * 因此拆为 [onUserClick] 与 [onLoginClick] 两个独立语义事件，由页面层按 [user.id] 决定触发哪个。
 */
data class MyNavActions(
    val onUserClick: (String) -> Unit = {},
    val onLoginClick: () -> Unit = {},
    val onDemoClick: () -> Unit = {},
    val onMyCoinClick: () -> Unit = {},
    val onMyCollectClick: () -> Unit = {},
    val onMyShareClick: () -> Unit = {},
    val onBrowseHistoryClick: () -> Unit = {},
    val onSettingClick: () -> Unit = {},
)

/**
 * 将导航回调转换为「我的」页语义动作，集中创建各 feature NavKey。
 */
fun myNavActions(onNavigate: (NavKey) -> Unit): MyNavActions = MyNavActions(
    onUserClick = { onNavigate(UserNavKey(it)) },
    onLoginClick = { onNavigate(LoginNavKey) },
    onDemoClick = { onNavigate(DemoNavKey) },
    onMyCoinClick = { onNavigate(MyCoinNavKey) },
    onMyCollectClick = { onNavigate(MyCollectNavKey) },
    onMyShareClick = { onNavigate(MyShareNavKey) },
    onBrowseHistoryClick = { onNavigate(BrowseHistoryNavKey) },
    onSettingClick = { onNavigate(SettingNavKey) },
)

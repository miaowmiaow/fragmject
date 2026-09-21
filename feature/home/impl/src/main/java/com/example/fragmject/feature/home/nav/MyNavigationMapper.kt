package com.example.fragmject.feature.home.nav

import com.example.fragmject.core.navigation.contracts.AuthNavigator
import com.example.fragmject.core.navigation.contracts.CollectionNavigator
import com.example.fragmject.core.navigation.contracts.DemoNavigator
import com.example.fragmject.core.navigation.contracts.UserNavigator

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
 * 将语义导航契约转换为「我的」页语义动作。
 *
 * 依赖 [com.example.fragmject.core.navigation.contracts] 下的语义接口，
 * 由 app 组合根提供实现并映射到具体 NavKey；本模块不再 import 任何跨 feature NavKey。
 */
fun myNavActions(
    userNavigator: UserNavigator,
    authNavigator: AuthNavigator,
    collectionNavigator: CollectionNavigator,
    demoNavigator: DemoNavigator,
): MyNavActions = MyNavActions(
    onUserClick = { userNavigator.openUserProfile(it) },
    onLoginClick = { authNavigator.openLogin() },
    onDemoClick = { demoNavigator.openDemo() },
    onMyCoinClick = { userNavigator.openMyCoin() },
    onMyCollectClick = { collectionNavigator.openMyCollect() },
    onMyShareClick = { collectionNavigator.openMyShare() },
    onBrowseHistoryClick = { userNavigator.openBrowseHistory() },
    onSettingClick = { userNavigator.openSetting() },
)

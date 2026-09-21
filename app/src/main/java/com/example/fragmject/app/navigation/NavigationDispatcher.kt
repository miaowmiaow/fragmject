package com.example.fragmject.app.navigation

import androidx.navigation3.runtime.NavKey
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 导航执行桥（app 组合根专属）。
 *
 * backStack 是 [androidx.navigation3.runtime.NavBackStack]（Composable 局部状态），
 * 无法直接注入到 Hilt 单例的语义 Navigator 实现中。本类作为可变持有者，
 * 由 [com.example.fragmject.app.navigation.AppNavGraph] 在组合期绑定当前导航能力，
 * 语义 Navigator 实现（如 AppArticleNavigator）通过本类把「语义动作」落到具体 NavKey。
 */
@Singleton
class NavigationDispatcher @Inject constructor() {

    /** 推入/跳转到指定路由。 */
    var navigate: (NavKey) -> Unit = { }

    /** 返回上一级。 */
    var navigateUp: () -> Unit = { }

    /** 弹出到指定路由所在位置。 */
    var popBackStack: (NavKey) -> Unit = { }
}

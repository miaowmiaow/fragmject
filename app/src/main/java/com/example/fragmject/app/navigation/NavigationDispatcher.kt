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

    // 导航能力由组合根（AppNavGraph）在组合期通过 bind 注入，
    // 语义 Navigator（AppXxxNavigator）只读调用，无法覆盖。
    private var navigateImpl: (NavKey) -> Unit = { }
    private var navigateUpImpl: () -> Unit = { }
    private var popBackStackImpl: (NavKey) -> Unit = { }
    private var onAuthSuccessImpl: () -> Unit = { }

    /** 推入/跳转到指定路由（只读）。 */
    val navigate: (NavKey) -> Unit get() = navigateImpl

    /** 返回上一级（只读）。 */
    val navigateUp: () -> Unit get() = navigateUpImpl

    /** 弹出到指定路由所在位置（只读）。 */
    val popBackStack: (NavKey) -> Unit get() = popBackStackImpl

    /** 认证（登录/注册）成功后回跳（只读）。 */
    val onAuthSuccess: () -> Unit get() = onAuthSuccessImpl

    /** 绑定当前导航能力，仅 [AppNavGraph]（组合根）在组合期调用。 */
    fun bind(
        navigate: (NavKey) -> Unit,
        navigateUp: () -> Unit,
        popBackStack: (NavKey) -> Unit,
        onAuthSuccess: () -> Unit,
    ) {
        navigateImpl = navigate
        navigateUpImpl = navigateUp
        popBackStackImpl = popBackStack
        onAuthSuccessImpl = onAuthSuccess
    }
}

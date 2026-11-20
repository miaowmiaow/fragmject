package com.example.fragmject.app.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.deeplink.DeepLinkRequest
import com.example.fragmject.core.domain.AuthStatus
import com.example.fragmject.core.navigation.contract.DetailPaneNavKey
import com.example.fragmject.core.navigation.contract.RequiresAuth
import com.example.fragmject.feature.home.MainNavKey

/*
 * 导航图的副作用段：只依赖 backStack 与两个导航动作，与「渲染」（NavDisplay / 详情面板）
 * 和「动作定义」（rememberNavActions）互不相关，因此从 AppNavGraph 拆出。
 *
 * 拆分的动因是行数：AppNavGraph 曾到 466 行（规则 28 上限 500，400 行即告警），
 * 继续往里加一段深链处理就会顶到上限，届时被迫在改动导航逻辑的同时做大重构。
 *
 * 这里全部是 `internal` 而不是 `private`：同包内只被 AppNavGraph 调用，但跨文件。
 */

/**
 * 图片流程退出期间清空上一次的选图结果。
 *
 * 结果只在「确认选择」时写入（PictureNavContentContributor.onConfirm），而系统返回/手势返回
 * 不会走选择器的取消回调，否则发起方下次进入仍会看到上一次选中的图片。
 * 在**进入**时清空比在退出时清空更可靠：退出时清空会把刚确认的结果一并擦掉。
 */
@Composable
internal fun ClearPictureSelectionOnEnter(
    hasPictureFlow: Boolean,
    clearSelection: () -> Unit,
) {
    val currentClearSelection by rememberUpdatedState(clearSelection)
    LaunchedEffect(hasPictureFlow) {
        if (hasPictureFlow) currentClearSelection()
    }
}

/** 登录态确定后放行暂存的受保护目标（未知时先暂存，确定后统一交给 navigate 守卫）。 */
@Composable
internal fun ConsumeDeferredNavigation(
    authStatus: AuthStatus,
    deferredNavigation: MutableState<NavKey?>,
    navigate: (NavKey) -> Unit,
) {
    val currentNavigate by rememberUpdatedState(navigate)
    LaunchedEffect(authStatus) {
        if (authStatus != AuthStatus.UNKNOWN) {
            deferredNavigation.value?.let { key ->
                deferredNavigation.value = null
                currentNavigate(key)
            }
        }
    }
}

/**
 * 冷启动深链守卫。
 *
 * 受保护目标一律经 navigate：未登录时跳转登录并在成功后回跳，登录态未知时先暂存。
 * 初始栈已剔除受保护目标与详情面板目标，这里只执行一次（rememberSaveable 覆盖配置变更与进程重建）。
 */
@Composable
internal fun GuardInitialDeepLinks(
    initialBackStack: List<NavKey>,
    navigate: (NavKey) -> Unit,
    placeKey: (NavKey) -> Unit,
) {
    var guarded by rememberSaveable { mutableStateOf(false) }
    val currentNavigate by rememberUpdatedState(navigate)
    val currentPlaceKey by rememberUpdatedState(placeKey)
    LaunchedEffect(Unit) {
        if (guarded) return@LaunchedEffect
        guarded = true
        initialBackStack.dropWhile { it == MainNavKey }.forEach { key ->
            when (key) {
                is RequiresAuth -> currentNavigate(key)
                is DetailPaneNavKey -> currentPlaceKey(key)
                // 全屏且无需登录的目标已直接进初始栈，此处无需处理
            }
        }
    }
}

/** 运行时深层链接（onNewIntent 触发）：首次启动由初始栈处理，此处仅处理后续动态深层链接。 */
@Composable
internal fun HandlePendingDeepLink(
    pendingDeepLink: DeepLinkRequest?,
    backStack: NavBackStack<NavKey>,
    navigate: (NavKey) -> Unit,
    placeKey: (NavKey) -> Unit,
    onDeepLinkConsumed: () -> Unit,
) {
    val currentNavigate by rememberUpdatedState(navigate)
    val currentPlaceKey by rememberUpdatedState(placeKey)
    val currentOnConsumed by rememberUpdatedState(onDeepLinkConsumed)
    LaunchedEffect(pendingDeepLink) {
        val request = pendingDeepLink ?: return@LaunchedEffect
        matchDeepLink(request)?.let { newStack ->
            // 保留栈底 Main，避免用户在多层流程中时收到深链导致在途流程被整体丢弃
            backStack.clear()
            backStack.add(MainNavKey)
            newStack.dropWhile { it == MainNavKey }.forEach { key ->
                if (key is RequiresAuth) currentNavigate(key) else currentPlaceKey(key)
            }
        }
        currentOnConsumed()
    }
}

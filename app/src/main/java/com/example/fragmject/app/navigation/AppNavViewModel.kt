package com.example.fragmject.app.navigation

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.navigation3.runtime.NavKey
import com.example.fragmject.core.domain.AuthState
import com.example.fragmject.core.domain.AuthStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

/**
 * 导航层 ViewModel：提供当前登录状态与导航守卫相关的暂存态。
 *
 * 通过 core:domain 的 [AuthState] 契约获取登录态，不再直接依赖 UserRepository 或 User。
 *
 * 状态作用域约定：下面两个暂存态都采用「配置变更保留、进程死亡放弃」的普通 [mutableStateOf]
 * 而非 SavedStateHandle——进程死亡后登录态会重新初始化，暂存的回跳意图随之作废是预期行为。
 */
@HiltViewModel
class AppNavViewModel @Inject constructor(
    authState: AuthState,
) : ViewModel() {

    val authStatus: StateFlow<AuthStatus> = authState.status

    /** 登录态尚未确定（UNKNOWN）时暂存的受保护目标，待登录态确定后放行或转登录。 */
    internal val deferredNavigationState = mutableStateOf<NavKey?>(null)

    /** 需要登录时暂存的待回跳目标（登录成功后回跳）。 */
    private val _pendingRedirect = mutableStateOf<NavKey?>(null)
    internal val pendingRedirect: NavKey? get() = _pendingRedirect.value

    internal fun setPendingRedirect(key: NavKey) {
        _pendingRedirect.value = key
    }

    internal fun clearPendingRedirect() {
        _pendingRedirect.value = null
    }

    /** 取出并清空待回跳目标（一次性消费，避免重复回跳）。 */
    internal fun consumePendingRedirect(): NavKey? =
        _pendingRedirect.value.also { _pendingRedirect.value = null }
}

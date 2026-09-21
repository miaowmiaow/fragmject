package com.example.fragmject.core.navigation

import kotlinx.coroutines.flow.StateFlow

/**
 * 应用级登录态契约，供导航守卫判断是否需要跳转登录。
 *
 * 定义在 core:navigation，使 app 层只依赖本接口，不再直接消费
 * [com.example.fragmject.core.domain.repository.UserRepository] 或
 * [com.example.fragmject.core.model.User]。
 */
interface AuthStateProvider {
    val isLoggedIn: StateFlow<Boolean>
}

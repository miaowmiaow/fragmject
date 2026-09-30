package com.example.fragmject.core.domain

import kotlinx.coroutines.flow.StateFlow

/**
 * 应用级认证状态契约：单一状态持有者，供导航守卫消费「是否已登录」。
 *
 * 由 core:data-repository 提供实现（映射自 UserRepository.observeCurrentUser），
 * app 层只依赖本接口，不再 import UserRepository / User 模型。
 *
 * 与 [CollectState]、[ThemeState] 同属 core:domain，
 * 与 core:navigation-runtime 的 RequiresAuth 标记接口互补：
 * RequiresAuth 声明「此页需要登录」，AuthState 提供运行时「是否已登录」。
 */
interface AuthState {
    val isLoggedIn: StateFlow<Boolean>
}

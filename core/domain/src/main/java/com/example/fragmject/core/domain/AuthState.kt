package com.example.fragmject.core.domain

import kotlinx.coroutines.flow.StateFlow

/**
 * 登录态。[UNKNOWN] 表示本地用户记录尚未读取完成（冷启动初始瞬间），
 * 守卫不得把它当作「未登录」处理，否则会在已登录用户身上误触发登录跳转。
 */
enum class AuthStatus {
    UNKNOWN,
    LOGGED_IN,
    LOGGED_OUT,
}

/**
 * 应用级认证状态契约：单一状态持有者，供导航守卫消费登录态。
 *
 * 由 core:data-repository 提供实现（映射自 UserRepository.observeCurrentUser），
 * app 层只依赖本接口，不再 import UserRepository / User 模型。
 *
 * 与 [CollectState]、[ThemeState] 同属 core:domain，
 * 与 core:navigation-contract 的 RequiresAuth 标记接口互补：
 * RequiresAuth 声明「此页需要登录」，AuthState 提供运行时登录态。
 */
interface AuthState {
    val status: StateFlow<AuthStatus>
}

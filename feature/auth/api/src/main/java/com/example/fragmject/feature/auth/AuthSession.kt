package com.example.fragmject.feature.auth

import kotlinx.coroutines.flow.StateFlow

/**
 * Auth Feature — 认证会话契约（对外暴露的登录态查询）。
 *
 * 由 feature:auth:impl 提供实现（映射自 UserRepository.observeCurrentUser），
 * 供导航守卫（app 层）判断是否需要跳转登录。
 *
 * 与 core:navigation-runtime 的 RequiresAuth 标记接口互补：
 * RequiresAuth 声明「此页需要登录」，AuthSession 提供运行时「是否已登录」。
 */
interface AuthSession {
    val isLoggedIn: StateFlow<Boolean>
}

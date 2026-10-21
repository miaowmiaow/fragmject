package com.example.fragmject.core.domain.usecase

import com.example.fragmject.core.domain.auth.AuthGateway
import javax.inject.Inject

/**
 * 登出：远端 best effort + 本地必清。
 *
 * 原实现把这段策略写在 `UserRepositoryImpl.logout()` 里；上移后 Repository 只保留
 * 存储原子操作，策略由 [AuthSessionCoordinator] 与用例共同表达，可被单测直接覆盖。
 */
class LogoutUseCase @Inject constructor(
    private val gateway: AuthGateway,
    private val session: AuthSessionCoordinator,
) {
    /** 返回 true 表示本地清理已完成；清理失败且远端也失败时抛出异常。 */
    suspend operator fun invoke(): Boolean = session.logout { gateway.logout() }
}

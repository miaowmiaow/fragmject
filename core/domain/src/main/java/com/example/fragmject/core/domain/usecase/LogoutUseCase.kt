package com.example.fragmject.core.domain.usecase

import com.example.fragmject.core.domain.repository.UserRepository
import javax.inject.Inject

/**
 * 登出：调用领域端口，登出成功后由 data 层 Adapter 清空本地会话。
 */
class LogoutUseCase @Inject constructor(
    private val userRepo: UserRepository,
) {
    suspend operator fun invoke(): Boolean {
        return userRepo.logout().success
    }
}
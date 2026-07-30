package com.example.fragmject.core.domain.usecase

import com.example.fragmject.core.data.repository.UserRepository
import com.example.fragmject.core.database.model.UserEntity
import com.example.fragmject.core.database.store.UserStore
import javax.inject.Inject

/**
 * 登出：调用 API → 删除本地用户数据。
 */
class LogoutUseCase @Inject constructor(
    private val userRepo: UserRepository,
) {
    suspend operator fun invoke(user: UserEntity?): Boolean {
        val response = userRepo.logout()
        if (response.errorCode == "0") {
            user?.let { UserStore.deleteUser(it) }
            return true
        }
        return false
    }
}
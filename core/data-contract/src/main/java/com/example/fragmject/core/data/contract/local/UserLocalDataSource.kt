package com.example.fragmject.core.data.contract.local

import com.example.fragmject.core.model.User
import kotlinx.coroutines.flow.Flow

/**
 * 用户本地数据源：会话持久化能力契约。
 *
 * 定义在 data-contract 层，由 core:database 使用 Room DAO/Entity/Mapping 实现；
 * core:data 的 Repository 只依赖本契约，不感知 Room DAO、Entity 或 Store。
 */
interface UserLocalDataSource {
    fun observeCurrentUser(): Flow<User?>
    suspend fun saveUser(user: User)
    suspend fun clearUser()
}

package com.example.fragmject.core.database.local

import com.example.fragmject.core.database.dao.UserDao
import com.example.fragmject.core.database.model.toDomain
import com.example.fragmject.core.database.model.toEntity
import com.example.fragmject.core.data.contract.local.UserLocalDataSource
import com.example.fragmject.core.model.User
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * [UserLocalDataSource] 的 Room 适配器实现。
 *
 * 直接注入 [UserDao] 完成 User ↔ UserEntity 转换与持久化；
 * 对外只暴露领域模型 [User]，Room Entity 不泄漏到 data 层。
 */
@Singleton
class UserLocalDataSourceImpl @Inject constructor(
    private val userDao: UserDao,
) : UserLocalDataSource {

    override fun observeCurrentUser(): Flow<User?> =
        userDao.get().map { it?.toDomain() }

    override suspend fun saveUser(user: User) {
        userDao.clear()
        userDao.insert(user.toEntity())
    }

    override suspend fun clearUser() {
        userDao.clear()
    }
}

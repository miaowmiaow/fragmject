package com.example.fragmject.core.database.store

import com.example.fragmject.core.database.dao.UserDao
import com.example.fragmject.core.database.model.UserEntity
import com.example.fragmject.core.model.User
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 用户持久化存储 facade。
 *
 * 直接封装 [com.example.fragmject.core.database.dao.UserDao]，提供"获取/设置/删除"操作。
 * ViewModel / Repository 可直接注入本对象访问用户数据，无需经过中间层。
 */
@Singleton
class UserStore @Inject constructor(
    private val userDao: UserDao,
) {

    suspend fun setUser(user: UserEntity) {
        userDao.clear()
        userDao.insert(user)
    }

    suspend fun setUser(user: User) {
        setUser(user.toEntity())
    }

    suspend fun deleteUser(user: UserEntity) {
        userDao.delete(user)
    }

    suspend fun clearUser() {
        userDao.clear()
    }

    fun getUser(): Flow<UserEntity?> {
        return userDao.get()
    }
}

private fun User.toEntity() = UserEntity(
    id = id,
    username = username,
    nickname = nickname,
    token = token,
    password = password,
    admin = admin,
    email = email,
    icon = icon,
    type = type,
    publicName = publicName,
    coinCount = coinCount,
    collectIds = collectIds,
)

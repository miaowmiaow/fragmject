package com.example.fragmject.core.database.store

import com.example.fragmject.core.database.AppDatabase
import com.example.fragmject.core.database.model.UserEntity
import com.example.fragmject.core.model.User
import kotlinx.coroutines.flow.Flow

/**
 * 用户持久化存储 facade。
 *
 * 直接封装 [com.example.fragmject.core.database.AppDatabase] 的 UserDao，提供"获取/设置/删除"操作。
 * ViewModel / Repository 可直接注入本对象访问用户数据，无需经过 WanHelper。
 */
object UserStore {

    suspend fun setUser(user: UserEntity) {
        val dao = AppDatabase.getUserDao()
        dao.clear()
        dao.insert(user)
    }

    suspend fun setUser(user: User) {
        setUser(user.toEntity())
    }

    suspend fun deleteUser(user: UserEntity) {
        AppDatabase.getUserDao().delete(user)
    }

    fun getUser(): Flow<UserEntity?> {
        return AppDatabase.getUserDao().get()
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

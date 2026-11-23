package com.example.fragmject.core.database.local

import android.util.Log
import com.example.fragmject.core.database.crypto.UserFieldCipher
import com.example.fragmject.core.database.dao.UserDao
import com.example.fragmject.core.database.model.UserEntity
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
 *
 * `username` / `nickname` / `icon` 入库前用 [UserFieldCipher] 加密（密钥在 Android Keystore），
 * 出库后解密；加解密放在这一层而不是 `UserMapping`，让映射函数保持纯函数。
 */
@Singleton
class UserLocalDataSourceImpl @Inject constructor(
    private val userDao: UserDao,
) : UserLocalDataSource {

    private val cipher = UserFieldCipher()

    override fun observeCurrentUser(): Flow<User?> =
        userDao.get().map { entity -> entity?.let { decrypt(it) }?.toDomain() }

    override suspend fun saveUser(user: User) {
        userDao.replace(encrypt(user.toEntity()))
    }

    override suspend fun clearUser() {
        userDao.clear()
    }

    private fun encrypt(entity: UserEntity): UserEntity = entity.copy(
        username = cipher.encryptToText(entity.username),
        nickname = cipher.encryptToText(entity.nickname),
        icon = cipher.encryptToText(entity.icon),
    )

    private fun decrypt(entity: UserEntity): UserEntity = entity.copy(
        username = decryptField("username", entity.username),
        nickname = decryptField("nickname", entity.nickname),
        icon = decryptField("icon", entity.icon),
    )

    /**
     * 解密单个字段；「原来有值、解出来为空」才记录，避免正常空字段刷日志。
     *
     * 用 `Log.e` 而不是 `Log.w`：R8 的 `-assumenosideeffects` 会裁掉 v/d/i/w，
     * 而这里的失败通常意味着 Keystore 密钥变化（换机 / 密钥被清除），线上需要看得到。
     */
    private fun decryptField(field: String, stored: String?): String? {
        val value = cipher.decryptOrPlainText(stored)
        if (stored != null && value == null) {
            Log.e(TAG, "decrypt user.$field failed (keystore key changed?), field left empty")
        }
        return value
    }

    private companion object {
        const val TAG = "UserLocalDataSource"
    }
}

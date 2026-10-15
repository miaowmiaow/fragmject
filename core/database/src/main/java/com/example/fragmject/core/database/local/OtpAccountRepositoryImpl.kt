package com.example.fragmject.core.database.local

import com.example.fragmject.core.android.platform.crypto.KeystoreSecretCipher
import com.example.fragmject.core.database.dao.OtpAccountDao
import com.example.fragmject.core.database.model.OtpAccountEntity
import com.example.fragmject.core.domain.otp.OtpAccount
import com.example.fragmject.core.domain.otp.OtpAlgorithm
import com.example.fragmject.core.domain.otp.OtpType
import com.example.fragmject.core.domain.repository.OtpAccountRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Authenticator 账户实现：database **直接实现 domain 端口**（与 History/Theme 一致的
 * 零透传层做法），不再经过 data-repository 中转。
 *
 * 明文密钥只存在于内存：入库前用 Keystore 加密，出库后解密，
 * Room Entity 与数据库文件中都不出现明文。
 */
@Singleton
class OtpAccountRepositoryImpl @Inject constructor(
    private val otpAccountDao: OtpAccountDao,
) : OtpAccountRepository {

    override fun observeAll(): Flow<List<OtpAccount>> =
        otpAccountDao.observeAll()
            .map { entities -> entities.map { it.toDomain() } }
            .flowOn(Dispatchers.IO)

    override suspend fun getById(id: Long): OtpAccount? = withContext(Dispatchers.IO) {
        otpAccountDao.getById(id)?.toDomain()
    }

    override suspend fun add(account: OtpAccount): Long = withContext(Dispatchers.IO) {
        otpAccountDao.insert(
            OtpAccountEntity(
                issuer = account.issuer,
                accountName = account.accountName,
                secretCipher = KeystoreSecretCipher.encrypt(account.secret),
                algorithm = account.algorithm.ordinal,
                digits = account.digits,
                period = account.period,
                type = account.type.ordinal,
                counter = account.counter,
                sortOrder = otpAccountDao.maxSortOrder() + 1,
                createdAt = System.currentTimeMillis(),
            )
        )
    }

    override suspend fun update(account: OtpAccount) = withContext(Dispatchers.IO) {
        // created_at 不属于领域模型，沿用库中原有值
        val existing = otpAccountDao.getById(account.id) ?: return@withContext
        otpAccountDao.update(
            OtpAccountEntity(
                id = account.id,
                issuer = account.issuer,
                accountName = account.accountName,
                secretCipher = KeystoreSecretCipher.encrypt(account.secret),
                algorithm = account.algorithm.ordinal,
                digits = account.digits,
                period = account.period,
                type = account.type.ordinal,
                counter = account.counter,
                sortOrder = account.sortOrder,
                createdAt = existing.createdAt,
            )
        )
    }

    override suspend fun delete(id: Long) = withContext(Dispatchers.IO) {
        otpAccountDao.deleteById(id)
    }

    private fun OtpAccountEntity.toDomain(): OtpAccount = OtpAccount(
        id = id,
        issuer = issuer,
        accountName = accountName,
        secret = KeystoreSecretCipher.decrypt(secretCipher),
        algorithm = OtpAlgorithm.fromOrdinal(algorithm),
        digits = digits,
        period = period,
        type = OtpType.fromOrdinal(type),
        counter = counter,
        sortOrder = sortOrder,
    )
}

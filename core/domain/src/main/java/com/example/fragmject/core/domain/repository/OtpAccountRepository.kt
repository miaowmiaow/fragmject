package com.example.fragmject.core.domain.repository

import com.example.fragmject.core.domain.otp.OtpAccount
import kotlinx.coroutines.flow.Flow

/**
 * Authenticator 账户领域端口。
 *
 * 加解密由实现负责：领域层拿到的 [OtpAccount.secret] 是 Base32 明文，
 * 但持久化形态必须是密文，明文密钥不得落库。
 */
interface OtpAccountRepository {

    /** 按 sort_order 升序观察全部账户。 */
    fun observeAll(): Flow<List<OtpAccount>>

    suspend fun getById(id: Long): OtpAccount?

    /** 新增账户并返回新 id；sort_order 由实现统一追加到末尾，调用方无需关心。 */
    suspend fun add(account: OtpAccount): Long

    suspend fun update(account: OtpAccount)

    suspend fun delete(id: Long)
}

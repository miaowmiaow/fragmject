package com.example.fragmject.core.database.model

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.Index
import androidx.room3.PrimaryKey

/**
 * Authenticator（TOTP）账户持久化模型。
 *
 * [secretCipher] 是**密文**（IV||cipherText，见 `AesGcmCipher`）：
 * 明文密钥只应出现在内存的领域模型中，绝不落库。
 */
@Entity(
    tableName = "otp_account",
    // 列表按 sort_order 排序读取，建索引避免每次全表排序
    indices = [Index(value = ["sort_order"])],
)
data class OtpAccountEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val issuer: String,
    @ColumnInfo(name = "account_name") val accountName: String,
    @ColumnInfo(name = "secret_cipher") val secretCipher: ByteArray,
    /** `OtpAlgorithm` 的 ordinal；顺序即持久化契约，不得调整枚举顺序。 */
    val algorithm: Int,
    val digits: Int,
    val period: Int,
    /** `OtpType` 的 ordinal；同上。 */
    val type: Int,
    val counter: Long,
    @ColumnInfo(name = "sort_order") val sortOrder: Int,
    @ColumnInfo(name = "created_at") val createdAt: Long,
) {
    // secretCipher 为 ByteArray，data class 默认的 equals/hashCode 采用引用相等，
    // 会破坏按内容相等的语义（如集合去重、测试断言），故按内容重写。
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as OtpAccountEntity

        if (id != other.id) return false
        if (issuer != other.issuer) return false
        if (accountName != other.accountName) return false
        if (!secretCipher.contentEquals(other.secretCipher)) return false
        if (algorithm != other.algorithm) return false
        if (digits != other.digits) return false
        if (period != other.period) return false
        if (type != other.type) return false
        if (counter != other.counter) return false
        if (sortOrder != other.sortOrder) return false
        if (createdAt != other.createdAt) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + issuer.hashCode()
        result = 31 * result + accountName.hashCode()
        result = 31 * result + secretCipher.contentHashCode()
        result = 31 * result + algorithm
        result = 31 * result + digits
        result = 31 * result + period
        result = 31 * result + type
        result = 31 * result + counter.hashCode()
        result = 31 * result + sortOrder
        result = 31 * result + createdAt.hashCode()
        return result
    }
}

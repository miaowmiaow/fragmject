package com.example.fragmject.core.android.platform.crypto

import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * AES-256-GCM 对称加解密，用于保护 OTP 密钥这类长期凭据。
 *
 * 刻意把「算法」与「密钥来源」拆开：本类只依赖 JCA，密钥由调用方以 [SecretKey] 传入，
 * 因此**可在纯 JVM 下单测**；安卓 Keystore 的密钥生成见 [KeystoreSecretCipher]。
 *
 * 选 GCM（AEAD）而非 CBC：解密时会校验认证标签，密文被篡改会抛异常而不是解出乱码。
 * 对「密钥被改坏却浑然不知」这类问题，这一点很关键。
 *
 * 输出布局：**IV(12B) || 密文(含 128 位 GCM 标签)**，打包成单个 ByteArray，
 * 便于作为一个 BLOB 列存储，也避免 IV 与密文分开存放时错位。
 */
object AesGcmCipher {

    private const val ALGORITHM = "AES/GCM/NoPadding"
    private const val IV_LENGTH_BYTES = 12
    private const val TAG_LENGTH_BITS = 128

    /**
     * 加密；IV 由实现（provider）随机生成，因此相同明文两次加密结果不同。
     *
     * **刻意不传 `GCMParameterSpec`**：安卓 Keystore 的 `AndroidKeyStore` provider
     * 在加密时拒绝调用方提供的 IV（抛 `InvalidAlgorithmParameterException:
     * Caller-provided IV not permitted`），只能让它自己生成再从 `cipher.getIV()` 取回。
     * 不传参数时 SunJCE 同样会随机生成 12 字节 IV，因此纯 JVM 单测行为一致。
     * 解密方向仍可（也必须）传入 IV，那是允许的。
     */
    fun encrypt(key: SecretKey, plain: ByteArray): ByteArray {
        val cipher = Cipher.getInstance(ALGORITHM)
        cipher.init(Cipher.ENCRYPT_MODE, key)
        val iv = cipher.iv
        check(iv.size == IV_LENGTH_BYTES) { "预期 $IV_LENGTH_BYTES 字节 IV，实际 ${iv.size}" }
        return iv + cipher.doFinal(plain)
    }

    /** 解密；密文被篡改或密钥不匹配时抛出（GCM 标签校验失败）。 */
    fun decrypt(key: SecretKey, packed: ByteArray): ByteArray {
        require(packed.size > IV_LENGTH_BYTES) {
            "密文长度 ${packed.size} 不足以包含 $IV_LENGTH_BYTES 字节 IV"
        }
        val iv = packed.copyOfRange(0, IV_LENGTH_BYTES)
        val cipherText = packed.copyOfRange(IV_LENGTH_BYTES, packed.size)
        val cipher = Cipher.getInstance(ALGORITHM)
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(TAG_LENGTH_BITS, iv))
        return cipher.doFinal(cipherText)
    }
}

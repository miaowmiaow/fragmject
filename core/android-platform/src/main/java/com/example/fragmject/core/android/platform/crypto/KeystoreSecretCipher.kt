package com.example.fragmject.core.android.platform.crypto

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey

/**
 * OTP 密钥的静态加密入口：密钥由 Android Keystore 持有，加解密算法委托 [AesGcmCipher]。
 *
 * 密钥材料不导出 Keystore，因此即便数据库文件被导出，也无法离线还原 OTP 密钥。
 *
 * **不绑定用户认证**（`setUserAuthenticationRequired(false)`）：产品决策为不加应用锁，
 * 设备解锁后即可读取验证码。这与「不加生物识别/应用锁」的决策一致——
 * 保护目标是防止静态数据被导出，而非防止已解锁设备上的访问。
 */
object KeystoreSecretCipher {

    private const val ANDROID_KEY_STORE = "AndroidKeyStore"
    private const val KEY_ALIAS = "fragmject_otp_secret_key"
    private const val KEY_SIZE_BITS = 256

    /** 加密明文字符串（UTF-8），返回 IV||密文 的打包字节。 */
    fun encrypt(plain: String): ByteArray =
        AesGcmCipher.encrypt(getOrCreateKey(), plain.toByteArray(Charsets.UTF_8))

    /** 解密 [encrypt] 的输出；密文被篡改时抛出。 */
    fun decrypt(packed: ByteArray): String =
        String(AesGcmCipher.decrypt(getOrCreateKey(), packed), Charsets.UTF_8)

    private fun getOrCreateKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEY_STORE).apply { load(null) }
        val existing = keyStore.getKey(KEY_ALIAS, null)
        if (existing is SecretKey) return existing

        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEY_STORE)
        generator.init(
            KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
            )
                .setKeySize(KEY_SIZE_BITS)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setUserAuthenticationRequired(false)
                .setRandomizedEncryptionRequired(true)
                .build(),
        )
        return generator.generateKey()
    }
}

package com.example.miaow.base.http

import android.annotation.SuppressLint
import java.io.InputStream
import java.security.KeyStore
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import javax.net.ssl.KeyManager
import javax.net.ssl.KeyManagerFactory
import javax.net.ssl.TrustManagerFactory
import javax.net.ssl.X509TrustManager

object HttpsHelper {

    /**
     * 根据传入证书生成自定义的 X509TrustManager。
     * 当 certificates 为 null 时返回 null，调用方应回落到系统默认实现。
     * 注意：以前的实现会在 certificates 为 null 时返回信任所有证书的 UnSafeTrustManager，
     * 这等同于关闭 HTTPS 校验，存在严重安全隐患，已移除。
     */
    @Throws(Exception::class)
    fun prepareX509TrustManager(certificates: Array<InputStream>?): X509TrustManager? {
        if (certificates == null) {
            return null
        }
        val certificateFactory = CertificateFactory.getInstance("X.509")
        val keyStore = KeyStore.getInstance(KeyStore.getDefaultType())
        keyStore.load(null)
        for ((index, certificate) in certificates.withIndex()) {
            val certificateAlias = (index).toString()
            keyStore.setCertificateEntry(
                certificateAlias,
                certificateFactory.generateCertificate(certificate)
            )
            certificate.close()
        }
        val trustManagerFactory =
            TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())
        trustManagerFactory.init(keyStore)
        val trustManagers = trustManagerFactory.trustManagers
        return trustManagers
            ?.firstOrNull { it is X509TrustManager }
            ?.let { it as X509TrustManager }
    }

    @Throws(Exception::class)
    fun prepareKeyManager(bksFile: InputStream?, password: String?): Array<KeyManager>? {
        if (bksFile == null || password == null) return null
        val clientKeyStore = KeyStore.getInstance("PKCS12")
        clientKeyStore.load(bksFile, password.toCharArray())
        val keyManagerFactory =
            KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm())
        keyManagerFactory.init(clientKeyStore, password.toCharArray())
        return keyManagerFactory.keyManagers
    }

    /**
     * 信任所有证书的 TrustManager，仅用于显式声明的调试场景，禁止在生产环境使用。
     */
    @SuppressLint("TrustAllX509TrustManager", "CustomX509TrustManager")
    class UnSafeTrustManager : X509TrustManager {
        override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) {}
        override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) {}
        override fun getAcceptedIssuers(): Array<X509Certificate> {
            return arrayOf()
        }
    }
}
package com.example.fragment.library.base.http

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

    @Throws(Exception::class)
    fun prepareX509TrustManager(certificates: Array<InputStream>?): X509TrustManager {
        if (certificates == null) {
            return UnSafeTrustManager()
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
        return if (!trustManagers.isNullOrEmpty() && trustManagers[0] is X509TrustManager) {
            trustManagers[0] as X509TrustManager
        } else {
            UnSafeTrustManager()
        }
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

    @SuppressLint("TrustAllX509TrustManager")
    class UnSafeTrustManager : X509TrustManager {
        override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) {}
        override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) {}
        override fun getAcceptedIssuers(): Array<X509Certificate> {
            return arrayOf()
        }
    }
}
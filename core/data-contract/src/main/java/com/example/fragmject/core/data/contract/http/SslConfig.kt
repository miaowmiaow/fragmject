package com.example.fragmject.core.data.contract.http

import java.io.InputStream

/**
 * 自定义 SSL 证书配置（供后续快速接入双向 / 单向 TLS）。
 *
 * 默认由 DI 提供 null（使用系统默认校验）。需要时由 app 组合根提供具体实现。
 *
 * 注意：[clientCertificate] / [serverCertificates] 为 [InputStream]，其关闭责任归属提供方；
 * 由于 client 在注入时构建一次，流在构建完成后即不再被持有。
 */
data class SslConfig(
    val clientCertificate: InputStream? = null,
    val clientCertificatePwd: String? = null,
    val serverCertificates: Array<InputStream>? = null,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SslConfig

        if (clientCertificate != other.clientCertificate) return false
        if (clientCertificatePwd != other.clientCertificatePwd) return false
        if (!serverCertificates.contentEquals(other.serverCertificates)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = clientCertificate.hashCode()
        result = 31 * result + clientCertificatePwd.hashCode()
        result = 31 * result + (serverCertificates?.contentHashCode() ?: 0)
        return result
    }
}

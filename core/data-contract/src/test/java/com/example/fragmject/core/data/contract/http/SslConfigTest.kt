package com.example.fragmject.core.data.contract.http

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream

class SslConfigTest {

    @Test
    fun `默认配置彼此相等且哈希一致`() {
        val a = SslConfig()
        val b = SslConfig()

        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
    }

    @Test
    fun `证书流按引用比较，不同实例即使内容相同也不相等`() {
        // InputStream 没有值语义；这里刻意保持引用比较，避免两份流的内容被误认为同一证书配置
        val stream = ByteArrayInputStream(byteArrayOf(1, 2, 3))
        val same = SslConfig(clientCertificate = stream)

        assertEquals(SslConfig(clientCertificate = stream), same)
        assertNotEquals(SslConfig(clientCertificate = ByteArrayInputStream(byteArrayOf(1, 2, 3))), same)
    }

    @Test
    fun `密码不同则不相等`() {
        assertNotEquals(SslConfig(clientCertificatePwd = "a"), SslConfig(clientCertificatePwd = "b"))
        assertEquals(SslConfig(clientCertificatePwd = "a"), SslConfig(clientCertificatePwd = "a"))
    }

    @Test
    fun `服务端证书数组按内容比较`() {
        val s1 = ByteArrayInputStream(byteArrayOf(9))
        val s2 = ByteArrayInputStream(byteArrayOf(9))
        val configA = SslConfig(serverCertificates = arrayOf(s1))
        val configB = SslConfig(serverCertificates = arrayOf(s1))

        assertEquals(configA, configB)
        assertEquals(configA.hashCode(), configB.hashCode())
        assertNotEquals(configA, SslConfig(serverCertificates = arrayOf(s2)))
    }

    @Test
    fun `服务端证书为空与非空不相等，且空值哈希不抛异常`() {
        val withCerts = SslConfig(serverCertificates = arrayOf(ByteArrayInputStream(byteArrayOf(1))))

        assertFalse(SslConfig() == withCerts)
        assertFalse(withCerts == SslConfig())
        // serverCertificates 为 null 时哈希走 ?: 0 分支，不应抛 NPE
        assertEquals(SslConfig().hashCode(), SslConfig(serverCertificates = null).hashCode())
    }

    @Test
    fun `与其他类型比较时返回 false，且自身相等`() {
        val config = SslConfig()

        assertTrue(config == config)
        assertFalse(config.equals("not a config"))
        assertFalse(config.equals(null))
    }
}

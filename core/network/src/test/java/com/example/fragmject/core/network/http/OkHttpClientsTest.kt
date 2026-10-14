package com.example.fragmject.core.network.http

import com.example.fragmject.core.domain.session.CookieStore
import okhttp3.Cache
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class OkHttpClientsTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private val cookieJar = CookieJar(object : CookieStore {
        override fun getCookie(url: String): String? = null
        override fun setCookie(url: String, value: String) = Unit
        override fun flush() = Unit
        override suspend fun clearSession() = Unit
    })

    @Test
    fun `no cache client does not create disk cache`() {
        var cacheCreations = 0
        val clients = OkHttpClients(
            cacheFactory = {
                cacheCreations++
                Cache(temporaryFolder.newFolder("http"), 1024)
            },
            cookieJar = cookieJar,
        )

        val client = clients.noCache()

        assertNull(client.cache)
        assertSame(client, clients.noCache())
        assertEquals(0, cacheCreations)
    }

    @Test
    fun `clients share resources and create only one cache`() {
        var cacheCreations = 0
        val clients = OkHttpClients(
            cacheFactory = {
                cacheCreations++
                Cache(temporaryFolder.newFolder("http"), 1024)
            },
            cookieJar = cookieJar,
        )

        val media = clients.noCache()
        val http = clients.http()
        try {
            assertSame(http, clients.http())
            assertSame(http.connectionPool, media.connectionPool)
            assertSame(http.dispatcher, media.dispatcher)
            assertSame(http.cookieJar, media.cookieJar)
            assertEquals(1, cacheCreations)
        } finally {
            http.cache?.close()
        }
    }
}

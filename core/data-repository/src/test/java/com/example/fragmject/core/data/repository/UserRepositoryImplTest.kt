package com.example.fragmject.core.data.repository

import com.example.fragmject.core.model.User
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * [UserRepositoryImpl]：只剩「当前用户存储」三件事。
 *
 * 登录/登出的会话编排测试已迁至 `core:domain` 的 `AuthSessionCoordinatorTest`，
 * transport 的响应翻译见 [AuthGatewayImplTest]。
 */
class UserRepositoryImplTest {

    @Test
    fun `saveUser delegates to local storage`() = runBlocking {
        val local = FakeUserLocalDataSource()
        val repo = UserRepositoryImpl(local)

        repo.saveUser(User(id = 1, username = "alice"))

        assertEquals("alice", local.current()?.username)
        assertEquals("alice", repo.observeCurrentUser().first()?.username)
    }

    @Test
    fun `clearUser delegates to local storage`() = runBlocking {
        val local = FakeUserLocalDataSource().apply { seed(User(id = 1, username = "alice")) }
        val repo = UserRepositoryImpl(local)

        repo.clearUser()

        assertNull(local.current())
        assertEquals(1, local.clearCalls)
    }

    @Test
    fun `observeCurrentUser reflects the stored user`() = runBlocking {
        val local = FakeUserLocalDataSource()

        assertNull(UserRepositoryImpl(local).observeCurrentUser().first())

        local.seed(User(id = 2, username = "bob"))

        assertEquals("bob", UserRepositoryImpl(local).observeCurrentUser().first()?.username)
    }
}

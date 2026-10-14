package com.example.fragmject.core.data.repository

import com.example.fragmject.core.android.platform.app.AppCoroutineScope
import com.example.fragmject.core.domain.AuthStatus
import com.example.fragmject.core.domain.repository.UserRepository
import com.example.fragmject.core.domain.result.AuthResult
import com.example.fragmject.core.model.User
import java.io.IOException
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Test

class AuthStateImplTest {

    private class FakeUserRepository(private val users: Flow<User?>) : UserRepository {
        override suspend fun login(username: String, password: String): AuthResult = error("unused")
        override suspend fun register(username: String, password: String, repassword: String): AuthResult =
            error("unused")

        override suspend fun logout(): Boolean = error("unused")
        override suspend fun saveUser(user: User) = error("unused")
        override fun observeCurrentUser(): Flow<User?> = users
    }

    private fun <T> withState(users: Flow<User?>, block: suspend (AuthStateImpl) -> T): T {
        val scope = AppCoroutineScope()
        try {
            return runBlocking { block(AuthStateImpl(FakeUserRepository(users), scope)) }
        } finally {
            scope.cancel()
        }
    }

    private suspend fun AuthStateImpl.resolved(): AuthStatus =
        withTimeout(5_000) { status.first { it != AuthStatus.UNKNOWN } }

    @Test
    fun `status stays unknown until the local user is read`() {
        val neverEmits = flow<User?> { awaitCancellation() }
        withState(neverEmits) { auth ->
            assertEquals(AuthStatus.UNKNOWN, auth.status.value)
        }
    }

    @Test
    fun `no user means logged out`() {
        withState(flowOf(null)) { auth -> assertEquals(AuthStatus.LOGGED_OUT, auth.resolved()) }
    }

    @Test
    fun `valid user means logged in`() {
        withState(flowOf(User(id = 1, username = "test"))) { auth ->
            assertEquals(AuthStatus.LOGGED_IN, auth.resolved())
        }
    }

    @Test
    fun `user without positive id means logged out`() {
        withState(flowOf(User(id = 0, username = "ghost"))) { auth ->
            assertEquals(AuthStatus.LOGGED_OUT, auth.resolved())
        }
    }

    @Test
    fun `read failure resolves to logged out instead of staying unknown`() {
        withState(flow<User?> { throw IOException("database unavailable") }) { auth ->
            assertEquals(AuthStatus.LOGGED_OUT, auth.resolved())
        }
    }

    @Test
    fun `status follows later user changes`() {
        val users = MutableSharedFlow<User?>(replay = 1)
        withState(users) { auth ->
            users.emit(User(id = 7, username = "a"))
            assertEquals(AuthStatus.LOGGED_IN, auth.resolved())
            users.emit(null)
            assertEquals(AuthStatus.LOGGED_OUT, withTimeout(5_000) { auth.status.first { it == AuthStatus.LOGGED_OUT } })
        }
    }
}

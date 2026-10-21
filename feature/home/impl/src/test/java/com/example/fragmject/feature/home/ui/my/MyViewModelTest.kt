package com.example.fragmject.feature.home.ui.my

import com.example.fragmject.core.domain.repository.UserRepository
import com.example.fragmject.core.domain.result.AuthResult
import com.example.fragmject.core.model.User
import com.example.fragmject.core.testing.MainDispatcherRule
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/**
 * 我的页用户态：未登录或未读到本地用户时保持空用户，避免 UI 展示上一账号残留。
 */
class MyViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `init - keeps the empty user until room emits`() = runTest {
        val repo = FakeUserRepository(flow { awaitCancellation() })

        val viewModel = MyViewModel(repo)

        assertEquals(0L, viewModel.uiState.value.user.id)
        assertEquals("", viewModel.uiState.value.user.username)
        assertEquals("", viewModel.uiState.value.user.nickname)
    }

    @Test
    fun `emitted user replaces the empty user`() = runTest {
        val users = MutableStateFlow<User?>(User(id = 7, username = "alice", nickname = "Alice"))
        val viewModel = MyViewModel(FakeUserRepository(users))

        assertEquals(7L, viewModel.uiState.value.user.id)
        assertEquals("alice", viewModel.uiState.value.user.username)
        assertEquals("Alice", viewModel.uiState.value.user.nickname)
    }

    @Test
    fun `logout emission falls back to the empty user`() = runTest {
        val users = MutableStateFlow<User?>(User(id = 7, username = "alice"))
        val viewModel = MyViewModel(FakeUserRepository(users))

        users.value = null

        assertEquals(0L, viewModel.uiState.value.user.id)
        assertEquals("", viewModel.uiState.value.user.username)
    }
}

private class FakeUserRepository(private val users: Flow<User?>) : UserRepository {
    override suspend fun login(username: String, password: String): AuthResult = AuthResult.Error("未实现")
    override suspend fun register(username: String, password: String, repassword: String): AuthResult =
        AuthResult.Error("未实现")

    override suspend fun logout(): Boolean = false
    override suspend fun saveUser(user: User) = Unit
    override fun observeCurrentUser(): Flow<User?> = users
}

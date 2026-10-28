package com.example.fragmject.core.testing

import com.example.fragmject.core.domain.auth.AuthGateway
import com.example.fragmject.core.domain.repository.UserRepository
import com.example.fragmject.core.domain.result.AuthExchange
import com.example.fragmject.core.domain.session.CookieStore
import com.example.fragmject.core.model.User
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * 认证相关的共享测试替身：**全项目唯一一份**。
 *
 * 会话编排上移到 domain 后，各 feature 的 ViewModel 测试与 `core:domain` 的用例测试
 * 都需要用 `AuthGateway + AuthSessionCoordinator + UserRepository` 组装用例。此前
 * `core:domain/src/test` 里另有一份同名实现（且能力更强：多了读取/落库失败注入与
 * `ensureActive()`），导致同一端口有两套替身并行维护——改一次端口要改两处，而且
 * 用弱版本写的测试覆盖不到「读取失败」「落库失败」这些分支。现合并为这一份：
 * 保留两边全部能力，默认行为不变。
 *
 * 本模块只被各模块 `testImplementation` 消费，不进生产包（含 `core:domain` 自身的测试）。
 */
class FakeAuthGateway(
    var loginResult: AuthExchange = AuthExchange.Success(User(id = 1, username = "alice"), "登录成功"),
    var registerResult: AuthExchange = AuthExchange.Success(User(id = 2, username = "bob"), "注册成功"),
) : AuthGateway {

    /**
     * 需要动态行为（按入参返回不同结果、抛异常、模拟取消）时设置，
     * 优先级高于 [loginResult] / [registerResult]。
     */
    var loginHandler: (suspend () -> AuthExchange)? = null
    var registerHandler: (suspend () -> AuthExchange)? = null

    val loginCalls = mutableListOf<Pair<String, String>>()
    val registerCalls = mutableListOf<Triple<String, String, String>>()
    var logoutCalls = 0
    var logoutFailure: Exception? = null

    /** 需要自定义登出行为（抛取消、计数）时使用，优先级高于 [logoutFailure]。 */
    var logoutBehavior: (suspend () -> Unit)? = null

    /** 最近一次调用的参数快照：校验参数比比对整个调用列表更直观。 */
    var lastArguments: List<String> = emptyList()

    /** 调用次数（[loginCalls] 是参数列表，需要次数时用这两个属性）。 */
    val loginCallCount: Int get() = loginCalls.size
    val registerCallCount: Int get() = registerCalls.size

    override suspend fun login(username: String, password: String): AuthExchange {
        loginCalls += username to password
        lastArguments = listOf(username, password)
        return loginHandler?.invoke() ?: loginResult
    }

    override suspend fun register(
        username: String,
        password: String,
        repassword: String,
    ): AuthExchange {
        registerCalls += Triple(username, password, repassword)
        lastArguments = listOf(username, password, repassword)
        return registerHandler?.invoke() ?: registerResult
    }

    override suspend fun logout() {
        logoutCalls++
        val behavior = logoutBehavior
        if (behavior != null) {
            behavior()
            return
        }
        logoutFailure?.let { throw it }
    }
}

class FakeCookieStore(
    var session: String? = "session=active",
) : CookieStore {

    var clearCalls = 0
    var flushCalls = 0
    var clearFailure: Exception? = null

    /** 读取失败注入：`AuthSessionCoordinator` 对「读 Cookie 失败」有专门兜底，必须能覆盖。 */
    var readFailure: Exception? = null

    override fun getCookie(url: String): String? {
        readFailure?.let { throw it }
        return session
    }

    override fun setCookie(url: String, value: String) {
        session = value
    }

    override fun flush() {
        flushCalls++
    }

    override suspend fun clearSession() {
        // 取消必须先传播：清理动作本身也要能被取消中断，否则测试里取消信号会被吞掉
        currentCoroutineContext().ensureActive()
        clearCalls++
        clearFailure?.let { throw it }
        session = null
        flush()
    }
}

class FakeUserRepository(
    private val users: Flow<User?> = MutableStateFlow(null),
) : UserRepository {

    val savedUsers = mutableListOf<User>()
    var clearCalls = 0
    var saveFailure: Exception? = null
    var clearFailure: Exception? = null

    /**
     * 传入可变流（默认）时的直读/直写入口；传入 `flow { awaitCancellation() }`
     * 这类冷流时为 `null`，此时 [seed] / [current] 不生效（调用方只断言 observe 行为）。
     */
    private val sink = users as? MutableStateFlow<User?>

    fun current(): User? = sink?.value

    fun seed(user: User?) {
        sink?.value = user
    }

    override fun observeCurrentUser(): Flow<User?> = users

    override suspend fun saveUser(user: User) {
        saveFailure?.let { throw it }
        savedUsers += user
        seed(user)
    }

    override suspend fun clearUser() {
        currentCoroutineContext().ensureActive()
        clearCalls++
        clearFailure?.let { throw it }
        seed(null)
    }
}

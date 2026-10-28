package com.example.fragmject.core.ui.utils

import com.example.fragmject.core.domain.result.DomainResult
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 「observe(本地) + refresh(网络)」编排的状态测试。
 *
 * 锁定两条回归：
 * 1. 首帧（本地还是空、refresh 未结束）必须停在 Loading，不能变成 `Success(空)`——
 *    否则 NavScreen / ProjectScreen 会先闪一帧空态；
 * 2. refresh 结束但 observe 没有重发（接口确实返回空）时，必须收敛到 `Success(空)`，
 *    不能永远停在 Loading。
 *
 * 作用域说明：用测试调度器构造独立子 Job 的作用域，用例结束前 cancel——
 * observe 是永不结束的流，挂到测试作用域上会让 runTest 收不到结束信号。
 */
@OptIn(ExperimentalCoroutinesApi::class)
class RefreshableUiStateTest {

    @Test
    fun `empty local cache before refresh stays loading`() = runTest {
        val local = MutableStateFlow(emptyList<String>())
        val gate = CompletableDeferred<DomainResult<Unit>>()
        val state = MutableStateFlow<RefreshableUiState<List<String>>>(RefreshableUiState.Loading)
        val scope = CoroutineScope(coroutineContext + Job())

        try {
            scope.collectAndRefresh(
                observe = local,
                refresh = { gate.await() },
                state = state,
                isEmpty = { it.isEmpty() },
            )
            runCurrent()

            assertTrue("首帧空数据不应变成 Success(空)", state.value is RefreshableUiState.Loading)

            gate.complete(DomainResult.Success(Unit))
            advanceUntilIdle()
            // refresh 结束后本地仍为空：接口确实没有数据，此时才是真正的空态
            assertEquals(RefreshableUiState.Success(emptyList<String>()), state.value)
        } finally {
            scope.cancel()
        }
    }

    /**
     * 冷启动主链路：本地空 → refresh 写库 → 内容。
     *
     * 这里断言**整个状态序列**，而不只是终态：只断言终态会让「Loading → Success(空) → Success(数据)」
     * 这种闪空通过（空态在中间出现一帧，最终值仍然正确）。
     *
     * observe 用「冷流查询」建模 Room 的真实时序：首帧先发射当前库内容，写库之后要等
     * invalidation 才重查发射（异步）。因此 refresh 返回时最后观测到的值**必然是旧的空列表**——
     * 谁复用「最后一次观测值」补发，谁就会先发一帧 `Success(空)` 并被下面的序列断言抓住；
     * 只有重新查一次库（`observe.first()`）才能拿到刚写进去的数据。
     */
    @Test
    fun `empty local cache turns into content without empty flash`() = runTest {
        val db = MutableStateFlow(emptyList<String>())
        val invalidation = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
        val observe: Flow<List<String>> = flow {
            emit(db.value)
            invalidation.collect { emit(db.value) } // 模拟 invalidation → 重查 → 发射（异步）
        }
        val gate = CompletableDeferred<Unit>()
        val state = MutableStateFlow<RefreshableUiState<List<String>>>(RefreshableUiState.Loading)
        val scope = CoroutineScope(coroutineContext + Job())
        val seen = mutableListOf<RefreshableUiState<List<String>>>()

        try {
            scope.launch { state.collect { seen += it } }
            scope.collectAndRefresh(
                observe = observe,
                refresh = {
                    gate.await()
                    db.value = listOf("a") // 事务已提交，但 invalidation 尚未被 observe 消费
                    invalidation.tryEmit(Unit)
                    DomainResult.Success(Unit)
                },
                state = state,
                isEmpty = { it.isEmpty() },
            )
            runCurrent()
            assertTrue("首帧应停在骨架屏", state.value is RefreshableUiState.Loading)

            gate.complete(Unit)
            advanceUntilIdle()

            assertEquals(RefreshableUiState.Success(listOf("a")), state.value)
            assertTrue(
                "状态序列不应出现空态（会闪一帧无数据）：$seen",
                seen.none { it is RefreshableUiState.Success && it.data.isEmpty() },
            )
        } finally {
            scope.cancel()
        }
    }

    @Test
    fun `local data is published immediately`() = runTest {
        val state = MutableStateFlow<RefreshableUiState<List<String>>>(RefreshableUiState.Loading)
        val scope = CoroutineScope(coroutineContext + Job())

        try {
            scope.collectAndRefresh(
                observe = flowOf(listOf("a")),
                refresh = { DomainResult.Success(Unit) },
                state = state,
                isEmpty = { it.isEmpty() },
            )
            advanceUntilIdle()
            assertEquals(RefreshableUiState.Success(listOf("a")), state.value)
        } finally {
            scope.cancel()
        }
    }

    @Test
    fun `refresh failure becomes error`() = runTest {
        val state = MutableStateFlow<RefreshableUiState<List<String>>>(RefreshableUiState.Loading)
        val scope = CoroutineScope(coroutineContext + Job())

        try {
            scope.collectAndRefresh(
                observe = flowOf(emptyList()),
                refresh = { DomainResult.Failure(code = "test", message = "boom") },
                state = state,
                isEmpty = { it.isEmpty() },
            )
            advanceUntilIdle()
            assertEquals(RefreshableUiState.Error("boom"), state.value)
        } finally {
            scope.cancel()
        }
    }

    @Test
    fun `retry success converges even when observe does not re-emit`() = runTest {
        // Room 的 Flow 对相同结果不重发：重试成功后必须主动收敛，不能卡在骨架屏
        val state = MutableStateFlow<RefreshableUiState<List<String>>>(RefreshableUiState.Loading)
        val scope = CoroutineScope(coroutineContext + Job())

        try {
            scope.retryRefresh(
                observe = flowOf(listOf("a")),
                refresh = { DomainResult.Success(Unit) },
                state = state,
            )
            advanceUntilIdle()
            assertEquals(RefreshableUiState.Success(listOf("a")), state.value)
        } finally {
            scope.cancel()
        }
    }

    @Test
    fun `retry failure becomes error`() = runTest {
        val state = MutableStateFlow<RefreshableUiState<List<String>>>(RefreshableUiState.Loading)
        val scope = CoroutineScope(coroutineContext + Job())

        try {
            scope.retryRefresh(
                observe = flowOf(listOf("a")),
                refresh = { DomainResult.Failure(code = "test", message = "boom") },
                state = state,
            )
            advanceUntilIdle()
            assertEquals(RefreshableUiState.Error("boom"), state.value)
        } finally {
            scope.cancel()
        }
    }
}

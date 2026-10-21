package com.example.fragmject.core.data.repository

import androidx.paging.PagingData
import com.example.fragmject.core.android.platform.app.AppCoroutineScope
import com.example.fragmject.core.domain.repository.MyRepository
import com.example.fragmject.core.domain.result.SimpleResult
import com.example.fragmject.core.domain.usecase.CollectArticleUseCase
import com.example.fragmject.core.model.Article
import com.example.fragmject.core.model.Coin
import com.example.fragmject.core.model.MyCoin
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 收藏态的乐观更新语义：先翻转覆盖层，成功保留，失败回滚并广播服务端消息。
 */
class CollectStateImplTest {

    private class FakeMyRepository : MyRepository {
        var action: suspend (id: String, collect: Boolean) -> SimpleResult = { _, _ -> SimpleResult(true, "ok") }

        override suspend fun getUserCoin(): Coin? = error("unused")
        override fun getMyCoinPagingData(): Flow<PagingData<MyCoin>> = error("unused")
        override fun getMySharePagingData(): Flow<PagingData<Article>> = error("unused")
        override suspend fun shareArticle(title: String, link: String): SimpleResult = error("unused")
        override suspend fun collectArticle(id: String): SimpleResult = action(id, true)
        override suspend fun uncollectArticle(id: String): SimpleResult = action(id, false)
    }

    private fun <T> withState(repo: MyRepository, block: suspend CoroutineScope.(CollectStateImpl) -> T): T {
        val scope = AppCoroutineScope()
        try {
            return runBlocking { block(CollectStateImpl(CollectArticleUseCase(repo), scope)) }
        } finally {
            scope.cancel()
        }
    }

    @Test
    fun `toggle flips the override synchronously before the write completes`() {
        val repo = FakeMyRepository().apply { action = { _, _ -> awaitCancellation() } }

        withState(repo) { state ->
            state.toggle("a1", true)
            assertEquals(mapOf("a1" to true), state.overrides.value)

            state.toggle("a1", false)
            assertEquals(mapOf("a1" to false), state.overrides.value)
        }
    }

    @Test
    fun `failed collect rolls back the override and broadcasts the server message`() {
        val repo = FakeMyRepository().apply { action = { _, _ -> SimpleResult(false, "请先登录") } }

        withState(repo) { state ->
            val events = Channel<String>(Channel.UNLIMITED)
            val collector = launch(start = CoroutineStart.UNDISPATCHED) {
                state.collectFailed.collect { events.send(it) }
            }

            state.toggle("a1", true)
            assertEquals("请先登录", withTimeout(5_000) { events.receive() })
            assertNull(state.overrides.value["a1"])

            collector.cancel()
        }
    }

    @Test
    fun `failed write falls back to a readable message when the server is silent`() {
        val calls = mutableListOf<Pair<String, Boolean>>()
        val repo = FakeMyRepository().apply {
            action = { id, collect ->
                calls += id to collect
                SimpleResult(false, "")
            }
        }

        withState(repo) { state ->
            val events = Channel<String>(Channel.UNLIMITED)
            val collector = launch(start = CoroutineStart.UNDISPATCHED) {
                state.collectFailed.collect { events.send(it) }
            }

            state.toggle("a2", false)
            assertEquals("操作失败", withTimeout(5_000) { events.receive() })
            // 取消收藏必须走 uncollectArticle，不能误调收藏接口
            assertEquals(listOf("a2" to false), calls)
            assertTrue(state.overrides.value.isEmpty())

            collector.cancel()
        }
    }

    @Test
    fun `a newer toggle on the same article supersedes the in-flight one`() {
        val repo = FakeMyRepository().apply {
            action = { _, collect ->
                if (collect) awaitCancellation() else SimpleResult(true, "ok")
            }
        }

        withState(repo) { state ->
            val events = Channel<String>(Channel.UNLIMITED)
            val collector = launch(start = CoroutineStart.UNDISPATCHED) {
                state.collectFailed.collect { events.send(it) }
            }

            state.toggle("a3", true)
            state.toggle("a3", false)

            // 旧请求被取消不应回滚新意图，也不应广播失败
            delay(100)
            assertEquals(mapOf("a3" to false), state.overrides.value)
            assertTrue(events.tryReceive().isFailure)

            collector.cancel()
        }
    }
}

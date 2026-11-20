package com.example.fragmject.core.data.repository

import androidx.paging.PagingData
import com.example.fragmject.core.android.platform.app.AppCoroutineScope
import com.example.fragmject.core.domain.repository.MyRepository
import com.example.fragmject.core.domain.result.SimpleResult
import com.example.fragmject.core.domain.usecase.CollectArticleUseCase
import com.example.fragmject.core.model.Article
import com.example.fragmject.core.model.Coin
import com.example.fragmject.core.model.MyCoin
import java.util.Collections
import kotlinx.coroutines.CompletableDeferred
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
    fun `同 id 的请求串行发送：前一个未返回时不会并发发出新请求`() = runBlocking {
        val calls = Collections.synchronizedList(mutableListOf<Pair<String, Boolean>>())
        val firstCall = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()
        val repo = FakeMyRepository().apply {
            action = { id, collect ->
                calls += id to collect
                if (calls.size == 1) {
                    firstCall.complete(Unit)
                    // 模拟「请求已到达服务端，但客户端协程被取消也不会让服务端停下」
                    release.await()
                }
                SimpleResult(true, "ok")
            }
        }

        withState(repo) { state ->
            state.toggle("a3", true)
            withTimeout(5_000) { firstCall.await() }

            // 第一个请求仍在途中：此时连点两次，不能像旧实现那样「取消旧请求 + 立即发新请求」
            state.toggle("a3", false)
            state.toggle("a3", true)
            delay(200)
            assertEquals("在途请求返回前不允许并发发请求", 1, calls.size)

            release.complete(Unit)
            // 放开后按序补发一次，且携带的是最后一个意图（true，与在途请求一致 → 合并，不再发）
            delay(300)
            assertEquals(listOf("a3" to true), calls.toList())
            assertEquals(mapOf("a3" to true), state.overrides.value)
        }
    }

    @Test
    fun `在途请求返回后补发最新意图，服务端顺序与 UI 一致`() = runBlocking {
        val calls = Collections.synchronizedList(mutableListOf<Pair<String, Boolean>>())
        val firstCall = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()
        val repo = FakeMyRepository().apply {
            action = { id, collect ->
                calls += id to collect
                if (calls.size == 1) {
                    firstCall.complete(Unit)
                    release.await()
                }
                SimpleResult(true, "ok")
            }
        }

        withState(repo) { state ->
            state.toggle("a5", true)
            withTimeout(5_000) { firstCall.await() }
            state.toggle("a5", false)
            delay(200)
            assertEquals("第二次点按必须排队，不能并发发出", 1, calls.size)

            release.complete(Unit)
            withTimeout(5_000) {
                while (calls.size < 2) delay(10)
            }
            assertEquals(listOf("a5" to true, "a5" to false), calls.toList())
            assertEquals(mapOf("a5" to false), state.overrides.value)
        }
    }

    @Test
    fun `快速来回连点被合并为一次请求`() = runBlocking {
        val calls = Collections.synchronizedList(mutableListOf<Pair<String, Boolean>>())
        val firstCall = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()
        val repo = FakeMyRepository().apply {
            action = { id, collect ->
                calls += id to collect
                if (calls.size == 1) {
                    firstCall.complete(Unit)
                    release.await()
                }
                SimpleResult(true, "ok")
            }
        }

        withState(repo) { state ->
            state.toggle("a6", true)
            withTimeout(5_000) { firstCall.await() }
            // 来回点三次，最终意图与在途请求相同（true）
            state.toggle("a6", false)
            state.toggle("a6", true)
            state.toggle("a6", false)
            state.toggle("a6", true)

            release.complete(Unit)
            delay(300)
            assertEquals("意图回落到在途请求的目标，无需补发", listOf("a6" to true), calls.toList())
            assertEquals(mapOf("a6" to true), state.overrides.value)
        }
    }

    @Test
    fun `在途请求失败时保留并补发期间产生的最新意图`() = runBlocking {
        val calls = Collections.synchronizedList(mutableListOf<Pair<String, Boolean>>())
        val firstCall = CompletableDeferred<Unit>()
        val releaseFirst = CompletableDeferred<Unit>()
        val latestCall = CompletableDeferred<Unit>()
        val repo = FakeMyRepository().apply {
            action = { id, collect ->
                calls += id to collect
                if (calls.size == 1) {
                    firstCall.complete(Unit)
                    releaseFirst.await()
                    SimpleResult(false, "旧意图失败")
                } else {
                    latestCall.complete(Unit)
                    SimpleResult(true, "ok")
                }
            }
        }

        withState(repo) { state ->
            state.toggle("a8", true)
            withTimeout(5_000) { firstCall.await() }
            state.toggle("a8", false)

            releaseFirst.complete(Unit)
            withTimeout(5_000) { latestCall.await() }
            withTimeout(5_000) {
                while (state.overrides.value["a8"] != false) delay(10)
            }

            assertEquals(listOf("a8" to true, "a8" to false), calls.toList())
            assertEquals(mapOf("a8" to false), state.overrides.value)
        }
    }

    @Test
    fun `同值 ABA 意图不会被旧失败请求清除`() = runBlocking {
        val calls = Collections.synchronizedList(mutableListOf<Pair<String, Boolean>>())
        val firstCall = CompletableDeferred<Unit>()
        val releaseFirst = CompletableDeferred<Unit>()
        val retryCall = CompletableDeferred<Unit>()
        val repo = FakeMyRepository().apply {
            action = { id, collect ->
                calls += id to collect
                if (calls.size == 1) {
                    firstCall.complete(Unit)
                    releaseFirst.await()
                    SimpleResult(false, "旧请求失败")
                } else {
                    retryCall.complete(Unit)
                    SimpleResult(true, "ok")
                }
            }
        }

        withState(repo) { state ->
            state.toggle("a9", true)
            withTimeout(5_000) { firstCall.await() }
            state.toggle("a9", false)
            state.toggle("a9", true)

            releaseFirst.complete(Unit)
            withTimeout(5_000) { retryCall.await() }
            withTimeout(5_000) {
                while (state.overrides.value["a9"] != true || calls.size < 2) delay(10)
            }

            assertEquals(listOf("a9" to true, "a9" to true), calls.toList())
            assertEquals(mapOf("a9" to true), state.overrides.value)
        }
    }

    @Test
    fun `写失败回滚到最近一次成功状态，而不是列表快照`() = runBlocking {
        val calls = Collections.synchronizedList(mutableListOf<Pair<String, Boolean>>())
        val repo = FakeMyRepository().apply {
            action = { id, collect ->
                calls += id to collect
                if (collect) SimpleResult(true, "ok") else SimpleResult(false, "取消收藏失败")
            }
        }

        withState(repo) { state ->
            state.toggle("a7", true)
            withTimeout(5_000) {
                while (state.overrides.value["a7"] != true || calls.size < 1) delay(10)
            }
            delay(100)

            state.toggle("a7", false)
            withTimeout(5_000) {
                // 失败后覆盖层回到上一次成功状态（已收藏），不会显示成未收藏
                while (state.overrides.value["a7"] != true || calls.size < 2) delay(10)
            }
            assertEquals(listOf("a7" to true, "a7" to false), calls.toList())
            assertEquals(mapOf("a7" to true), state.overrides.value)
        }
    }
}

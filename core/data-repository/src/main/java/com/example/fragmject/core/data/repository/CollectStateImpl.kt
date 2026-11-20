package com.example.fragmject.core.data.repository

import com.example.fragmject.core.android.platform.app.AppCoroutineScope
import com.example.fragmject.core.domain.CollectState
import com.example.fragmject.core.domain.usecase.CollectArticleUseCase
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 收藏态 data 实现：进程级单例，跨页面共享乐观覆盖与失败通知。
 *
 * 下沉到 core:data-repository（领域端口装配层）；写操作挂在 [AppCoroutineScope]，
 * 不随列表 item 离屏取消，失败回滚并经由 [collectFailed] 通知 UI。
 *
 * 失败通道用 SharedFlow：订阅者有多个（首页/体系/项目/搜索/用户主页/收藏列表），
 * Channel 的单消费者语义会让它们互相争抢事件，导致提示错位或丢失。
 *
 * ============================ 并发模型：串行 + 合并 ============================
 * 同一篇文章**同一时刻最多只有一个写请求在途**，且最终落到服务端的一定是最后一次点按的意图。
 *
 * 早期实现是「取消旧协程 + 立即发新请求」，那是有缺陷的：客户端取消只终止本地协程，
 * 已经到达服务端的旧请求仍会被处理；两个相反操作（收藏 / 取消收藏）并发竞争时完成顺序不可控，
 * 服务端最终状态可能与 UI 的最新状态相反（UI 只反映最后一次点按）。
 *
 * 现在改为：
 * - [pending] 记录每篇文章的最新意图，[toggle] 每次都覆盖它；
 * - [locks] 给每篇文章一把锁，写请求在锁内串行执行；
 * - 拿到锁后重新读 [pending]：若期间已有请求把服务端带到该状态（[confirmed]）则直接跳过，
 *   连续来回点按因此被合并成一到两次请求，而不是排队几十个。
 */
@Singleton
class CollectStateImpl @Inject constructor(
    private val collectArticle: CollectArticleUseCase,
    private val appScope: AppCoroutineScope,
) : CollectState {

    private val _overrides = MutableStateFlow<Map<String, Boolean>>(emptyMap())
    override val overrides: StateFlow<Map<String, Boolean>> = _overrides.asStateFlow()

    private val _collectFailed = MutableSharedFlow<String>(extraBufferCapacity = 1)
    override val collectFailed: SharedFlow<String> = _collectFailed.asSharedFlow()

    /** 每篇文章的最新用户意图；revision 可区分值相同但先后不同的点按。 */
    private data class PendingIntent(val collect: Boolean, val revision: Long)

    private val pending = ConcurrentHashMap<String, PendingIntent>()
    private val nextRevision = AtomicLong()

    /** 将意图写入与失败回滚短暂串行化，避免旧请求失败覆盖新的 UI 意图。 */
    private val intentLock = Any()

    /** 最近一次写成功的意图；用于「已收敛则跳过」与失败回滚。 */
    private val confirmed = ConcurrentHashMap<String, Boolean>()

    /**
     * 每篇文章一把写锁。
     *
     * 用 [ConcurrentHashMap.computeIfAbsent]（而不是 `getOrPut`）取锁：后者非原子，
     * 并发下可能给同一 id 造出两把锁，等于没有互斥。锁与 [overrides] 同生命周期
     * （用户点过的文章），不做回收——回收时的「无人使用」判断本身就是新的竞态来源。
     */
    private val locks = ConcurrentHashMap<String, Mutex>()

    override fun toggle(id: String, collect: Boolean) {
        synchronized(intentLock) {
            // revision 使 true → false → true 仍能识别为新意图，而非旧请求的相同值。
            pending[id] = PendingIntent(collect, nextRevision.incrementAndGet())
            _overrides.update { it + (id to collect) }
        }
        // 只登记意图，不取消在途请求（取消拦不住服务端，见类注释）
        appScope.launch { flush(id) }
    }

    /** 排队写入：同一 id 串行，失败提示在锁外广播，避免慢订阅者拖住后续请求。 */
    private suspend fun flush(id: String) {
        val failure = lockFor(id).withLock { sendPendingIntent(id) }
        if (failure != null) _collectFailed.emit(failure)
    }

    /**
     * 把 [id] 当前的最新意图发送到服务端。
     *
     * @return 失败时返回要提示的文案；成功或无需发送时返回 null。
     */
    private suspend fun sendPendingIntent(id: String): String? {
        val intent = pending[id] ?: return null
        if (confirmed[id] == intent.collect) {
            // 期间已有请求把服务端带到这个状态，无需重复发送。
            pending.remove(id, intent)
            return null
        }

        val result = collectArticle(id, intent.collect)
        if (!result.success) {
            // 仅失败的 revision 仍是最新意图时才回滚；值相同的后续点按也必须保留并补发。
            val intentWasCurrent = synchronized(intentLock) {
                if (pending.remove(id, intent)) {
                    rollback(id)
                    true
                } else {
                    false
                }
            }
            return if (intentWasCurrent) result.message.ifBlank { "操作失败" } else null
        }

        synchronized(intentLock) {
            confirmed[id] = intent.collect
            // 只清除本次发送的 revision；期间更新的最新意图留给排队的 flush。
            pending.remove(id, intent)
        }
        return null
    }

    private fun lockFor(id: String): Mutex = locks.computeIfAbsent(id) { Mutex() }

    /** 回滚覆盖层：优先回到最近一次写成功的状态，没有则交还列表快照（移除覆盖）。 */
    private fun rollback(id: String) {
        val lastConfirmed = confirmed[id]
        _overrides.update { if (lastConfirmed == null) it - id else it + (id to lastConfirmed) }
    }
}

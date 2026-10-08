package com.example.fragmject.core.data.repository

import com.example.fragmject.core.android.platform.app.AppCoroutineScope
import com.example.fragmject.core.domain.CollectState
import com.example.fragmject.core.domain.usecase.CollectArticleUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap
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

    /**
     * 按文章 id 跟踪在途请求：同一 id 再次点击时取消旧请求，避免结果乱序覆盖。
     *
     * 用 [ConcurrentHashMap]：`toggle` 在主线程调用，而 `finally` 的清理跑在 [AppCoroutineScope]（IO），
     * 普通 HashMap 跨线程无同步有结构损坏风险。
     *
     * 清理时必须用 `remove(id, job)` 带上 Job 引用比对：被 cancel 的旧协程其 finally 仍会执行，
     * 若无条件 `remove(id)` 会把**同 id 的新 job** 记录删掉，导致下一次点击无法取消在途请求。
     */
    private val jobs = ConcurrentHashMap<String, Job>()

    override fun toggle(id: String, collect: Boolean) {
        _overrides.update { it + (id to collect) }
        jobs[id]?.cancel()
        val job = appScope.launch {
            try {
                val result = collectArticle(id, collect)
                if (!result.success) {
                    _overrides.update { it - id }
                    _collectFailed.emit(result.message.ifBlank { "操作失败" })
                }
            } finally {
                coroutineContext[Job]?.let { jobs.remove(id, it) }
            }
        }
        jobs[id] = job
        // 协程已同步完成（极快路径）时表项已无意义，立即清掉避免残留
        if (job.isCompleted) jobs.remove(id, job)
    }
}

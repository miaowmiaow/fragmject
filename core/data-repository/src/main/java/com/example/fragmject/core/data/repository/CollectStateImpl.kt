package com.example.fragmject.core.data.repository

import com.example.fragmject.core.android.platform.AppScope
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
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 收藏态 data 实现：进程级单例，跨页面共享乐观覆盖与失败通知。
 *
 * 下沉到 core:data-repository（领域端口装配层）；写操作挂在 [AppScope]，
 * 不随列表 item 离屏取消，失败回滚并经由 [collectFailed] 通知 UI。
 *
 * 失败通道用 SharedFlow：订阅者有多个（首页/体系/项目/搜索/用户主页/收藏列表），
 * Channel 的单消费者语义会让它们互相争抢事件，导致提示错位或丢失。
 */
@Singleton
class CollectStateImpl @Inject constructor(
    private val collectArticle: CollectArticleUseCase,
) : CollectState {

    private val _overrides = MutableStateFlow<Map<String, Boolean>>(emptyMap())
    override val overrides: StateFlow<Map<String, Boolean>> = _overrides.asStateFlow()

    private val _collectFailed = MutableSharedFlow<String>(extraBufferCapacity = 1)
    override val collectFailed: SharedFlow<String> = _collectFailed.asSharedFlow()

    /** 按文章 id 跟踪在途请求：同一 id 再次点击时取消旧请求，避免结果乱序覆盖。 */
    private val jobs = mutableMapOf<String, Job>()

    override fun toggle(id: String, collect: Boolean) {
        _overrides.update { it + (id to collect) }
        jobs[id]?.cancel()
        jobs[id] = AppScope.launch {
            try {
                val result = collectArticle(id, collect)
                if (!result.success) {
                    _overrides.update { it - id }
                    _collectFailed.emit(result.message.ifBlank { "操作失败" })
                }
            } finally {
                jobs.remove(id)
            }
        }
    }
}

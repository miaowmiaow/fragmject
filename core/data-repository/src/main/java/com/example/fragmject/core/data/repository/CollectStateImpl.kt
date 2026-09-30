package com.example.fragmject.core.data.repository

import com.example.fragmject.core.android.platform.AppScope
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
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 收藏状态实现：把 [CollectArticleUseCase] 的写操作收敛为应用级乐观覆盖层。
 *
 * 下沉到 core:data-repository（领域端口装配层），与 ThemeState / AuthState 归位方式一致；
 * 写操作挂在 [AppScope]，不随列表 item 离屏取消，失败回滚并经由 [collectFailed] 通知 UI。
 */
@Singleton
class CollectStateImpl @Inject constructor(
    private val collectArticle: CollectArticleUseCase,
) : CollectState {

    private val _overrides = MutableStateFlow<Map<String, Boolean>>(emptyMap())
    override val overrides: StateFlow<Map<String, Boolean>> = _overrides.asStateFlow()

    private val _collectFailed = MutableSharedFlow<String>(extraBufferCapacity = 1)
    override val collectFailed: SharedFlow<String> = _collectFailed.asSharedFlow()

    override fun toggle(id: String, collect: Boolean) {
        _overrides.update { it + (id to collect) }
        AppScope.launch {
            val result = collectArticle(id, collect)
            if (!result.success) {
                _overrides.update { it - id }
                _collectFailed.emit(result.message.ifBlank { "操作失败" })
            }
        }
    }
}

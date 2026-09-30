package com.example.fragmject.core.domain

import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * 应用级收藏状态契约：单一状态持有者，跨 feature 共享文章收藏的乐观覆盖层。
 *
 * 服务端真值由 [com.example.fragmject.core.model.Article.collect] 提供，
 * 本地 pending 覆盖（[overrides]）与真值合并后才是 UI 展示的选中态；
 * 收藏 / 取消收藏失败时通过 [collectFailed] 发一次性消息并回滚覆盖。
 *
 * 与 [ThemeState] 同属 core:domain，由 core:data-repository 提供实现并绑定，
 * 各 feature 只依赖本接口，避免收藏状态逻辑在多个 ViewModel 间重复。
 */
interface CollectState {
    /** 乐观覆盖层：articleId -> 是否已收藏（临时翻转），未覆盖项由 Article.collect 兜底。 */
    val overrides: StateFlow<Map<String, Boolean>>

    /** 收藏失败的一次性消息。 */
    val collectFailed: SharedFlow<String>

    /** 切换收藏态：乐观翻转 + 失败回滚 + 发事件。 */
    fun toggle(id: String, collect: Boolean)
}

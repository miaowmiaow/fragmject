package com.example.fragmject.core.domain

import kotlinx.coroutines.flow.Flow
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

    /**
     * 收藏失败消息（广播，允许多订阅者）。
     *
     * 订阅方有多个页面；曾使用单消费者 Channel，导致同屏多页面互相争抢事件、
     * 提示错位或丢失，故改为 SharedFlow。
     */
    val collectFailed: Flow<String>

    /**
     * 切换收藏态：乐观翻转 + **串行写** + 失败回滚 + 发事件。
     *
     * 同一篇文章的连续快速切换由实现负责排队与合并：调用方不需要自己防抖或忽略点击。
     * 实现保证「同一时刻只有一个写请求在途」，且最终请求携带最后一次调用传入的 [collect]，
     * 因此服务端最终状态与 UI 一致（不能靠取消旧请求来提速——取消只作用于本地，服务端仍可能处理）。
     */
    fun toggle(id: String, collect: Boolean)
}

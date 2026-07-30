package com.example.fragmject.core.common

import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

/**
 * 转场动画守卫：在页面首次数据到达后、更新 UI 前，
 * 确保过渡动画已完成，避免 Compose 在动画期间重组导致掉帧。
 *
 * ## 典型用法
 * ```kotlin
 * viewModelScope.launch {
 *     val t = TransitionGuard.now()
 *     val result = useCase()              // ← 数据加载不等待
 *     TransitionGuard.await(t)            // ← 动画没结束就在这里等一下
 *     _uiState.update { ... }             // ← 安全地更新 UI
 * }
 * ```
 *
 * ## 与老方案的区别
 * - 不再继承 ViewModel，按需引入
 * - 不切线程（`delay` 本身就是挂起，不会阻塞调用线程）
 * - `now()` + `await()` 两行分离，清晰明了
 */
object TransitionGuard {
    /** 默认转场动画时长 (ms)，与页面切换动画同步 */
    const val DURATION_MS = 350L

    /**
     * 记录当前时间戳作为计时起点。
     * 应在请求发起**之前**调用。
     */
    fun now(): Long = System.currentTimeMillis()

    /**
     * 挂起当前协程，直到从 [startMs] 起经过 [durationMs] 毫秒。
     * 如果已经过了足够时间，则立即返回（不挂起）。
     */
    suspend fun await(startMs: Long = now(), durationMs: Long = DURATION_MS) {
        val elapsed = System.currentTimeMillis() - startMs
        val remaining = durationMs - elapsed
        if (remaining > 0) delay(remaining.milliseconds)
    }
}
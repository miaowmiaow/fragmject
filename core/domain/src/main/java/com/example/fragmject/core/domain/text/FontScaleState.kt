package com.example.fragmject.core.domain.text

import kotlinx.coroutines.flow.StateFlow

/**
 * 应用级字体缩放状态契约。
 *
 * 与 `FontScaleRepository.observeBodyFontSp()` 的区别是**生命周期**：仓库给的是冷 `Flow`，
 * 每个订阅方都要等第一次发射；本契约是应用作用域的 `StateFlow`（`SharingStarted.Eagerly`），
 * 进程存活期间一直持值，**Activity 重建后首帧就是真实值**。
 *
 * 为什么必须这样：`MainActivity` 用冷 `Flow` 时只能给 `initialValue = DEFAULT_BODY_SP`，
 * 而切深色模式、进分屏、折叠屏展开都会重建 Activity —— 首帧按 1.0 倍渲染，用户会看到
 * 界面文字「闪一下再变大」，这类跳变比没有功能更糟。
 *
 * 已知限制：**冷启动**时 Room 首发射仍是异步的，首帧为
 * [FontScale.DEFAULT_BODY_SP]，随后立刻纠正。
 */
interface FontScaleState {

    val bodyFontSp: StateFlow<Float>
}

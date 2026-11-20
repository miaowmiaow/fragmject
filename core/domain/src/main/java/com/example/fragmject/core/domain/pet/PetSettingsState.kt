package com.example.fragmject.core.domain.pet

import kotlinx.coroutines.flow.StateFlow

/**
 * 应用级宠物设置状态契约。
 *
 * 与 `PetSettingsRepository.observePetSettings()` 的区别是**生命周期**：仓库给的是冷 [kotlinx.coroutines.flow.Flow]，
 * 每个订阅方都要等第一次发射；本契约是应用作用域的 `StateFlow`（`SharingStarted.Eagerly`），
 * 进程存活期间一直持值，**Activity 重建后首帧就是真实值**。
 *
 * 为什么必须这样：`MainActivity` 用冷 Flow 时只能给 `initialValue = PetSettings(enabled = false)`，
 * 而 `configChanges` 不含 `uiMode` / `screenLayout` / `smallestScreenSize`，切深色模式、进分屏、
 * 折叠屏展开都会重建 Activity —— 首帧会按「宠物已关闭」渲染，表现为鱼闪一下才出现。
 *
 * **本契约只负责「开关读得准」**：重建后鱼的位置、朝向由 `core:pet` 的 `FishMotion` 自己带上
 * （SavedState），不依赖这里；两件事分工明确，别把位置延续的锅甩给本契约。
 *
 * 已知限制：**冷启动**时 Room 查询仍是异步的，首帧为 [PetSettings.Default]，鱼会晚几帧出现；
 * 这不在本契约的职责内（进程刚起来时鱼本来就不在屏幕上）。
 */
interface PetSettingsState {
    val settings: StateFlow<PetSettings>
}

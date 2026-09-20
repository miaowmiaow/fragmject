package com.example.fragmject.core.navigation

import androidx.navigation3.runtime.NavKey

/**
 * 标记接口：实现此接口的路由在 Expanded（列表-详情同屏）模式下，
 * 由右侧详情面板渲染，而非全屏推入 backStack。
 *
 * 与 [RequiresAuth] 对称：以标记接口替代 `isDetailPaneKey` 中的硬编码类型判断，
 * 新增详情页时只需让路由 key 实现本接口，无需改动导航层的分发逻辑。
 */
interface DetailPaneNavKey : NavKey

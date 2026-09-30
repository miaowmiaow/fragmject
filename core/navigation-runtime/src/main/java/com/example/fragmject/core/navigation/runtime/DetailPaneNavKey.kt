package com.example.fragmject.core.navigation.runtime

import androidx.navigation3.runtime.NavKey

/**
 * 标记接口：实现此接口的路由在 Expanded（列表-详情同屏）模式下，
 * 由右侧详情面板渲染，而非全屏推入 backStack。
 *
 * 与 [RequiresAuth] 对称：以标记接口替代 `isDetailPaneKey` 中的硬编码类型判断，
 * 新增详情页时只需让路由 key 实现本接口，无需改动导航层的分发逻辑。
 *
 * 依赖提示：本接口定义于 :core:navigation-runtime。凡在 feature 的 api 模块中
 * 声明了实现本接口的 NavKey，该 api 模块须声明对 navigation-runtime 的依赖
 * （article/home/collection/user 已如此）；纯全屏页 NavKey 则无需此依赖。
 */
interface DetailPaneNavKey : NavKey

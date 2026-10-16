package com.example.fragmject.core.designsystem

/**
 * 页面转场动画时长 (ms)。
 *
 * 同时被两处引用，二者必须保持同一数值：
 * 1. `AppNavGraph` 的 `NavDisplay` 转场动画（`transitionSpec` / `popTransitionSpec`）；
 * 2. 骨架屏的最小展示时长（`ContentStateBox.minShowMillis`）——进入页面后即便数据已就绪，
 *    也先展示骨架屏覆盖整个转场动画，避免「数据渲染」与「转场动画」叠加导致掉帧。
 */
const val NAV_TRANSITION_DURATION_MS = 350

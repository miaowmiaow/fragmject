package com.example.fragmject.core.navigation.contract

import androidx.navigation3.runtime.NavKey

/**
 * 标记接口：属于同一条**多步流程**（选图 → 预览 → 编辑 …）的路由 key。
 *
 * 与 [RequiresAuth] / [DetailPaneNavKey] 同属「供组合根做分发判定的标记」：
 * app 组合根据此识别「某流程是否仍在 backStack 中」，进而决定流程级状态的创建与销毁。
 *
 * 为什么放在契约层而不是某个 feature 的 api：它是**通用编排知识**（流程是否还在栈里），
 * 不属于任何单一业务域；放在 feature/api 会迫使 :app 依赖该 feature 才能做编排判断。
 */
interface FlowScopedNavKey : NavKey

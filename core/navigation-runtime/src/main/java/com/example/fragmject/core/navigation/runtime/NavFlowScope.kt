package com.example.fragmject.core.navigation.runtime

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.navigation3.runtime.NavKey

/**
 * 流程作用域：与一组「流程 NavKey」绑定的生命周期单元。
 *
 * 用于让 feature 把跨多个导航 entry 共享的可变状态挂到导航流程上，而非进程级
 * @Singleton。app 组合根（AppNavGraph）根据 backStack 中是否存在对应流程的 key，
 * 驱动作用域的创建与 [close] 销毁；作用域经 [LocalNavFlowScopes] 下发给各 entry。
 */
interface NavFlowScope {
    /** 结束流程：取消在途工作并释放内部资源。 */
    fun close()
}

/**
 * 流程作用域贡献者：由 feature impl 经 Hilt multibinding 提供。
 *
 * 与 [NavContentContributor] 同属 navigation-runtime 的「multibinding 扩展点」：
 * feature 通过它向 app 组合根贡献自身流程作用域的创建能力。app 组合根只依赖本通用
 * 接口，不感知任何 feature 的具体流程类型；[matches] 由 feature 内部声明某 key 是否
 * 属于本流程，[create] 据此创建对应的作用域实例。
 */
interface NavFlowScopeContributor {
    /** 判断给定 key 是否属于本流程。 */
    fun matches(key: NavKey): Boolean

    /** 创建一个新的流程作用域。 */
    fun create(): NavFlowScope
}

/**
 * 当前所有活跃流程作用域的 Compose 访问入口。
 *
 * 键为 contributor（Hilt 单例、稳定引用），值为对应作用域。feature 的 entry 渲染器
 * 通过 `values.filterIsInstance<XxxFlowState>()` 取回自身作用域。
 */
val LocalNavFlowScopes =
    staticCompositionLocalOf<Map<NavFlowScopeContributor, NavFlowScope>> { emptyMap() }

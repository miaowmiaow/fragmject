package com.example.fragmject.core.navigation.contract

import androidx.navigation3.runtime.NavKey

/**
 * 标记接口：实现此接口的路由需要登录态才能访问。
 * 新增需登录页面时，只需让路由 key 实现本接口即可。
 *
 * 本接口为纯契约标记，定义于 :core:navigation-contract；判定逻辑位于
 * :app 组合根的 AppNavGraph.kt（requiredLoginNavKey），feature 不感知。
 */
interface RequiresAuth : NavKey
package com.example.fragmject.core.common

import androidx.navigation3.runtime.NavKey

/**
 * 标记接口：实现此接口的路由需要登录态才能访问。
 * 新增需登录页面时，只需让路由 key 实现本接口即可。
 *
 * 原位于 :app 的 WanNavGraph.kt 中，现移至 core:common 供 feature 模块直接引用，
 * 避免 feature 模块反向依赖 :app。
 */
interface RequiresAuth : NavKey

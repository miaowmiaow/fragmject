package com.example.fragmject.core.navigation.contracts

/**
 * 认证域导航契约（语义动作，不含任何 NavKey）。
 *
 * 由 `:app` 组合根实现并映射到具体路由；调用方（home 等）依赖本接口
 * 而非 auth feature 的 NavKey。
 */
interface AuthNavigator {

    /** 打开登录页。 */
    fun openLogin()

    /** 打开注册页。 */
    fun openRegister()
}

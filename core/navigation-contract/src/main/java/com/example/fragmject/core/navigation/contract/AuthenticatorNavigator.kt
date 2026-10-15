package com.example.fragmject.core.navigation.contract

/**
 * 验证器域导航契约（语义动作，不含任何 NavKey）。
 *
 * 由 `:app` 组合根实现并映射到具体路由；「我的」页等调用方依赖本接口，
 * 不感知 `feature:authenticator` 的 NavKey。
 */
interface AuthenticatorNavigator {

    /** 打开验证器账户列表。 */
    fun openAuthenticator()

    /** 打开新增账户页（扫码 / 手输密钥）。 */
    fun openAuthenticatorAdd()

    /** 打开账号转移页（选择要导出的账户）。 */
    fun openAuthenticatorTransfer()

    /**
     * 打开二维码展示页（导出流程第二步）。
     *
     * @param ids  要导出的账户 id 列表
     * @param mode 导出格式（单账户一张码 / 批量一张码），不提供默认值以免误用默认项
     */
    fun openAuthenticatorExportQr(ids: List<Long>, mode: AuthenticatorExportMode)
}

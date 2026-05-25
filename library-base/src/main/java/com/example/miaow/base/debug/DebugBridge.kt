package com.example.miaow.base.debug

import com.example.miaow.base.BuildConfig

/**
 * Debug-only 行为的统一入口。
 *
 * 散落在各处的 `if (BuildConfig.DEBUG)` 难以治理；
 * 通过 [DebugBridge] 集中暴露相关开关，便于：
 * 1. 一处关闭所有 Debug 行为（例如灰度调试包），无需到处修改；
 * 2. 测试 / Mock 时可以替换实现；
 * 3. 与具体业务解耦，降低误用风险（如 WebView 调试接口在 Release 包被打开）。
 *
 * 当前覆盖的能力：
 * - [isDebugBuild]: 当前是否 Debug 构建；
 * - [isHttpVerboseLogging]: 是否打印 HTTP body 级日志；
 * - [allowAssetsFallback]: 网络失败时是否回退到 assets/json 兜底数据；
 * - [allowWebContentsDebugging]: 是否允许打开 WebView 调试。
 */
object DebugBridge {

    val isDebugBuild: Boolean
        get() = BuildConfig.DEBUG

    /** HTTP 详细日志：Debug 包默认开启，Release 包关闭。 */
    val isHttpVerboseLogging: Boolean
        get() = BuildConfig.DEBUG

    /** 网络失败时是否启用 assets/json 兜底数据：仅 Debug 包，Release 包不允许隐式兜底。 */
    val allowAssetsFallback: Boolean
        get() = BuildConfig.DEBUG

    /** WebView 远程调试入口：仅 Debug 包暴露，避免 Release 包被攻击者利用。 */
    val allowWebContentsDebugging: Boolean
        get() = BuildConfig.DEBUG
}

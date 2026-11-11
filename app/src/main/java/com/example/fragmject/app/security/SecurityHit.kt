package com.example.fragmject.app.security

/**
 * 命中结果：原因 + 严重级别。
 *
 * ============================ 为什么需要分级 ============================
 * 拆出前所有命中都是一个 `String?`，命中即 `SIGKILL`。问题在于误报成本完全一样：
 * - 定制 ROM / 工程机自带 su、云手机、部分车机、模拟器都会命中 Root / 模拟器检测；
 * - 内容类应用的主链路（浏览、收藏）不因 Root 阻断，把这类用户直接杀掉属于负收益。
 *
 * 因此把「是否终止进程」的决策从检测项里拿出来，交给 [SecurityConfig] 的
 * [SecurityConfig.rootAction] / [SecurityConfig.emulatorAction] 配置：
 * - [Severity.CRITICAL]：正在被调试 / 注入 / 篡改，终止进程；
 * - [Severity.WARN]：环境可疑但业务可继续，只记录（见 [SecurityGuard.securityEvents]）。
 *
 * 无论哪一级，事件都会进入引擎的环形缓冲并回调 [SecurityConfig.onHit]，
 * 供宿主上报或做灰度观测。
 */
data class SecurityHit(
    /** 面向用户的中文原因，可直接展示。 */
    val reason: String,
    /** 严重级别；决定是否终止进程。 */
    val severity: Severity,
)

/** 命中级别。 */
enum class Severity {
    /** 只记录、不终止：用于 Root / 模拟器等环境类信号。 */
    WARN,

    /** 记录并终止进程：用于调试器、注入、篡改、签名异常等明确攻击特征。 */
    CRITICAL,
}

/**
 * 环境类检测项的处置方式（Root / 模拟器）。
 *
 * 三态而不是布尔：除了「拦 / 放」，还需要「完全不检测」，
 * 例如自动化测试包、或业务明确不关心 Root 的版本。
 */
enum class Action {
    /** 不检测该项。 */
    IGNORE,

    /** 检测但不阻断，只记录事件。 */
    WARN,

    /** 检测并阻断（进程会在 [SecurityConfig.blockGraceMs] 后终止）。 */
    BLOCK,
}

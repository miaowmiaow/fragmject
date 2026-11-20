package com.example.security

/*
 * [SecurityEngine] 的纯函数策略。
 *
 * 拆出原因：引擎文件超过项目的 500 行上限。这里的函数都不依赖 Handler / Looper 或 Context，
 * 可直接在 JVM 单测中覆盖，拆出后引擎文件只保留时序与阻断逻辑。
 */

/**
 * 是否应当降级为「只上报」。
 *
 * 抽成纯函数的原因：[SecurityEngine] 依赖 Handler/Looper，无法在 JVM 里构造；
 * 而「连续失败达到阈值才降级」这条规则是安全语义的关键，必须有单测兜住。
 *
 * @param failures 当前连续失败次数
 * @param limit 阈值；<=0 表示不降级（保持原有「命中即阻断」语义）
 * @param alreadyDegraded 已降级则不重复触发（避免重复事件）
 */
internal fun shouldDegrade(failures: Int, limit: Int, alreadyDegraded: Boolean): Boolean =
    limit in 1..failures && !alreadyDegraded

/**
 * native 库不可用时的判定（纯函数，便于单测）。
 *
 * - 可用：无命中；
 * - 不可用且 [requireNative]：CRITICAL 阻断（视为篡改）；
 * - 不可用且默认：WARN 事件，宿主可见，JVM 侧检测继续运行。
 *
 * 为什么必须可见：阻止 so 加载即可关掉 Frida / CRC / PLT 等全部 native 检测，
 * 若只打一条日志，宿主与攻击者面对的都是「静默放行」。
 */
internal fun nativeUnavailableHit(available: Boolean, requireNative: Boolean): SecurityHit? {
    if (available) return null
    return if (requireNative) {
        SecurityHit(
            reason = "安全组件（native）不可用，应用即将关闭",
            severity = Severity.CRITICAL,
            code = "internal.native_unavailable",
        )
    } else {
        SecurityHit(
            reason = "安全组件（native）不可用，仅启用 JVM 侧检测",
            severity = Severity.WARN,
            code = "internal.native_unavailable",
        )
    }
}

/**
 * 核心防护类命中：完整性 / 注入 / 调试器 / 篡改。
 *
 * 降级期间这些命中仍然阻断（见 [SecurityEngine.block]）；只有环境类（env.*）与内部事件放松。
 */
internal fun SecurityHit.isCoreCheck(): Boolean = CORE_CODE_PREFIXES.any { code.startsWith(it) }

private val CORE_CODE_PREFIXES = listOf("integrity.", "injection.", "debugger.", "tamper.")

/**
 * 退避策略：连续干净达到 [SecurityConfig.cleanRoundsToBackoff] 轮后把间隔翻倍，上限封顶。
 *
 * 抽成顶层纯函数是为了能在 JVM 单测里覆盖（[SecurityEngine] 本身依赖 Handler/Looper）。
 *
 * @param currentMs 当前间隔
 * @param cleanRounds 本轮结束后的连续干净轮数（已自增）
 * @param config 配置（提供阈值与上限）
 */
internal fun nextIntervalMs(currentMs: Long, cleanRounds: Int, config: SecurityConfig): Long =
    if (cleanRounds >= config.cleanRoundsToBackoff) {
        minOf(currentMs * 2, config.maxIntervalMs)
    } else {
        currentMs
    }

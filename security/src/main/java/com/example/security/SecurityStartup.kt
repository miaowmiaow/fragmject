package com.example.security

import android.util.Log

/**
 * 启动阶段的「一次动作」：反 dump 加固与诊断输出。
 *
 * 单独成文件的原因：[SecurityEngine] 只该负责时序与阻断；这两件事一个是内核状态设置、
 * 一个是排障日志，与轮询/退避无关。分开后引擎文件也能守住本项目的 500 行单文件上限。
 */

/**
 * 反 dump 的可做部分：收紧 `/proc` 可读性 + 禁 core dump。
 *
 * 失败只告警：被 seccomp 拦截或厂商 ROM 限制时，不能因此阻断启动。
 * 详细边界见 [SecurityConfig.antiDump] 的注释。
 */
internal fun applyAntiDump(config: SecurityConfig, native: NativeChecks) {
    if (!config.antiDump || !native.available) return
    if (native.hardenProcess() != 0) {
        Log.w(SecurityGuard.TAG, "hardenProcess failed (PR_SET_DUMPABLE), anti-dump degraded")
    }
}

/**
 * 诊断模式输出（默认关）。
 *
 * 用 `Log.e` 而不是 `Log.i`：release 会裁剪 v/d/i/w，诊断信息恰恰要在 release 包里可见。
 * 内容只含「配置与能力状态」，不含用户数据。
 */
internal fun logDiagnostics(config: SecurityConfig, native: NativeChecks) {
    if (!config.diagnosticMode) return
    Log.e(
        SecurityGuard.TAG,
        "security diag: checks=${config.checks.joinToString(",") { it.name }} " +
            "native=${native.available} antiDump=${config.antiDump} " +
            "signature=${config.expectedSignatureSha256.isNotEmpty()} " +
            "buildType=${config.buildType.ifEmpty { "<unset>" }}",
    )
}

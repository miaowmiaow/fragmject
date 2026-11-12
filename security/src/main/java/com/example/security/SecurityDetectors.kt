package com.example.security

/**
 * 安全检测的**编排层**：检测顺序、短路逻辑与严重级别判定。
 *
 * 拆出原因：调度（何时检测、命中后怎么办）在 [SecurityEngine]；单项判定在
 * `SecurityEnvironmentChecks` / `SecurityIntegrityChecks`；这里只负责「按什么顺序问、
 * 问到第一个命中就停」。
 *
 * 全部是纯函数，依赖由参数传入，可在 JVM 单测中构造任意设备状态。
 */

/**
 * 返回首个命中的检测项；未命中返回 null。
 *
 * 排序原则：**明确攻击特征优先，环境类信号垫底**。
 * - 前四项（调试器 / Frida / CRC / 端口）命中即 [Severity.CRITICAL]；
 * - Root / 模拟器放在最后，其级别由 [SecurityConfig.rootAction] /
 *   [SecurityConfig.emulatorAction] 决定（默认 WARN，不终止进程）。
 *
 * 这样排序的两个原因：
 * 1. native 判定优先：返回值不走 Java 方法调用，Frida 的 Java hook 打不到，
 *    尽早命中就能尽早阻断，减少后续检测在「已被控制的环境」里执行；
 * 2. 环境类检测需要查文件、查包名（binder 调用），成本高于读一个小文件，
 *    放在 CRITICAL 项之后可以避免「已确认被注入还要把 root 检查跑完」。
 *
 * 各检测项说明与已知局限：
 * 1. TracerPid：覆盖 ptrace/lldb/调试壳；对 Java 调试无效（由第 2 项覆盖）。
 * 2. Java 调试：`Debug.isDebuggerConnected()` 可被 hook，故不作为唯一判据。
 * 3. Frida maps 特征：覆盖 agent/gadget；对「改名 so」可能漏检。
 * 4. Frida 线程名：so 删除后线程仍可能残留；`gmain/gdbus` 有误报可能。
 * 5. Frida 落地文件：覆盖 frida-server 二进制；对内存注入无效。
 * 6. 匿名可执行段：对内存注入最通用；受 ROM/JIT 差异影响，阈值需可调。
 * 7. libc `.text` CRC：覆盖 inline hook；对 PLT/GOT hook、Java hook 无效。
 * 8. 端口扫描：只覆盖 frida-server 形态；gadget 不监听端口。
 * 9. Root：多信号综合（su/Magisk/root 管理应用/maps 注入），存在定制 ROM 误报。
 * 10. 模拟器：Build 多字段 + QEMU 设备，Score >= 2 才判定，降低误报。
 *
 * @param includePortScan 本轮是否执行端口扫描（由调用方按时间降频决定）。
 * @param includeCrc      本轮是否执行 CRC 校验（由调用方按时间降频决定）。
 * @param includeInjectionScan 本轮是否执行「重量级注入扫描」（解析 maps 的 Frida 特征与匿名段）。
 */
internal fun detectHit(
    facts: SecurityFacts,
    native: NativeChecks,
    config: SecurityConfig,
    includePortScan: Boolean,
    includeCrc: Boolean,
    includeInjectionScan: Boolean = true,
): SecurityHit? {
    // 按「启用的检测维度」逐项短路：裁掉的项不执行、也不产生事件（省掉对应 native 调用）
    if (SecurityCheck.DEBUGGER in config.checks) {
        detectDebugHit(facts, native)?.let { return it }
    }
    if (SecurityCheck.INJECTION in config.checks) {
        detectFridaHit(native, config, includeInjectionScan)?.let { return it }
    }
    if (SecurityCheck.LIB_CRC in config.checks) {
        detectIntegrityHit(native, includeCrc)?.let { return it }
    }
    if (SecurityCheck.PLT_HOOK in config.checks) {
        detectPltHookHit(native, includeCrc)?.let { return it }
    }
    if (SecurityCheck.PORT_SCAN in config.checks) {
        detectPortHit(facts, includePortScan)?.let { return it }
    }
    return detectEnvironmentHit(facts, native, config)
}

/**
 * 1-2：调试器检测（CRITICAL）。
 * native TracerPid 优先（抗 Java hook），>0 才判定命中；-1（读取失败）视为未知，
 * 继续后续检测而不是误杀。Java 层 Debug 接口覆盖 Android Studio / jdb 等纯 Java 调试。
 */
internal fun detectDebugHit(facts: SecurityFacts, native: NativeChecks): SecurityHit? {
    if (native.available) {
        val tracerPid = native.readTracerPid()
        if (tracerPid > 0) {
            return SecurityHit(
                reason = "检测到 Native 调试器附加（TracerPid=$tracerPid）",
                severity = Severity.CRITICAL,
                code = "debugger.native",
            )
        }
    }
    if (facts.isDebuggerConnected() || facts.waitingForDebugger()) {
        return SecurityHit("检测到 Java 层调试器（JDWP）", Severity.CRITICAL, code = "debugger.java")
    }
    return null
}

/**
 * 3-6：Frida 与内存注入检测（CRITICAL）。
 * maps 特征串已 XOR 混淆（二进制中无明文）；线程名覆盖「so 已删除/改名但线程残留」；
 * 落地文件覆盖 frida-server 二进制；匿名可执行段对无文件映射的可执行内存做通用检测
 * （native 返回 -1 时 `-1 > 阈值` 为 false，不会误杀）。
 */
internal fun detectFridaHit(
    native: NativeChecks,
    config: SecurityConfig,
    includeScan: Boolean = true,
): SecurityHit? {
    // 重量级部分（解析 maps）按降频执行；线程名/落地文件很便宜，每轮都查
    if (includeScan && native.available && native.findFridaInMaps() > 0) {
        return SecurityHit("检测到 Frida 注入特征（内存映射）", Severity.CRITICAL, code = "injection.frida_maps")
    }
    if (hasFridaThread(native)) {
        return SecurityHit("检测到 Frida 特征线程", Severity.CRITICAL, code = "injection.frida_thread")
    }
    if (hasFridaFile(native)) {
        return SecurityHit("检测到 Frida 落地文件", Severity.CRITICAL, code = "injection.frida_file")
    }

    if (includeScan && native.available) {
        // 带上实际段数：调 anonExecThreshold 时先看这个数字，比「命中/未命中」有用得多
        val segments = native.countAnonymousExecSegments()
        if (segments > config.anonExecThreshold) {
            return SecurityHit(
                reason = "检测到异常匿名可执行内存段（${segments} 段，阈值 ${config.anonExecThreshold}）",
                severity = Severity.CRITICAL,
                code = "injection.anon_segment",
            )
        }
    }
    return null
}

/** 7：libc `.text` CRC（CRITICAL），只在 includeCrc 为 true（30s 降频）时执行，返回 1 才判定。 */
internal fun detectIntegrityHit(native: NativeChecks, includeCrc: Boolean): SecurityHit? {
    if (!includeCrc) return null
    if (!native.available) return null
    if (native.verifyLibcTextCrc() != CRC_MISMATCH) return null
    return SecurityHit(
        "检测到 libc 代码段被篡改（疑似 inline hook）",
        Severity.CRITICAL,
        code = "integrity.libc_crc",
    )
}

/**
 * GOT/PLT 劫持（CRITICAL）：导入函数指针被改写，代码段本身没变，逐页 CRC 抓不到。
 *
 * 与 libc CRC 共用同一个降频开关 [includeCrc]（默认 30 s）：两者都是「读表 + 比对」，
 * 每轮都跑没有额外收益，反而增加稳态开销。
 *
 * 默认不启用（[SecurityCheck.PLT_HOOK] 不在 [SecurityCheck.DEFAULT] 里），
 * 原因见该枚举值的注释：符号解析差异会带来误报，必须先在自己的机型矩阵验证。
 */
internal fun detectPltHookHit(native: NativeChecks, includeCrc: Boolean): SecurityHit? {
    if (!includeCrc) return null
    if (!native.available) return null
    val hooks = native.countPltHooks()
    // 0=干净；-1=无法解析（放行）；>0=被劫持
    if (hooks <= 0) return null
    return SecurityHit(
        reason = "检测到导入函数被劫持（${hooks} 处）",
        severity = Severity.CRITICAL,
        code = "integrity.plt_hook",
    )
}

/** 8：端口扫描（CRITICAL），只在 includePortScan 为 true（60s 降频）时执行，仅覆盖 frida-server 形态。 */
internal fun detectPortHit(facts: SecurityFacts, includePortScan: Boolean): SecurityHit? {
    // 拆成两个判断而不是 `!a || !b`：后者会被 IDE/静态检查提示可化简，
    // 而且「本轮不做」与「做了但没命中」是两种语义，分开读更清楚。
    if (!includePortScan) return null
    if (!isFridaPortOpen(facts)) return null
    return SecurityHit(
        "检测到 Frida Server 监听端口（27042-27052）",
        Severity.CRITICAL,
        code = "injection.port",
    )
}

/**
 * 9-10：Root / 模拟器（级别由配置决定，默认 WARN）。
 *
 * 这两项是**环境特征**而非攻击证据：定制 ROM、云手机、自动化测试都会命中，
 * 一律杀进程的误杀成本高于收益，因此默认只记录、不阻断。
 */
internal fun detectEnvironmentHit(
    facts: SecurityFacts,
    native: NativeChecks,
    config: SecurityConfig,
): SecurityHit? {
    val rootEnabled = SecurityCheck.ROOT in config.checks && config.rootAction != Action.IGNORE
    if (rootEnabled && hasRootSignals(facts, native)) {
        return SecurityHit("检测到 Root 环境", severityOf(config.rootAction), code = "env.root")
    }
    val emulatorEnabled = SecurityCheck.EMULATOR in config.checks && config.emulatorAction != Action.IGNORE
    if (emulatorEnabled && hasEmulatorSignals(facts)) {
        return SecurityHit("检测到模拟器环境", severityOf(config.emulatorAction), code = "env.emulator")
    }
    return null
}

/** [Action.BLOCK] → CRITICAL（终止进程）；[Action.WARN] → WARN（只记录）。 */
private fun severityOf(action: Action): Severity =
    if (action == Action.BLOCK) Severity.CRITICAL else Severity.WARN

/** `verifyLibcTextCrc` 返回 1 表示「磁盘与内存不一致」。 */
private const val CRC_MISMATCH = 1

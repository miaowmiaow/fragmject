package com.example.security

/**
 * 环境类检测项：Frida（线程 / 文件 / 端口）、Root 信号、可调试构建、模拟器特征。
 *
 * 全部是**无状态内部函数**，依赖通过 [SecurityFacts] / [NativeChecks] 传入：
 * - 检测项可以在纯 JVM 单测里穷举分支（见 `SecurityDetectorsTest`）；
 * - 不依赖全局 `object` 状态，库化时可直接搬走。
 *
 * ============================ 特征表已下沉 native（H3） ============================
 * Frida 的线程名 / 落地文件名、以及 Root 的 su 与 Magisk 路径，原本是 Kotlin 常量表：
 * `strings classes.dex | grep frida-server` 就能把检测逻辑连锅端。
 * 现在这些表放在 `security.cpp` 里，经 [XorString] 编译期混淆，Kotlin 侧只拿「命中数」。
 *
 * 保留在 Kotlin 的两张表：
 * - [FRIDA_PORTS]：端口扫描本身在 Java 侧建 socket，需要具体端口号；
 * - [ROOT_APP_PACKAGES]：查询已安装应用要通过 `PackageManager`，包名由 Java 传入更自然。
 *   两者都不是「检测手法指纹」，泄露价值低。
 */
internal fun hasFridaThread(native: NativeChecks): Boolean =
    native.available && native.findFridaThreads() > 0

internal fun hasFridaFile(native: NativeChecks): Boolean =
    native.available && native.findFridaFiles() > 0

/**
 * 扫描本机回环的 Frida Server 常见端口。
 *
 * 为什么连 127.0.0.1 而不是 0.0.0.0：frida-server 通常监听本机回环，
 * 连回环不会触发网络权限/流量统计，也不需要 INTERNET 权限之外的额外声明。
 *
 * 超时取 50ms：本机回环上「端口未监听」会立刻返回 Connection refused，
 * 只有极少数情况下才会等到超时；50ms 足以避免拖慢检测。
 *
 * 局限：只能发现 frida-server；gadget 注入不监听端口。
 */
internal fun isFridaPortOpen(facts: SecurityFacts): Boolean =
    FRIDA_PORTS.any { facts.isPortOpen(it, FRIDA_PORT_TIMEOUT_MS) }

/**
 * Root 综合判定。
 *
 * 采用多信号，任一「强信号」命中即判定：
 * 1. su 二进制 / Magisk 目录存在（强信号，native 侧扫描）；
 * 2. 已安装 root 管理应用（强信号，PackageManager 查询）；
 * 3. native maps 中出现 zygisk/riru/lsposed 等注入特征（强信号）；
 * 4. build.prop 可调试（弱信号，仅在 ro.debuggable=1 时触发）。
 *
 * 为什么不用「which su」：Runtime.exec 容易被 hook，且部分 ROM 有 su 但未启用 root；
 * 这里用「文件存在 + 包名 + maps」交叉验证，误报率更低。
 */
internal fun hasRootSignals(facts: SecurityFacts, native: NativeChecks): Boolean {
    if (native.available && native.findRootFiles() > 0) return true
    if (hasRootManagementApp(facts)) return true
    if (native.available && native.findRootInMaps() > 0) return true
    if (isDebuggableBuild(facts)) return true
    return false
}

/**
 * 是否安装了常见 root 管理应用。
 *
 * 包不存在视为未安装；查询失败（无 Context / ROM 限制）返回 false，不误杀。
 */
internal fun hasRootManagementApp(facts: SecurityFacts): Boolean =
    ROOT_APP_PACKAGES.any { facts.isPackageInstalled(it) }

/**
 * 系统是否为「可调试构建」。
 *
 * 读取 `ro.debuggable` / `ro.secure`：正常量产机为 0/1；部分 root/工程机为 1/0。
 * 这是弱信号，仅作为 Root 的补充；读取失败返回 false。
 */
internal fun isDebuggableBuild(facts: SecurityFacts): Boolean {
    val props = facts.readText("/system/build.prop") ?: return false
    return props.contains("ro.debuggable=1") || props.contains("ro.secure=0")
}

/**
 * 模拟器综合判定。
 *
 * 用 Score 累加而不是「任一命中」，避免单一字段误判（例如某些真机 MODEL 含 sdk）。
 * 阈值取 2：需要至少两个独立信号同时成立。
 */
internal fun hasEmulatorSignals(facts: SecurityFacts): Boolean {
    val build = facts.buildValues()
    var score = 0
    if (build.fingerprint.startsWith("generic") ||
        build.fingerprint.containsAny("unknown", "emulator", "vbox", "genymotion")
    ) {
        score++
    }
    if (build.model.containsAny("google_sdk", "emulator", "android sdk built for", "droid4x")) {
        score++
    }
    if (build.product.containsAny("sdk", "google_sdk", "emulator", "simulator", "vbox86p")) {
        score++
    }
    if (build.hardware.contains("goldfish") ||
        build.hardware.contains("ranchu") ||
        build.hardware.contains("vbox86")
    ) {
        score++
    }
    if (build.brand.startsWith("generic") || build.brand.contains("genymotion")) {
        score++
    }
    if (build.manufacturer.contains("genymotion")) {
        score++
    }
    if (facts.exists("/dev/qemu_pipe") || facts.exists("/dev/socket/qemud")) {
        score++
    }
    if (facts.exists("/system/lib/libc_malloc_debug_qemu.so")) {
        score++
    }
    return score >= EMULATOR_SCORE_THRESHOLD
}

/** 命中任一关键字即视为可疑特征（模拟器指纹/型号/产品名共用）。 */
private fun String.containsAny(vararg hints: String): Boolean = hints.any { contains(it) }

/** 本机回环端口探测超时：见 [isFridaPortOpen] 的说明。 */
private const val FRIDA_PORT_TIMEOUT_MS = 50

/** 模拟器判定的分数阈值：至少两个独立信号同时成立。 */
private const val EMULATOR_SCORE_THRESHOLD = 2

package com.example.fragmject.app.security

import android.content.pm.PackageManager
import android.os.Build
import java.io.File
import java.net.InetSocketAddress
import java.net.Socket
import java.util.Locale

/**
 * 环境类检测项：Frida（线程 / 文件 / 端口）、Root 信号、可调试构建、模拟器特征。
 *
 * 与 `SecurityDetectors.kt` 同包，编排层调用无需 import。
 * 纯环境探测（Frida 线程 / 文件 / 端口、build.prop、模拟器特征）不需要 `AppSecurityGuard`
 * 实例，写成顶层函数；只有要读 native 状态或 Context 的项（[AppSecurityGuard.hasRootSignals]、
 * [AppSecurityGuard.hasRootManagementApp]）才保留为扩展函数。
 *
 * 误报说明都写在各检测项的 KDoc 里——调阈值或关开关前先读对应条目，
 * 不要直接改 `when` 条件（历史上误杀多来自「顺手放宽判定」）。
 */

    /**
     * 遍历 `/proc/self/task/<tid>/comm` 检测 Frida 特征线程。
     *
     * 为什么读 `comm` 而不是 `stat`：`comm` 只有线程名，解析简单且不易受
     * 进程名中括号/空格影响（`stat` 的 comm 字段可能被进程名里的括号干扰）。
     *
     * 局限：线程名可被攻击者修改；这里只作为「多信号之一」。
     * 读取失败（权限/文件消失）时返回 false，不误杀。
     */
    internal fun hasFridaThread(): Boolean = try {
        File("/proc/self/task").listFiles()?.any { tidDir ->
            val comm = File(tidDir, "comm")
            if (!comm.exists()) return@any false
            val name = try {
                comm.readText().trim().lowercase(Locale.US)
            } catch (_: Exception) {
                return@any false
            }
            FRIDA_THREAD_SIGNATURES.any { name.contains(it) }
        } == true
    } catch (_: Exception) {
        false
    }

    /**
     * 检测 Frida 落地文件。
     *
     * 检查两处：
     * - `/proc/self/fd`：frida-server 运行时可能持有相关文件的 fd；
     * - `/data/local/tmp`：frida-server 最常用的落地路径。
     *
     * 局限：只覆盖「落地到磁盘」的形态；纯内存注入（gadget 直接 push so）不会命中。
     */
    internal fun hasFridaFile(): Boolean = try {
        listOf(File("/proc/self/fd"), File("/data/local/tmp")).any { dir ->
            dir.isDirectory && dir.listFiles()?.any { file ->
                val name = file.name.lowercase(Locale.US)
                FRIDA_FILE_SIGNATURES.any { name.contains(it) }
            } == true
        }
    } catch (_: Exception) {
        false
    }

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
    internal fun isFridaPortOpen(): Boolean {
        for (port in FRIDA_PORTS) {
            try {
                Socket().use { socket ->
                    socket.connect(InetSocketAddress("127.0.0.1", port), 50)
                    return true
                }
            } catch (_: Exception) {
                // 该端口不可连接，继续下一个
            }
        }
        return false
    }

    /**
     * Root 综合判定。
     *
     * 采用多信号，任一「强信号」命中即判定：
     * 1. su 二进制存在（强信号）；
     * 2. Magisk/Zygisk/Riru 目录存在（强信号）；
     * 3. 已安装 root 管理应用（强信号）；
     * 4. native maps 中出现 zygisk/riru/lsposed 等注入特征（强信号）；
     * 5. build.prop 可调试（弱信号，仅在 ro.debuggable=1 时触发）。
     *
     * 为什么不用「which su」：Runtime.exec 容易被 hook，且部分 ROM 有 su 但未启用 root；
     * 这里用「文件存在 + 包名 + maps」交叉验证，误报率更低。
     */
    internal fun AppSecurityGuard.hasRootSignals(): Boolean {
        if (ROOT_FILE_PATHS.any { File(it).exists() }) return true
        if (MAGISK_PATHS.any { File(it).exists() }) return true
        if (hasRootManagementApp()) return true
        if (nativeLoaded && nativeFindRootInMaps() > 0) return true
        if (isDebuggableBuild()) return true
        return false
    }

    /**
     * 是否安装了常见 root 管理应用。
     *
     * 用 `getPackageInfo` 探测；包不存在会抛 [PackageManager.NameNotFoundException]，
     * 视为未安装。查询失败/无 Context 时返回 false，不误杀。
     */
    internal fun AppSecurityGuard.hasRootManagementApp(): Boolean {
        val pm = appContext?.packageManager ?: return false
        return ROOT_APP_PACKAGES.any { pkg ->
            try {
                pm.getPackageInfo(pkg, 0)
                true
            } catch (_: PackageManager.NameNotFoundException) {
                false
            }
        }
    }

    /**
     * 系统是否为「可调试构建」。
     *
     * 读取 `ro.debuggable` / `ro.secure`：正常量产机为 0/1；部分 root/工程机为 1/0。
     * 这是弱信号，仅作为 Root 的补充；读取失败返回 false。
     */
    internal fun isDebuggableBuild(): Boolean {
        return try {
            val propsFile = File("/system/build.prop")
            if (!propsFile.exists()) return false
            val props = propsFile.readText()
            props.contains("ro.debuggable=1") || props.contains("ro.secure=0")
        } catch (_: Exception) {
            false
        }
    }

    /**
     * 模拟器综合判定。
     *
     * 用 Score 累加而不是「任一命中」，避免单一字段误判（例如某些真机 MODEL 含 sdk）。
     * 阈值取 2：需要至少两个独立信号同时成立。
     */
    internal fun hasEmulatorSignals(): Boolean {
        val fingerprint = Build.FINGERPRINT.lowercase(Locale.US)
        val model = Build.MODEL.lowercase(Locale.US)
        val product = Build.PRODUCT.lowercase(Locale.US)
        val hardware = Build.HARDWARE.lowercase(Locale.US)
        val brand = Build.BRAND.lowercase(Locale.US)
        val manufacturer = Build.MANUFACTURER.lowercase(Locale.US)

        var score = 0
        if (fingerprint.startsWith("generic") ||
            fingerprint.containsAny("unknown", "emulator", "vbox", "genymotion")
        ) {
            score++
        }
        if (model.containsAny("google_sdk", "emulator", "android sdk built for", "droid4x")) {
            score++
        }
        if (product.containsAny("sdk", "google_sdk", "emulator", "simulator", "vbox86p")) {
            score++
        }
        if (hardware.contains("goldfish") ||
            hardware.contains("ranchu") ||
            hardware.contains("vbox86")
        ) {
            score++
        }
        if (brand.startsWith("generic") || brand.contains("genymotion")) {
            score++
        }
        if (manufacturer.contains("genymotion")) {
            score++
        }
        if (File("/dev/qemu_pipe").exists() || File("/dev/socket/qemud").exists()) {
            score++
        }
        if (File("/system/lib/libc_malloc_debug_qemu.so").exists()) {
            score++
        }
        return score >= 2
    }

/** 命中任一关键字即视为可疑特征（模拟器指纹/型号/产品名共用）。 */
private fun String.containsAny(vararg hints: String): Boolean = hints.any { contains(it) }

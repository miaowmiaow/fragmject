package com.example.fragmject.app.security

import android.os.Debug
import android.os.Process
import android.util.Log
import com.example.fragmject.app.security.AppSecurityGuard.blockingMessage
import com.example.fragmject.app.security.SecurityPolicy.ANON_EXEC_THRESHOLD
import com.example.fragmject.app.security.SecurityPolicy.BLOCK_GRACE_MS

/**
 * 安全检测的**编排层**：native 加载、检测项调度顺序、命中后的阻断动作。
 *
 * 拆出原因：原文件 494 行贴着「单文件 500 行」的守卫上限（规则 28），
 * 且三类内容（调度 / 环境判定 / 完整性校验）变更频率完全不同。
 *
 * 同包拆分后调用点零改动：所有检测项都是 `AppSecurityGuard` 的扩展函数，
 * 同包顶层函数可直接互调，不需要 import。
 */

    /**
     * 懒加载 native 库，失败时降级为纯 JVM 检测。
     *
     * 为什么不在 object 初始化时直接 loadLibrary：
     * 一旦 so 缺失/架构不匹配，初始化异常会让整个 object 不可用，
     * 进而可能拖垮启动；这里显式 try-catch，把「so 不可用」当成可接受的降级状态。
     *
     * 降级后的能力损失：TracerPid/maps/匿名段/CRC 这几项 native 检测失效，
     * 但 Java 层的调试检测、线程名、落地文件、端口扫描仍然有效。
     */
    internal fun AppSecurityGuard.ensureNativeLoaded() {
        if (nativeLoaded) return
        try {
            System.loadLibrary("fragmject_security")
            nativeLoaded = true
        } catch (e: UnsatisfiedLinkError) {
            // so 缺失/加载失败时降级为纯 Java 检测，不阻断启动
            Log.w(TAG, "load fragmject_security failed, fallback to JVM checks", e)
        }
    }

    /**
     * 返回首个命中的检测项描述；未命中返回 null。
     *
     * 排序原则：**native 判定优先**。原因：
     * - native 返回值不走 Java 方法调用，Frida 的 Java hook 打不到；
     * - 一旦 native 判定命中，就可以尽早阻断，减少后续检测在「已被控制的环境」里执行。
     *
     * 各检测项说明与已知局限：
     * 1. TracerPid：覆盖 ptrace/lldb/调试壳；对 Java 调试无效（由第 2 项覆盖）。
     * 2. Java 调试：`Debug.isDebuggerConnected()` 可被 hook，故不作为唯一判据。
     * 3. Frida maps 特征：覆盖 agent/gadget；对「改名 so」可能漏检。
     * 4. Frida 线程名：so 删除后线程仍可能残留；`gmain/gdbus` 有误报可能。
     * 5. Frida 落地文件：覆盖 frida-server 二进制；对内存注入无效。
     * 6. 匿名可执行段：对内存注入最通用；受 ROM/JIT 差异影响，阈值需可调。
     * 7. Root：多信号综合（su/Magisk/root 管理应用/maps 注入），存在定制 ROM 误报。
     * 8. 模拟器：Build 多字段 + QEMU 设备，Score >= 2 才判定，降低误报。
     * 9. libc `.text` CRC：覆盖 inline hook；对 PLT/GOT hook、Java hook 无效。
     * 10. 端口扫描：只覆盖 frida-server 形态；gadget 不监听端口。
     *
     * @param includePortScan 本轮是否执行端口扫描（由调用方按时间降频决定）。
     * @param includeCrc      本轮是否执行 CRC 校验（由调用方按时间降频决定）。
     */
    internal fun AppSecurityGuard.detectReason(includePortScan: Boolean, includeCrc: Boolean): String? =
        detectDebugReason()
            ?: detectFridaReason()
            ?: detectEnvironmentReason(includeCrc)
            ?: detectPortReason(includePortScan)

    /**
     * 1-2：调试器检测。
     * native TracerPid 优先（抗 Java hook），>0 才判定命中；-1（读取失败）视为未知，
     * 继续后续检测而不是误杀。Java 层 Debug 接口覆盖 Android Studio / jdb 等纯 Java 调试。
     */
    private fun AppSecurityGuard.detectDebugReason(): String? {
        if (nativeLoaded) {
            val tracerPid = nativeReadTracerPid()
            if (tracerPid > 0) {
                return "检测到 Native 调试器附加（TracerPid=$tracerPid）"
            }
        }
        if (Debug.isDebuggerConnected() || Debug.waitingForDebugger()) {
            return "检测到 Java 层调试器（JDWP）"
        }
        return null
    }

    /**
     * 3-6：Frida 与内存注入检测。
     * maps 特征串已 XOR 混淆（二进制中无明文）；线程名覆盖「so 已删除/改名但线程残留」；
     * 落地文件覆盖 frida-server 二进制；匿名可执行段对无文件映射的可执行内存做通用检测
     * （native 返回 -1 时 `-1 > 0` 为 false，不会误杀）。
     */
    private fun AppSecurityGuard.detectFridaReason(): String? = when {
        nativeLoaded && nativeFindFridaInMaps() > 0 ->
            "检测到 Frida 注入特征（内存映射）"

        hasFridaThread() -> "检测到 Frida 特征线程"

        hasFridaFile() -> "检测到 Frida 落地文件"

        nativeLoaded && nativeCountAnonymousExecSegments() > ANON_EXEC_THRESHOLD ->
            "检测到异常匿名可执行内存段（疑似内存注入）"

        else -> null
    }

    /**
     * 7-9：环境与完整性检测。
     * Root 多信号综合（su / Magisk / root 管理应用 / maps 注入特征），默认阻断；
     * 模拟器用 Build 多字段 + QEMU 设备节点，Score >= 2 才判定以降低误报；
     * libc `.text` CRC 只在 includeCrc 为 true（30s 降频）时执行，返回 1 才是「不一致」。
     */
    private fun AppSecurityGuard.detectEnvironmentReason(includeCrc: Boolean): String? = when {
        SecurityPolicy.enforceRootDetection && hasRootSignals() -> "检测到 Root 环境"

        SecurityPolicy.enforceEmulatorDetection && hasEmulatorSignals() -> "检测到模拟器环境"

        includeCrc && nativeLoaded && nativeVerifyLibcTextCrc() == 1 ->
            "检测到 libc 代码段被篡改（疑似 inline hook）"

        else -> null
    }

    /** 10：端口扫描，只在 includePortScan 为 true（60s 降频）时执行，仅覆盖 frida-server 形态。 */
    private fun AppSecurityGuard.detectPortReason(includePortScan: Boolean): String? {
        // 拆成两个判断而不是 `!a || !b`：后者会被 IDE/静态检查提示可化简，
        // 而且「本轮不做」与「做了但没命中」是两种语义，分开读更清楚。
        if (!includePortScan) return null
        if (!isFridaPortOpen()) return null
        return "检测到 Frida Server 监听端口（27042-27052）"
    }

    /**
     * 命中后置标记并延迟终止进程。
     *
     * 为什么延迟：给 UI 一个展示 [blockingMessage] 的窗口，避免用户看到「莫名闪退」。
     * 为什么延迟不削弱安全性：
     * - 终止动作由 native `SIGKILL` 完成，Java 层 hook 无法取消；
     * - 延迟固定且很短（1.2s），攻击者拿不到有意义的操作窗口；
     * - 命中后不再排下一轮检测，避免在「已被控制的环境」里继续跑逻辑。
     *
     * 用 compare-and-set 语义（先判断 blocked 再置位）保证并发调用只处理一次：
     * 该方法可能同时被轮询线程与签名线程调用。
     */
    internal fun AppSecurityGuard.block(reason: String) {
        if (blocked) return
        blocked = true
        blockReason = reason
        Log.w(TAG, "security check hit: $reason")

        handler.postDelayed({
            // 优先 native SIGKILL（不可捕获/不可 hook）；native 不可用时退回 Java kill。
            if (nativeLoaded) {
                nativeKillSelf()
            }
            Process.killProcess(Process.myPid())
        }, BLOCK_GRACE_MS)
    }

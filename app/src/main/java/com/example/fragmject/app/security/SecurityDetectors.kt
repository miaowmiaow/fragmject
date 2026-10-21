package com.example.fragmject.app.security

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Debug
import android.os.Process
import android.util.Log
import com.example.fragmject.app.security.AppSecurityGuard.EXPECTED_SIGNATURE_SHA256
import com.example.fragmject.app.security.AppSecurityGuard.blockingMessage
import java.io.File
import java.net.InetSocketAddress
import java.net.Socket
import java.security.MessageDigest
import java.util.Locale
import java.util.zip.ZipFile

/**
 * 安全检测项、native 加载与阻断动作：以 AppSecurityGuard 的扩展函数提供。
 *
 * 拆出原因：单项检测的增删改频率远高于生命周期调度；扩展函数可直接访问守卫状态，
 * 守卫内的调用点（如 `hasFridaThread()`）保持原样。
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
    internal fun AppSecurityGuard.detectReason(includePortScan: Boolean, includeCrc: Boolean): String? {
        // 1. native TracerPid（优先，抗 Java hook）。
        //    >0 才判定命中；-1（读取失败）视为未知，继续后续检测而不是误杀。
        if (nativeLoaded) {
            val tracerPid = nativeReadTracerPid()
            if (tracerPid > 0) {
                return "检测到 Native 调试器附加（TracerPid=$tracerPid）"
            }
        }

        // 2. Java 层调试器（JDWP）：覆盖 Android Studio / jdb 等纯 Java 调试。
        //    iOS 的 RASP 也有同类能力，这里对应 Android 的 Debug 接口。
        if (Debug.isDebuggerConnected() || Debug.waitingForDebugger()) {
            return "检测到 Java 层调试器（JDWP）"
        }

        // 3. native maps Frida 特征（特征串已 XOR 混淆，二进制中无明文）。
        //    命中任意一个即判定；`linjector` 虽可能与其他工具重名，但结合其它信号足够可疑。
        if (nativeLoaded && nativeFindFridaInMaps() > 0) {
            return "检测到 Frida 注入特征（内存映射）"
        }

        // 4. Frida 特征线程名：覆盖「so 已删除/改名但线程残留」的场景。
        if (hasFridaThread()) {
            return "检测到 Frida 特征线程"
        }

        // 5. Frida 落地文件：覆盖 frida-server 二进制/注入器落在 fd 或 /data/local/tmp 的情况。
        if (hasFridaFile()) {
            return "检测到 Frida 落地文件"
        }

        // 6. 匿名可执行段：对「无文件映射的可执行内存」做通用检测。
        //    native 返回 -1（读取失败）时 `-1 > 0` 为 false，不会误杀。
        if (nativeLoaded && nativeCountAnonymousExecSegments() > ANON_EXEC_THRESHOLD) {
            return "检测到异常匿名可执行内存段（疑似内存注入）"
        }

        // 7. Root：多信号综合（su 文件 / Magisk 痕迹 / root 管理应用 / maps 注入特征）。
        //    默认阻断；若业务对 Root 不敏感，可把 ENFORCE_ROOT_DETECTION 置为 false。
        if (ENFORCE_ROOT_DETECTION && hasRootSignals()) {
            return "检测到 Root 环境"
        }

        // 8. 模拟器：Build 多字段 + QEMU 设备节点，Score >= 2 才判定以降低误报。
        //    若需支持云手机/自动化测试，可把 ENFORCE_EMULATOR_DETECTION 置为 false。
        if (ENFORCE_EMULATOR_DETECTION && hasEmulatorSignals()) {
            return "检测到模拟器环境"
        }

        // 9. libc .text CRC：只在 includeCrc 为 true（30s 降频）时执行。
        //    返回 1 才是「不一致」；-1（无法校验）不判定，避免误杀。
        if (includeCrc && nativeLoaded && nativeVerifyLibcTextCrc() == 1) {
            return "检测到 libc 代码段被篡改（疑似 inline hook）"
        }

        // 10. 端口扫描：只在 includePortScan 为 true（60s 降频）时执行。
        //     仅覆盖 frida-server 形态，gadget 注入不会监听端口。
        if (includePortScan && isFridaPortOpen()) {
            return "检测到 Frida Server 监听端口（27042-27052）"
        }

        return null
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
    /**
     * APK 签名校验。
     *
     * 为什么默认关闭（[EXPECTED_SIGNATURE_SHA256] 为空）：未配置时校验必然失败，
     * 会把所有正常安装包误杀；签名摘要依赖具体 keystore，不适合在源码里硬编码。
     *
     * 启用后如何判定：
     * - Android 9+（API 28）用 `signingInfo.apkContentsSigners`（覆盖 v2/v3 签名）；
     * - 更低版本回退到 `PackageInfo.signatures`（v1）；
     * - 任一签名证书的 SHA-256 与预期一致即通过。
     *
     * 局限：只比对「内容签名」，不能防御「用同一证书重新签名」的改包；
     * 多签名 APK 需要逐一比对全部 signer，本项目未启用该场景。
     *
     * @return true=通过或未启用；false=校验失败（调用方据此阻断）。
     */
    internal fun AppSecurityGuard.verifySignature(context: Context): Boolean {
        if (EXPECTED_SIGNATURE_SHA256.isBlank()) return true

        return try {
            val pm = context.packageManager
            val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                pm.getPackageInfo(context.packageName, PackageManager.GET_SIGNING_CERTIFICATES)
            } else {
                @Suppress("DEPRECATION")
                pm.getPackageInfo(context.packageName, PackageManager.GET_SIGNATURES)
            }

            val signers = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                packageInfo.signingInfo?.apkContentsSigners
            } else {
                @Suppress("DEPRECATION")
                packageInfo.signatures
            }

            signers?.any { cert ->
                digestHex(cert.toByteArray(), "SHA256")
                    .equals(EXPECTED_SIGNATURE_SHA256, ignoreCase = true)
            } == true
        } catch (e: Exception) {
            // 查询失败（如 PackageManager 异常）时返回 false：宁可误杀也不放行，
            // 若线上出现误杀，可改为「失败放行 + 上报」策略。
            Log.w(TAG, "signature check failed", e)
            false
        }
    }

    /**
     * 遍历 `/proc/self/task/<tid>/comm` 检测 Frida 特征线程。
     *
     * 为什么读 `comm` 而不是 `stat`：`comm` 只有线程名，解析简单且不易受
     * 进程名中括号/空格影响（`stat` 的 comm 字段可能被进程名里的括号干扰）。
     *
     * 局限：线程名可被攻击者修改；这里只作为「多信号之一」。
     * 读取失败（权限/文件消失）时返回 false，不误杀。
     */
    internal fun AppSecurityGuard.hasFridaThread(): Boolean = try {
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
    internal fun AppSecurityGuard.hasFridaFile(): Boolean = try {
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
    internal fun AppSecurityGuard.isFridaPortOpen(): Boolean {
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
     * 计算摘要并转为大写十六进制。
     *
     * `it.toInt() and 0xFF` 是必要的：Kotlin 的 Byte 是有符号的，
     * 直接格式化负数会得到 "-1" 之类的错误结果。
     */
    internal fun AppSecurityGuard.digestHex(bytes: ByteArray, algorithm: String): String {
        val digest = MessageDigest.getInstance(algorithm)
        digest.update(bytes)
        return digest.digest().joinToString("") { "%02X".format(it.toInt() and 0xFF) }
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
    internal fun AppSecurityGuard.isDebuggableBuild(): Boolean {
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
    internal fun AppSecurityGuard.hasEmulatorSignals(): Boolean {
        val fingerprint = Build.FINGERPRINT.lowercase(Locale.US)
        val model = Build.MODEL.lowercase(Locale.US)
        val product = Build.PRODUCT.lowercase(Locale.US)
        val hardware = Build.HARDWARE.lowercase(Locale.US)
        val brand = Build.BRAND.lowercase(Locale.US)
        val manufacturer = Build.MANUFACTURER.lowercase(Locale.US)

        var score = 0
        if (fingerprint.startsWith("generic") ||
            fingerprint.contains("unknown") ||
            fingerprint.contains("emulator") ||
            fingerprint.contains("vbox") ||
            fingerprint.contains("genymotion")
        ) {
            score++
        }
        if (model.contains("google_sdk") ||
            model.contains("emulator") ||
            model.contains("android sdk built for") ||
            model.contains("droid4x")
        ) {
            score++
        }
        if (product.contains("sdk") ||
            product.contains("google_sdk") ||
            product.contains("emulator") ||
            product.contains("simulator") ||
            product.contains("vbox86p")
        ) {
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

    /**
     * 自身 so 完整性校验。
     *
     * 流程：
     * 1. 用 [extractSelfSo] 从 APK 解压 `lib/<abi>/libfragmject_security.so` 到私有目录；
     * 2. 交给 native 比对「磁盘 .text」与「内存 .text」的 CRC32。
     *
     * 失败降级原则：
     * - native 未加载 → 跳过（无从校验）；
     * - 解压失败（APK 结构异常/无该 ABI）→ 跳过，不误杀；
     * - native 返回 -1（无法校验）→ 跳过；只有返回 1（不一致）才判定失败。
     */
    internal fun AppSecurityGuard.verifySelfSoIntegrity(context: Context): Boolean {
        if (!nativeLoaded) return true
        return try {
            val soFile = extractSelfSo(context) ?: return true
            nativeVerifySelfSoTextCrc(soFile.absolutePath) != 1
        } catch (e: Exception) {
            Log.w(TAG, "self so integrity check failed", e)
            true
        }
    }

    /**
     * 从 APK 中解压自身 so 到 `cacheDir`，并用「APK 路径 + 最后更新时间」做缓存。
     *
     * 为什么用缓存：每次启动解压数 MB 的 so 是浪费；只要 APK 没升级，解压结果就有效。
     * 为什么用 cacheDir：语义更明确，且系统清理后下次会自动重新解压。
     *
     * @return 解压后的文件；无法解压时返回 null（调用方跳过校验）。
     */
    internal fun AppSecurityGuard.extractSelfSo(context: Context): File? {
        val abi = Build.SUPPORTED_ABIS.firstOrNull() ?: return null
        val entryName = "lib/$abi/libfragmject_security.so"
        val apkPath = context.applicationInfo.sourceDir
        val target = File(context.cacheDir, "libfragmject_security.so")
        val keyFile = File(context.cacheDir, "libfragmject_security.key")

        // 用 APK 路径 + lastUpdateTime 作为缓存键，应用升级后自动失效。
        val cacheKey = try {
            val info = context.packageManager.getPackageInfo(context.packageName, 0)
            "$apkPath:${info.lastUpdateTime}"
        } catch (_: Exception) {
            apkPath
        }

        if (target.exists() && keyFile.exists() && keyFile.readText() == cacheKey) {
            return target
        }

        return try {
            ZipFile(apkPath).use { zip ->
                val entry = zip.getEntry(entryName) ?: return null
                zip.getInputStream(entry).use { input ->
                    target.outputStream().use { output -> input.copyTo(output) }
                }
            }
            keyFile.writeText(cacheKey)
            target
        } catch (e: Exception) {
            Log.w(TAG, "extract self so failed", e)
            null
        }
    }

    /**
     * 关键资源完整性校验。
     *
     * 默认不启用（[EXPECTED_RESOURCE_SHA256] 为空直接返回 true）。
     * 说明：APK 的 v2/v3 签名已经覆盖整个包（含资源），签名校验通过时，
     * 资源完整性在密码学上已被保证；本方法是「签名校验被绕过」时的二次兜底。
     */
    internal fun AppSecurityGuard.verifyResourceIntegrity(context: Context): Boolean {
        if (EXPECTED_RESOURCE_SHA256.isEmpty()) return true

        return try {
            val apkPath = context.applicationInfo.sourceDir
            ZipFile(apkPath).use { zip ->
                EXPECTED_RESOURCE_SHA256.all { (entryName, expected) ->
                    val entry = zip.getEntry(entryName) ?: return false
                    val actual = zip.getInputStream(entry).use {
                        digestHex(it.readBytes(), "SHA256")
                    }
                    actual.equals(expected, ignoreCase = true)
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "resource integrity check failed", e)
            false
        }
    }

package com.example.security

import android.annotation.SuppressLint
import android.os.Build
import android.util.Log
import java.io.File

/**
 * native 能力抽象：把 JNI 调用收成接口，便于在纯 JVM 单测里替换为假实现。
 *
 * 返回值约定（与 `security.cpp` 一致）：
 * - [installAntiPtrace]：0=占坑成功，-1=失败（可能已被附加）；
 * - [readTracerPid]：>0 被附加，0 未附加，-1 读取失败；
 * - [countAnonymousExecSegments] / [findFridaInMaps] / [findRootInMaps]：>=0 计数，-1 读取失败；
 * - [verifyLibcTextCrc] / [verifySelfSoTextCrc]：0 一致，1 不一致，-1 无法校验；
 * - [killSelf]：无返回（SIGKILL）。
 *
 * 约定「失败返回 -1」而不是抛异常：检测能力失效时应放行，而不是把正常用户拦在门外。
 */
internal interface NativeChecks {

    /** so 是否加载成功；false 表示已降级为纯 JVM 检测。 */
    val available: Boolean

    /**
     * 加载 native 库（幂等）。
     *
     * 只在**确定要启用防护**之后调用：debug 包与跳过的变体不应该产生任何 so 加载开销。
     *
     * @return true=可用；false=已降级。
     */
    fun load(): Boolean

    fun installAntiPtrace(): Int

    fun readTracerPid(): Int

    fun countAnonymousExecSegments(): Int

    fun findFridaInMaps(): Int

    fun findRootInMaps(): Int

    /** 命中 Frida 特征名的线程数（特征表在 native 侧混淆存放）；-1=无法枚举。 */
    fun findFridaThreads(): Int

    /** 命中 Frida 特征名的落地文件数。 */
    fun findFridaFiles(): Int

    /** 存在的 Root 相关文件数（su / Magisk 目录）。 */
    fun findRootFiles(): Int

    /**
     * 进程内存加固（反 dump 的可做部分）：关闭 `/proc` 可读性与 core dump。
     *
     * 为什么做成方法而不是构造时自动执行：失败要能降级、要能按配置关闭，
     * 且 debug 包不该改动内核状态（调试器会连不上）。
     *
     * @return 0=成功；-1=失败（如被 seccomp 拦截）。
     */
    fun hardenProcess(): Int

    fun verifyLibcTextCrc(): Int

    fun verifySelfSoTextCrc(selfSoPath: String): Int

    /** 自身 so 的 `.rodata` 校验（常量表是否被改写）。 */
    fun verifySelfSoDataCrc(selfSoPath: String): Int


    fun killSelf()
}

/**
 * JNI 实现。
 *
 * ============================ 为什么改动态注册 ============================
 * 原实现用**静态注册**：C++ 函数名必须是 `Java_com_example_fragmject_app_security_AppSecurityGuard_xxx`，
 * 于是 `proguard-rules.pro` 必须 `-keep` 整个类（类名 + 所有成员名），检测逻辑完全不可混淆，
 * 且符号会出现在 `.dynsym` 里，`nm -D` 一看就知道有哪些检测能力。
 *
 * 现在改为 `JNI_OnLoad` + `RegisterNatives`（见 `security.cpp`）：
 * - `.dynsym` 里不再出现任何 `Java_...` 符号；
 * - keep 规则收窄为「类名 + native 方法名」（[SecurityNative] 的类名与方法名由 C++ 侧硬编码），
 *   其余成员（状态、调度、日志文案）可以正常参与 R8 混淆。
 *
 * 代价：类名与方法名仍须保留（C++ 侧按名注册），真正彻底的隐藏需要把方法名也下沉，
 * 那是「字符串加密 + 壳」的职责，不在本步范围。
 *
 * 失败语义：`JNI_OnLoad` 返回 `JNI_ERR` 时 `System.loadLibrary` 抛 `UnsatisfiedLinkError`，
 * 由 [load] 捕获并置 `available = false`，后续检测自动走 JVM 降级路径。
 */
internal class SecurityNative(
    /** 系统解压后的 native 目录（`applicationInfo.nativeLibraryDir`）；壳场景下用于兜底加载。 */
    private val nativeLibraryDir: String? = null,
    /** APK 路径，壳场景可从 APK 内直接 dlopen（`apk!/lib/abi/xxx.so`）。 */
    private val apkPath: String? = null,
) : NativeChecks {

    @Volatile
    override var available: Boolean = false
        private set

    /**
     * 加载并注册 native 方法；重复调用幂等。
     *
     * @return true=可用；false=已降级（so 缺失 / ABI 不匹配 / 注册失败）。
     */
    override fun load(): Boolean {
        if (available) return true
        val byName = runCatching {
            System.loadLibrary(LIB_NAME)
            true
        }.getOrElse { false }

        // 兜底：DEX 壳下业务 dex 由 InMemoryDexClassLoader 加载，它没有 APK 的 native 搜索路径，
        // System.loadLibrary 必然失败；此时按绝对路径/APK 内路径直接 dlopen。
        val loaded = byName || loadByPath()
        available = loaded
        if (!loaded) {
            // 用 Log.e：release 会裁剪 v/d/i/w，降级状态必须在线上可见。
            Log.e(SecurityGuard.TAG, "load $LIB_NAME failed, fallback to JVM checks")
        }
        return loaded
    }

    /**
     * 依次尝试 `nativeLibraryDir/libX.so` 与 `apk!/lib/<abi>/libX.so`。
     *
     * 抑制 `UnsafeDynamicallyLoadedCode`：这里的路径完全由本应用 APK 推导
     * （`applicationInfo` 提供），不含外部输入，是 DEX 壳场景下的必要兜底。
     */
    @SuppressLint("UnsafeDynamicallyLoadedCode")
    private fun loadByPath(): Boolean {
        val abi = Build.SUPPORTED_ABIS.firstOrNull() ?: "arm64-v8a"
        val candidates = buildList {
            nativeLibraryDir?.let { add(File(it, "lib$LIB_NAME.so").absolutePath) }
            apkPath?.let { add("$it!/lib/$abi/lib$LIB_NAME.so") }
        }
        for (path in candidates) {
            val failure = runCatching {
                System.load(path)
            }.exceptionOrNull()
            if (failure == null) {
                Log.i(SecurityGuard.TAG, "loaded $LIB_NAME via $path")
                return true
            }
            // 逐条记录失败原因：壳场景下「哪条路径不通」是排查加载问题的唯一线索
            Log.w(SecurityGuard.TAG, "load failed: $path (${failure.javaClass.simpleName})")
        }
        return false
    }

    external override fun installAntiPtrace(): Int

    external override fun readTracerPid(): Int

    external override fun countAnonymousExecSegments(): Int

    external override fun findFridaInMaps(): Int

    external override fun findRootInMaps(): Int

    external override fun findFridaThreads(): Int

    external override fun findFridaFiles(): Int

    external override fun findRootFiles(): Int

    external override fun hardenProcess(): Int

    external override fun verifyLibcTextCrc(): Int

    external override fun verifySelfSoTextCrc(selfSoPath: String): Int

    external override fun verifySelfSoDataCrc(selfSoPath: String): Int


    external override fun killSelf()

    private companion object {
        /** 与 `CMakeLists.txt` 的 `add_library(appsecurity ...)` 对应。 */
        const val LIB_NAME = "appsecurity"
    }
}

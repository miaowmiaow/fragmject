package com.example.security

import android.content.Context

/**
 * 应用运行时安全防护入口。
 *
 * ============================ 用法 ============================
 * 在 `Application.attachBaseContext` 中调用（越早越好，早于 onCreate / ContentProvider，
 * 以压缩「调试器在启动窗口内附加」的时间差）：
 *
 * ```
 * override fun attachBaseContext(base: Context) {
 *     super.attachBaseContext(base)
 *     SecurityGuard.install(this, SecurityConfig(buildType = BuildConfig.BUILD_TYPE))
 * }
 * ```
 *
 * 可调试包（`FLAG_DEBUGGABLE`）与 [SecurityConfig.skipBuildTypes] 中的构建类型会自动跳过；
 * 是否跳过由引擎判定，调用方无需关心。
 *
 * ============================ 能力概览 ============================
 * 检测：Java/JDWP 调试、native TracerPid、Frida（maps / 线程名 / 落地文件 / 端口扫描）、
 * 匿名可执行段、Root（su/Magisk/root 管理应用/maps 注入）、模拟器（Build 特征 + QEMU 设备）、
 * libc `.text` CRC、APK 签名、自身 so `.text` CRC、关键资源摘要。
 *
 * 阻断：命中后置标记（供 UI 读取），等待 [SecurityConfig.blockGraceMs] 后由 native `SIGKILL`
 * 终止进程；Java 层被 hook 也无法取消。
 *
 * ============================ 安全边界（务必知晓）============================
 * 这是「用户态静态特征检测」，不是绝对防护：
 * - /proc 内容可被 hook/伪造；ptrace 可被更底层注入绕过；
 * - CRC32 不是密码学哈希；更有针对性的是直接 patch 检测点；
 * - Root/模拟器检测存在误报（定制 ROM、开发者设备），可通过配置关闭；
 * - 它提高的是自动化扫描与低水平逆向的成本，不能替代服务端鉴权与密钥管理。
 *
 * 需要服务端配合的能力（Play Integrity、Key Attestation、设备画像）不在本组件范围。
 */
object SecurityGuard {

    /**
     * 日志 TAG。
     *
     * 历史值曾是 `AppSecurityGuard`（改造前的类名）；库化后统一为 `AppSecurity`，
     * 与包名/模块名一致。线上日志过滤条件需同步更新（`adb logcat -s AppSecurity`）。
     */
    const val TAG = "AppSecurity"

    /** 未命中时的兜底提示：让调用方无需判空。 */
    private const val DEFAULT_BLOCK_MESSAGE = "检测到异常运行环境，应用即将关闭"

    /** 单进程单实例；并发 install 由 [lock] 保护。 */
    private val lock = Any()

    @Volatile
    private var engine: SecurityEngine? = null

    /**
     * 安装并启动防护；重复调用幂等。
     *
     * @param context 任意 Context（内部只保留 applicationContext），建议直接传 Application。
     * @param config  策略与阈值；默认值等价于历史行为。
     */
    @JvmStatic
    @JvmOverloads
    fun install(context: Context, config: SecurityConfig = SecurityConfig()) {
        if (!config.enabled) return
        if (engine != null) return

        synchronized(lock) {
            if (engine != null) return
            val appContext = context.applicationContext ?: context
            val facts = AndroidSecurityFacts(appContext)
            // 这里刻意**不**加载 native：debug 包与被跳过的变体不应产生 so 加载开销，
            // 加载动作由 SecurityEngine.start() 在跳过判定之后执行。
            engine = SecurityEngine(
                config = config,
                facts = facts,
                // 壳场景下 System.loadLibrary 找不到 APK 内的 so，需要目录/APK 路径兜底
                native = SecurityNative(
                    nativeLibraryDir = appContext.applicationInfo.nativeLibraryDir,
                    apkPath = appContext.applicationInfo.sourceDir,
                ),
            ).also { it.start(context) }
        }
    }

    /**
     * 卸载防护：停止轮询、释放检测线程，并清空单例。
     *
     * 使用场景：宿主在测试里需要「干净重启」、或做多用户切换时的显式清理。
     * 注意：已注册的 Activity 生命周期回调无法反注册（Platform 限制），
     * 但引擎已被清空，回调变为空操作；重复 [install] 会重新注册，不影响正确性。
     */
    @JvmStatic
    fun uninstall() {
        synchronized(lock) {
            engine?.stop()
            engine = null
        }
    }

    /**
     * 是否已命中阻断（供 UI 查询并展示提示）。
     *
     * 该标记只用于 UI 展示，不是安全决策的唯一依据：
     * 即使被篡改，终止进程仍由 native 侧执行。
     */
    @JvmStatic
    fun isBlocked(): Boolean = engine?.blocked == true

    /** 阻断提示文案；未命中时返回兜底文案。 */
    @JvmStatic
    fun blockingMessage(): String = engine?.blockReason ?: DEFAULT_BLOCK_MESSAGE

    /**
     * 最近的安全事件（最多 20 条，时间正序，含 WARN 与 CRITICAL）。
     *
     * 用途：用户反馈「打开就闪退」时，让他在设置页/反馈入口导出这段文本，
     * 就能看到命中的是哪一项（release 下 `Log.w` 会被裁剪，日志不可指望）。
     */
    @JvmStatic
    fun securityEvents(): List<String> = engine?.recentEvents().orEmpty()

    /**
     * 最近的安全事件（结构化版本，带稳定 `code`，适合直接上报）。
     *
     * 与 [securityEvents] 的区别：那个是给人看的字符串，这个是给系统对接的；
     * 上报口径应以 `code` 为准（文案会改）。
     */
    @JvmStatic
    fun events(): List<SecurityEvent> = engine?.events().orEmpty()

    /**
     * 是否已降级为「只上报、不阻断」。
     *
     * 触发条件：检测过程连续异常达到 [SecurityConfig.maxConsecutiveFailures]。
     * 宿主可据此在设置页/工单里说明「本次保护能力降级」，便于排查误杀与漏杀。
     */
    @JvmStatic
    fun isDegraded(): Boolean = engine?.degraded == true

    /** 进入前台：立即检测一次并重置退避（由生命周期回调调用）。 */
    @JvmStatic
    fun onForeground() {
        engine?.onForeground()
    }

    /** 进入后台：暂停轮询（由生命周期回调调用）。 */
    @JvmStatic
    fun onBackground() {
        engine?.onBackground()
    }
}

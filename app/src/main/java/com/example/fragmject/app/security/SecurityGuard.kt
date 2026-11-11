package com.example.fragmject.app.security

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

    /** 日志 TAG：沿用历史值，便于线上日志按既有过滤条件检索。 */
    const val TAG = "AppSecurityGuard"

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
            engine = SecurityEngine(config, facts, SecurityNative()).also { it.start(context) }
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

package com.example.fragmject.app.security

import android.app.Application
import android.content.Context
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.util.Log
import com.example.fragmject.app.BuildConfig

/**
 * 应用运行时安全防护。
 *
 * ============================ 设计来源 ============================
 * 本类结合了两套方案的长处：
 *
 * 1. 检测广度、调度与工程性：
 *    - Java/JDWP 调试、Frida 线程名、Frida 落地文件、Frida Server 端口扫描；
 *    - Root（su/Magisk/Zygisk/Riru/LSPosed 注入）、模拟器（Build 特征/QEMU 设备）；
 *    - APK 签名校验、自身 so `.text` 完整性、关键资源完整性；
 *    - 指数退避轮询（1s → 60s），兼顾及时性与省电；
 *    - 前后台联动：回前台立即全量检测，后台暂停轮询；
 *    - native 加载失败自动降级为纯 JVM 检测，不阻断启动。
 *
 * 2. 关键判定与隐蔽性：
 *    - TracerPid、匿名可执行段、libc/自身 so 的 `.text` CRC32 下沉 native，
 *      避免 Java 层被直接 hook；
 *    - 特征字符串在 native 侧编译期 XOR 混淆，二进制中无明文；
 *    - 命中后由 native `SIGKILL` 终止，不依赖 Java 分支。
 *
 * ============================ 检测链路 ============================
 * ```
 * Application.attachBaseContext
 *   └─ start()
 *        ├─ (Release) native ptrace 占坑         —— 主动阻断调试器 attach
 *        ├─ 首轮同步轻量检测                      —— 覆盖「启动前已被附加」的窗口
 *        ├─ 异步完整性校验（仅一次）              —— 签名 / 自身 so / 关键资源
 *        ├─ 注册 Activity 生命周期回调            —— 前后台联动
 *        └─ 后台线程轮询（指数退避 1s→60s）
 *             ├─ native: TracerPid
 *             ├─ Java  : Debug.isDebuggerConnected()
 *             ├─ native: Frida maps 特征
 *             ├─ Java  : Frida 线程名 / 落地文件
 *             ├─ native: 匿名可执行段
 *             ├─ native: Root maps 特征（zygisk/riru/...）
 *             ├─ Java  : Root 文件/包名、模拟器特征
 *             ├─ native: libc .text CRC（30s 降频）
 *             └─ Java  : Frida 端口扫描（60s 降频）
 * ```
 *
 * ============================ 阻断策略 ============================
 * 命中后先置 [isBlocked] 标记（供 UI 读取并提示），等待 [SecurityPolicy.BLOCK_GRACE_MS] 后终止进程。
 * 宽限窗口只用于「让用户看到提示」，不会削弱阻断效果：
 * 终止动作优先由 native `SIGKILL` 完成，即使 Java 层被 hook 也无法取消。
 *
 * ============================ 服务端方案（本项目未接入，仅作说明）============================
 * 以下能力需要服务端配合，客户端无法独立完成，故本类**不实现**，仅在此记录后续方向：
 *
 * - Play Integrity API：客户端请求 token，服务端用 Google 公钥校验
 *   `MEETS_BASIC_INTEGRITY / MEETS_DEVICE_INTEGRITY / MEETS_STRONG_INTEGRITY`，
 *   再决定放行、降级或要求二次认证。它比自研检测更可靠（硬件支持、Google 维护），
 *   但必须由服务端消费 verdict，客户端单方面判断没有意义。
 * - Android Key Attestation：用硬件密钥证明 bootloader 锁状态与系统完整性，
 *   同样需要服务端校验证书链；注意 2026 年 2 月 Android 平台根证书轮换。
 * - 设备画像与风控：服务端累积设备指纹、登录地、行为，做风险分级；
 *   客户端只上报信号，不做最终裁决。
 *
 * 建议接入位置：登录、支付、授权校验等高价值链路，并采用**风险分级**而非一律阻断，
 * 避免安全服务不可用时中断核心业务。
 *
 * ============================ 安全边界（务必知晓）============================
 * 这是「用户态静态特征检测」，不是绝对防护：
 * - /proc 内容可被 hook/伪造；ptrace 可被更底层注入绕过；
 * - CRC32 不是密码学哈希；更有针对性的是直接 patch 检测点；
 * - Root/模拟器检测存在误报（定制 ROM、开发者设备），可通过开关关闭阻断；
 * - 自身 so 完整性只覆盖 `.text`，不覆盖 `.data/.got`（PLT/GOT hook）；
 * - 它提高的是自动化扫描与低水平逆向的成本，不能替代服务端鉴权与密钥管理。
 *
 * ============================ 使用方式 ============================
 * 在 [Application.attachBaseContext] 中调用 [start]（越早越好，早于 onCreate /
 * ContentProvider，以压缩「调试器在启动窗口内附加」的时间差）。Debug 包会自动跳过；
 * `nonMinifiedRelease`（Baseline Profile 采集变体）也一并跳过，原因见 [start] 内注释。
 */
object AppSecurityGuard {

    const val TAG = "AppSecurityGuard"

    // ========================================================================
    // native 能力
    //
    // 这些方法在 security.cpp 中以「静态注册」实现，函数名与这里一一对应。
    // 返回值约定：
    //   nativeInstallAntiPtrace         0=占坑成功，-1=失败
    //   nativeReadTracerPid            >0=被附加，0=未附加，-1=读取失败
    //   nativeCountAnonymousExecSegments >=0=段数，-1=读取失败
    //   nativeVerifyLibcTextCrc         0=一致，1=不一致，-1=无法校验
    //   nativeFindFridaInMaps          >=0=命中数，-1=读取失败
    //   nativeFindRootInMaps           >=0=命中数，-1=读取失败
    //   nativeVerifySelfSoTextCrc       0=一致，1=不一致，-1=无法校验
    //   nativeKillSelf                  无返回（SIGKILL）
    // ========================================================================

    external fun nativeInstallAntiPtrace(): Int
    external fun nativeReadTracerPid(): Int
    external fun nativeCountAnonymousExecSegments(): Int
    external fun nativeVerifyLibcTextCrc(): Int
    external fun nativeFindFridaInMaps(): Int
    external fun nativeFindRootInMaps(): Int
    external fun nativeVerifySelfSoTextCrc(selfSoPath: String): Int
    external fun nativeKillSelf()

    @Volatile
    var nativeLoaded = false


    // ========================================================================
    // 运行时状态
    //
    // 全部使用 @Volatile：写操作发生在后台检测线程/签名线程，
    // 读操作可能发生在主线程（isBlocked）或其它检测轮次，需要保证可见性。
    // ========================================================================

    /** 是否已经启动，防止重复注册轮询与生命周期回调。 */
    @Volatile
    private var started = false

    /** 是否已命中阻断。置位后不再重复处理，直到进程被终止。 */
    @Volatile
    var blocked = false

    /** 命中原因，用于 UI 提示与日志排查。 */
    @Volatile
    var blockReason: String? = null

    /** 当前轮询间隔，随退避机制动态调整。 */
    @Volatile
    private var currentIntervalMs = SecurityPolicy.INITIAL_INTERVAL_MS

    /** 距离上次退避已连续干净的轮数。 */
    @Volatile
    private var consecutiveCleanRounds = 0

    /** 上次端口扫描时间戳，用于按时间降频。 */
    @Volatile
    private var lastPortScanMs = 0L

    /** 上次 CRC 校验时间戳，用于按时间降频。 */
    @Volatile
    private var lastCrcCheckMs = 0L

    /**
     * 应用 Context。
     *
     * 用于 Root 检测中的「已安装 root 管理应用」查询（需要 PackageManager）。
     * 只持有 applicationContext，避免泄漏 Activity；null 表示尚未启动。
     */
    @Volatile
    var appContext: Context? = null

    /**
     * 后台检测线程。
     *
     * 为什么用 HandlerThread 而不是裸 Thread：
     * - 需要「延迟执行 + 可取消重排」的轮询语义，Handler 的 postDelayed/removeCallbacks
     *   正好匹配；
     * - onBackground 时要能立即取消下一轮，裸 Thread + sleep 做不到这一点。
     *
     * 为什么 lazy：如果 Debug 包或检测从未启动，不创建线程、不产生任何开销。
     */
    val handler by lazy {
        val thread = HandlerThread("AppSec-Check")
        thread.start()
        Handler(thread.looper)
    }

    /**
     * 启动检测。应在 [Application.attachBaseContext] 中调用。
     *
     * 执行顺序及原因：
     * 1. Debug 包直接返回：避免影响本地开发（断点、Frida 调试自身）。
     * 2. native 加载 + ptrace 占坑：占坑必须早于任何可能被附加的时机。
     * 3. 首轮**同步**全量检测：在进入应用主流程前，先判断「启动前是否已被附加/注入」；
     *    这里同步执行是为了在 UI 出现前就完成判定，避免先展示界面再闪退。
     * 4. 异步签名校验：签名在进程生命周期内不变，只需一次；放后台线程避免拖慢启动。
     * 5. 注册生命周期回调 + 启动轮询：覆盖运行期新附加的情况。
     *
     * 注意：首轮检测包含 CRC 与端口扫描，会比后续轮次重一些（几十毫秒量级），
     * 这是「尽早发现」与「启动耗时」之间的有意取舍。
     */
    @JvmStatic
    fun start(context: Context) {
        // Debug 包不启用，避免影响本地开发调试
        if (BuildConfig.DEBUG) return
        // 自动化采集变体（nonMinifiedRelease，由 androidx.baselineprofile 插件创建）关闭守卫：
        // 模拟器 ART/JIT 的匿名可执行段会命中 anon-exec 检测，进程在启动 1.2s 内被 SIGKILL，
        // 导致 Macrobenchmark 拿不到运行中的被测进程、Baseline Profile 无法采集。
        // 该变体不参与发布；debug 之外的其它变体（含 release / benchmarkRelease）仍启用守卫。
        if (BuildConfig.BUILD_TYPE == SecurityPolicy.collectionBuildType) {
            Log.i(TAG, "security guard skipped for ${SecurityPolicy.collectionBuildType} (Baseline Profile collection)")
            return
        }
        if (started) return
        started = true

        // 保存 applicationContext 供后续 Root 检测查询已安装应用。
        // 只持有 Application 级 Context，不会泄漏 Activity。
        appContext = context.applicationContext

        // start() 的契约是「主线程调用」：
        // - 唯一调用点 Application.attachBaseContext 由 ActivityThread 在主线程执行；
        // - registerActivityLifecycleCallbacks 的回调派发依赖主线程 Looper，
        //   从子线程注册属于未定义行为。
        // 这里做防御性检查：非主线程调用只记日志并跳过生命周期注册，
        // 轮询仍会启动，避免「因为误用直接崩溃」。
        val onMainThread = Looper.myLooper() == Looper.getMainLooper()
        if (!onMainThread) {
            Log.w(TAG, "start() should be called on the main thread, lifecycle link skipped")
        }

        ensureNativeLoaded()

        // 最早时机占坑 ptrace，杜绝后续调试器附加。
        // 失败不直接判定为「被调试」：部分 ROM 的 ptrace 策略会导致正常进程也失败，
        // 这里只记录日志，真正判定仍交给 TracerPid 检测。
        if (nativeLoaded && nativeInstallAntiPtrace() != 0) {
            Log.w(TAG, "ptrace self-trace failed (already attached?)")
        }

        // 首次同步检测：只做轻量项（TracerPid / Java 调试 / maps / 线程 / 落地文件 / 匿名段），
        // 均为读小文件或短遍历，主线程开销可忽略。
        //
        // 有意**不**在这里执行 CRC 与端口扫描：
        // - 端口扫描最坏要等 11 × 50ms 的超时，会明显拖慢冷启动；
        // - CRC 需要读写数 MB 数据，虽在毫秒级，但没必要占用启动主线程。
        // 两者由后台轮询的第一次执行立即补上（checkTask 中 lastXxxMs 初值为 0，
        // 首轮必然 due），所以不会漏掉「启动前已被 inline hook」的场景。
        val firstReason = detectReason(includePortScan = false, includeCrc = false)
        if (firstReason != null) {
            block(firstReason)
            return
        }

        // 完整性校验在进程生命周期内基本不变，只需做一次。
        // 全部放到后台线程：涉及 PackageManager 查询、APK 解压与摘要计算，不应占用启动主线程。
        Thread {
            if (!verifySignature(context)) {
                block("APK 签名异常，应用即将关闭")
                return@Thread
            }
            // 自身 so 完整性：检测 libfragmject_security.so 的 .text 是否被改写。
            if (!verifySelfSoIntegrity(context)) {
                block("安全组件完整性异常，应用即将关闭")
                return@Thread
            }
            // 关键资源完整性：仅在配置了基线时生效（默认空，不启用）。
            if (!verifyResourceIntegrity(context)) {
                block("关键资源完整性异常，应用即将关闭")
                return@Thread
            }
        }.start()

        // 只有拿到 Application 且在主线程时，才能安全注册 Activity 生命周期回调；
        // 其它 Context 或子线程调用时跳过前后台联动，轮询仍会常驻。
        if (context is Application && onMainThread) {
            AppSecurityLifecycle.register(context, ::onForeground, ::onBackground)
        }

        handler.removeCallbacks(checkTask)
        handler.post(checkTask)
    }

    /**
     * 进入前台：立即全量检测一次，并把退避重置为最短间隔。
     *
     * 为什么需要：用户切回前台是「调试器/Frida 新附加」的高发窗口，
     * 如果沿用后台的长间隔（可能已退避到 60s），会错过这个窗口。
     * 这里同时把 lastPortScanMs 归零，强制下一轮重做端口扫描。
     */
    @JvmStatic
    fun onForeground() {
        if (!started) return
        consecutiveCleanRounds = 0
        currentIntervalMs = SecurityPolicy.INITIAL_INTERVAL_MS
        lastPortScanMs = 0L
        handler.removeCallbacks(checkTask)
        handler.post(checkTask)
    }

    /**
     * 进入后台：暂停轮询。
     *
     * 进程在后台时用户不可见，继续高频检测只会耗电/发热；
     * 回到前台会通过 [onForeground] 立即恢复。
     */
    @JvmStatic
    fun onBackground() {
        if (!started) return
        handler.removeCallbacks(checkTask)
    }

    /**
     * 是否已命中阻断（供 UI 查询并展示提示）。
     *
     * 该标记只用于 UI 展示，不是安全决策的唯一依据：
     * 即使被篡改，终止进程仍由 native 侧执行。
     */
    @JvmStatic
    fun isBlocked(): Boolean = blocked

    /**
     * 阻断提示文案。
     *
     * 未命中时返回兜底文案，方便调用方无需判空。
     */
    @JvmStatic
    fun blockingMessage(): String = blockReason ?: "检测到异常运行环境，应用即将关闭"

    /**
     * 轮询任务。
     *
     * 每轮：
     * 1. 按时间决定本轮是否执行「重量级检测」（端口扫描 / CRC）；
     * 2. 执行轻量检测；
     * 3. 命中则 [block] 并停止轮询（进程即将被终止，不必再排下一轮）；
     * 4. 未命中则按退避策略排下一轮。
     *
     * 注意：命中后直接 return，不调用 postDelayed，避免终止前又跑一轮。
     */
    private val checkTask = object : Runnable {
        override fun run() {
            val now = System.currentTimeMillis()

            // 用「时间差」而不是「轮数」控制重量级检测频率：
            // 轮询间隔会随退避变化，按时间判断才能保证「最多每 60s / 30s 一次」。
            val portScanDue = (now - lastPortScanMs >= SecurityPolicy.PORT_SCAN_INTERVAL_MS).also {
                if (it) lastPortScanMs = now
            }
            val crcCheckDue = (now - lastCrcCheckMs >= SecurityPolicy.CRC_CHECK_INTERVAL_MS).also {
                if (it) lastCrcCheckMs = now
            }

            val reason = detectReason(includePortScan = portScanDue, includeCrc = crcCheckDue)
            if (reason != null) {
                block(reason)
                return
            }

            // 指数退避：连续干净后逐步拉长间隔（1s → 2s → 4s … → 60s 封顶）。
            // 一旦命中会重置（见 block/onForeground），保证异常时恢复高频。
            consecutiveCleanRounds++
            if (consecutiveCleanRounds >= SecurityPolicy.CLEAN_ROUNDS_TO_BACKOFF) {
                consecutiveCleanRounds = 0
                currentIntervalMs = minOf(currentIntervalMs * 2, SecurityPolicy.MAX_INTERVAL_MS)
            }
            handler.postDelayed(this, currentIntervalMs)
        }
    }

}

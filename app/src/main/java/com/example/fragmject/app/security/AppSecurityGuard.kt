package com.example.fragmject.app.security

import android.app.Activity
import android.app.Application
import android.content.Context
import android.os.Bundle
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
 * 命中后先置 [isBlocked] 标记（供 UI 读取并提示），等待 [BLOCK_GRACE_MS] 后终止进程。
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

    /**
     * Baseline Profile 采集专用变体名。
     *
     * 由 `androidx.baselineprofile` 插件创建（`nonMinifiedRelease`），不对外发布。
     * 该变体下跳过运行时检测：模拟器 ART/JIT 会产生匿名可执行段，命中
     * [ANON_EXEC_THRESHOLD] 后进程在 1.2s 内被 SIGKILL，采集必然失败。
     */
    private const val COLLECTION_BUILD_TYPE = "nonMinifiedRelease"

    /**
     * 首轮检测与「回前台」后的轮询间隔。
     *
     * 取 1s 的原因：调试器/Frida 往往在应用刚启动或切回前台时附加，
     * 这个窗口需要高频率覆盖；随后由退避机制逐步拉长，避免长期耗电。
     */
    private const val INITIAL_INTERVAL_MS = 1_000L

    /**
     * 轮询间隔上限。
     *
     * 60s 是「及时性」与「耗电」的折中：即使攻击者在应用长时间驻留后才附加，
     * 最迟也能在一分钟内发现，同时避免后台长期高频唤醒 CPU。
     */
    private const val MAX_INTERVAL_MS = 60_000L

    /**
     * 连续多少轮「未命中」后把间隔翻倍。
     *
     * 取 3 的原因：连续几轮干净通常说明当前环境稳定，可以开始退避；
     * 太小（如 1）会让间隔增长过快，漏掉「刚附加」的瞬间。
     */
    private const val CLEAN_ROUNDS_TO_BACKOFF = 3

    /**
     * 端口扫描的独立降频间隔。
     *
     * 端口扫描是重量级操作（每个端口都要建一次 socket，最坏 11 次连接），
     * 因此不能跟着 1s 的轮询节奏跑，单独限制为最多每 60s 一次。
     */
    private const val PORT_SCAN_INTERVAL_MS = 60_000L

    /**
     * libc `.text` CRC 校验的降频间隔。
     *
     * CRC 需要把 libc 的数 MB 代码段读两遍并计算，单次耗时虽在毫秒级，
     * 但没必要每轮都做；30s 一次足以发现「运行中才挂上的 inline hook」。
     */
    private const val CRC_CHECK_INTERVAL_MS = 30_000L

    /**
     * 命中后到终止进程的宽限时间。
     *
     * 仅用于让 UI 有机会展示 [blockingMessage]；终止动作本身不可取消。
     * 取值不宜过长：窗口越长，攻击者可利用的时间越多。
     */
    const val BLOCK_GRACE_MS = 1_200L

    /**
     * 匿名可执行段容忍阈值。
     *
     * 正常环境下，除 ART JIT、vdso 等白名单段外，不应存在「无对应文件却可执行」的内存，
     * 因此默认 0（发现即判定）。但不同 ROM/ART 实现可能产生少量合法匿名可执行段，
     * 若真机出现误杀，可放宽到 1~2；不要设得过宽，否则会漏掉「少量匿名段」的注入。
     */
    const val ANON_EXEC_THRESHOLD = 0

    /**
     * 预期 APK 签名 SHA-256（大写十六进制）。
     *
     * 默认留空表示**不启用**签名校验：
     * - 未配置时若强行校验，会导致所有安装包（包括自己的 Release）被误杀；
     * - 项目里同时存在 debug/release 签名，硬编码单一值也容易出错。
     *
     * 启用方式（二选一）：
     * 1. 用 `keytool -list -v -keystore <your.jks>` 取 SHA256，去掉冒号后填入；
     * 2. 由构建脚本在 release 构建时通过 BuildConfig 注入（更适合多环境）。
     *
     * 注意：只校验「当前包名的签名证书」，不能防御「用同一证书签名的改包」；
     * 多签名 APK 需要改为逐一比对，本项目未启用该场景。
     */
    const val EXPECTED_SIGNATURE_SHA256 = ""

    /**
     * 是否把 Root 作为阻断项。
     *
     * 误报来源：部分定制 ROM / 工程机自带 su，但用户并无恶意。
     * 若业务对 Root 不敏感（如普通内容类应用），可置为 false，仅保留日志。
     */
    const val ENFORCE_ROOT_DETECTION = true

    /**
     * 是否把模拟器作为阻断项。
     *
     * 误报来源：云手机、投屏、自动化测试环境。
     * 若业务需要支持云游戏/自动化，可置为 false。
     */
    const val ENFORCE_EMULATOR_DETECTION = true


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
    private var currentIntervalMs = INITIAL_INTERVAL_MS

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
        if (BuildConfig.BUILD_TYPE == COLLECTION_BUILD_TYPE) {
            Log.i(TAG, "security guard skipped for $COLLECTION_BUILD_TYPE (Baseline Profile collection)")
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
            registerLifecycle(context)
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
        currentIntervalMs = INITIAL_INTERVAL_MS
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
     * 用 Activity 生命周期统计前后台，注册前后台联动。
     *
     * 为什么不用 ProcessLifecycleOwner：需要额外引入 lifecycle-process 依赖；
     * 本项目是单 Activity 架构，用 ActivityLifecycleCallbacks 统计 started 数量
     * 已经足够，且不增加依赖。
     *
     * 判定规则：第一个 Activity onStart 视为进入前台；最后一个 onStop 视为进入后台。
     * 用计数器而不是布尔值，是为了正确处理 Activity 切换（A.stop 先于 B.start 的情况）。
     */
    private fun registerLifecycle(app: Application) {
        app.registerActivityLifecycleCallbacks(object : Application.ActivityLifecycleCallbacks {
            private var startedActivities = 0

            override fun onActivityStarted(activity: Activity) {
                if (startedActivities == 0) {
                    onForeground()
                }
                startedActivities++
            }

            override fun onActivityStopped(activity: Activity) {
                startedActivities--
                if (startedActivities <= 0) {
                    startedActivities = 0
                    onBackground()
                }
            }

            // 其余回调与前后台判定无关，保持空实现。
            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit
            override fun onActivityResumed(activity: Activity) = Unit
            override fun onActivityPaused(activity: Activity) = Unit
            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit
            override fun onActivityDestroyed(activity: Activity) = Unit
        })
    }

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
            val portScanDue = (now - lastPortScanMs >= PORT_SCAN_INTERVAL_MS).also {
                if (it) lastPortScanMs = now
            }
            val crcCheckDue = (now - lastCrcCheckMs >= CRC_CHECK_INTERVAL_MS).also {
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
            if (consecutiveCleanRounds >= CLEAN_ROUNDS_TO_BACKOFF) {
                consecutiveCleanRounds = 0
                currentIntervalMs = minOf(currentIntervalMs * 2, MAX_INTERVAL_MS)
            }
            handler.postDelayed(this, currentIntervalMs)
        }
    }

}

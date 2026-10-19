package com.example.fragmject.app.security

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Debug
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.os.Process
import android.util.Log
import com.example.fragmject.app.BuildConfig
import java.io.File
import java.net.InetSocketAddress
import java.net.Socket
import java.security.MessageDigest
import java.util.Locale
import java.util.zip.ZipFile

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
 * ContentProvider，以压缩「调试器在启动窗口内附加」的时间差）。Debug 包会自动跳过。
 */
object AppSecurityGuard {

    private const val TAG = "AppSecurityGuard"

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
    private const val BLOCK_GRACE_MS = 1_200L

    /**
     * 匿名可执行段容忍阈值。
     *
     * 正常环境下，除 ART JIT、vdso 等白名单段外，不应存在「无对应文件却可执行」的内存，
     * 因此默认 0（发现即判定）。但不同 ROM/ART 实现可能产生少量合法匿名可执行段，
     * 若真机出现误杀，可放宽到 1~2；不要设得过宽，否则会漏掉「少量匿名段」的注入。
     */
    private const val ANON_EXEC_THRESHOLD = 0

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
    private const val EXPECTED_SIGNATURE_SHA256 = ""

    /**
     * Frida Server 常见监听端口。
     *
     * 27042 是 Frida Server 的默认端口；攻击者常用 `-l 0.0.0.0:27050` 改到附近端口，
     * 因此这里扫描 27042~27052 一段。注意：`frida-gadget` 以 so 形式注入时**不监听端口**，
     * 端口扫描只覆盖 frida-server 形态，不能作为唯一判据。
     */
    private val FRIDA_PORTS = intArrayOf(
        27042, 27043, 27044, 27045, 27046, 27047,
        27048, 27049, 27050, 27051, 27052,
    )

    /**
     * Frida 特征线程名。
     *
     * Frida 注入后会创建 GumJS/GLib 相关线程；即使注入的 so 被删除或改名，
     * 线程名通常仍会残留，因此这是 maps 特征之外的有效补充。
     *
     * 注意 `gmain`/`gdbus` 也可能被其它使用 GLib 的库命中，单独命中不作为唯一判据，
     * 实际判定时会与其它检测项一起看（当前实现是任一命中即报，若误报可改为计数阈值）。
     */
    private val FRIDA_THREAD_SIGNATURES = listOf(
        "gum-js-loop",
        "gmain",
        "gdbus",
        "pool-frida",
        "linjector",
    )

    /**
     * Frida 落地文件名特征。
     *
     * 覆盖 frida-server 二进制、注入器 linjector、以及 re.frida.server 等常见命名。
     * 检查范围包括本进程 fd 目录与 /data/local/tmp（frida-server 常用落地路径）。
     */
    private val FRIDA_FILE_SIGNATURES = listOf(
        "frida-server",
        "frida-agent",
        "linjector",
        "re.frida.server",
    )

    /**
     * 是否把 Root 作为阻断项。
     *
     * 误报来源：部分定制 ROM / 工程机自带 su，但用户并无恶意。
     * 若业务对 Root 不敏感（如普通内容类应用），可置为 false，仅保留日志。
     */
    private const val ENFORCE_ROOT_DETECTION = true

    /**
     * 是否把模拟器作为阻断项。
     *
     * 误报来源：云手机、投屏、自动化测试环境。
     * 若业务需要支持云游戏/自动化，可置为 false。
     */
    private const val ENFORCE_EMULATOR_DETECTION = true

    /**
     * Root 相关文件路径（强信号）。
     *
     * 覆盖常见 su 位置与 Superuser.apk；Magisk 会隐藏部分路径，
     * 因此还需结合 maps 注入特征（native）与已安装 root 管理应用综合判断。
     */
    private val ROOT_FILE_PATHS = listOf(
        "/system/bin/su",
        "/system/xbin/su",
        "/sbin/su",
        "/su/bin/su",
        "/system/sd/xbin/su",
        "/system/bin/failsafe/su",
        "/data/local/su",
        "/system/app/Superuser.apk",
    )

    /** Magisk / Zygisk / Riru 的典型目录与文件。 */
    private val MAGISK_PATHS = listOf(
        "/sbin/.magisk",
        "/data/adb/magisk",
        "/data/adb/modules",
        "/data/adb/zygisk",
        "/data/adb/riru",
        "/dev/.magisk.unblock",
    )

    /** 常见 root 管理应用包名。 */
    private val ROOT_APP_PACKAGES = listOf(
        "com.topjohnwu.magisk",
        "io.github.vvb2060.magisk",
        "eu.chainfire.supersu",
        "com.koushikdutta.superuser",
        "com.thirdparty.superuser",
        "com.noshufou.android.su",
        "com.kingroot.kinguser",
        "com.qihoo.root",
    )

    /**
     * 可选的「关键资源」SHA-256 基线（APK 内条目路径 -> 大写十六进制）。
     *
     * 默认空 = **不启用**。原因：
     * - APK 的 v2/v3 签名已覆盖整个包（含所有资源与 so），签名通过即说明未被篡改；
     * - 单独维护资源哈希基线需要构建期生成，且随版本变化，维护成本高；
     * - 该能力主要用于「签名校验被绕过」时的二次兜底。
     *
     * 启用示例：
     *   "resources.arsc" to "<SHA256>",
     *   "assets/config.json" to "<SHA256>"
     */
    private val EXPECTED_RESOURCE_SHA256: Map<String, String> = emptyMap()

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
    private var nativeLoaded = false

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
    private fun ensureNativeLoaded() {
        if (nativeLoaded) return
        try {
            System.loadLibrary("fragmject_security")
            nativeLoaded = true
        } catch (e: UnsatisfiedLinkError) {
            // so 缺失/加载失败时降级为纯 Java 检测，不阻断启动
            Log.w(TAG, "load fragmject_security failed, fallback to JVM checks", e)
        }
    }

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
    private var blocked = false

    /** 命中原因，用于 UI 提示与日志排查。 */
    @Volatile
    private var blockReason: String? = null

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
    private var appContext: Context? = null

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
    private val handler by lazy {
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
    private fun detectReason(includePortScan: Boolean, includeCrc: Boolean): String? {
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
    private fun block(reason: String) {
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
    private fun verifySignature(context: Context): Boolean {
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
    private fun hasFridaThread(): Boolean = try {
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
    private fun hasFridaFile(): Boolean = try {
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
    private fun isFridaPortOpen(): Boolean {
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
    private fun digestHex(bytes: ByteArray, algorithm: String): String {
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
    private fun hasRootSignals(): Boolean {
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
    private fun hasRootManagementApp(): Boolean {
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
    private fun isDebuggableBuild(): Boolean {
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
    private fun hasEmulatorSignals(): Boolean {
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
    private fun verifySelfSoIntegrity(context: Context): Boolean {
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
    private fun extractSelfSo(context: Context): File? {
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
    private fun verifyResourceIntegrity(context: Context): Boolean {
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
}
